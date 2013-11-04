/* $Id: ProGuard.java,v 1.29 2002/11/03 14:29:56 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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

import proguard.classfile.ClassPool;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.obfuscate.*;
import proguard.shrink.*;

import java.io.*;


/**
 * Tool for obfuscating and shrinking Java class files.
 *
 * @author  Eric Lafortune
 * @created May 27, 2001
 */
public class ProGuard
{
    public static final String VERSION = "ProGuard, version 1.4";

    private ProGuardOptions options;
    private ClassPool       programClassPool = new ClassPool();
    private ClassPool       libraryClassPool = new ClassPool();


    /**
     * Creates a new ProGuard to process jars as specified by the given options.
     */
    public ProGuard(ProGuardOptions options)
    {
        this.options = options;
    }


    /**
     * Reads the input jars.
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
            for (int index = 0; index < options.inJars.size(); index++)
            {
                String jarFileName = (String)options.inJars.elementAt(index);
                readJar(jarFileName, false);
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
            for (int index = 0; index < options.libraryJars.size(); index++)
            {
                String jarFileName = (String)options.libraryJars.elementAt(index);
                readJar(jarFileName, true);
            }
        }
    }


    /**
     * Reads the given input jar.
     */
    private void readJar(String jarFileName, boolean isLibrary) throws IOException
    {
        System.out.println("Reading " + (isLibrary?"library":"program") +
                           " jar [" + jarFileName + "]");

        ClassPool classPool = isLibrary ?
            libraryClassPool :
            programClassPool;

        JarReader jarReader = new JarReader(jarFileName);

        try
        {
            // Let a class pool filler visit all read class files.
            // It will collect them in the class pool.
            jarReader.readZipEntries(
                new ZipEntryClassFileReader(
                new ClassPoolFiller(classPool),
                    isLibrary,
                    options.skipNonPublicLibraryClasses));

            classPool.setManifest(jarReader.getManifest());
        }
        catch (IOException ex)
        {
            System.err.println("Can't read [" + jarFileName + "]");
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
            throw new IOException("");
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
            KeepCommand command = (KeepCommand)options.keepCommands.elementAt(index);
            command.executeCheckingPhase(programClassPool, libraryClassPool, ps);
        }

        if (options.printSeeds.length() > 0)
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
            KeepCommand command = (KeepCommand)options.keepCommands.elementAt(index);
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

            if (options.printUsage.length() > 0)
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
            new MultiClassFileVisitor(new ClassFileVisitor[] {
                new ClassFileShrinker(),
                new ClassPoolFiller(newProgramClassPool)
            })));
        newProgramClassPool.setManifest(programClassPool.getManifest());
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

        // Mark element names that have to be kept.
        for (int index = 0; index < options.keepCommands.size(); index++) {
            KeepCommand command = (KeepCommand)options.keepCommands.elementAt(index);
            command.executeObfuscationPhase(programClassPool, libraryClassPool);
        }


        // Come up with new names for all class files and their class members.
        programClassPool.classFilesAccept(new ClassFileObfuscator(programClassPool,
                                                                  options.defaultPackage,
                                                                  options.useMixedCaseClassNames,
                                                                  options.overloadAggressively));
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

            if (options.printMapping.length() > 0)
            {
                ps.close();
            }
        }

        if (options.verbose)
        {
            System.out.println("Renaming program classes and class elements...");
        }

        // Actually apply these new names.
        programClassPool.classFilesAccept(new ClassFileRenamer(options.newSourceFileAttribute));

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
    private void writeJars()
    {
        if (options.verbose)
        {
            System.out.println("Writing jar...");
        }

        System.out.println("Writing output jar [" + options.outJar + "]...");

        // Create a writer for the output jar.
        JarWriter jarWriter = new JarWriter(options.outJar,
                                            programClassPool.getManifest(),
                                            ProGuard.VERSION);

        // Create a reader for all input jars (for copying any resource files).
        MultiJarReader jarReader = new MultiJarReader(options.inJars);

        try
        {
            jarWriter.open();

            // Write all Java class files.
            programClassPool.classFilesAccept(
                new ZipEntryClassFileWriter(jarWriter));

            // Copy all resource files.
            jarReader.readZipEntries(
                new ZipEntryResourceFileReader(
                new ZipEntryCopier(jarWriter)));
        }
        catch (IOException ex)
        {
            System.err.println("Can't write [" + options.outJar + "]");
        }
        finally
        {
            try
            {
                jarWriter.close();
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
    private void execute() throws IOException
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

        writeJars();

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
