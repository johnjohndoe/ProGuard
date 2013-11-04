/* $Id: $
 *
 * ProGuard - integration into Ant.
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
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;

import proguard.*;

import proguard.classfile.util.*;


/**
 * Storage for recurrent configuration settings.
 *
 * @author Dirk Schnelle
 * @version $Revision: 1.1 $
 */
public class ProGuardConfigurationTask
        extends Task
{
    /** Options read from a configuration file. */
    protected AntConfiguration options;

    /** Nested configuration settings. */
    private Collection configurations;

    /** Subtasks. */
    private Collection subtasks;

    /** Helper to resolve the full path name. */
    private final FileUtils fileUtils = FileUtils.newFileUtils();

    /** Reference to a path. */
    protected Reference reference;

    /**
     * Create a new Configuration object.
     */
    public ProGuardConfigurationTask() {}

    /**
     * Try to convert the given string into a boolean. Valid inputs are
     * <code>yes, true, on, no, false, off</code> independent of case.
     *
     * @param value The string to be converted into a boolean.
     *
     * @return Boolean value if the string contains a valid boolean,
     *         <code>null</code> otherwise.
     */
    private Boolean getBoolean(String value)
    {
        if (value == null)
        {
            return null;
        }

        if (value.equalsIgnoreCase("yes") ||
                    value.equalsIgnoreCase("true") ||
                    value.equalsIgnoreCase("on"))
        {
            return Boolean.TRUE;
        }

        if (value.equalsIgnoreCase("no") ||
                    value.equalsIgnoreCase("false") ||
                    value.equalsIgnoreCase("off"))
        {
            return Boolean.FALSE;
        }

        return null;
    }

    /**
     * Initializes this task.
     *
     * @throws BuildException Never thrown.
     */
    public void init()
            throws BuildException
    {
        options            = new AntConfiguration();
        configurations     = null;
        subtasks           = new ArrayList();
        reference          = null;

        setProguardProperties();
    }

    /**
     * Set some properties to have a workaround for some proguard keywords
     * encapsulated in &lt; and &gt; chars. If the properties are already set,
     * this method has no effect.
     */
    private void setProguardProperties()
    {
        project.setNewProperty("proguard.init", "<init>");
        project.setNewProperty("proguard.methods", "<methods>");
        project.setNewProperty("proguard.fields", "<fields>");
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
     * @param injar jar to be added.
     */
    void addInjar(ClassPathEntry injar)
    {
        log("adding injar: '" + injar.getName() + "'", Project.MSG_VERBOSE);
        options.addInJar(injar);
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
     * @param libraryjar jar to be added.
     */
    void addLibraryjar(ClassPathEntry libraryjar)
    {
        log("adding libraryjar: '" + libraryjar.getName() + "'",
            Project.MSG_VERBOSE);

        options.addLibraryJar(libraryjar);
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
     * @param resourcejar jar to be added.
     */
    void addResourcejar(ClassPathEntry resourcejar)
    {
        log("adding resourcejar: '" + resourcejar.getName() + "'",
            Project.MSG_VERBOSE);

        options.addResourceJar(resourcejar);
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

        log("adding keepattribute '" + keepattribute.toString() + "'",
            Project.MSG_VERBOSE);
    }

    /**
     * Adds a nested keep subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeep(KeepClassSpecification keep)
    {
        keep.init(this, true, false, false);
        subtasks.add(keep);
    }

    /**
     * Adds a nested keepclassmember subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclassmembers(KeepClassSpecification keep)
    {
        keep.init(this, false, false, false);
        subtasks.add(keep);
    }

    /**
     * Adds a nested keepclasseswithmembers subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclasseswithmembers(KeepClassSpecification keep)
    {
        keep.init(this, false, true, false);
        subtasks.add(keep);
    }

    /**
     * Adds a class file specification to the specifications to keep.
     *
     * @param keepClassFileOption The class file specification to keep.
     */
    void addKeepClassFileOption(KeepClassFileOption keepClassFileOption)
    {
        log("adding KeepClassFileOption: '" + keepClassFileOption.toString() +
            "'", Project.MSG_VERBOSE);
        options.addKeepClassFileOption(keepClassFileOption);
    }

    /**
     * Adds a nested keepnames subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepnames(KeepClassSpecification keep)
    {
        keep.init(this, true, false, true);
        subtasks.add(keep);
    }

    /**
     * Adds a nested keepclassmembernames subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclassmembernames(KeepClassSpecification keep)
    {
        keep.init(this, false, false, true);
        subtasks.add(keep);
    }

    /**
     * Adds a nested keepclasseswithmembernames subtask.
     *
     * @param keep Handler for the nested task.
     */
    public void addKeepclasseswithmembernames(KeepClassSpecification keep)
    {
        keep.init(this, false, true, true);
        subtasks.add(keep);
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
        {
            return;
        }

        log("parsing additional text: '" + trimmed + "'", Project.MSG_VERBOSE);

        String[] args = getTextAsArgs(trimmed);

        try
        {
            ConfigurationParser parser = new ConfigurationParser(args);
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
     * Converts the given string into an args array.
     *
     * @param text The text tobe converted
     *
     * @return Text in an args array.
     */
    private String[] getTextAsArgs(String text)
    {
        StringTokenizer tokenizer = new StringTokenizer(text);

        String[]        args  = new String[tokenizer.countTokens()];
        int             index = 0;

        while (tokenizer.hasMoreTokens())
        {
            final String nextToken = tokenizer.nextToken();
            args[index++] = getProject().replaceProperties(nextToken);
        }

        return args;
    }

    /**
     * Sets the ID of a configuration reference.
     *
     * @param reference Reference to a path.
     *
     * @exception BuildException The user tried to set a ref id in the proguard
     *            task
     */
    public void setRefid(Reference reference)
    {
        if (getClass() != ProGuardTask.class)
        {
            throw new BuildException(
                "The <proguard> task must not have a refid!");
        }

        Object referencedObject = reference.getReferencedObject(project);

        if (referencedObject.getClass() != ProGuardConfigurationTask.class)
        {
            throw new BuildException("reference '" + reference.getRefId() +
                "' must be a reference to proguardconfiguration!");
        }

        log("including referenced configuration: " + reference.getRefId(),
            Project.MSG_DEBUG);

        this.reference = reference;
    }

    /**
     * Set the name of the configuration file.
     *
     * @param file Name of the configuration file.
     *
     * @throws BuildException Error parsing the file.
     */
    public void setFile(String file)
            throws BuildException
    {
        options = new AntConfiguration();

        try
        {
            String              fileFullPath = getFullPathName(file);

            ConfigurationParser parser = new ConfigurationParser(fileFullPath);
            log("parsing configuration file: '" + fileFullPath + "'",
                Project.MSG_VERBOSE);
            parser.parse(options);
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }

    /**
     * Add the nested configuration file.
     *
     * @param configuration The nested configuration.
     */
    public void addConfiguration(ProGuardConfigurationTask configuration)
    {
        if (configurations == null)
        {
            configurations = new ArrayList();
        }

        configurations.add(configuration);
    }

    /**
     * Turns verbose mode on or off.
     *
     * @param verbose <code>true</code> if verbose mode should be turned on.
     */
    public void setVerbose(boolean verbose)
    {
        options.setVerbose(verbose);
    }

    /**
     * Turns warnings on or off.
     *
     * @param on <code>true</code> if warnings should be turned on.
     */
    public void setWarn(boolean on)
    {
        options.setWarn(on);
    }

    /**
     * Turns notes on or off.
     *
     * @param on <code>true</code> if notes should be turned on.
     */
    public void setNote(boolean on)
    {
        options.setNote(on);
    }

    /**
     * Turns ignorewarnings on or off.
     *
     * @param warn <code>true</code> if warnings should be turned on.
     */
    public void setIgnorewarnings(boolean warn)
    {
        options.setIgnorewarnings(warn);
    }

    /**
     * Enables ProGuard's shrink option.
     *
     * @param on <code>true</code> if ProGuard should shrink the output.
     */
    public void setShrink(boolean on)
    {
        options.setShrink(on);
    }

    /**
     * Enables ProGuard's obfuscation option.
     *
     * @param on <code>true</code> if the output should be obfuscated.
     */
    public void setObfuscate(boolean on)
    {
        options.setObfuscate(on);
    }

    /**
     * Enables ProGuard's mixed class names option.
     *
     * @param on <code>true</code> if the class names should contain both
     *        uppercase and lowercase characters.
     */
    public void setUsemixedclassnames(boolean on)
    {
        options.setUsemixedclassnames(on);
    }

    /**
     * Enables ProGuard's aggressive overloading option.
     *
     * @param on <code>true</code> if class member names should be overloaded
     *        aggressively.
     */
    public void setOverloadaggressively(boolean on)
    {
        options.setOverloadaggressively(on);
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
     * @param on <code>true</code> if the non public class members should be
     *        skipped while reading library jars.
     */
    public void setSkipnonpubliclibraryclasses(boolean on)
    {
        options.setSkipnonpubliclibraryclasses(on);
    }

    /**
     * Sets the output for the print mapping.
     *
     * @param out Filename for the print mapping.
     */
    public void setPrintmapping(String out)
    {
      Boolean printMapping = getBoolean(out);

      if (printMapping == null)
      {
          options.printMapping = getFullPathName(out);
      }
      else
      {
          if (Boolean.TRUE.equals(printMapping))
          {
            options.printSeeds = "";
          }
      }
    }

    /**
     * Sets the mapping file to reuse.
     *
     * @param mapping Name of a mapping file from a previous run.
     */
    public void setApplymapping(String mapping)
    {
        options.applyMapping = getFullPathName(mapping);
    }

    /**
     * Sets the filename where to store the seeds.
     *
     * @param filename Name of the file or a floag to print to stdout.
     */
    public void setPrintseeds(String filename)
    {
        Boolean printSeeds = getBoolean(filename);

        if (printSeeds == null)
        {
            options.printSeeds = getFullPathName(filename);
        }
        else
        {
            if (Boolean.TRUE.equals(printSeeds))
            {
              options.printSeeds = "";
            }
        }
    }

    /**
     * Sets the filename where to store the usage
     *
     * @param filename Name of the file, or a flag to print to stdout.
     */
    public void setPrintusage(String filename)
    {
        Boolean printUsage = getBoolean(filename);

        if (printUsage == null)
        {
            options.printUsage = getFullPathName(filename);
        }
        else
        {
            if (Boolean.TRUE.equals(printUsage))
            {
              options.printUsage = "";
            }
        }
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
        String outjarFullPath = getFullPathName(outjar);
        log("setting outjar: '" + outjarFullPath + "'", Project.MSG_VERBOSE);

        ClassPathEntry entry = new ClassPathEntry(outjarFullPath);
        options.addOutJar(entry);
    }

    /**
     * Adds an outjar nested task to this task.
     *
     * @param outjar Handler for the nested task.
     */
    public void addOutjar(OutJar outjar)
    {
        outjar.setParent(this);
        subtasks.add(outjar);
    }

    /**
     * Adds the given jar to the outjars.
     *
     * @param outjar jar to be added.
     */
    void addOutjar(ClassPathEntry outjar)
    {
        log("adding outjar: '" + outjar.getName() + "'", Project.MSG_VERBOSE);

        options.addOutJar(outjar);
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
     * Gets the full path name of the given relative file name.
     *
     * @param relativeFileName A relative filename.
     *
     * @return Full path name.
     */
    protected String getFullPathName(String relativeFileName)
    {
        final File projectDir   = getProject().getBaseDir();
        final File relativeFile =
            fileUtils.resolveFile(projectDir, relativeFileName);

        return relativeFile.getAbsolutePath();
    }

    /**
     * Executes this task.
     *
     * @throws BuildException Error executing ProGuard.
     */
    public void execute()
            throws BuildException
    {
        evalReference();

        executeSubtasks();

        if (configurations == null)
        {
            return;
        }

        Iterator iterator = configurations.iterator();

        while (iterator.hasNext())
        {
            ProGuardConfigurationTask configuration =
                (ProGuardConfigurationTask) iterator.next();
            configuration.execute();
            options.merge(configuration.options);
        }
    }

    /**
     * Evaluates a given referenced path.
     */
    private void evalReference()
    {
        if (reference == null)
        {
            return;
        }

        ProGuardConfigurationTask configuration =
            (ProGuardConfigurationTask) reference.getReferencedObject(project);
        this.options = configuration.options;
    }

    /**
     * Executes all subtasks.
     *
     * @exception BuildException Validation of subtask not successful.
     */
    private void executeSubtasks()
            throws BuildException
    {
        if (subtasks == null)
        {
            return;
        }

        Iterator iterator = subtasks.iterator();

        while (iterator.hasNext())
        {
            Subtask subtask = (Subtask) iterator.next();
            subtask.validate();
            subtask.execute(this);
        }
    }
}
