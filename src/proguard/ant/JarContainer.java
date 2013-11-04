/* $Id: JarContainer.java,v 1.10 2003/12/19 04:17:03 eric Exp $
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
 * ANY WARRANTY; without even the implied warranty of MERCHAntABILITY or
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

import proguard.*;


/**
 * Handler for some jar files that are added to a configuration list.
 *
 * @author Dirk Schnelle
 */
public abstract class JarContainer
        implements Subtask
{
    /** Flag that a jar was provided. */
    private boolean gotJarFile;

    /** The parent task. */
    protected ProGuardConfigurationTask proGuardConfiguration;

    /** The ant project. */
    protected Project project;

    /** The configured file sets. */
    protected Collection fileSets;

    /** Reference to a path. */
    protected Reference reference;

    /** The class path entry, if directly set. */
    protected ClassPathEntry classpathentry;

    /**
     * Creates a new JarContainer.
     */
    protected JarContainer()
    {
        fileSets       = new ArrayList();
        gotJarFile     = false;
    }

    /**
     * Adds the fileSet of jars to the jars.
     *
     * @param fileSet The jars to be added.
     */
    public void addFileset(FileSet fileSet)
    {
        isJarFileSet();
        gotJarFile = true;
        fileSets.add(fileSet);
    }

    /**
     * Sets the parent task.
     *
     * @param configuration The parent task.
     */
    void setParent(ProGuardConfigurationTask configuration)
    {
        this.proGuardConfiguration = configuration;
    }

    /**
     * Adds the found jar.
     *
     * @param jar jar to add.
     */
    protected abstract void addJar(ClassPathEntry jar);

    /**
     * Sets the ant project
     *
     * @param project The ant project.
     */
    void setProject(Project project)
    {
        this.project = project;
    }

    /**
     * Sets the ID of a path reference.
     *
     * @param reference Reference to a path.
     */
    public void setRefid(Reference reference)
    {
        isJarFileSet();
        gotJarFile         = true;
        this.reference     = reference;
    }

    /**
     * Get a class path entry for the given filen name.
     *
     * @param name Name of the jar file.
     *
     * @return Corresponding class path entry.
     */
    protected ClassPathEntry getClassPathEntry(String name)
    {
        String fullName = proGuardConfiguration.getFullPathName(name);

        return new ClassPathEntry(fullName);
    }

    /**
     * Adds a jar file.
     *
     * @param jar Name of the jar file to add.
     */
    public void setName(String jar)
    {
        isJarFileSet();
        gotJarFile     = true;

        classpathentry = getClassPathEntry(jar);
        addJar(classpathentry);
    }

    /**
     * Add an exclude filter.
     *
     * @param filter Exclude filter.
     */
    public void addExclude(JarContainerExclude filter)
    {
        filter.setParent(this);
    }

    /**
     * Add the given filter to the class path entry.
     *
     * @param filter Filter to be added.
     */
    private void addFilter(String filter)
    {
        String currentFilter = classpathentry.getFilter();

        if (currentFilter == null)
        {
            currentFilter = "";
        }
        else if (currentFilter.length() > 0)
        {
            currentFilter = currentFilter + ",";
        }

        classpathentry.setFilter(currentFilter + filter);
    }

    /**
     * Apply the exclude filter.
     *
     * @param filter Filter for exclusion.
     *
     * @throws BuildException There is no single jar.
     */
    void exclude(String filter)
    {
        if (classpathentry == null)
        {
            throw new BuildException(
                "Exclude filters require direct setting of a jar.");
        }

        final String excludeFilter = "!" + filter;
        proGuardConfiguration.log("excluding '" + excludeFilter + "'",
            Project.MSG_VERBOSE);

        addFilter(excludeFilter);
    }

    /**
     * Add an include filter.
     *
     * @param filter Include filter.
     */
    public void addInclude(JarContainerInclude filter)
    {
        filter.setParent(this);
    }

    /**
     * Apply the include filter.
     *
     * @param filter Filter for inclusion.
     *
     * @throws BuildException There is no single jar.
     */
    void include(String filter)
    {
        if (classpathentry == null)
        {
            throw new BuildException(
                "Include filters require direct setting of a jar.");
        }

        proGuardConfiguration.log("including '" + filter + "'",
            Project.MSG_VERBOSE);

        addFilter(filter);
    }

    /**
     * Executes this subtask for the given parent task.
     *
     * @param parent Parent task object.
     */
    public void execute(ProGuardConfigurationTask parent)
    {
        setProject(parent.getProject());

        // Must call evalReference before evalFileset because
        // a fileset could be added by a reference.
        evalReference();
        evalFilesets();
    }

    /**
     * Evaluates a given referenced path.
     *
     * @throws BuildException Reference to unsupported object type.
     */
    private void evalReference()
    {
        if (reference == null)
        {
            return;
        }

        Object referencedObject = reference.getReferencedObject(project);

        if (referencedObject instanceof FileSet)
        {
            fileSets.add((FileSet) referencedObject);
        }
        else if (referencedObject instanceof Path)
        {
            Path     path = (Path) referencedObject;
            String[] list = path.list();

            for (int i = 0; i < list.length; i++)
            {
                ClassPathEntry entry = getClassPathEntry(list[i]);
                addJar(entry);
            }
        }
        else
        {
            throw new BuildException("Reference '" + reference.getRefId() +
                "' must be a fileset or a path");
        }
    }

    /**
     * Evaluates the given fileSets.
     */
    private void evalFilesets()
    {
        Iterator iterator = fileSets.iterator();

        while (iterator.hasNext())
        {
            FileSet          fileSet = (FileSet) iterator.next();

            DirectoryScanner directoryscanner =
                fileSet.getDirectoryScanner(project);
            String[]         libfiles = directoryscanner.getIncludedFiles();
            int              size     = libfiles.length;

            for (int i = 0; i < size; i++)
            {
                String libfile =
                    directoryscanner.getBasedir() + File.separator +
                    libfiles[i];

                ClassPathEntry entry = getClassPathEntry(libfile);
                addJar(entry);
            }
        }
    }

    /**
     * Checks if a jar file has been alread set.
     *
     * @exception BuildException A jar file has been already set.
     */
    private void isJarFileSet()
            throws BuildException
    {
        if (gotJarFile)
        {
            throw new BuildException(
                "take multiple injar or libraryjar tasks to set a jar file in several ways!");
        }
    }

    /**
     * Validates this subtask.
     *
     * @exception BuildException Validation not successful.
     */
    public void validate()
            throws BuildException {}
}
