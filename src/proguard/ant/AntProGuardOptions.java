/* $Id: AntProGuardOptions.java,v 1.4 2003/05/03 11:55:45 eric Exp $
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

import java.util.ArrayList;
import proguard.KeepCommand;
import proguard.ProGuardOptions;


/**
 * A wrapper around the ProGuard options to add settings. This class provides
 * some convenient methods to add items to the list settings without caring
 * about an uninitialized list. In addition, it provides a method to merge the
 * settings of two option-objects.
 *
 * @author $author$
 * @version $Revision: 1.4 $
 */
public class AntProGuardOptions
        extends ProGuardOptions
{
    /**
     * Add the given jar to the list of known library jars
     *
     * @param jar The jar to add.
     */
    public void addLibraryJar(String jar)
    {
        if (libraryJars == null)
        {
            libraryJars = new ArrayList();
        }

        libraryJars.add(jar);
    }

    /**
     * Add the given jar to the list of known in jars
     *
     * @param jar The jar to add.
     */
    public void addInJar(String jar)
    {
        if (inJars == null)
        {
            inJars = new ArrayList();
        }

        inJars.add(jar);
    }

    /**
     * Add the given jar to the list of known resource jars
     *
     * @param jar The jar to add.
     */
    public void addResourceJar(String jar)
    {
        if (resourceJars == null)
        {
            resourceJars = new ArrayList();
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
        if (keepAttributes == null)
        {
            keepAttributes = new ArrayList();
        }

        keepAttributes.add(keepattribute);
    }

    /**
     * Adds a class specification to the specifications to keep.
     *
     * @param keepcommand The class specification to keep.
     */
    void addKeepCommand(KeepCommand keepcommand)
    {
        if (keepCommands == null)
        {
            keepCommands = new ArrayList();
        }

        keepCommands.add(keepcommand);
    }

    /**
     * Merge the configuration of the given configuration file into this one. This
     * does not include settings for variables of type boolean. Lists are added,
     * other settings of the given options override settings in this options.<br>
     * This feature is just needed by Ant. A priori, the Ant task does not
     * know if the user specifies a configuration file. In order to override
     * settings of a given configuration file another object must be used. These
     * settings must be overridden (merged) with the settings of the ProGuard
     * task.
     *
     * @param options The options to merge.
     */
    public void merge(ProGuardOptions options)
    {
        if (options == null)
        {
            return;
        }

        if ((libraryJars == null) && (options.libraryJars != null))
        {
            libraryJars = options.libraryJars;
        }
        else
        {
            libraryJars.addAll(options.libraryJars);
        }

        if ((inJars == null) && (options.inJars != null))
        {
            inJars = options.inJars;
        }
        else
        {
            inJars.addAll(options.inJars);
        }

        if (options.outJar != null)
        {
            outJar = options.outJar;
        }

        if ((keepCommands == null) && (options.keepCommands != null))
        {
            keepCommands = options.keepCommands;
        }
        else
        {
            keepCommands.addAll(options.keepCommands);
        }

        if ((keepAttributes == null) && (options.keepAttributes != null))
        {
            keepAttributes = options.keepAttributes;
        }
        else
        {
            keepAttributes.addAll(options.keepAttributes);
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
