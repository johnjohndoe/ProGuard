/* $Id: ProGuard.java,v 1.20 2002/08/02 16:40:28 eric Exp $
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

import proguard.classfile.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.shrink.*;
import proguard.obfuscate.*;

import java.io.*;
import java.util.*;


/**
 * Tool for obfuscating and shrinking Java class files.
 *
 * @author  Eric Lafortune
 * @created May 27, 2001
 */
public class ProGuard
{
    public static final String VERSION = "ProGuard, version 1.2";

    private CompoundCommand commands         = new CompoundCommand();
    private ProGuardOptions options          = new ProGuardOptions();
    private ClassPool       programClassPool = new ClassPool();
    private ClassPool       libraryClassPool = new ClassPool();


    /**
     * Reads the given configuration file.
     */
    private void readCommands(String[] args) throws IOException
    {
        CommandParser parser = new CommandParser(options, args);

        try
        {
            while (true)
            {
                Command command = parser.nextCommand();
                if (command == null)
                {
                    break;
                }

                commands.addCommand(command);
            }
        }
        catch (ParseException ex)
        {
            throw new IOException(ex.getMessage());
        }
    }


    /**
     * Performs the initialization phase.
     */
    private void initialize()
    {
        if (options.verbose)
        {
            System.out.println("Loading jars...");
        }

        // Load all library jars and program jars.
        executeCommands(Command.PHASE_INITIALIZE);

        // Initialize the cross-references between all class files.

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
            System.err.println("      their implementations.");
        }

        int classFileWarningCount = hierarchyInitializer.getWarningCount();
        if (classFileWarningCount > 0)
        {
            System.err.println("Warning: there were " + classFileWarningCount +
                               " unresolved references to superclasses or interfaces");
            System.err.println("         You may need to specify additional library jars.");
        }

        int memberWarningCount = referenceInitializer.getWarningCount();
        if (memberWarningCount > 0)
        {
            System.err.println("Warning: there were " + memberWarningCount +
                               " unresolved references to program class members");
            System.err.println("         Your input class files appear to be inconsistent.");
            System.err.println("         You may need to recompile them and try again.");
        }

        if ((classFileWarningCount > 0 ||
             memberWarningCount > 0) &&
            !options.ignoreWarnings)
        {
            System.err.println("         If you are sure the mentioned classes are not used anyway,");
            System.err.println("         you could try your luck using the '-ignorewarnings' option.");
            System.exit(-1);
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
     * Performs the checking phase.
     */
    private void check()
    {
        System.out.println("=== Start of kept classes, fields, and methods ===");
        executeCommands(Command.PHASE_CHECK);
        System.out.println("=== End of kept classes, fields, and methods ===");
    }


    /**
     * Performs the shrinking phase.
     */
    private void shrink()
    {
        if (options.verbose)
        {
            System.out.println("Shrinking...");
        }

        // Mark elements that have to be kept.
        executeCommands(Command.PHASE_SHRINK);

        // Mark interfaces that have to be kept.
        programClassPool.classFilesAccept(new InterfaceUsageMarker());

        // Mark the inner class information that has to be kept.
        programClassPool.classFilesAccept(new InnerUsageMarker());

        if (options.printUsage)
        {
            System.out.println("=== Start of unused classes, fields, and methods ===");

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(new UsagePrinter(true));

            System.out.println("=== End of unused classes, fields, and methods ===");
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

        if (programClassPool.size() == 0)
        {
            System.err.println("Warning: the final jar is empty. Did you specify the proper -keep options?");
            System.exit(-1);
        }
    }


    /**
     * Performs the obfuscation phase.
     */
    private void obfuscate()
    {
        if (options.verbose)
        {
            System.out.println("Obfuscating...");
        }

        // Cleans up any old visitor info.
        programClassPool.classFilesAccept(new ClassFileCleaner());

        // Mark element names that have to be kept.
        executeCommands(Command.PHASE_OBFUSCATE);

        // Come up with new names for all class files and their class members.
        programClassPool.classFilesAccept(new ClassFileObfuscator(programClassPool,
                                                                  options.defaultPackage,
                                                                  options.overloadAggressively));
        if (options.printMapping)
        {
            System.out.println("=== Start of remapped classes, fields, and methods ===");

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(new MappingPrinter());

            System.out.println("=== End of remapped classes, fields, and methods ===");
        }

        if (options.verbose)
        {
            System.out.println("Renaming program classes and class elements...");
        }

        // Actually apply these new names.
        programClassPool.classFilesAccept(new ClassFileRenamer());

        // Mark attributes that have to be kept and remove the other ones.
        AttributeUsageMarker attributeUsageMarker = new AttributeUsageMarker();
        if (options.keepAttributes != null)
        {
            if (options.keepAttributes.length > 0)
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
     * Performs the writing phase.
     */
    private void write()
    {
        if (options.verbose)
        {
            System.out.println("Writing jars...");
        }

        // Mark element names that have to be kept.
        executeCommands(Command.PHASE_WRITE);
    }


    /**
     * Prints out the contents of the program class files.
     */
    private void dump()
    {
        if (options.verbose)
        {
            System.out.println("Printing classes...");
        }

        programClassPool.classFilesAccept(new ClassFilePrinter());
    }


    /**
     * Executes all parsed commands, for a given phase.
     */
    private void executeCommands(int phase)
    {
        commands.execute(phase, programClassPool, libraryClassPool);
    }


    /**
     * Performs all subsequent ProGuard operations.
     */
    private void execute(String[] args)
    {
        try
        {
            readCommands(args);

            initialize();

            if (options.printSeeds)
            {
                check();
            }

            if (options.shrink)
            {
                shrink();
            }

            if (options.obfuscate)
            {
                obfuscate();
            }

            write();

            if (options.dump)
            {
                dump();
            }
        }
        catch (Exception ex)
        {
            if (options.verbose)
            {
                ex.printStackTrace();
            }
            else
            {
                System.err.println(ex.getMessage());
            }
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
        else
        {
            ProGuard proGuard = new ProGuard();
            proGuard.execute(args);
            System.exit(0);
        }
    }
}
