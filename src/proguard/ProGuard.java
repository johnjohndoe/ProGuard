/* $Id: ProGuard.java,v 1.97 2005/06/26 16:20:23 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
 *
 * Copyright (c) 2002-2005 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
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
import proguard.classfile.attribute.*;
import proguard.classfile.editor.*;
import proguard.classfile.instruction.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.io.*;
import proguard.obfuscate.*;
import proguard.optimize.*;
import proguard.optimize.evaluation.PartialEvaluator;
import proguard.optimize.peephole.*;
import proguard.shrink.*;
import proguard.util.*;

import java.io.*;
import java.util.*;


/**
 * Tool for shrinking, optimizing, and obfuscating Java class files.
 *
 * @author Eric Lafortune
 */
public class ProGuard
{
    public static final String VERSION = "ProGuard, version 3.3.1";

    private Configuration configuration;
    private ClassPool     programClassPool = new ClassPool();
    private ClassPool     libraryClassPool = new ClassPool();


    /**
     * Creates a new ProGuard object to process jars as specified by the given
     * configuration.
     */
    public ProGuard(Configuration configuration)
    {
        this.configuration = configuration;
    }


    /**
     * Performs all subsequent ProGuard operations.
     */
    public void execute() throws IOException
    {
        System.out.println(VERSION);

        GPL.check();

        readInput();

        if (configuration.shrink   ||
            configuration.optimize ||
            configuration.obfuscate)
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

        if (configuration.optimize)
        {
            optimize();

            // Shrink again, if we may.
            if (configuration.shrink)
            {
                // Don't print any usage this time around.
                configuration.printUsage       = null;
                configuration.whyAreYouKeeping = null;

                shrink();
            }
        }

        if (configuration.obfuscate)
        {
            obfuscate();
        }

        if (configuration.shrink   ||
            configuration.optimize ||
            configuration.obfuscate)
        {
            sortConstantPools();
        }

        if (configuration.programJars.hasOutput())
        {
            writeOutput();
        }

        if (configuration.dump != null)
        {
            dump();
        }
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

        // Check if we have at least some program jars.
        if (configuration.programJars == null)
        {
            throw new IOException("The input is empty. You have to specify one or more '-injars' options.");
        }

        // Read the input program jars.
        readInput("Reading program ",
                  configuration.programJars,
                  createDataEntryClassPoolFiller(false));

        // Check if we have at least some input class files.
        if (programClassPool.size() == 0)
        {
            throw new IOException("The input doesn't contain any class files. Did you specify the proper '-injars' options?");
        }

        // Read all library jars.
        if (configuration.libraryJars != null)
        {
            readInput("Reading library ",
                      configuration.libraryJars,
                      createDataEntryClassPoolFiller(true));
        }

        // The defaultPackage option implies the allowAccessModification option.
        if (configuration.defaultPackage != null)
        {
            configuration.allowAccessModification = true;
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
            new ClassFileFilter(
            new ClassFileReader(isLibrary,
                                configuration.skipNonPublicLibraryClasses,
                                configuration.skipNonPublicLibraryClassMembers,
                                configuration.note,
            new ClassPoolFiller(classPool, configuration.note)));
    }


    /**
     * Reads all input entries from the given class path.
     */
    private void readInput(String          messagePrefix,
                           ClassPath       classPath,
                           DataEntryReader reader) throws IOException
    {
        readInput(messagePrefix,
                  classPath,
                  0,
                  classPath.size(),
                  reader);
    }


    /**
     * Reads all input entries from the given section of the given class path.
     */
    private void readInput(String          messagePrefix,
                           ClassPath       classPath,
                           int             fromIndex,
                           int             toIndex,
                           DataEntryReader reader) throws IOException
    {
        for (int index = fromIndex; index < toIndex; index++)
        {
            ClassPathEntry entry = classPath.get(index);
            if (!entry.isOutput())
            {
                readInput(messagePrefix, entry, reader);
            }
        }
    }


    /**
     * Reads the given input class path entry.
     */
    private void readInput(String          messagePrefix,
                           ClassPathEntry  classPathEntry,
                           DataEntryReader dataEntryReader) throws IOException
    {
        try
        {
            // Create a reader that can unwrap jars, wars, ears, and zips.
            DataEntryReader reader =
                DataEntryReaderFactory.createDataEntryReader(messagePrefix,
                                                             classPathEntry,
                                                             dataEntryReader);

            // Create the data entry pump.
            DirectoryPump directoryPump =
                new DirectoryPump(classPathEntry.getFile());

            // Pump the data entries into the reader.
            directoryPump.pumpDataEntries(reader);
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
        if (configuration.verbose)
        {
            System.out.println("Initializing...");
        }

        int originalLibraryClassPoolSize = libraryClassPool.size();

        // Initialize the class hierarchy for program class files.
        ClassFileHierarchyInitializer classFileHierarchyInitializer =
            new ClassFileHierarchyInitializer(programClassPool,
                                              libraryClassPool,
                                              configuration.warn);

        programClassPool.classFilesAccept(classFileHierarchyInitializer);

        // Initialize the class hierarchy for library class files.
        ClassFileHierarchyInitializer classFileHierarchyInitializer2 =
            new ClassFileHierarchyInitializer(programClassPool,
                                              libraryClassPool,
                                              false);

        libraryClassPool.classFilesAccept(classFileHierarchyInitializer2);

        // Initialize the Class.forName and .class references.
        ClassFileClassForNameReferenceInitializer classFileClassForNameReferenceInitializer =
            new ClassFileClassForNameReferenceInitializer(programClassPool,
                                                          libraryClassPool,
                                                          configuration.note,
                                                          createNoteExceptionMatcher(configuration.keep));

        programClassPool.classFilesAccept(
            new AllMethodVisitor(
            new AllAttrInfoVisitor(
            new AllInstructionVisitor(classFileClassForNameReferenceInitializer))));

        // Initialize the class references from program class members and attributes.
        ClassFileReferenceInitializer classFileReferenceInitializer =
            new ClassFileReferenceInitializer(programClassPool,
                                              libraryClassPool,
                                              configuration.warn);

        programClassPool.classFilesAccept(classFileReferenceInitializer);

        // Reinitialize the library class pool with only those library classes
        // whose hierarchies are referenced by the program classes.
        ClassPool newLibraryClassPool = new ClassPool();
        programClassPool.classFilesAccept(
            new AllCpInfoVisitor(
            new ReferencedClassFileVisitor(
            new LibraryClassFileFilter(
            new ClassFileHierarchyTraveler(true, true, true, false,
            new LibraryClassFileFilter(
            new ClassPoolFiller(newLibraryClassPool, false)))))));

        libraryClassPool = newLibraryClassPool;

        // Initialize the class references from library class members.
        ClassFileReferenceInitializer classFileReferenceInitializer2 =
            new ClassFileReferenceInitializer(programClassPool,
                                              libraryClassPool,
                                              false);

        libraryClassPool.classFilesAccept(classFileReferenceInitializer2);

        int noteCount = classFileClassForNameReferenceInitializer.getNoteCount();
        if (noteCount > 0)
        {
            System.err.println("Note: there were " + noteCount +
                               " class casts of dynamically created class instances.");
            System.err.println("      You might consider explicitly keeping the mentioned classes and/or");
            System.err.println("      their implementations (using '-keep').");
        }

        int hierarchyWarningCount = classFileHierarchyInitializer.getWarningCount();
        if (hierarchyWarningCount > 0)
        {
            System.err.println("Warning: there were " + hierarchyWarningCount +
                               " unresolved references to superclasses or interfaces.");
            System.err.println("         You may need to specify additional library jars (using '-libraryjars'),");
            System.err.println("         or perhaps the '-dontskipnonpubliclibraryclasses' option.");
        }

        int referenceWarningCount = classFileReferenceInitializer.getWarningCount();
        if (referenceWarningCount > 0)
        {
            System.err.println("Warning: there were " + referenceWarningCount +
                               " unresolved references to program class members.");
            System.err.println("         Your input class files appear to be inconsistent.");
            System.err.println("         You may need to recompile them and try again.");
        }

        if ((hierarchyWarningCount > 0 ||
             referenceWarningCount > 0) &&
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
            System.out.println("  Original number of library classes: " + originalLibraryClassPoolSize);
            System.out.println("  Final number of library classes:    " + libraryClassPool.size());
        }
    }


    /**
     * Extracts a list of exceptions for which not to print notes, from the
     * keep configuration.
     */
    private ClassNameListMatcher createNoteExceptionMatcher(List noteExceptions)
    {
        if (noteExceptions != null)
        {
            List noteExceptionNames = new ArrayList(noteExceptions.size());
            for (int index = 0; index < noteExceptions.size(); index++)
            {
                ClassSpecification classSpecification = (ClassSpecification)noteExceptions.get(index);
                if (classSpecification.markClassFiles)
                {
                    // If the class itself is being kept, it's ok.
                    String className = classSpecification.className;
                    if (className != null)
                    {
                        noteExceptionNames.add(className);
                    }

                    // If all of its extensions are being kept, it's ok too.
                    String extendsClassName = classSpecification.extendsClassName;
                    if (extendsClassName != null)
                    {
                        noteExceptionNames.add(extendsClassName);
                    }
                }
            }

            if (noteExceptionNames.size() > 0)
            {
                return new ClassNameListMatcher(noteExceptionNames);
            }
        }

        return null;
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
        if (configuration.keep == null)
        {
            throw new IOException("You have to specify '-keep' options for the shrinking step.");
        }

        PrintStream ps = isFile(configuration.printSeeds) ?
            new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printSeeds))) :
            System.out;

        // Create a visitor for printing out the seeds. Note that we're only
        // printing out the program elements that are preserved against shrinking.
        SimpleClassFilePrinter printer = new SimpleClassFilePrinter(false, ps);
        ClassPoolVisitor classPoolvisitor =
            ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep,
                                                                    new ProgramClassFileFilter(printer),
                                                                    new ProgramMemberInfoFilter(printer));

        // Print out the seeds.
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);

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
        if (configuration.keep == null)
        {
            throw new IOException("You have to specify '-keep' options for the shrinking step.");
        }

        int originalProgramClassPoolSize = programClassPool.size();

        // Clean up any old visitor info.
        programClassPool.classFilesAccept(new ClassFileCleaner());
        libraryClassPool.classFilesAccept(new ClassFileCleaner());

        // Create a visitor for marking the seeds.
        UsageMarker usageMarker = configuration.whyAreYouKeeping == null ?
            new UsageMarker() :
            new ShortestUsageMarker();

        ClassPoolVisitor classPoolvisitor =
            ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep,
                                                                    usageMarker,
                                                                    usageMarker);
        // Mark the seeds.
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);

        // Mark interfaces that have to be kept.
        programClassPool.classFilesAccept(new InterfaceUsageMarker(usageMarker));

        // Mark the inner class information that has to be kept.
        programClassPool.classFilesAccept(new InnerUsageMarker(usageMarker));

        if (configuration.whyAreYouKeeping != null)
        {
            if (configuration.verbose)
            {
                System.out.println("Explaining why classes and class members are being kept...");
            }

            System.out.println();

            // Create a visitor for explaining classes and class members.
            ShortestUsagePrinter shortestUsagePrinter =
                new ShortestUsagePrinter((ShortestUsageMarker)usageMarker,
                                         configuration.verbose);

            ClassPoolVisitor whyClassPoolvisitor =
                ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.whyAreYouKeeping,
                                                                        shortestUsagePrinter,
                                                                        shortestUsagePrinter);

            // Mark the seeds.
            programClassPool.accept(whyClassPoolvisitor);
            libraryClassPool.accept(whyClassPoolvisitor);
        }

        if (configuration.printUsage != null)
        {
            if (configuration.verbose)
            {
                System.out.println("Printing usage" +
                                   (isFile(configuration.printUsage) ?
                                       " to [" + configuration.printUsage.getAbsolutePath() + "]" :
                                       "..."));
            }

            PrintStream ps = isFile(configuration.printUsage) ?
                new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printUsage))) :
                System.out;

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(
                new UsagePrinter(usageMarker, true, ps));

            if (ps != System.out)
            {
                ps.close();
            }
        }

        // Discard unused program classes.
        ClassPool newProgramClassPool = new ClassPool();
        programClassPool.classFilesAccept(
            new UsedClassFileFilter(usageMarker,
            new MultiClassFileVisitor(
            new ClassFileVisitor[] {
                new ClassFileShrinker(usageMarker, 1024),
                new ClassPoolFiller(newProgramClassPool, false)
            })));
        programClassPool = newProgramClassPool;

        if (configuration.verbose)
        {
            System.out.println("Removing unused program classes and class elements...");
            System.out.println("  Original number of program classes: " + originalProgramClassPoolSize);
            System.out.println("  Final number of program classes:    " + programClassPool.size());
        }

        // Check if we have at least some output class files.
        if (programClassPool.size() == 0)
        {
            throw new IOException("The output jar is empty. Did you specify the proper '-keep' options?");
        }
    }


    /**
     * Performs the optimization step.
     */
    private void optimize() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Optimizing...");
        }

        // Clean up any old visitor info.
        programClassPool.classFilesAccept(new ClassFileCleaner());
        libraryClassPool.classFilesAccept(new ClassFileCleaner());

        // Link all methods that should get the same optimization info.
        programClassPool.classFilesAccept(new BottomClassFileFilter(
                                          new MethodInfoLinker()));

        // Check if we have at least some keep commands.
        if (configuration.keep         == null &&
            configuration.keepNames    == null &&
            configuration.applyMapping == null &&
            configuration.printMapping == null)
        {
            throw new IOException("You have to specify '-keep' options for the optimization step.");
        }

        // Create a visitor for marking the seeds.
        KeepMarker keepMarker = new KeepMarker();
        ClassPoolVisitor classPoolvisitor =
            new MultiClassPoolVisitor(new ClassPoolVisitor[]
            {
                ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep,
                                                                        keepMarker,
                                                                        keepMarker),
                ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keepNames,
                                                                        keepMarker,
                                                                        keepMarker)
            });

        // Mark the seeds.
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);

        // Attach some optimization info to all methods, so it can be filled
        // out later.
        programClassPool.classFilesAccept(new AllMethodVisitor(
                                          new MethodOptimizationInfoSetter()));
        libraryClassPool.classFilesAccept(new AllMethodVisitor(
                                          new MethodOptimizationInfoSetter()));

        // Mark all interfaces that have single implementations.
        programClassPool.classFilesAccept(new SingleImplementationMarker(configuration.allowAccessModification));

        // Make class files and methods final, as far as possible.
        programClassPool.classFilesAccept(new ClassFileFinalizer());

        // Mark all fields that are write-only, and mark the used local variables.
        programClassPool.classFilesAccept(new AllMethodVisitor(
                                          new AllAttrInfoVisitor(
                                          new AllInstructionVisitor(
                                          new MultiInstructionVisitor(
                                          new InstructionVisitor[]
                                          {
                                              new WriteOnlyFieldMarker(),
                                              new VariableUsageMarker(),
                                          })))));

        // Mark all methods that can not be made private.
        programClassPool.classFilesAccept(new NonPrivateMethodMarker());
        libraryClassPool.classFilesAccept(new NonPrivateMethodMarker());

        // Make all final and unmarked methods private.
        programClassPool.classFilesAccept(new AllMethodVisitor(
                                          new MethodPrivatizer()));

        // Mark all used parameters, including the 'this' parameters.
        programClassPool.classFilesAccept(new AllMethodVisitor(
                                          new ParameterUsageMarker()));
        libraryClassPool.classFilesAccept(new AllMethodVisitor(
                                          new ParameterUsageMarker()));

        if (configuration.assumeNoSideEffects != null)
        {
            // Create a visitor for marking methods that don't have any side effects.
            NoSideEffectMethodMarker noSideEffectMethodMarker = new NoSideEffectMethodMarker();
            ClassPoolVisitor noClassPoolvisitor =
                ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.assumeNoSideEffects,
                                                                        null,
                                                                        noSideEffectMethodMarker);

            // Mark the seeds.
            programClassPool.accept(noClassPoolvisitor);
            libraryClassPool.accept(noClassPoolvisitor);
        }

        // Mark all methods that have side effects.
        programClassPool.accept(new SideEffectMethodMarker());

        // Perform partial evaluation.
        programClassPool.classFilesAccept(new AllMethodVisitor(
                                          new PartialEvaluator()));

        // Inline interfaces with single implementations.
        programClassPool.classFilesAccept(new SingleImplementationInliner());

        // Restore the interface references from these single implementations.
        programClassPool.classFilesAccept(new SingleImplementationFixer());

        // Shrink the method parameters and make methods static.
        programClassPool.classFilesAccept(new AllMethodVisitor(
                                          new ParameterShrinker(1024, 64)));

        // Fix all references to class files.
        programClassPool.classFilesAccept(new ClassFileReferenceFixer());

        // Fix all references to class members.
        programClassPool.classFilesAccept(new MemberReferenceFixer(1024));

        // Create a branch target marker and a code attribute editor that can
        // be reused for all code attributes.
        BranchTargetFinder branchTargetFinder = new BranchTargetFinder(1024);
        CodeAttrInfoEditor codeAttrInfoEditor = new CodeAttrInfoEditor(1024);

        // Visit all code attributes.
        // First let the branch marker mark all branch targets.
        // Then perform peephole optimisations on the instructions:
        // - Remove push/pop instruction pairs.
        // - Remove load/store instruction pairs.
        // - Replace store/load instruction pairs by dup/store instructions.
        // - Replace branches to return instructions by return instructions.
        // - Remove nop instructions.
        // - Fix invocations of methods that have become private, static,...
        // - Inline simple getters and setters.
        // Finally apply all changes to the code.
        programClassPool.classFilesAccept(
            new AllMethodVisitor(
            new AllAttrInfoVisitor(
            new MultiAttrInfoVisitor(
            new AttrInfoVisitor[]
            {
                branchTargetFinder,
                new CodeAttrInfoEditorResetter(codeAttrInfoEditor),
                new AllInstructionVisitor(
                new MultiInstructionVisitor(
                new InstructionVisitor[]
                {
                    new PushPopRemover(branchTargetFinder, codeAttrInfoEditor),
                    new LoadStoreRemover(branchTargetFinder, codeAttrInfoEditor),
                    new StoreLoadReplacer(branchTargetFinder, codeAttrInfoEditor),
                    new GotoReturnReplacer(codeAttrInfoEditor),
                    new NopRemover(codeAttrInfoEditor),
                    new MethodInvocationFixer(codeAttrInfoEditor),
                    new GetterSetterInliner(codeAttrInfoEditor, configuration.allowAccessModification),
                })),
                codeAttrInfoEditor
            }))));
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
        if (configuration.keep         == null &&
            configuration.keepNames    == null &&
            configuration.applyMapping == null &&
            configuration.printMapping == null)
        {
            throw new IOException("You have to specify '-keep' options for the obfuscation step.");
        }

        // Clean up any old visitor info.
        programClassPool.classFilesAccept(new ClassFileCleaner());
        libraryClassPool.classFilesAccept(new ClassFileCleaner());

        // Link all methods that should get the same names.
        programClassPool.classFilesAccept(new BottomClassFileFilter(
                                          new MethodInfoLinker()));

        // Create a visitor for marking the seeds.
        NameMarker nameMarker = new NameMarker();
        ClassPoolVisitor classPoolvisitor =
            new MultiClassPoolVisitor(new ClassPoolVisitor[]
            {
                ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep,
                                                                        nameMarker,
                                                                        nameMarker),
                ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keepNames,
                                                                        nameMarker,
                                                                        nameMarker)
            });

        // Mark the seeds.
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);

        // All library classes and library class members keep their names.
        libraryClassPool.classFilesAccept(nameMarker);
        libraryClassPool.classFilesAccept(new AllMemberInfoVisitor(nameMarker));

        // Apply the mapping, if one has been specified. The mapping can
        // override the names of library classes and of library class members.
        if (configuration.applyMapping != null)
        {
            if (configuration.verbose)
            {
                System.out.println("Applying mapping [" + configuration.applyMapping.getAbsolutePath() + "]");
            }

            MappingReader    reader = new MappingReader(configuration.applyMapping);
            MappingProcessor keeper =
                new MultiMappingProcessor(new MappingProcessor[]
                {
                    new MappingKeeper(programClassPool),
                    new MappingKeeper(libraryClassPool),
                });

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

        // Remove the attributes that can be discarded.
        programClassPool.classFilesAccept(new AttributeShrinker());

        if (configuration.verbose)
        {
            System.out.println("Renaming program classes and class elements...");
        }

        // Come up with new names for all class files.
        programClassPool.classFilesAccept(new ClassFileObfuscator(programClassPool,
                                                                  configuration.defaultPackage,
                                                                  configuration.useMixedCaseClassNames));

        NameFactory nameFactory = new SimpleNameFactory();

        if (configuration.obfuscationDictionary != null)
        {
            nameFactory = new DictionaryNameFactory(configuration.obfuscationDictionary, nameFactory);
        }

        Map descriptorMap = new HashMap();

        // Come up with new names for all non-private class members.
        programClassPool.classFilesAccept(
            new BottomClassFileFilter(
            new MultiClassFileVisitor(new ClassFileVisitor[]
            {
                // Collect all non-private member names in this name space.
                new ClassFileHierarchyTraveler(true, true, true, false,
                new AllMemberInfoVisitor(
                new MemberInfoAccessFilter(0, ClassConstants.INTERNAL_ACC_PRIVATE,
                new MemberInfoNameCollector(configuration.overloadAggressively,
                                            descriptorMap)))),

                // Assign new names to all non-private members in this name space.
                new ClassFileHierarchyTraveler(true, true, true, false,
                new AllMemberInfoVisitor(
                new MemberInfoAccessFilter(0, ClassConstants.INTERNAL_ACC_PRIVATE,
                new MemberInfoObfuscator(configuration.overloadAggressively,
                                         nameFactory,
                                         descriptorMap)))),

                // Clear the collected names.
                new MapCleaner(descriptorMap)
            })));

        // Come up with new names for all private class members.
        programClassPool.classFilesAccept(
            new MultiClassFileVisitor(new ClassFileVisitor[]
            {
                // Collect all member names in this class.
                new AllMemberInfoVisitor(
                new MemberInfoNameCollector(configuration.overloadAggressively,
                                            descriptorMap)),

                // Collect all non-private member names higher up the hierarchy.
                new ClassFileHierarchyTraveler(false, true, true, false,
                new AllMemberInfoVisitor(
                new MemberInfoAccessFilter(0, ClassConstants.INTERNAL_ACC_PRIVATE,
                new MemberInfoNameCollector(configuration.overloadAggressively,
                                            descriptorMap)))),

                // Assign new names to all private members in this class.
                new AllMemberInfoVisitor(
                new MemberInfoAccessFilter(ClassConstants.INTERNAL_ACC_PRIVATE, 0,
                new MemberInfoObfuscator(configuration.overloadAggressively,
                                         nameFactory,
                                         descriptorMap))),

                // Clear the collected names.
                new MapCleaner(descriptorMap)
            }));

        // Some class members may have ended up with conflicting names.
        // Collect all special member names.
        programClassPool.classFilesAccept(
            new AllMemberInfoVisitor(
            new MemberInfoSpecialNameFilter(
            new MemberInfoNameCollector(configuration.overloadAggressively,
                                        descriptorMap))));
        libraryClassPool.classFilesAccept(
            new AllMemberInfoVisitor(
            new MemberInfoSpecialNameFilter(
            new MemberInfoNameCollector(configuration.overloadAggressively,
                                        descriptorMap))));

        // Replace the conflicting member names with special, globally unique names.
        programClassPool.classFilesAccept(
            new AllMemberInfoVisitor(
            new MemberInfoNameConflictFilter(
            new MultiMemberInfoVisitor(new MemberInfoVisitor[]
            {
                new MemberInfoNameCleaner(),
                new MemberInfoObfuscator(configuration.overloadAggressively,
                                         new SpecialNameFactory(new SimpleNameFactory()),
                                         descriptorMap),
            }))));

        descriptorMap.clear();

        // Print out the mapping, if requested.
        if (configuration.printMapping != null)
        {
            if (configuration.verbose)
            {
                System.out.println("Printing mapping" +
                                   (isFile(configuration.printMapping) ?
                                       " to [" + configuration.printMapping.getAbsolutePath() + "]" :
                                       "..."));
            }

            PrintStream ps = isFile(configuration.printMapping) ?
                new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printMapping))) :
                System.out;

            // Print out items that will be removed.
            programClassPool.classFilesAcceptAlphabetically(new MappingPrinter(ps));

            if (ps != System.out)
            {
                ps.close();
            }
        }

        // Actually apply the new names.
        programClassPool.classFilesAccept(new ClassFileRenamer());
        libraryClassPool.classFilesAccept(new ClassFileRenamer());

        // Update all references to these new names.
        programClassPool.classFilesAccept(new ClassFileReferenceFixer());
        libraryClassPool.classFilesAccept(new ClassFileReferenceFixer());
        programClassPool.classFilesAccept(new MemberReferenceFixer(1024));

        // Make package visible elements public, if necessary.
        if (configuration.defaultPackage != null)
        {
            programClassPool.classFilesAccept(new ClassFileOpener());
        }

        // Rename the source file attributes, if requested.
        if (configuration.newSourceFileAttribute != null)
        {
            programClassPool.classFilesAccept(new SourceFileRenamer(configuration.newSourceFileAttribute));
        }

        // Mark NameAndType constant pool entries that have to be kept
        // and remove the other ones.
        programClassPool.classFilesAccept(new NameAndTypeUsageMarker());
        programClassPool.classFilesAccept(new NameAndTypeShrinker(1024));

        // Mark Utf8 constant pool entries that have to be kept
        // and remove the other ones.
        programClassPool.classFilesAccept(new Utf8UsageMarker());
        programClassPool.classFilesAccept(new Utf8Shrinker(1024));
    }


    /**
     * Sorts the constant pools of all program class files.
     */
    private void sortConstantPools()
    {
        // TODO: Avoid duplicate constant pool entries.
        programClassPool.classFilesAccept(new ConstantPoolSorter(1024));
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

        ClassPath programJars = configuration.programJars;

        // Perform a check on the first jar.
        ClassPathEntry firstEntry = programJars.get(0);
        if (firstEntry.isOutput())
        {
            throw new IOException("The output jar [" + firstEntry.getName() +
                                  "] must be specified after an input jar, or it will be empty.");
        }

        // Perform some checks on the output jars.
        for (int index = 0; index < programJars.size() - 1; index++)
        {
            ClassPathEntry entry = programJars.get(index);
            if (entry.isOutput())
            {
                // Check if all but the last output jars have filters.
                if (entry.getFilter()    == null &&
                    entry.getJarFilter() == null &&
                    entry.getWarFilter() == null &&
                    entry.getEarFilter() == null &&
                    entry.getZipFilter() == null &&
                    programJars.get(index + 1).isOutput())
                {
                    throw new IOException("The output jar [" + entry.getName() +
                                          "] must have a filter, or all subsequent jars will be empty.");
                }

                // Check if the output jar name is different from the input jar names.
                for (int inIndex = 0; inIndex < programJars.size(); inIndex++)
                {
                    ClassPathEntry otherEntry = programJars.get(inIndex);

                    if (!otherEntry.isOutput() &&
                        entry.getFile().equals(otherEntry.getFile()))
                    {
                        throw new IOException("The output jar [" + entry.getName() +
                                              "] must be different from all input jars.");
                    }
                }
            }
        }

        int firstInputIndex = 0;
        int lastInputIndex  = 0;

        // Go over all program class path entries.
        for (int index = 0; index < programJars.size(); index++)
        {
            // Is it an input entry?
            ClassPathEntry entry = programJars.get(index);
            if (!entry.isOutput())
            {
                // Remember the index of the last input entry.
                lastInputIndex = index;
            }
            else
            {
                // Check if this the last output entry in a series.
                int nextIndex = index + 1;
                if (nextIndex == programJars.size() ||
                    !programJars.get(nextIndex).isOutput())
                {
                    // Write the processed input entries to the output entries.
                    writeOutput(programJars,
                                firstInputIndex,
                                lastInputIndex + 1,
                                nextIndex);

                    // Start with the next series of input entries.
                    firstInputIndex = nextIndex;
                }
            }
        }
    }




    /**
     * Transfers the specified input jars to the specified output jars.
     */
    private void writeOutput(ClassPath classPath,
                             int       fromInputIndex,
                             int       fromOutputIndex,
                             int       toOutputIndex)
    throws IOException
    {
        try
        {
            // Construct the writer that can write jars, wars, ears, zips, and
            // directories, cascading over the specified output entries.
            DataEntryWriter writer =
                DataEntryWriterFactory.createDataEntryWriter(classPath,
                                                             fromOutputIndex,
                                                             toOutputIndex);

            // Create the reader that can write class files and copy resource
            // files to the above writer.
            DataEntryReader reader =
                new ClassFileFilter(new ClassFileRewriter(programClassPool,
                                                          writer),
                                    new DataEntryCopier(writer));

            // Read and handle the specified input entries.
            readInput("  Copying resources from program ",
                      classPath,
                      fromInputIndex,
                      fromOutputIndex,
                      reader);

            // Close all output entries.
            writer.close();
        }
        catch (IOException ex)
        {
            throw new IOException("Can't write [" + classPath.get(fromOutputIndex).getName() + "] (" + ex.getMessage() + ")");
        }
    }


    /**
     * Prints out the contents of the program class files.
     */
    private void dump() throws IOException
    {
        if (configuration.verbose)
        {
            System.out.println("Printing classes" +
                               (isFile(configuration.dump) ?
                                   " to [" + configuration.dump.getAbsolutePath() + "]" :
                                   "..."));
        }

        PrintStream ps = isFile(configuration.dump) ?
            new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.dump))) :
            System.out;

        programClassPool.classFilesAccept(new ClassFilePrinter(ps));

        if (isFile(configuration.dump))
        {
            ps.close();
        }
    }


    /**
     * Returns whether the given file is actually a file, or just a placeholder
     * for the standard output.
     */
    private boolean isFile(File file){
        return file.getPath().length() > 0;
    }


    /**
     * The main method for ProGuard.
     */
    public static void main(String[] args)
    {
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

            try
            {
                parser.parse(configuration);

                // Execute ProGuard with these options.
                ProGuard proGuard = new ProGuard(configuration);
                proGuard.execute();
            }
            finally
            {
                parser.close();
            }
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
