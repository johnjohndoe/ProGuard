/* $Id: ProGuard.java,v 1.52 2003/12/06 22:12:42 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard;

import proguard.classfile.*;
import proguard.classfile.io.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.obfuscate.*;
import proguard.shrink.*;
import proguard.util.*;

import java.io.*;
import java.util.jar.*;


/**
 * Tool for obfuscating and shrinking Java class files.
 *
 * @author Eric Lafortune
 */
public class ProGuard
{
    public static final String VERSION = "ProGuard, version 2.1";

    private Configuration configuration;
    private ClassPool     programClassPool = new ClassPool();
    private ClassPool     libraryClassPool = new ClassPool();
    private Manifest      manifest;


    /**
     * Creates a new ProGuard object to process jars as specified by the given
     * configuration.
     */
    public ProGuard(Configuration configuration)
    {
        this.configuration = configuration;
    }


    /**
     * Reads the input jars (or directories).
     */
    private void readInput() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Reading jars...");
        }

        // Check if we have at least some input jars.
        if (configuration.inJars == null)
        {
            throw new IOException("The input is empty. You have to specify one or more '-injars' options.");
        }

        // Read all program jars.
        if (configuration.inJars != null)
        {
            readClassPath(configuration.inJars, false, false,
                          createDataEntryClassPoolFiller(false));
        }

        // Check if we have at least some input class files.
        if (programClassPool.size() == 0)
        {
            throw new IOException("The input jars are empty. Did you specify the proper '-injars' options?");
        }

        // Read all library jars.
        if (configuration.libraryJars != null)
        {
            readClassPath(configuration.libraryJars, true, false,
                          createDataEntryClassPoolFiller(true));
        }
    }


    /**
     * Creates a DataEntryReader that will decode class files and put them in
     * the proper class pool.
     */
    private DataEntryReader createDataEntryClassPoolFiller(boolean isLibrary)
    {
        // Get the proper class pool.
        ClassPool classPool = isLibrary ?
            libraryClassPool :
            programClassPool;

        // Prepare a data entry reader to filter all class files,
        // which are then decoded to class files by a class file reader,
        // which are then put in the class pool by a class pool filler.
        return
            new DataEntryClassFileFilter(
                new ClassFileReader(
                    new ClassPoolFiller(classPool,
                                        configuration.note),
                    isLibrary,
                    configuration.skipNonPublicLibraryClasses));
    }


    /**
     * Reads the given class path.
     */
    private void readClassPath(ClassPath       classPath,
                               boolean         isLibrary,
                               boolean         isResource,
                               DataEntryReader reader) throws IOException
    {
        for (int index = 0; index < classPath.size(); index++)
        {
            ClassPathEntry entry = (ClassPathEntry)classPath.get(index);
            readClassPathEntry(entry, isLibrary, isResource, reader);
        }
    }


    /**
     * Reads the given class path entry.
     */
    private void readClassPathEntry(ClassPathEntry  classPathEntry,
                                    boolean         isLibrary,
                                    boolean         isResource,
                                    DataEntryReader reader) throws IOException
    {
        try
        {
            File jarFile = new File(classPathEntry.getName());
            boolean isDirectory = jarFile.isDirectory();

            System.out.println((isResource ? "Adding resources from " : "Reading " +
                                (isLibrary ? "library " : "program ")) +
                               (isDirectory ? "directory" : "jar") +
                               " [" + classPathEntry.getName() + "]" +
                               (classPathEntry.getFilter() != null ? " (filtered)" : ""));

            // Filter the reader if required.
            if (classPathEntry.getFilter() != null)
            {
                reader = new FilteredDataEntryReader(
                             new FileNameListMatcher(classPathEntry.getFilter()),
                                                     reader,
                                                     null);
            }

            // Filter out any manifest files.
            DataEntryManifestFileFilter manifestFilter =
                new DataEntryManifestFileFilter(reader);

            reader = manifestFilter;

            // Create the appropriate data entry pump, depending on whether the
            // file is actually a directory or a jar.
            DataEntryPump pump = isDirectory ?
                (DataEntryPump)new DirectoryReader(jarFile) :
                (DataEntryPump)new JarReader(jarFile);

            // Pump the data entries into the reader.
            pump.pumpDataEntries(reader);

            // Get the program's manifest if we hadn't found one yet.
            if (!isLibrary && !isResource && manifest == null)
            {
                manifest = manifestFilter.getManifest();
            }
        }
        catch (IOException ex)
        {
            throw new IOException("Can't read [" + classPathEntry + "] (" + ex.getMessage() + ")");
        }
    }


    /**
     * Initializes the cross-references between all class files.
     */
    private void initialize() throws IOException
    {
        // First the class file hierarchy.
        ClassFileHierarchyInitializer hierarchyInitializer =
            new ClassFileHierarchyInitializer(programClassPool,
                                              libraryClassPool,
                                              configuration.warn);

        programClassPool.classFilesAccept(hierarchyInitializer);

        // Then the class item references.
        ClassFileReferenceInitializer referenceInitializer =
            new ClassFileReferenceInitializer(programClassPool,
                                              configuration.warn,
                                              configuration.note);

        programClassPool.classFilesAccept(referenceInitializer);

        int noteCount = referenceInitializer.getNoteCount();
        if (noteCount > 0)
        {
            System.err.println("Note: there were " + noteCount +
                               " class casts of dynamically created class instances.");
            System.err.println("      You might consider explicitly keeping the mentioned classes and/or");
            System.err.println("      their implementations (using '-keep').");
        }

        int classFileWarningCount = hierarchyInitializer.getWarningCount();
        if (classFileWarningCount > 0)
        {
            System.err.println("Warning: there were " + classFileWarningCount +
                               " unresolved references to superclasses or interfaces.");
            System.err.println("         You may need to specify additional library jars (using '-libraryjars'),");
            System.err.println("         or perhaps the '-dontskipnonpubliclibraryclasses' option.");
        }

        int memberWarningCount = referenceInitializer.getWarningCount();
        if (memberWarningCount > 0)
        {
            System.err.println("Warning: there were " + memberWarningCount +
                               " unresolved references to program class members.");
            System.err.println("         Your input class files appear to be inconsistent.");
            System.err.println("         You may need to recompile them and try again.");
        }

        if ((classFileWarningCount > 0 ||
             memberWarningCount > 0) &&
            !configuration.ignoreWarnings)
        {
            System.err.println("         If you are sure the mentioned classes are not used anyway,");
            System.err.println("         you could try your luck using the '-ignorewarnings' option.");
            throw new IOException("Please correct the above warnings first.");
        }

        // Discard unused library classes.
        if (configuration.verbose)
        {
                    System.out.println("Removing unused library classes...");
                    System.out.println("    Original number of library classes: "+libraryClassPool.size());
        }

        ClassPool newLibraryClassPool = new ClassPool();
        libraryClassPool.classFilesAccept(
            new SubclassedClassFileFilter(
            new ClassPoolFiller(newLibraryClassPool, false)));
        libraryClassPool = newLibraryClassPool;

        if (configuration.verbose)
        {
            System.out.println("    Final number of library classes:    "+libraryClassPool.size());
        }
    }


    /**
     * Prints out classes and class members that are used as seeds in the
     * shrinking and obfuscation steps.
     */
    private void printSeeds() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Printing kept classes, fields, and methods...");
        }

        // Check if we have at least some keep commands.
        if (configuration.keepClassFileOptions == null)
        {
            throw new IOException("You have to specify '-keep' options for shrinking and obfuscation.");
        }

        PrintStream ps = configuration.printSeeds.length() > 0 ?
            new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printSeeds))) :
            System.out;

        // Print out items that are used as seeds.
        for (int index = 0; index < configuration.keepClassFileOptions.size(); index++) {
            KeepCommand command = new KeepCommand((KeepClassFileOption)configuration.keepClassFileOptions.get(index));
            command.executeCheckingPhase(programClassPool, libraryClassPool, ps);
        }

        if (ps != System.out)
        {
            ps.close();
        }
    }


    /**
     * Performs the shrinking step.
     */
    private void shrink() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Shrinking...");
        }

        // Check if we have at least some keep commands.
        if (configuration.keepClassFileOptions == null)
        {
            throw new IOException("You have to specify '-keep' options for the shrinking step.");
        }

        // Mark elements that have to be kept.
        for (int index = 0; index < configuration.keepClassFileOptions.size(); index++) {
            KeepCommand command = new KeepCommand((KeepClassFileOption)configuration.keepClassFileOptions.get(index));
            command.executeShrinkingPhase(programClassPool, libraryClassPool);
        }

        // Mark interfaces that have to be kept.
        programClassPool.classFilesAccept(new InterfaceUsageMarker());

        // Mark the inner class information that has to be kept.
        programClassPool.classFilesAccept(new InnerUsageMarker());

        if (configuration.printUsage != null)
        {
            if (configuration.verbose)
            {
                System.out.println("Printing usage...");
            }

            PrintStream ps = configuration.printUsage.length() > 0 ?
                new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printUsage))) :
                System.out;

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(new UsagePrinter(true, ps));

            if (ps != System.out)
            {
                ps.close();
            }
        }

        // Discard unused program classes.
        if (configuration.verbose)
        {
            System.out.println("Removing unused program classes and class elements...");
            System.out.println("    Original number of program classes: "+programClassPool.size());
        }

        ClassPool newProgramClassPool = new ClassPool();
        programClassPool.classFilesAccept(
            new UsedClassFileFilter(
            new MultiClassFileVisitor(
            new ClassFileVisitor[] {
                new ClassFileShrinker(),
                new ClassPoolFiller(newProgramClassPool, false)
            })));
        programClassPool = newProgramClassPool;

        if (configuration.verbose)
        {
            System.out.println("    Final number of program classes:    "+programClassPool.size());
        }

        // Check if we have at least some output class files.
        if (programClassPool.size() == 0)
        {
            throw new IOException("The output jar is empty. Did you specify the proper '-keep' options?");
        }
    }


    /**
     * Performs the obfuscation step.
     */
    private void obfuscate() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Obfuscating...");
        }

        // Check if we have at least some keep commands.
        if (configuration.keepClassFileOptions == null)
        {
            throw new IOException("You have to specify '-keep' options for the obfuscation step.");
        }

        // Clean up any old visitor info.
        programClassPool.classFilesAccept(new ClassFileCleaner());
        libraryClassPool.classFilesAccept(new ClassFileCleaner());

        // Link all class members that should get the same names.
        programClassPool.classFilesAccept(new BottomClassFileFilter(
                                          new MemberInfoLinker()));

        // Mark element names that have to be kept.
        for (int index = 0; index < configuration.keepClassFileOptions.size(); index++) {
            KeepCommand command = new KeepCommand((KeepClassFileOption)configuration.keepClassFileOptions.get(index));
            command.executeObfuscationPhase(programClassPool, libraryClassPool);
        }

        // Apply a given mapping, if any.
        if (configuration.applyMapping != null)
        {
            if (configuration.verbose)
            {
                System.out.println("Applying mapping...");
            }

            MappingReader reader = new MappingReader(configuration.applyMapping);
            MappingKeeper keeper = new MappingKeeper(programClassPool);

            reader.pump(keeper);
        }

        // Mark attributes that have to be kept.
        AttributeUsageMarker attributeUsageMarker = new AttributeUsageMarker();
        if (configuration.keepAttributes != null)
        {
            if (configuration.keepAttributes.size() != 0)
            {
                attributeUsageMarker.setKeepAttributes(configuration.keepAttributes);
            }
            else
            {
                attributeUsageMarker.setKeepAllAttributes();
            }
        }
        programClassPool.classFilesAccept(attributeUsageMarker);

        if (configuration.verbose)
        {
            System.out.println("Renaming program classes and class elements...");
        }

        // Come up with new names for all class files.
        programClassPool.classFilesAccept(new ClassFileObfuscator(programClassPool,
                                                                  configuration.defaultPackage,
                                                                  configuration.useMixedCaseClassNames));

        // Come up with new names for all class members.
        programClassPool.classFilesAccept(new BottomClassFileFilter(
                                          new MemberInfoObfuscator(configuration.overloadAggressively)));

        // Print out the mapping, if requested.
        if (configuration.printMapping != null)
        {
            if (configuration.verbose)
            {
                System.out.println("Printing mapping...");
            }

            PrintStream ps = configuration.printMapping.length() > 0 ?
                new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printMapping))) :
                System.out;

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(new MappingPrinter(ps));

            if (ps != System.out)
            {
                ps.close();
            }
        }

        // Actually apply these new names.
        programClassPool.classFilesAccept(new ClassFileRenamer(configuration.defaultPackage != null,
                                                               configuration.newSourceFileAttribute));

        // Remove the attributes that can be discarded.
        programClassPool.classFilesAccept(new AttributeShrinker());

        // Mark NameAndType constant pool entries that have to be kept and remove the other ones.
        programClassPool.classFilesAccept(new NameAndTypeUsageMarker());
        programClassPool.classFilesAccept(new NameAndTypeShrinker());

        // Mark Utf8 constant pool entries that have to be kept and remove the other ones.
        programClassPool.classFilesAccept(new Utf8UsageMarker());
        programClassPool.classFilesAccept(new Utf8Shrinker());
    }


    /**
     * Writes the output jars.
     */
    private void writeOutput() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Writing jars...");
        }

        // Perform some checks on the output jars.
        for (int outIndex = 0; outIndex < configuration.outJars.size(); outIndex++)
        {
            ClassPathEntry outEntry = configuration.outJars.get(outIndex);

            // Check if all but the last output jars have filters.
            if (outIndex < configuration.outJars.size() - 1 &&
                outEntry.getFilter() == null)
            {
                throw new IOException("The output jar [" + outEntry.getName() +
                                      "] must have a filter, or all subsequent jars will be empty.");
            }

            // Check if the output jar name is different from the input jar names.
            for (int inIndex = 0; inIndex < configuration.inJars.size(); inIndex++)
            {
                ClassPathEntry inEntry = configuration.inJars.get(inIndex);

                if (outEntry.getName().equals(inEntry.getName()))
                {
                    throw new IOException("The output jar [" + outEntry.getName() +
                                          "] must be different from all input jars.");
                }
            }
        }

        // Set up the output jars.
        DataEntryWriter dataEntryWriter = createClassPathWriter(configuration.outJars);

        try
        {
            // Write all Java class files.
            programClassPool.classFilesAccept(
                new ClassFileWriter(dataEntryWriter));

            // Use the program jars as default resource jars.
            ClassPath resourceJars = configuration.resourceJars != null ?
                configuration.resourceJars :
                configuration.inJars;

            // Copy the resource files, if any.
            if (resourceJars != null)
            {
                // Prepare a data entry reader to filter all resource files,
                // which are then copied to the jar writer.
                DataEntryReader reader =
                    new DataEntryResourceFileFilter(
                        new DataEntryCopier(dataEntryWriter));

                readClassPath(resourceJars, false, true, reader);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new IOException("Can't write output jars (" + ex.getMessage() + ")");
        }
        finally
        {
            try
            {
                if (dataEntryWriter != null)
                {
                    dataEntryWriter.close();
                }
            }
            catch (IOException ex)
            {
            }
        }
    }


    /**
     * Creates a DataEntryWriter that can write to the given class path entry.
     */
    private DataEntryWriter createClassPathWriter(ClassPath classPath)
    throws IOException
    {
        DataEntryWriter writer = null;

        try
        {
            // Create a chain of writers, one for each class path entry.
            for (int index = classPath.size() - 1; index >= 0; index--)
            {
                ClassPathEntry entry = classPath.get(index);
                writer = createClassPathEntryWriter(entry, writer);
            }

            return writer;
        }
        catch (IOException ex)
        {
            // See if we can close the writers we had constructed so far,
            // even though they will not contain anything at this point.
            if (writer != null)
            {
                writer.close();
            }

            throw new IOException("Can't open output jars (" + ex.getMessage() + ")");
        }
    }


    /**
     * Creates a DataEntryWriter that can write to the given class path entry,
     * or delegate to another DataEntryWriter if its filters don't match.
     */
    private DataEntryWriter createClassPathEntryWriter(ClassPathEntry  classPathEntry,
                                                       DataEntryWriter nonMatchingWriter)
    throws IOException
    {
        File file = new File(classPathEntry.getName());
        boolean isDirectory = file.isDirectory();

        System.out.println("Writing output " +
                           (isDirectory ? "directory" : "jar") +
                           " [" + classPathEntry.getName() + "]" +
                           (classPathEntry.getFilter() != null ? " (filtered)" : ""));

        // Create a different writer depending on whether the file is a
        // directory or a jar.
        DataEntryWriter matchingWriter = isDirectory ?
            (DataEntryWriter)new DirectoryWriter(file) :
            (DataEntryWriter)new JarWriter(file,
                                           manifest,
                                           ProGuard.VERSION);

        // Filter the writer, if required.
        return classPathEntry.getFilter() != null ?
            new FilteredDataEntryWriter(
                new FileNameListMatcher(classPathEntry.getFilter()),
                                        matchingWriter,
                                        nonMatchingWriter) :
            matchingWriter;
    }


    /**
     * Prints out the contents of the program class files.
     */
    private void dump() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Printing classes...");
        }

        PrintStream ps = configuration.dump.length() > 0 ?
            new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.dump))) :
            System.out;

        programClassPool.classFilesAccept(new ClassFilePrinter(ps));

        if (configuration.dump.length() > 0)
        {
            ps.close();
        }
    }


    /**
     * Performs all subsequent ProGuard operations.
     */
    public void execute() throws IOException
    {
        readInput();

        if (configuration.shrink || configuration.obfuscate)
        {
            initialize();
        }

        if (configuration.printSeeds != null)
        {
            printSeeds();
        }

        if (configuration.shrink)
        {
            shrink();
        }

        if (configuration.obfuscate)
        {
            obfuscate();
        }

        if (configuration.outJars != null)
        {
            writeOutput();
        }

        if (configuration.dump != null)
        {
            dump();
        }
    }


    /**
     * The main method for ProGuard.
     */
    public static void main(String[] args)
    {
        System.out.println(VERSION);

        if (args.length == 0)
        {
            System.out.println("Usage: java proguard.ProGuard [options ...]");
            System.exit(1);
        }

        // Create the default options.
        Configuration configuration = new Configuration();

        try
        {
            // Parse the options specified in the command line arguments.
            ConfigurationParser parser = new ConfigurationParser(args);
            parser.parse(configuration);

            // Execute ProGuard with these options.
            ProGuard proGuard = new ProGuard(configuration);
            proGuard.execute();
        }
        catch (Exception ex)
        {
            if (configuration.verbose)
            {
                // Print a verbose stack trace.
                ex.printStackTrace();
            }
            else
            {
                // Print just the stack trace message.
                System.err.println("Error: "+ex.getMessage());
            }

            System.exit(1);
        }

        System.exit(0);
    }
}
