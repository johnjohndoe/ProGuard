/* $Id: CommandParser.java,v 1.35 2003/03/10 19:46:58 eric Exp $
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
import proguard.classfile.util.*;

import java.io.*;
import java.util.*;


/**
 * This class parses ProGuard commands. Commands can be read from an array of
 * arguments and/or from command files.
 *
 * @author Eric Lafortune
 */
public class CommandParser
{
    private static final String OPTION_PREFIX                               = "-";
    private static final String AT_DIRECTIVE                                = "@";
    private static final String INCLUDE_DIRECTIVE                           = "-include";
    private static final String LIBRARYJARS_OPTION                          = "-libraryjars";
    private static final String INJARS_OPTION                               = "-injars";
    private static final String RESOURCEJARS_OPTION                         = "-resourcejars";
    private static final String OUTJAR_OPTION                               = "-outjar";
    private static final String KEEP_OPTION                                 = "-keep";
    private static final String KEEP_CLASS_MEMBERS_OPTION                   = "-keepclassmembers";
    private static final String KEEP_CLASSES_WITH_MEMBERS_OPTION            = "-keepclasseswithmembers";
    private static final String KEEP_NAMES_OPTION                           = "-keepnames";
    private static final String KEEP_CLASS_MEMBER_NAMES_OPTION              = "-keepclassmembernames";
    private static final String KEEP_CLASSES_WITH_MEMBER_NAMES_OPTION       = "-keepclasseswithmembernames";
    private static final String KEEP_ATTRIBUTES_OPTION                      = "-keepattributes";
    private static final String RENAME_SOURCE_FILE_ATTRIBUTE_OPTION         = "-renamesourcefileattribute";
    private static final String PRINT_SEEDS_OPTION                          = "-printseeds";
    private static final String PRINT_USAGE_OPTION                          = "-printusage";
    private static final String PRINT_MAPPING_OPTION                        = "-printmapping";
    private static final String VERBOSE_OPTION                              = "-verbose";
    private static final String DUMP_OPTION                                 = "-dump";
    private static final String IGNORE_WARNINGS_OPTION                      = "-ignorewarnings";
    private static final String DONT_WARN_OPTION                            = "-dontwarn";
    private static final String DONT_NOTE_OPTION                            = "-dontnote";
    private static final String DONT_SHRINK_OPTION                          = "-dontshrink";
    private static final String DONT_OBFUSCATE_OPTION                       = "-dontobfuscate";
    private static final String DONT_USE_MIXED_CASE_CLASS_NAMES_OPTION      = "-dontusemixedcaseclassnames";
    private static final String OVERLOAD_AGGRESSIVELY_OPTION                = "-overloadaggressively";
    private static final String DEFAULT_PACKAGE_OPTION                      = "-defaultpackage";
    private static final String DONT_SKIP_NON_PUBLIC_LIBRARY_CLASSES_OPTION = "-dontskipnonpubliclibraryclasses";

    private static final String ANY_ATTRIBUTE_KEYWORD       = "*";
    private static final String ATTRIBUTE_SEPARATOR_KEYWORD = ",";

    private static final String JAR_SEPARATOR_KEY       = "path.separator";
    private static final String JAR_SEPARATOR_KEYWORD   = System.getProperty(JAR_SEPARATOR_KEY);

    private static final char   OPEN_SYSTEM_PROPERTY    = '<';
    private static final char   CLOSE_SYSTEM_PROPERTY   = '>';

    private static final String NEGATOR_KEYWORD         = "!";
    private static final String CLASS_KEYWORD           = "class";
    private static final String ANY_CLASS_KEYWORD       = "*";
    private static final String IMPLEMENTS_KEYWORD      = "implements";
    private static final String EXTENDS_KEYWORD         = "extends";
    private static final String AS_KEYWORD              = "as";
    private static final String OPEN_KEYWORD            = "{";
    private static final String ANY_CLASS_MEMBER_KEYWORD  = "*";
    private static final String ANY_FIELD_KEYWORD       = "<fields>";
    private static final String ANY_METHOD_KEYWORD      = "<methods>";
    private static final String OPEN_ARGUMENTS_KEYWORD  = "(";
    private static final String ARGUMENT_SEPARATOR_KEYWORD = ",";
    private static final String CLOSE_ARGUMENTS_KEYWORD = ")";
    private static final String SEPARATOR_KEYWORD       = ";";
    private static final String CLOSE_KEYWORD           = "}";


    private WordReader reader;
    private String     nextWord;


    /**
     * Creates a new CommandParser for the given String arguments.
     */
    public CommandParser(String[] args) throws IOException
    {
        reader = new ArgumentWordReader(args);
        readNextWord();
    }


    /**
     * Creates a new CommandParser for the given file name.
     */
    public CommandParser(String commandFile) throws IOException
    {
        reader = new FileWordReader(commandFile);
        readNextWord();
    }


    /**
     * Parses and returns the options.
     * @param options the options that are updated as a side-effect.
     * @throws ParseException if the any of the commands contains a syntax
     *                        error.
     * @throws IOException if an IO error occurs reading a command file.
     */
    public void parse(ProGuardOptions options) throws ParseException, IOException
    {
        while (nextWord != null)
        {
            // First include directives.
            if      (AT_DIRECTIVE                    .startsWith(nextWord) ||
                     INCLUDE_DIRECTIVE               .startsWith(nextWord)) parseIncludeArgument();

            // Then options with or without arguments.
            else if (LIBRARYJARS_OPTION                         .startsWith(nextWord)) options.libraryJars                 = parseInJarsArgument(options.libraryJars);
            else if (INJARS_OPTION                              .startsWith(nextWord)) options.inJars                      = parseInJarsArgument(options.inJars);
            else if (RESOURCEJARS_OPTION                        .startsWith(nextWord)) options.resourceJars                = parseInJarsArgument(options.resourceJars);
            else if (OUTJAR_OPTION                              .startsWith(nextWord)) options.outJar                      = parseOutJarArgument();
            else if (KEEP_OPTION                                .startsWith(nextWord)) options.keepCommands                = parseKeepArguments(options.keepCommands, true,  false, false);
            else if (KEEP_CLASS_MEMBERS_OPTION                  .startsWith(nextWord)) options.keepCommands                = parseKeepArguments(options.keepCommands, false, false, false);
            else if (KEEP_CLASSES_WITH_MEMBERS_OPTION           .startsWith(nextWord)) options.keepCommands                = parseKeepArguments(options.keepCommands, false, true,  false);
            else if (KEEP_NAMES_OPTION                          .startsWith(nextWord)) options.keepCommands                = parseKeepArguments(options.keepCommands, true,  false, true);
            else if (KEEP_CLASS_MEMBER_NAMES_OPTION             .startsWith(nextWord)) options.keepCommands                = parseKeepArguments(options.keepCommands, false, false, true);
            else if (KEEP_CLASSES_WITH_MEMBER_NAMES_OPTION      .startsWith(nextWord)) options.keepCommands                = parseKeepArguments(options.keepCommands, false, true,  true);
            else if (KEEP_ATTRIBUTES_OPTION                     .startsWith(nextWord)) options.keepAttributes              = parseKeepAttributesArguments(options.keepAttributes);
            else if (RENAME_SOURCE_FILE_ATTRIBUTE_OPTION        .startsWith(nextWord)) options.newSourceFileAttribute      = parseOptionalArgument();
            else if (PRINT_SEEDS_OPTION                         .startsWith(nextWord)) options.printSeeds                  = parseOptionalArgument();
            else if (PRINT_USAGE_OPTION                         .startsWith(nextWord)) options.printUsage                  = parseOptionalArgument();
            else if (PRINT_MAPPING_OPTION                       .startsWith(nextWord)) options.printMapping                = parseOptionalArgument();
            else if (VERBOSE_OPTION                             .startsWith(nextWord)) options.verbose                     = parseNoArgument(true);
            else if (DUMP_OPTION                                .startsWith(nextWord)) options.dump                        = parseOptionalArgument();
            else if (IGNORE_WARNINGS_OPTION                     .startsWith(nextWord)) options.ignoreWarnings              = parseNoArgument(true);
            else if (DONT_WARN_OPTION                           .startsWith(nextWord)) options.warn                        = parseNoArgument(false);
            else if (DONT_NOTE_OPTION                           .startsWith(nextWord)) options.note                        = parseNoArgument(false);
            else if (DONT_SHRINK_OPTION                         .startsWith(nextWord)) options.shrink                      = parseNoArgument(false);
            else if (DONT_OBFUSCATE_OPTION                      .startsWith(nextWord)) options.obfuscate                   = parseNoArgument(false);
            else if (DONT_USE_MIXED_CASE_CLASS_NAMES_OPTION     .startsWith(nextWord)) options.useMixedCaseClassNames      = parseNoArgument(false);
            else if (OVERLOAD_AGGRESSIVELY_OPTION               .startsWith(nextWord)) options.overloadAggressively        = parseNoArgument(true);
            else if (DEFAULT_PACKAGE_OPTION                     .startsWith(nextWord)) options.defaultPackage              = ClassUtil.internalClassName(parseOptionalArgument());
            else if (DONT_SKIP_NON_PUBLIC_LIBRARY_CLASSES_OPTION.startsWith(nextWord)) options.skipNonPublicLibraryClasses = parseNoArgument(false);
            else
            {
                throw new ParseException("Unknown command " + reader.locationDescription());
            }
        }
    }


    private void parseIncludeArgument()
    throws ParseException, IOException
    {
        // Read the configuation file name.
        readFileName("configuration file name");

        reader.includeWordReader(new FileWordReader(nextWord));

        readNextWord();
    }


    private List parseInJarsArgument(List inJars)
    throws ParseException, IOException
    {
        // Create a new List if necessary.
        if (inJars == null)
        {
            inJars = new ArrayList();
        }

        while (true)
        {
            // Read the next jar name.
            readFileName("input jar name");

            inJars.add(nextWord);

            // Read the separator, if any.
            readNextWord();
            if (commandEnd())
            {
                return inJars;
            }

            if (!nextWord.equals(JAR_SEPARATOR_KEYWORD))
            {
                throw new ParseException("Expecting jar name separator '" + JAR_SEPARATOR_KEYWORD +
                                         "' before " + reader.locationDescription());
            }
        }
    }


    private String parseOutJarArgument()
    throws ParseException, IOException
    {
        // Read the jar name.
        readFileName("output jar name");

        String fileName = nextWord;

        readNextWord();

        return fileName;
    }


    private List parseKeepAttributesArguments(List keepAttributes)
    throws ParseException, IOException
    {
        // Create a new List if necessary.
        if (keepAttributes == null)
        {
            keepAttributes = new ArrayList();
        }

        // Read the first attribute name.
        readNextWord();

        // Should we keep all attributes?
        if (commandEnd())
        {
            keepAttributes.clear();
            return keepAttributes;
        }

        if (nextWord.equals(ANY_ATTRIBUTE_KEYWORD))
        {
            keepAttributes.clear();
            readNextWord();
            return keepAttributes;
        }

        while (true)
        {
            // Add the attribute name to the list.
            keepAttributes.add(nextWord);

            // Read the separator, if any.
            readNextWord();
            if (commandEnd())
            {
                break;
            }

            if (!nextWord.equals(ATTRIBUTE_SEPARATOR_KEYWORD))
            {
                throw new ParseException("Expecting attribute name separator '" + ATTRIBUTE_SEPARATOR_KEYWORD +
                                         "' before " + reader.locationDescription());
            }

            // Read the next attribute name.
            readNextWord("attribute name");
        }

        return keepAttributes;
    }


    private String parseOptionalArgument()
    throws ParseException, IOException
    {
        readNextWord();

        // Didn't the user specify a file name?
        if (commandEnd())
        {
            return "";
        }

        String fileName = nextWord;

        readNextWord();

        return fileName;
    }


    private boolean parseNoArgument(boolean value)
    throws ParseException, IOException
    {
        readNextWord();

        return value;
    }


    private List parseKeepArguments(List    keepCommands,
                                      boolean markClassFiles,
                                      boolean markClassFilesConditionally,
                                      boolean onlyKeepNames)
    throws ParseException, IOException
    {
        // Create a new List if necessary.
        if (keepCommands == null)
        {
            keepCommands = new ArrayList();
        }

        // Read and add the keep command.
        keepCommands.add(parseKeepArguments(markClassFiles,
                                                 markClassFilesConditionally,
                                                 onlyKeepNames));

        return keepCommands;
    }


    private KeepCommand parseKeepArguments(boolean markClassFiles,
                                           boolean markClassFilesConditionally,
                                           boolean onlyKeepNames)
    throws ParseException, IOException
    {
        // Parse the class flag specification part, if any.
        int requiredSetClassAccessFlags   = 0;
        int requiredUnsetClassAccessFlags = 0;

        while (true)
        {
            readNextWord("keyword '" + CLASS_KEYWORD + "'" +
                         " or '" + ClassConstants.EXTERNAL_ACC_INTERFACE + "'");

            if (CLASS_KEYWORD.equals(nextWord))
            {
                // The class keyword. Stop parsing the class flags.
                break;
            }

            // Strip the negating sign, if any.
            String strippedWord = nextWord.startsWith(NEGATOR_KEYWORD) ?
                nextWord.substring(1) :
                nextWord;

            int accessFlag =
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_PUBLIC)    ? ClassConstants.INTERNAL_ACC_PUBLIC    :
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_FINAL)     ? ClassConstants.INTERNAL_ACC_FINAL     :
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_INTERFACE) ? ClassConstants.INTERNAL_ACC_INTERFACE :
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_ABSTRACT)  ? ClassConstants.INTERNAL_ACC_ABSTRACT  :
                                                                             unknownAccessFlag();
            if (strippedWord == nextWord)
            {
                requiredSetClassAccessFlags   |= accessFlag;
            }
            else
            {
                requiredUnsetClassAccessFlags |= accessFlag;
            }


            if ((requiredSetClassAccessFlags &
                 requiredUnsetClassAccessFlags) != 0)
            {
                throw new ParseException("Conflicting class access modifiers for '" + strippedWord +
                                         "' before " + reader.locationDescription());
            }

            if (ClassConstants.EXTERNAL_ACC_INTERFACE.equals(strippedWord))
            {
                accessFlag = ClassConstants.INTERNAL_ACC_INTERFACE;

                // The interface keyword. Stop parsing the class flags.
                break;
            }
        }

        readNextWord("class name or interface name");
        checkJavaIdentifier("class name or interface name");

        // Parse the class name part.
        String externalClassName = nextWord;
        String className = ANY_CLASS_KEYWORD.equals(externalClassName) ?
            null :
            ClassUtil.internalClassName(externalClassName);

        readNextWord();

        String extendsClassName = null;
        String asClassName      = null;

        if (!commandEnd())
        {
            // Parse 'implements ...' or 'extends ...' part, if any.
            if (IMPLEMENTS_KEYWORD.equals(nextWord) ||
                EXTENDS_KEYWORD.equals(nextWord))
            {
                readNextWord("class name or interface name");
                checkJavaIdentifier("class name or interface name");
                extendsClassName = ClassUtil.internalClassName(nextWord);

                readNextWord();
            }
        }

        if (!commandEnd())
        {
            // Parse the 'as ...' part, if any.
            if (AS_KEYWORD.equals(nextWord))
            {
                readNextWord("new class name");
                checkJavaIdentifier("new class name");
                asClassName = ClassUtil.internalClassName(nextWord);

                readNextWord();
            }
        }

        // Create the essential KeepCommand, for keeping the class or classes.
        KeepCommand keepCommand = new KeepCommand(requiredSetClassAccessFlags,
                                                  requiredUnsetClassAccessFlags,
                                                  className,
                                                  extendsClassName,
                                                  asClassName,
                                                  markClassFiles,
                                                  markClassFilesConditionally,
                                                  onlyKeepNames);


        // Now modify this KeepCommand, for keeping any class members.
        // Parse the class member opening part, if any.
        if (!commandEnd())
        {
            if (!OPEN_KEYWORD.equals(nextWord))
            {
                throw new ParseException("Expecting opening '" + OPEN_KEYWORD +
                                         "' at " + reader.locationDescription());
            }

            while (true)
            {
                // Parse the class member flag specification part, if any.
                int requiredSetMemberAccessFlags   = 0;
                int requiredUnsetMemberAccessFlags = 0;

                while (true)
                {
                    readNextWord("class member description" +
                                 " or closing '" + CLOSE_KEYWORD + "'");

                    if (requiredSetMemberAccessFlags   == 0 &&
                        requiredUnsetMemberAccessFlags == 0 &&
                        CLOSE_KEYWORD.equals(nextWord))
                    {
                        // The closing brace. Stop parsing the class members.
                        readNextWord();
                        return keepCommand;
                    }

                    String strippedWord = nextWord.startsWith("!") ?
                        nextWord.substring(1) :
                        nextWord;

                    int accessFlag =
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_PUBLIC)       ? ClassConstants.INTERNAL_ACC_PUBLIC       :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_PRIVATE)      ? ClassConstants.INTERNAL_ACC_PRIVATE      :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_PROTECTED)    ? ClassConstants.INTERNAL_ACC_PROTECTED    :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_STATIC)       ? ClassConstants.INTERNAL_ACC_STATIC       :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_FINAL)        ? ClassConstants.INTERNAL_ACC_FINAL        :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_SYNCHRONIZED) ? ClassConstants.INTERNAL_ACC_SYNCHRONIZED :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_VOLATILE)     ? ClassConstants.INTERNAL_ACC_VOLATILE     :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_TRANSIENT)    ? ClassConstants.INTERNAL_ACC_TRANSIENT    :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_NATIVE)       ? ClassConstants.INTERNAL_ACC_NATIVE       :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_ABSTRACT)     ? ClassConstants.INTERNAL_ACC_ABSTRACT     :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_STRICT)       ? ClassConstants.INTERNAL_ACC_STRICT       :
                                                                                        0;
                    if (accessFlag == 0)
                    {
                        // Not a class member flag. Stop parsing them.
                        break;
                    }

                    if (strippedWord == nextWord)
                    {
                        requiredSetMemberAccessFlags   |= accessFlag;
                    }
                    else
                    {
                        requiredUnsetMemberAccessFlags |= accessFlag;
                    }

                    // Make sure the user doesn't try to set and unset the same
                    // access flags simultaneously.
                    if ((requiredSetMemberAccessFlags &
                         requiredUnsetMemberAccessFlags) != 0)
                    {
                        throw new ParseException("Conflicting class member access modifiers for " +
                                                 reader.locationDescription());
                    }
                }

                // Parse the class member type and name part.

                // Did we get a special wildcard?
                if (ANY_CLASS_MEMBER_KEYWORD.equals(nextWord) ||
                    ANY_FIELD_KEYWORD       .equals(nextWord) ||
                    ANY_METHOD_KEYWORD      .equals(nextWord))
                {
                    // Act acording to the type of wildcard..
                    if (ANY_CLASS_MEMBER_KEYWORD.equals(nextWord))
                    {
                        checkFieldAccessFlags(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags);
                        checkMethodAccessFlags(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags);

                        keepCommand.keepField(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags,
                                              null,
                                              null,
                                              null);
                        keepCommand.keepMethod(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags,
                                               null,
                                               null,
                                               null);
                    }
                    else if (ANY_FIELD_KEYWORD.equals(nextWord))
                    {
                        checkFieldAccessFlags(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags);

                        keepCommand.keepField(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags,
                                              null,
                                              null,
                                              null);
                    }
                    else if (ANY_METHOD_KEYWORD.equals(nextWord))
                    {
                        checkMethodAccessFlags(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags);

                        keepCommand.keepMethod(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags,
                                               null,
                                               null,
                                               null);
                    }

                    // We still have to read the closing separator.
                    readNextWord("separator '" + SEPARATOR_KEYWORD + "'");

                    if (!SEPARATOR_KEYWORD.equals(nextWord))
                    {
                        throw new ParseException("Expecting separator '" + SEPARATOR_KEYWORD +
                                                 "' before " + reader.locationDescription());
                    }
                }
                else
                {
                    // Make sure we have a proper type.
                    checkJavaIdentifier("class member type");
                    String type = nextWord;

                    readNextWord("class member name");
                    String name = nextWord;

                    // Did we get just one word before the opening parenthesis?
                    if (OPEN_ARGUMENTS_KEYWORD.equals(name))
                    {
                        // This must be a constructor then.
                        // Make sure the type is a proper constructor name.
                        if (!(type.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT) ||
                              type.equals(externalClassName) ||
                              type.equals(ClassUtil.externalShortClassName(externalClassName))))
                        {
                            throw new ParseException("Expecting type and name " +
                                                     "instead of just '" + type +
                                                     "' before " + reader.locationDescription());
                        }

                        // Assign the fixed constructor type and name.
                        type = ClassConstants.EXTERNAL_TYPE_VOID;
                        name = ClassConstants.INTERNAL_METHOD_NAME_INIT;
                    }
                    else
                    {
                        // It's not a constructor.
                        // Make sure we have a proper name.
                        checkJavaIdentifier("class member name");

                        // Read the opening parenthesis or the separating
                        // semi-colon.
                        readNextWord("opening '" + OPEN_ARGUMENTS_KEYWORD +
                                     "' or separator '" + SEPARATOR_KEYWORD + "'");
                    }

                    // Are we looking at a field, a method, or something else?
                    if (SEPARATOR_KEYWORD.equals(nextWord))
                    {
                        // It's a field.
                        checkFieldAccessFlags(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags);

                        // We already have a field descriptor.
                        String descriptor = ClassUtil.internalType(type);

                        // Keep the field.
                        keepCommand.keepField(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags,
                                              name,
                                              descriptor,
                                              null);
                    }
                    else if (OPEN_ARGUMENTS_KEYWORD.equals(nextWord))
                    {
                        // It's a method.
                        checkMethodAccessFlags(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags);

                        // Parse the method arguments.
                        String descriptor =
                            ClassUtil.internalMethodDescriptor(type,
                                                               parseArguments());

                        // Keep the method.
                        keepCommand.keepMethod(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags,
                                               name,
                                               descriptor,
                                               null);
                    }
                    else
                    {
                        // It doesn't look like a field or a method.
                        throw new ParseException("Expecting opening '" + OPEN_ARGUMENTS_KEYWORD +
                                                 "' or separator '" + SEPARATOR_KEYWORD +
                                                 "' before " + reader.locationDescription());
                    }
                }
            }
        }

        return keepCommand;
    }


    /**
     * Reads method arguments, starting after the opening parenthesis and ending
     * after the closing parenthesis and the separator.
     */
    private List parseArguments()
    throws ParseException, IOException
    {
        List arguments = new ArrayList();

        while (true)
        {
            readNextWord("argument type or closing '" + CLOSE_ARGUMENTS_KEYWORD + "'");

            if (arguments.size() == 0 &&
                CLOSE_ARGUMENTS_KEYWORD.equals(nextWord))
            {
                break;
            }

            checkJavaIdentifier("argument type");
            arguments.add(nextWord);

            readNextWord("separating '" + ARGUMENT_SEPARATOR_KEYWORD + "'" +
                         " or closing '" + CLOSE_ARGUMENTS_KEYWORD + "'");;

            if (CLOSE_ARGUMENTS_KEYWORD.equals(nextWord))
            {
                break;
            }

            if (!ARGUMENT_SEPARATOR_KEYWORD.equals(nextWord))
            {
                throw new ParseException("Expecting separating '" + ARGUMENT_SEPARATOR_KEYWORD +
                                         "' or closing '" + CLOSE_ARGUMENTS_KEYWORD +
                                         "' before " + reader.locationDescription());
            }
        }

        // Read the separator after the closing parenthesis.
        readNextWord("separator '" + SEPARATOR_KEYWORD + "'");

        if (!SEPARATOR_KEYWORD.equals(nextWord))
        {
            throw new ParseException("Expecting separator '" + SEPARATOR_KEYWORD +
                                     "' before " + reader.locationDescription());
        }

        return arguments;
    }


    private int unknownAccessFlag() throws ParseException
    {
        throw new ParseException("Unexpected keyword " + reader.locationDescription());
    }


    /**
     * Reads the next word as a file name, replacing any system properties
     * (e.g. "<java.home>") by their values.
     */
    private void readFileName(String expectedDescription)
    throws ParseException, IOException
    {
        readNextWord(expectedDescription);

        int fromIndex = 0;
        while (true)
        {
            fromIndex = nextWord.indexOf(OPEN_SYSTEM_PROPERTY, fromIndex);
            if (fromIndex < 0)
            {
                break;
            }

            int toIndex = nextWord.indexOf(CLOSE_SYSTEM_PROPERTY, fromIndex+1);
            if (toIndex < 0)
            {
                throw new ParseException("Expecting closing '" + CLOSE_SYSTEM_PROPERTY +
                                         "' after opening '" + OPEN_SYSTEM_PROPERTY +
                                         "' in " + reader.locationDescription());
            }

            String propertyName  = nextWord.substring(fromIndex+1, toIndex);
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue == null)
            {
                throw new ParseException("Value of system property '" + propertyName +
                                         "' is undefined in " + reader.locationDescription());
            }

            nextWord = nextWord.substring(0, fromIndex) +
                       propertyValue +
                       nextWord.substring(toIndex+1);

            fromIndex = toIndex+1;
        }
    }


    private void readNextWord(String expectedDescription)
    throws ParseException, IOException
    {
        readNextWord();
        if (commandEnd())
        {
            throw new ParseException("Expecting " + expectedDescription +
                                     " before " + reader.locationDescription());
        }
    }


    private void readNextWord()
    throws IOException
    {
        nextWord = reader.nextWord();
    }


    private boolean commandEnd()
    {
        return nextWord == null ||
               nextWord.startsWith(OPTION_PREFIX) ||
               nextWord.equals(AT_DIRECTIVE);
    }


    private void checkJavaIdentifier(String expectedDescription)
    throws ParseException
    {
        if (!isJavaIdentifier(nextWord))
        {
            throw new ParseException("Expecting " + expectedDescription +
                                     " before " + reader.locationDescription());
        }
    }


    private boolean isJavaIdentifier(String aWord)
    {
        for (int index = 0; index < aWord.length(); index++)
        {
            char c = aWord.charAt(index);
            if (!(Character.isJavaIdentifierPart(c) ||
                  c == '.' ||
                  c == '[' ||
                  c == ']' ||
                  c == '<' ||
                  c == '>' ||
                  c == '*' ||
                  c == '?'))
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Makes sure the given access flags are valid field access flags.
     */
    private void checkFieldAccessFlags(int requiredSetMemberAccessFlags,
                                       int requiredUnsetMemberAccessFlags)
    throws ParseException
    {
        if (((requiredSetMemberAccessFlags |
              requiredUnsetMemberAccessFlags) &
            ~ClassConstants.VALID_INTERNAL_ACC_FIELD) != 0)
        {
            throw new ParseException("Invalid method access modifier for field before " +
                                     reader.locationDescription());
        }
    }


    /**
     * Makes sure the given access flags are valid method access flags.
     */
    private void checkMethodAccessFlags(int requiredSetMemberAccessFlags,
                                        int requiredUnsetMemberAccessFlags)
    throws ParseException
    {
        if (((requiredSetMemberAccessFlags |
              requiredUnsetMemberAccessFlags) &
            ~ClassConstants.VALID_INTERNAL_ACC_METHOD) != 0)
        {
            throw new ParseException("Invalid field access modifier for method before " +
                                     reader.locationDescription());
        }
    }


    /**
     * A main method for testing command parsing.
     */
    private static void main(String[] args) {
        try
        {
            CommandParser parser = new CommandParser(args);

            parser.parse(new ProGuardOptions());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
