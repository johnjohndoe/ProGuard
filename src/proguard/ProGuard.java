/* $Id: ProGuard.java,v 1.43 2003/05/01 18:05:53 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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

import java.io.*;
import java.util.jar.*;
import java.util.*;


/**
 * Tool for obfuscating and shrinking Java class files.
 *
 * @author Eric Lafortune
 */
public class ProGuard
{
    public static final String VERSION = "ProGuard, version 1.6";

    private ProGuardOptions options;
    private ClassPool       programClassPool = new ClassPool();
    private ClassPool       libraryClassPool = new ClassPool();
    private Manifest        manifest;


    /**
     * Creates a new ProGuard to process jars as specified by the given options.
     */
    public ProGuard(ProGuardOptions options)
    {
        this.options = options;
    }


    /**
     * Reads the input jars (or directories).
     */
    private void readJars() throws IOException
    {
        if (options.verbose)
        {
            System.out.println("Reading jars...");
        }

        // Check if we have at least some input jars.
        if (options.inJars == null)
        {
            throw new IOException("The input is empty. You have to specify one or more '-injars' options.");
        }

        // Read all program jars.
        if (options.inJars != null)
        {
            DataEntryReader reader = createDataEntryClassPoolFiller(false);

            for (int index = 0; index < options.inJars.size(); index++)
            {
                String jarFileName = (String)options.inJars.get(index);
                readJar(jarFileName, false, false, reader);
            }
        }

        // Check if we have at least some input class files.
        if (programClassPool.size() == 0)
        {
            throw new IOException("The input jars are empty. Did you specify the proper '-injars' options?");
        }

        // Read all library jars.
        if (options.libraryJars != null)
        {
            DataEntryReader reader = createDataEntryClassPoolFiller(true);

            for (int index = 0; index < options.libraryJars.size(); index++)
            {
                String jarFileName = (String)options.libraryJars.get(index);
                readJar(jarFileName, true, false, reader);
            }
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
                    new ClassPoolFiller(classPool),
                    isLibrary,
                    options.skipNonPublicLibraryClasses));
    }


    /**
     * Reads the given input jar (or directory).
     */
    private void readJar(String          jarFileName,
                         boolean         isLibrary,
                         boolean         isResource,
                         DataEntryReader reader) throws IOException
    {
        try
        {
            File jarFile = new File(jarFileName);
            boolean isDirectory = jarFile.isDirectory();

            System.out.println((isResource ? "Adding resources from " : "Reading " +
                                (isLibrary ? "library " : "program ")) +
                               (isDirectory ? "directory" : "jar") +
                               " [" + jarFileName + "]");

            // We'll have to act differently depending on whether the file is
            // actually a directory or a jar.
            if (isDirectory)
            {
                // Read the directory files recursively.
                DirectoryReader directoryReader = new DirectoryReader(jarFile);
                directoryReader.readFiles(reader);
            }
            else
            {
                // Read the ZIP entries.
                JarReader jarReader = new JarReader(jarFile);
                jarReader.readZipEntries(reader);

                // Get the program's manifest if we haven't found one yet.
                if (!isLibrary && !isResource && manifest == null)
                {
                    manifest = jarReader.getManifest();
                }
            }
        }
        catch (IOException ex)
        {
            throw new IOException("Can't read [" + jarFileName + "] (" + ex.getMessage() + ")");
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
                                              options.warn);

        programClassPool.classFilesAccept(hierarchyInitializer);

        // Then the class item references.
        ClassFileReferenceInitializer referenceInitializer =
            new ClassFileReferenceInitializer(programClassPool,
                                              options.warn,
                                              options.note);

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
            System.err.println("         You may need to specify additional library jars (using '-libraryjars').");
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
            !options.ignoreWarnings)
        {
            System.err.println("         If you are sure the mentioned classes are not used anyway,");
            System.err.println("         you could try your luck using the '-ignorewarnings' option.");
            throw new IOException("Please correct the above warnings first.");
        }

        // Discard unused library classes.
        if (options.verbose)
        {
                    System.out.println("Removing unused library classes...");
                    System.out.println("    Original number of library classes: "+libraryClassPool.size());
        }

        ClassPool newLibraryClassPool = new ClassPool();
        libraryClassPool.classFilesAccept(
            new SubclassedClassFileFilter(
            new ClassPoolFiller(newLibraryClassPool)));
        libraryClassPool = newLibraryClassPool;

        if (options.verbose)
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
        if (options.verbose)
        {
            System.out.println("Printing kept classes, fields, and methods...");
        }

        // Check if we have at least some keep commands.
        if (options.keepCommands == null)
        {
            throw new IOException("You have to specify '-keep' options for shrinking and obfuscation.");
        }

        PrintStream ps = options.printSeeds.length() > 0 ?
            new PrintStream(new BufferedOutputStream(new FileOutputStream(options.printSeeds))) :
            System.out;

        // Print out items that are used as seeds.
        for (int index = 0; index < options.keepCommands.size(); index++) {
            KeepCommand command = (KeepCommand)options.keepCommands.get(index);
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
        if (options.verbose)
        {
            System.out.println("Shrinking...");
        }

        // Check if we have at least some keep commands.
        if (options.keepCommands == null)
        {
            throw new IOException("You have to specify '-keep' options for the shrinking step.");
        }

        // Mark elements that have to be kept.
        for (int index = 0; index < options.keepCommands.size(); index++) {
            KeepCommand command = (KeepCommand)options.keepCommands.get(index);
            command.executeShrinkingPhase(programClassPool, libraryClassPool);
        }

        // Mark interfaces that have to be kept.
        programClassPool.classFilesAccept(new InterfaceUsageMarker());

        // Mark the inner class information that has to be kept.
        programClassPool.classFilesAccept(new InnerUsageMarker());

        if (options.printUsage != null)
        {
            if (options.verbose)
            {
                System.out.println("Printing usage...");
            }

          PrintStream ps = options.printUsage.length() > 0 ?
                new PrintStream(new BufferedOutputStream(new FileOutputStream(options.printUsage))) :
                System.out;

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(new UsagePrinter(true, ps));

            if (ps != System.out)
            {
                ps.close();
            }
        }

        // Discard unused program classes.
        if (options.verbose)
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
                new ClassPoolFiller(newProgramClassPool)
            })));
        programClassPool = newProgramClassPool;

        if (options.verbose)
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
        if (options.verbose)
        {
            System.out.println("Obfuscating...");
        }

        // Check if we have at least some keep commands.
        if (options.keepCommands == null)
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
        for (int index = 0; index < options.keepCommands.size(); index++) {
            KeepCommand command = (KeepCommand)options.keepCommands.get(index);
            command.executeObfuscationPhase(programClassPool, libraryClassPool);
        }


        // Come up with new names for all class files.
        programClassPool.classFilesAccept(new ClassFileObfuscator(programClassPool,
                                                                  options.defaultPackage,
                                                                  options.useMixedCaseClassNames));

        // Come up with new names for all class members.
        programClassPool.classFilesAccept(new BottomClassFileFilter(
                                          new MemberInfoObfuscator(options.overloadAggressively)));

        if (options.printMapping != null)
        {
            if (options.verbose)
            {
                System.out.println("Printing mapping...");
            }

            PrintStream ps = options.printMapping.length() > 0 ?
                new PrintStream(new BufferedOutputStream(new FileOutputStream(options.printMapping))) :
                System.out;

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(new MappingPrinter(ps));

            if (ps != System.out)
            {
                ps.close();
            }
        }

        if (options.verbose)
        {
            System.out.println("Renaming program classes and class elements...");
        }

        // Actually apply these new names.
        programClassPool.classFilesAccept(new ClassFileRenamer(options.defaultPackage != null,
                                                               options.newSourceFileAttribute));

        // Mark attributes that have to be kept and remove the other ones.
        AttributeUsageMarker attributeUsageMarker = new AttributeUsageMarker();
        if (options.keepAttributes != null)
        {
            if (options.keepAttributes != null)
            {
                attributeUsageMarker.keepAttributes(options.keepAttributes);
            }
            else
            {
                attributeUsageMarker.keepAllAttributes();
            }
        }

        programClassPool.classFilesAccept(attributeUsageMarker);
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
    private void writeJar() throws IOException
    {
        if (options.verbose)
        {
            System.out.println("Writing jar...");
        }

        // Make sure the output jar is different from the input jars.
        if (options.inJars.contains(options.outJar))
        {
            throw new IOException("The output jar [" + options.outJar + "] must be different from all input jars.");
        }

        File jarFile = new File(options.outJar);
        boolean isDirectory = jarFile.isDirectory();

        System.out.println("Writing output " +
                           (isDirectory ? "directory" : "jar") +
                           " [" + options.outJar + "]...");

        DataEntryWriter dataEntryWriter = null;

        try
        {
            // Create a different writer depending on whether the file is a
            // directory or a jar.
            dataEntryWriter = isDirectory ?
                (DataEntryWriter)new DirectoryWriter(jarFile) :
                (DataEntryWriter)new JarWriter(jarFile, manifest, ProGuard.VERSION);

            // Write all Java class files.
            programClassPool.classFilesAccept(
                new ClassFileWriter(dataEntryWriter));

            List resourceJars = options.resourceJars;
            if (resourceJars == null)
            {
                resourceJars = options.inJars;
            }

            // Read all program jars again, for copying the resource files.
            if (resourceJars != null)
            {
                // Prepare a data entry reader to filter all resource files,
                // which are then copied to the jar writer.
                DataEntryReader reader =
                    new DataEntryResourceFileFilter(
                        new DataEntryCopier(dataEntryWriter));

                for (int index = 0; index < resourceJars.size(); index++)
                {
                    String jarFileName = (String)resourceJars.get(index);
                    readJar(jarFileName, false, true, reader);
                }
            }
        }
        catch (Exception ex)
        {
            throw new IOException("Can't write [" + options.outJar + "] (" + ex.getMessage() + ")");
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
     * Prints out the contents of the program class files.
     */
    private void dump() throws IOException
    {
        if (options.verbose)
        {
            System.out.println("Printing classes...");
        }

        PrintStream ps = options.dump.length() > 0 ?
            new PrintStream(new BufferedOutputStream(new FileOutputStream(options.dump))) :
            System.out;

        programClassPool.classFilesAccept(new ClassFilePrinter(ps));

        if (options.dump.length() > 0)
        {
            ps.close();
        }
    }


    /**
     * Performs all subsequent ProGuard operations.
     */
    public void execute() throws IOException
    {
        readJars();

        if (options.shrink || options.obfuscate)
        {
            initialize();
        }

        if (options.printSeeds != null)
        {
            printSeeds();
        }

        if (options.shrink)
        {
            shrink();
        }

        if (options.obfuscate)
        {
            obfuscate();
        }

        if (options.outJar != null)
        {
            writeJar();
        }

        if (options.dump != null)
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
        ProGuardOptions options = new ProGuardOptions();

        try
        {
            // Parse the options specified in the command line arguments.
            CommandParser parser = new CommandParser(args);
            parser.parse(options);

            // Execute ProGuard with these options.
            ProGuard proGuard = new ProGuard(options);
            proGuard.execute();
        }
        catch (Exception ex)
        {
            if (options.verbose)
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
