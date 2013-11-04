/* $Id: ConfigurationWriter.java,v 1.4 2003/12/06 22:12:42 eric Exp $
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
import proguard.classfile.util.*;
import proguard.util.*;

import java.io.*;
import java.util.*;


/**
 * This class writes ProGuard configurations to a file.
 *
 * @author Eric Lafortune
 */
public class ConfigurationWriter
{
    private PrintWriter writer;


    /**
     * Creates a new ConfigurationWriter for the given file name.
     */
    public ConfigurationWriter(String configurationFile) throws IOException
    {
        this(new PrintWriter(new FileWriter(configurationFile)));
    }


    /**
     * Creates a new ConfigurationWriter for the given OutputStream.
     */
    public ConfigurationWriter(OutputStream outputStream) throws IOException
    {
        this(new PrintWriter(outputStream));
    }


    /**
     * Creates a new ConfigurationWriter for the given PrintWriter.
     */
    public ConfigurationWriter(PrintWriter writer) throws IOException
    {
        this.writer = writer;
    }


    /**
     * Closes this ConfigurationWriter.
     */
    public void close() throws IOException
    {
        writer.close();
    }


    /**
     * Writes the given configuration.
     * @param configuration the configuration that is to be written out.
     * @throws IOException if an IO error occurs while writing the configuration.
     */
    public void write(Configuration configuration) throws IOException
    {
        // Write the input jars and directories.
        writeJarOptions(ConfigurationConstants.INJARS_OPTION,       configuration.inJars);
        writeJarOptions(ConfigurationConstants.RESOURCEJARS_OPTION, configuration.resourceJars);
        writeJarOptions(ConfigurationConstants.LIBRARYJARS_OPTION,  configuration.libraryJars);
        writeJarOptions(ConfigurationConstants.OUTJARS_OPTION,      configuration.outJars);
        writer.println();

        // Write the other options.
        writeOption(ConfigurationConstants.PRINT_SEEDS_OPTION,                          configuration.printSeeds);
        writeOption(ConfigurationConstants.PRINT_USAGE_OPTION,                          configuration.printUsage);
        writeOption(ConfigurationConstants.PRINT_MAPPING_OPTION,                        configuration.printMapping);
        writeOption(ConfigurationConstants.APPLY_MAPPING_OPTION,                        configuration.applyMapping);
        writeOption(ConfigurationConstants.VERBOSE_OPTION,                              configuration.verbose);
        writeOption(ConfigurationConstants.IGNORE_WARNINGS_OPTION,                      configuration.ignoreWarnings);
        writeOption(ConfigurationConstants.DONT_WARN_OPTION,                            !configuration.warn);
        writeOption(ConfigurationConstants.DONT_NOTE_OPTION,                            !configuration.note);
        writeOption(ConfigurationConstants.DONT_SHRINK_OPTION,                          !configuration.shrink);
        writeOption(ConfigurationConstants.DONT_OBFUSCATE_OPTION,                       !configuration.obfuscate);
        writeOption(ConfigurationConstants.DONT_USE_MIXED_CASE_CLASS_NAMES_OPTION,      !configuration.useMixedCaseClassNames);
        writeOption(ConfigurationConstants.OVERLOAD_AGGRESSIVELY_OPTION,                configuration.overloadAggressively);
        writeOption(ConfigurationConstants.DEFAULT_PACKAGE_OPTION,                      configuration.defaultPackage);
        writeOption(ConfigurationConstants.KEEP_ATTRIBUTES_OPTION,                      ListUtil.commaSeparatedString(configuration.keepAttributes));
        writeOption(ConfigurationConstants.RENAME_SOURCE_FILE_ATTRIBUTE_OPTION,         configuration.newSourceFileAttribute);
        writeOption(ConfigurationConstants.DONT_SKIP_NON_PUBLIC_LIBRARY_CLASSES_OPTION, !configuration.skipNonPublicLibraryClasses);
        writer.println();

        // Write the keep options.
        writeKeepClassFileOptions(configuration.keepClassFileOptions);
    }


    private void writeJarOptions(String optionName, ClassPath classPath)
    throws IOException
    {
        if (classPath != null)
        {
            for (int index = 0; index < classPath.size(); index++)
            {
                ClassPathEntry entry = classPath.get(index);

                writer.print(optionName + " " + quotedString(entry.getName()));

                // Append the filter, if any.
                String filter = entry.getFilter();
                if (filter != null)
                {
                    writer.print(ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD);
                    writer.print(quotedString(filter));
                    writer.print(ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD);
                }

                writer.println();
            }
        }
    }


    private void writeOption(String optionName, boolean flag)
    throws IOException
    {
        if (flag)
        {
            writer.println(optionName);
        }
    }


    private void writeOption(String optionName, String arguments)
    throws IOException
    {
        if (arguments != null)
        {
            writer.println(optionName + " " + quotedString(arguments));
        }
    }


    private void writeKeepClassFileOption(KeepClassFileOption keepClassFileOption)
    throws IOException
    {
        writer.println();

        // Write out the comments for this option.
        writeComments(keepClassFileOption.comments);

        // Write out the proper keep option name.
        writeKeepOptionName(keepClassFileOption.markClassFiles,
                            keepClassFileOption.markConditionally,
                            keepClassFileOption.onlyKeepNames);

        writer.print(" ");

        // Write out the class access flags.
        writer.print(ClassUtil.externalClassAccessFlags(keepClassFileOption.requiredUnsetAccessFlags,
                                                        ConfigurationConstants.NEGATOR_KEYWORD));

        writer.print(ClassUtil.externalClassAccessFlags(keepClassFileOption.requiredSetAccessFlags));

        // Write out the class keyword, if we didn't write the interface
        // keyword earlier.
        if (((keepClassFileOption.requiredSetAccessFlags |
              keepClassFileOption.requiredUnsetAccessFlags) &
             ClassConstants.INTERNAL_ACC_INTERFACE) == 0)
        {
            writer.print("class");
        }

        writer.print(" ");

        // Write out the class name.
        writer.print(keepClassFileOption.className != null ?
            ClassUtil.externalClassName(keepClassFileOption.className) :
            "*");

        // Write out the extends template, if any.
        if (keepClassFileOption.extendsClassName != null)
        {
            writer.print(" extends " + ClassUtil.externalClassName(keepClassFileOption.extendsClassName));
        }

        // Write out the as directive, if any.
        if (keepClassFileOption.asClassName != null)
        {
            writer.print(" as " + ClassUtil.externalClassName(keepClassFileOption.asClassName));
        }

        // Write out the keep field and keep method options, if any.
        if (keepClassFileOption.keepFieldOptions  != null ||
            keepClassFileOption.keepMethodOptions != null)
        {
            writer.println(" {");

            writeKeepFieldOptions( keepClassFileOption.keepFieldOptions);
            writeKeepMethodOptions(keepClassFileOption.keepMethodOptions,
                                   keepClassFileOption.className);
            writer.println("}");
        }
        else
        {
            writer.println();
        }
    }



    private void writeComments(String comments)
    {
        if (comments != null)
        {
            int index = 0;
            while (index < comments.length())
            {
                int breakIndex = comments.indexOf('\n', index);
                if (breakIndex < 0)
                {
                    breakIndex = comments.length();
                }

                writer.print('#');
                writer.println(comments.substring(index, breakIndex));

                index = breakIndex + 1;
            }
        }
    }


    private void writeKeepOptionName(boolean markClassFiles, boolean markConditionally, boolean onlyKeepNames)
    {
        writer.print(onlyKeepNames ?
            (markConditionally ? ConfigurationConstants.KEEP_CLASSES_WITH_MEMBER_NAMES_OPTION :
             markClassFiles    ? ConfigurationConstants.KEEP_NAMES_OPTION                     :
                                 ConfigurationConstants.KEEP_CLASS_MEMBER_NAMES_OPTION) :
            (markConditionally ? ConfigurationConstants.KEEP_CLASSES_WITH_MEMBERS_OPTION :
             markClassFiles    ? ConfigurationConstants.KEEP_OPTION                      :
                                 ConfigurationConstants.KEEP_CLASS_MEMBERS_OPTION));
    }


    private void writeKeepFieldOptions(List keepClassMemberOptions)
    throws IOException
    {
        if (keepClassMemberOptions != null)
        {
            for (int index = 0; index < keepClassMemberOptions.size(); index++)
            {
                KeepClassMemberOption keepClassMemberOption =
                    (KeepClassMemberOption)keepClassMemberOptions.get(index);

                writer.print("    ");

                // Write out the field access flags.
                writer.print(ClassUtil.externalFieldAccessFlags(keepClassMemberOption.requiredUnsetAccessFlags,
                                                                ConfigurationConstants.NEGATOR_KEYWORD));

                writer.print(ClassUtil.externalFieldAccessFlags(keepClassMemberOption.requiredSetAccessFlags));

                // Write out the field name and descriptor.
                writer.print(keepClassMemberOption.name       != null ||
                             keepClassMemberOption.descriptor != null ?
                    ClassUtil.externalFullFieldDescription(0,
                                                           keepClassMemberOption.name,
                                                           keepClassMemberOption.descriptor) :
                    ConfigurationConstants.ANY_FIELD_KEYWORD);

                writer.println(";");
            }
        }
    }


    private void writeKeepMethodOptions(List keepClassMemberOptions, String className)
    throws IOException
    {
        if (keepClassMemberOptions != null)
        {
            for (int index = 0; index < keepClassMemberOptions.size(); index++)
            {
                KeepClassMemberOption keepClassMemberOption =
                    (KeepClassMemberOption)keepClassMemberOptions.get(index);

                writer.print("    ");

                // Write out the method access flags.
                writer.print(ClassUtil.externalMethodAccessFlags(keepClassMemberOption.requiredUnsetAccessFlags,
                                                                 ConfigurationConstants.NEGATOR_KEYWORD));

                writer.print(ClassUtil.externalMethodAccessFlags(keepClassMemberOption.requiredSetAccessFlags));

                // Write out the method name and descriptor.
                writer.print(keepClassMemberOption.name       != null ||
                             keepClassMemberOption.descriptor != null ?
                    ClassUtil.externalFullMethodDescription(className,
                                                            0,
                                                            keepClassMemberOption.name,
                                                            keepClassMemberOption.descriptor) :
                    ConfigurationConstants.ANY_METHOD_KEYWORD);

                writer.println(";");
            }
        }
    }


    private String quotedString(String string)
    {
        return
            string.length()     == 0 ||
            string.indexOf(' ') >= 0  ? ("'" + string + "'") :
                                        (      string      );
    }


    private void writeKeepClassFileOptions(List keepClassFileOptions)
    throws IOException
    {
        if (keepClassFileOptions != null)
        {
            for (int index = 0; index < keepClassFileOptions.size(); index++)
            {
                writeKeepClassFileOption((KeepClassFileOption)keepClassFileOptions.get(index));
            }
        }
    }


    /**
     * A main method for testing configuration writing.
     */
    private static void main(String[] args) {
        try
        {
            ConfigurationWriter writer = new ConfigurationWriter(args[0]);

            writer.write(new Configuration());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
