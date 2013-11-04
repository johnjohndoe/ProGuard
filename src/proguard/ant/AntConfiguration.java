/* $Id: AntConfiguration.java,v 1.2 2003/12/19 04:17:03 eric Exp $
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
 * ANY WARRAntY; without even the implied warranty of MERCHAntABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.ant;

import java.util.*;

import proguard.*;


/**
 * A wrapper around the ProGuard options to add settings. This class provides
 * some convenient methods to add items to the list settings without caring
 * about an uninitialized list. In addition, it provides a method to merge the
 * settings of two option-objects.
 *
 * @author $author$
 */
public class AntConfiguration
        extends Configuration
{
    /** The verbose attribute was modified. */
    private boolean verboseSet = false;

    /** The ignoreWarnings attribute was modified. */
    private boolean ignoreWarningsSet = false;

    /** The warn attribute was modified. */
    private boolean warnSet = false;

    /** The note attribute was modified. */
    private boolean noteSet = false;

    /** The shrink attribute was modified. */
    private boolean shrinkSet = false;

    /** The obfuscate attribute was modified. */
    private boolean obfuscateSet = false;

    /** The useMixedCaseClassNames attribute was modified. */
    private boolean useMixedCaseClassNamesSet = false;

    /** The overloadAggressively attribute was modified. */
    private boolean overloadAggressivelySet = false;

    /** The skipNonPublicLibraryClasses attribute was modified. */
    private boolean skipNonPublicLibraryClassesSet = false;

    /**
     * Turns verbose mode on or off.
     *
     * @param verbose true if verbose mode should be turned on.
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose     = verbose;
        verboseSet       = true;
    }

    /**
     * Turns warnings on or off.
     *
     * @param on <code>true</code> if warnings should be turned on.
     */
    public void setWarn(boolean on)
    {
        this.warn     = on;
        warnSet       = true;
    }

    /**
     * Turns notes on or off.
     *
     * @param on <code>true</code> if notes should be turned on.
     */
    public void setNote(boolean on)
    {
        this.note     = on;
        noteSet       = true;
    }

    /**
     * Turns ignorewarnings on or off.
     *
     * @param warn <code>true</code> if warnings should be turned on.
     */
    public void setIgnorewarnings(boolean warn)
    {
        this.ignoreWarnings     = warn;
        ignoreWarningsSet       = true;
    }

    /**
     * Enables ProGuard's shrink option.
     *
     * @param on <code>true</code> if ProGuard should shrink the output.
     */
    public void setShrink(boolean on)
    {
        this.shrink     = on;
        shrinkSet       = true;
    }

    /**
     * Enables ProGuard's obfuscation option.
     *
     * @param on <code>true</code> if the output should be obfuscated.
     */
    public void setObfuscate(boolean on)
    {
        this.obfuscate     = on;
        obfuscateSet       = true;
    }

    /**
     * Enables ProGuard's mixed class names option.
     *
     * @param on <code>true</code> if the class names should contain both
     *        uppercase and lowercase characters.
     */
    public void setUsemixedclassnames(boolean on)
    {
        this.useMixedCaseClassNames     = on;
        useMixedCaseClassNamesSet       = true;
    }

    /**
     * Enables ProGuard's aggressive overloading option.
     *
     * @param on <code>true</code> if class member names should be overloaded
     *        aggressively.
     */
    public void setOverloadaggressively(boolean on)
    {
        this.overloadAggressively     = on;
        overloadAggressivelySet       = true;
    }

    /**
     * Sets the skipnonpubliclibraryclasses.
     *
     * @param on <code>true</code> if the non public class members should be
     *        skipped while reading library jars.
     */
    public void setSkipnonpubliclibraryclasses(boolean on)
    {
        this.skipNonPublicLibraryClasses     = on;
        skipNonPublicLibraryClassesSet       = true;
    }

    /**
     * Add the given jar to the list of known library jars
     *
     * @param jar The jar to add.
     */
    public void addLibraryJar(ClassPathEntry jar)
    {
        if (jar == null)
        {
            return;
        }

        if (libraryJars == null)
        {
            libraryJars = new ClassPath();
        }

        libraryJars.add(jar);
    }

    /**
     * Add the given jar to the list of known in jars
     *
     * @param jar The jar to add.
     */
    public void addInJar(ClassPathEntry jar)
    {
        if (jar == null)
        {
            return;
        }

        if (inJars == null)
        {
            inJars = new ClassPath();
        }

        inJars.add(jar);
    }

    /**
     * Add the given jar to the list of known out jars
     *
     * @param jar The jar to add.
     */
    public void addOutJar(ClassPathEntry jar)
    {
        if (jar == null)
        {
            return;
        }

        if (outJars == null)
        {
            outJars = new ClassPath();
        }

        outJars.add(jar);
    }

    /**
     * Add the given jar to the list of known resource jars
     *
     * @param jar The jar to add.
     */
    public void addResourceJar(ClassPathEntry jar)
    {
        if (jar == null)
        {
            return;
        }

        if (resourceJars == null)
        {
            resourceJars = new ClassPath();
        }

        resourceJars.add(jar);
    }

    /**
     * Adds the given attribute to the list of attributes to keep.
     *
     * @param keepattribute Name of the attribute
     */
    public void addKeepAttribute(String keepattribute)
    {
        if (keepattribute == null)
        {
            return;
        }

        if (keepAttributes == null)
        {
            keepAttributes = new ArrayList();
        }

        keepAttributes.add(keepattribute);
    }

    /**
     * Adds a class file specification to the specifications to keep.
     *
     * @param keepClassFileOption The class specification to keep.
     */
    void addKeepClassFileOption(KeepClassFileOption keepClassFileOption)
    {
        if (keepClassFileOption == null)
        {
            return;
        }

        if (keepClassFileOptions == null)
        {
            keepClassFileOptions = new ArrayList();
        }

        keepClassFileOptions.add(keepClassFileOption);
    }

    /**
     * Merge the configuration of the given configuration file into this one.
     * Settings for variables of type boolean must have been set via asetter
     * method. Lists are added, other settings of the given options override
     * settings in this options.<br>
     * This feature is just needed by Ant. A priori, the Ant task does not
     * know if the user specifies a configuration file. In order to override
     * settings of a given configuration file another object must be used.
     * These settings must be overridden (merged) with the settings of the
     * ProGuard task.
     *
     * @param options The options to merge.
     */
    public void merge(AntConfiguration options)
    {
        if (options == null)
        {
            return;
        }

        if (options.verboseSet)
        {
            verbose = options.verbose;
        }

        if (options.ignoreWarningsSet)
        {
            ignoreWarnings = options.ignoreWarnings;
        }

        if (options.warnSet)
        {
            warn = options.warn;
        }

        if (options.noteSet)
        {
            note = options.note;
        }

        if (options.shrinkSet)
        {
            shrink = options.shrink;
        }

        if (options.obfuscateSet)
        {
            obfuscate = options.obfuscate;
        }

        if (options.useMixedCaseClassNamesSet)
        {
            useMixedCaseClassNames = options.useMixedCaseClassNames;
        }

        if (options.overloadAggressivelySet)
        {
            overloadAggressively = options.overloadAggressively;
        }

        if (options.skipNonPublicLibraryClassesSet)
        {
            skipNonPublicLibraryClasses = options.skipNonPublicLibraryClasses;
        }

        if (options.libraryJars != null)
        {
            if (libraryJars == null)
            {
                libraryJars = options.libraryJars;
            }
            else
            {
                libraryJars.addAll(options.libraryJars);
            }
        }

        if (options.inJars != null)
        {
            if (inJars == null)
            {
                inJars = options.inJars;
            }
            else
            {
                inJars.addAll(options.inJars);
            }
        }

        if (options.outJars != null)
        {
            if (outJars == null)
            {
                outJars = options.outJars;
            }
            else
            {
                outJars.addAll(options.outJars);
            }
        }

        if (options.keepClassFileOptions != null)
        {
            if (keepClassFileOptions == null)
            {
                keepClassFileOptions = options.keepClassFileOptions;
            }
            else
            {
                keepClassFileOptions.addAll(options.keepClassFileOptions);
            }
        }

        if (options.keepAttributes != null)
        {
            if (keepAttributes == null)
            {
                keepAttributes = options.keepAttributes;
            }
            else
            {
                keepAttributes.addAll(options.keepAttributes);
            }
        }

        if (options.newSourceFileAttribute != null)
        {
            newSourceFileAttribute = options.newSourceFileAttribute;
        }

        if (options.printSeeds != null)
        {
            printSeeds = options.printSeeds;
        }

        if (options.printUsage != null)
        {
            printUsage = options.printUsage;
        }

        if (options.printMapping != null)
        {
            printMapping = options.printMapping;
        }

        if (options.applyMapping != null)
        {
            applyMapping = options.applyMapping;
        }

        if (options.dump != null)
        {
            dump = options.dump;
        }

        if (options.defaultPackage != null)
        {
            defaultPackage = options.defaultPackage;
        }
    }
}
