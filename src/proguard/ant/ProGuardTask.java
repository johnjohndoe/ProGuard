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

import org.apache.tools.ant.*;

import proguard.*;


/**
 * Ant support for ProGuard. This class is the main task for all ProGuard
 * activities in Ant.
 *
 * @author Dirk Schnelle
 */
public class ProGuardTask
        extends ProGuardConfigurationTask
{
    /** The options to configure. */
    private AntConfiguration optionsFromConfigFile;

    /**
     * Creates a new task for ProGuard.
     */
    public ProGuardTask() {}

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
        setFile(file);
    }

    /**
     * Executes this task.
     *
     * @throws BuildException Error executing ProGuard.
     */
    public void execute()
            throws BuildException
    {
        super.execute();

        mergeWithConfigFile();

        if (!isExecutionNecessary())
        {
            log("obfuscated jar is up to date", Project.MSG_VERBOSE);

            return;
        }

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
     * Merges the settings from the configuration file with Ant settings. Ant
     * settings override the settings from the configuration file.
     */
    private void mergeWithConfigFile()
    {
        if (optionsFromConfigFile == null)
        {
            return;
        }

        optionsFromConfigFile.merge(options);

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
        if (options.outJars == null)
        {
            log("no outjar specified: execution necessary", Project.MSG_VERBOSE);

            return true;
        }

        long outjarLastModified = getLastModified(options.outJars);

        if (outjarLastModified == -1)
        {
            return true;
        }

        // I do not know how to handle this. Delegate the decision about
        // it to ProGuard.
        if ((options.inJars == null) && (options.resourceJars == null))
        {
            return true;
        }

        if (checkJarContainerForDate(options.inJars, outjarLastModified))
        {
            return true;
        }

        return (checkJarContainerForDate(options.resourceJars,
            outjarLastModified));
    }

    /**
     * Checks the files in the given jar container if they contain changes
     * since the last run of ProGuard.
     *
     * @param classPath A classpath.
     * @param lastModified Date of the last ProGuard generated jar.
     *
     * @return true, if changes have been made to at least one file.
     *
     * @throws BuildException An entry of the jar container does not exist.
     */
    private boolean checkJarContainerForDate(
        ClassPath classPath,
        long      lastModified)
            throws BuildException
    {
        if (classPath == null)
        {
            return false;
        }

        final ClassPathIterator iterator = new ClassPathIterator(classPath);

        while (iterator.hasNext())
        {
            final ClassPathEntry classPathEntry = iterator.nextClassPathEntry();
            final String         jar     = classPathEntry.getName();
            final File           jarFile = new File(jar);

            if (!jarFile.exists())
            {
                throw new BuildException("file or directory'" + jar +
                    "' does not exist!");
            }

            if (jarFile.isDirectory())
            {
                if (checkSubDirForDate(jarFile, lastModified))
                {
                    return true;
                }
            }
            else if (jarFile.lastModified() > lastModified)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the minimal date of the outjars.
     *
     * @param classPath Entries to search in.
     *
     * @return Date of the newest file, -1 if the date could not be determined.
     */
    long getLastModified(ClassPath classPath)
    {
        if (classPath == null)
        {
            return -1;
        }

        ClassPathIterator iterator = new ClassPathIterator(classPath);
        long              lastModified = -1;

        while (iterator.hasNext())
        {
            final ClassPathEntry classPathEntry = iterator.nextClassPathEntry();
            final String         jar     = classPathEntry.getName();
            final File           jarFile = new File(jar);

            if (jarFile.exists())
            {
                if (jarFile.isDirectory())
                {
                    long lastModifiedSub = getLastModifiedFromSubDir(jarFile);

                    if (lastModifiedSub > lastModified)
                    {
                        lastModified = lastModifiedSub;
                    }
                }
                else
                {
                    if (jarFile.lastModified() > lastModified)
                    {
                        lastModified = jarFile.lastModified();
                    }
                }
            }
        }

        return lastModified;
    }

    /**
     * Get the time stamp of the newest file in the given directory
     *
     * @param directory The directory to search in.
     *
     * @return Time stamp of the new set file in the directory, -1 if the time
     *         stamp could not be determined.
     */
    private long getLastModifiedFromSubDir(File directory)
    {
        File[] children     = directory.listFiles();
        int    size         = children.length;
        long   lastModified = -1;

        for (int i = 0; i < size; i++)
        {
            final File actChild = children[i];

            if (actChild.isDirectory())
            {
                long lastModifiedSub = getLastModifiedFromSubDir(actChild);
                if (lastModifiedSub > lastModified)
                {
                    lastModified = lastModifiedSub;
                }
            }
            else
            {
                if (actChild.lastModified() > lastModified)
                {
                    lastModified = actChild.lastModified();
                }
            }
        }

        return lastModified;
    }

    /**
     * Checks the files in the given directory if they contain changes since
     * the last run of proguard.
     *
     * @param directory The directory to scan.
     * @param lastModified Date of the last proguard generated jar.
     *
     * @return true, if changes have been made to at least one file.
     */
    private boolean checkSubDirForDate(
        File directory,
        long lastModified)
    {
        File[] children = directory.listFiles();
        int    size = children.length;

        for (int i = 0; i < size; i++)
        {
            final File actChild = children[i];

            if (actChild.isDirectory())
            {
                if (checkSubDirForDate(actChild, lastModified))
                {
                    return true;
                }
            }
            else
            {
                if (actChild.lastModified() > lastModified)
                {
                    return true;
                }
            }
        }

        return false;
    }
}
