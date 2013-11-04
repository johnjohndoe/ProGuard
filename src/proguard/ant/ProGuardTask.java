/* $Id: ProGuardTask.java,v 1.15 2003/05/03 12:05:30 eric Exp $
 *
 * ProGuard -- integration into Ant.
 *
 * Copyright (c) 2003 Dirk Schnelle (dirk.schnelle@web.de)
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
package proguard.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;
import proguard.*;
import proguard.classfile.util.*;


/**
 * Ant support for ProGuard. This class is the main task for all ProGuard
 * activities in Ant.
 *
 * @author Dirk Schnelle
 */
public class ProGuardTask
     extends Task
{
    /** The options to configure. */
    private AntProGuardOptions options;

    /** Options read from a configuration file. */
    private AntProGuardOptions optionsFromConfigFile;

    /** Subtasks. */
    private Collection subtasks;

    /** The verbose attribute was modified. */
    private boolean verboseSet;

    /** The ignoreWarnings attribute was modified. */
    private boolean ignoreWarningsSet;

    /** The warn attribute was modified. */
    private boolean warnSet;

    /** The note attribute was modified. */
    private boolean noteSet;

    /** The shrink attribute was modified. */
    private boolean shrinkSet;

    /** The obfuscate attribute was modified. */
    private boolean obfuscateSet;

    /** The useMixedCaseClassNames attribute was modified. */
    private boolean useMixedCaseClassNamesSet;

    /** The overloadAggressively attribute was modified. */
    private boolean overloadAggressivelySet;

    /** The skipNonPublicLibraryClasses attribute was modified. */
    private boolean skipNonPublicLibraryClassesSet;

    /** Helper to resolve the full path name. */
    private final FileUtils fileUtils = FileUtils.newFileUtils();


    /**
     * Creates a new task for ProGuard.
     */
    public ProGuardTask()
    {
    }


    /**
     * Evaluates nested text (PCDATA).
     *
     * @param nestedText The PCDATA to parse.
     *
     * @throws BuildException Error parsing the given string.
     */
    public void addText(String nestedText)
    {
        String trimmed = nestedText.trim();

        if (trimmed.length() == 0)
            return;

        String[] args = getTextAsArgs(trimmed);

        try
        {
            CommandParser parser = new CommandParser(args);
            parser.parse(options);
        }
        catch (IOException ioe)
        {
            throw new BuildException(ioe);
        }
        catch (ParseException pe)
        {
            throw new BuildException(pe);
        }
    }


    /**
     * Converts the given string into an array.
     *
     * @param text The text tobe converted
     *
     * @return Text in an args array.
     */
    private String[] getTextAsArgs(String text)
    {
        StringTokenizer tokenizer = new StringTokenizer(text);

        String[] args = new String[tokenizer.countTokens()];
        int index = 0;

        while (tokenizer.hasMoreTokens())
            args[index++] = expandEnvSettings(tokenizer.nextToken());

        return args;
    }


    /**
     * Expands the environment variables in the given string.
     *
     * @param variableString The string probably containing environment variables
     *
     * @return String with resolved environment values.
     */
    private String expandEnvSettings(String variableString)
    {
        int startEnv = variableString.indexOf("${");

        if (startEnv < 0)
            return variableString;

        int endEnv = variableString.indexOf("}", startEnv);

        if (endEnv < 0)
            return variableString;

        String envKey = variableString.substring(startEnv + 2, endEnv);
        String envValue = project.getProperty(envKey);

        if (envValue == null)
            return variableString;

        return StringUtils.replace(variableString, "${" + envKey + "}", envValue);
    }


    /**
     * Adds an injar nested task to this task.
     *
     * @param injar Handler for the nested task.
     */
    public void addInjar(InJar injar)
    {
        injar.setParent(this);
        subtasks.add(injar);
    }


    /**
     * Adds the given jar to the injars.
     *
     * @param injar name of the jar file to be added.
     */
    void addInjar(String injar)
    {
        options.addInJar(getFullPathName(injar));
    }


    /**
     * Adds a libraryjar nested task to this task.
     *
     * @param libraryjar Handler for the nested task.
     */
    public void addLibraryjar(LibraryJar libraryjar)
    {
        libraryjar.setParent(this);
        subtasks.add(libraryjar);
    }


    /**
     * Adds the given jar to the libraryjars.
     *
     * @param libraryjar name of the jar file to be added.
     */
    void addLibraryjar(String libraryjar)
    {
        options.addLibraryJar(getFullPathName(libraryjar));
    }


    /**
     * Adds an injar nested task to this task.
     *
     * @param resourcejar Handler for the nested task.
     */
    public void addResourcejar(ResourceJar resourcejar)
    {
        resourcejar.setParent(this);
        subtasks.add(resourcejar);
    }


    /**
     * Adds the given jar to the resourcejars.
     *
     * @param resourcejar name of the jar file to be added.
     */
    void addResourcejar(String resourcejar)
    {
        options.addResourceJar(getFullPathName(resourcejar));
    }


    /**
     * Adds a keepattribute nested task to this task.
     *
     * @param keepattribute Handler for the nested task.
     */
    public void addKeepattribute(KeepAttribute keepattribute)
    {
        subtasks.add(keepattribute);
    }


    /**
     * Adds the given attribute to the list of attributes to keep.
     *
     * @param keepattribute Name of the attribute.
     */
    void addKeepattribute(String keepattribute)
    {
        options.addKeepAttribute(keepattribute);
    }


    /**
     * Adds a nested keep subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeep(KeepClassSpecification keep)
    {
        keep.init(true, false, false);
        subtasks.add(keep);
    }


    /**
     * Adds a nested keepclassmember subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclassmembers(KeepClassSpecification keep)
    {
        keep.init(false, false, false);
        subtasks.add(keep);
    }


    /**
     * Adds a nested keepclasseswithmembers subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclasseswithmembers(KeepClassSpecification keep)
    {
        keep.init(false, true, false);
        subtasks.add(keep);
    }


    /**
     * Adds a class specification to the specifications to keep.
     *
     * @param keepcommand The class specification to keep.
     */
    void addKeepCommand(KeepCommand keepcommand)
    {
        options.addKeepCommand(keepcommand);
    }


    /**
     * Adds a nested keepnames subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepnames(KeepClassSpecification keep)
    {
        keep.init(true, false, true);
        subtasks.add(keep);
    }


    /**
     * Adds a nested keepclassmembernames subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclassmembernames(KeepClassSpecification keep)
    {
        keep.init(false, false, true);
        subtasks.add(keep);
    }


    /**
     * Adds a nested keepclasseswithmembernames subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclasseswithmembernames(KeepClassSpecification keep)
    {
        keep.init(false, true, true);
        subtasks.add(keep);
    }


    /**
     * Sets the configuration file for the ProGuard parser.
     *
     * @param file Name of the configuration file.
     *
     * @throws BuildException
     */
    public void setConfiguration(String file)
        throws BuildException
    {
        optionsFromConfigFile = new AntProGuardOptions();

        try
        {
            CommandParser parser = new CommandParser(file);
            parser.parse(optionsFromConfigFile);
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }


    /**
     * Turns verbose mode on or off.
     *
     * @param verbose true if verbose mode should be turned on.
     */
    public void setVerbose(boolean verbose)
    {
        options.verbose = verbose;
        verboseSet = true;
    }


    /**
     * Turns warnings on or off.
     *
     * @param on true if warnings should be turned on.
     */
    public void setWarn(boolean on)
    {
        options.warn = on;
        warnSet = true;
    }


    /**
     * Turns notes on or off.
     *
     * @param on true if notes should be turned on.
     */
    public void setNote(boolean on)
    {
        options.note = on;
        noteSet = true;
    }


    /**
     * Turns ignorewarnings on or off.
     *
     * @param warn true if warnings should be turned on.
     */
    public void setIgnorewarnings(boolean warn)
    {
        options.ignoreWarnings = warn;
        ignoreWarningsSet = true;
    }


    /**
     * Enables ProGuard's shrink option.
     *
     * @param on true if ProGuard should shrink the output.
     */
    public void setShrink(boolean on)
    {
        options.shrink = on;
        shrinkSet = true;
    }


    /**
     * Enables ProGuard's obfuscation option.
     *
     * @param on true if the output should be obfuscated.
     */
    public void setObfuscate(boolean on)
    {
        options.obfuscate = on;
        obfuscateSet = true;
    }


    /**
     * Enables ProGuard's mixed class names option.
     *
     * @param on true if the class names should contain both uppercase and
     *           lowercase characters.
     */
    public void setUsemixedclassnames(boolean on)
    {
        options.useMixedCaseClassNames = on;
        useMixedCaseClassNamesSet = true;
    }


    /**
     * Enables ProGuard's aggressive overloading option.
     *
     * @param on true if class member names should be overloaded aggressively.
     */
    public void setOverloadaggressively(boolean on)
    {
        options.overloadAggressively = on;
        overloadAggressivelySet = true;
    }


    /**
     * Sets the default package for the obfuscation.
     *
     * @param defaultPackage Name of the default package.
     */
    public void setDefaultpackage(String defaultPackage)
    {
        options.defaultPackage = ClassUtil.internalClassName(defaultPackage);
    }


    /**
     * Sets the skipnonpubliclibraryclasses.
     *
     * @param on true if the non public class members should be skipped while
     *           reading library jars.
     */
    public void setSkipnonpubliclibraryclasses(boolean on)
    {
        options.skipNonPublicLibraryClasses = on;
        skipNonPublicLibraryClassesSet = true;
    }


    /**
     * Sets the output for the print mapping.
     *
     * @param out Filename for the print mapping.
     */
    public void setPrintmapping(String out)
    {
        options.printMapping = getFullPathName(out);
    }


    /**
     * Sets the filename where to store the seeds.
     *
     * @param filename Name of the file
     */
    public void setPrintseeds(String filename)
    {
        options.printSeeds = getFullPathName(filename);
    }


    /**
     * Sets the filename where to store the usage
     *
     * @param filename Name of the file.
     */
    public void setPrintusage(String filename)
    {
        options.printUsage = getFullPathName(filename);
    }


    /**
     * Sets the filename where to store a dump.
     *
     * @param filename Name of the file.
     */
    public void setDump(String filename)
    {
        options.dump = getFullPathName(filename);
    }


    /**
     * Sets the output jar file name.
     *
     * @param outjar Output jar file name.
     */
    public void setOutjar(String outjar)
    {
        options.outJar = getFullPathName(outjar);
    }


    /**
     * Sets the rename source file attribute.
     *
     * @param newSourceFileAttribute Source file attribute.
     */
    public void setRenamesourcefileattribute(String newSourceFileAttribute)
    {
        options.newSourceFileAttribute = newSourceFileAttribute;
    }


    /**
     * Initializes this task.
     *
     * @throws BuildException Never thrown.
     */
    public void init()
        throws BuildException
    {
        System.out.println(ProGuard.VERSION);

        options = new AntProGuardOptions();
        optionsFromConfigFile = null;
        subtasks = new Vector();
        verboseSet = false;
        ignoreWarningsSet = false;
        warnSet = false;
        noteSet = false;
        shrinkSet = false;
        obfuscateSet = false;
        useMixedCaseClassNamesSet = false;
        overloadAggressivelySet = false;
        skipNonPublicLibraryClassesSet = false;
    }


    /**
     * Executes this task.
     *
     * @throws BuildException Error executing ProGuard.
     */
    public void execute()
        throws BuildException
    {
        executeSubtasks();

        mergeWithConfigFile();

        if (!isExecutionNecessary())
            return;

        ProGuard proguard = new ProGuard(options);

        try
        {
            proguard.execute();
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }


    /**
     * Executes all subtasks.
     *
     * @exception BuildException Validation of subtask not successful.
     */
    private void executeSubtasks()
        throws BuildException
    {
        Iterator iterator = subtasks.iterator();

        while (iterator.hasNext())
        {
            Subtask subtask = (Subtask)iterator.next();
            subtask.validate();
            subtask.execute(this);
        }
    }


    /**
     * Mix up the settings from the configuration file with Ant settings. Ant
     * settings override the settings from the configuration file.
     */
    private void mergeWithConfigFile()
    {
        if (optionsFromConfigFile == null)
            return;

        optionsFromConfigFile.merge(options);

        if (verboseSet)
            optionsFromConfigFile.verbose = options.verbose;

        if (ignoreWarningsSet)
            optionsFromConfigFile.ignoreWarnings = options.ignoreWarnings;

        if (warnSet)
            optionsFromConfigFile.warn = options.warn;

        if (noteSet)
            optionsFromConfigFile.note = options.note;

        if (shrinkSet)
            optionsFromConfigFile.shrink = options.shrink;

        if (obfuscateSet)
            optionsFromConfigFile.obfuscate = options.obfuscate;

        if (useMixedCaseClassNamesSet)
            optionsFromConfigFile.useMixedCaseClassNames =
                options.useMixedCaseClassNames;

        if (overloadAggressivelySet)
            optionsFromConfigFile.overloadAggressively =
                options.overloadAggressively;

        if (skipNonPublicLibraryClassesSet)
            optionsFromConfigFile.skipNonPublicLibraryClasses =
                options.skipNonPublicLibraryClasses;

        options = optionsFromConfigFile;
    }


    /**
     * Checks if it is necessary to execute ProGuard.
     *
     * @return true if ProGuard should be executed.
     *
     * @exception BuildException Error examining the jar files.
     */
    private boolean isExecutionNecessary()
        throws BuildException
    {
        if (options.outJar == null)
            return true;

        File outjar = new File(options.outJar);

        if (!outjar.exists())
            return true;

        long outjarLastModified = outjar.lastModified();

        // I do not know how to handle this. Delegate the decision about
        // it to ProGuard.
        if ((options.inJars == null) && (options.resourceJars == null))
            return true;

        if (checkJarContainerForDate(options.inJars, outjarLastModified))
            return true;

        return (checkJarContainerForDate(options.resourceJars,
                                         outjarLastModified));
    }


    /**
     * Check the files in the given jar container if they contain changes since
     * the last run of ProGuard.
     *
     * @param jarContainer The jar container to check for changes
     * @param lastModified Date of the last ProGuard generated jar.
     *
     * @return true, if changes have been made to at least one file.
     *
     * @throws BuildException An entry of the jar container does not exist.
     */
    private boolean checkJarContainerForDate(Collection jarContainer,
                                             long lastModified)
        throws BuildException
    {
        if (jarContainer == null)
            return false;

        Iterator iterator = jarContainer.iterator();

        while (iterator.hasNext())
        {
            String jar = (String)iterator.next();
            File jarFile = new File(jar);

            if (!jarFile.exists())
                throw new BuildException("file or directory'" + jar +
                                         "' does not exist!");

            if (jarFile.isDirectory())
            {
                if (checkSubDirForDate(jarFile, lastModified))
                    return true;
            }
            else if (jarFile.lastModified() > lastModified)
                return true;
        }

        return false;
    }


    /**
     * Checks the files in the given directory if they contain changes since the
     * last run of ProGuard.
     *
     * @param directory The directory to scan.
     * @param lastModified Date of the last ProGuard generated jar.
     *
     * @return true, if changes have been made to at least one file.
     */
    private boolean checkSubDirForDate(File directory,
                                       long lastModified)
    {
        File[] children = directory.listFiles();
        int size = children.length;

        for (int i = 0; i < size; i++)
        {
            File actChild = children[i];

            if (actChild.isDirectory())
            {
                if (checkSubDirForDate(actChild, lastModified))
                    return true;
            }
            else
            {
                if (actChild.lastModified() > lastModified)
                    return true;
            }
        }

        return false;
    }


    /**
     * Gets the full path name of the given relative file name.
     *
     * @param relativeFileName A relative filename.
     *
     * @return Full path name.
     */
    private String getFullPathName(String relativeFileName)
    {
        Project project = getProject();
        File projectDir = project.getBaseDir();
        File relativeFile =
            fileUtils.resolveFile(projectDir, relativeFileName);

        return relativeFile.getAbsolutePath();
    }
}
