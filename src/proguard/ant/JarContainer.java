/* $Id: JarContainer.java,v 1.6 2003/05/01 18:05:53 eric Exp $
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

import java.io.File;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;


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
    protected ProGuardTask proGuardTask;

    /** The ant project. */
    protected Project project;

    /** The configured file sets. */
    protected Collection fileSets;

    /** Reference to a path. */
    protected Reference reference;

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
     * @param proGuardTask The parent task.
     */
    void setParent(ProGuardTask proGuardTask)
    {
        this.proGuardTask = proGuardTask;
    }

    /**
     * Adds the found jar file.
     *
     * @param jar Name of the jar file to add.
     */
    protected abstract void addJar(String jar);

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
     * Adds a jar file.
     *
     * @param jar Name of the jar file to add.
     */
    public void setName(String jar)
    {
        isJarFileSet();
        gotJarFile = true;
        addJar(jar);
    }

    /**
     * Executes this subtask for the given parent task.
     *
     * @param parent Parent task object.
     */
    public void execute(ProGuardTask parent)
    {
        setProject(parent.getProject());

        evalReference();
        evalFilesets();
    }

    /**
     * Evaluates a given referenced path.
     */
    private void evalReference()
    {
        if (reference == null)
            return;

        Path     path = (Path) reference.getReferencedObject(project);
        String[] list = path.list();

        for (int i = 0; i < list.length; i++)
            addJar(list[i]);
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
                addJar(libfile);
            }
        }
    }

    /**
     * Checks if a jar file has already been set.
     *
     * @exception BuildException A jar file has been already set.
     */
    private void isJarFileSet()
            throws BuildException
    {
        if (gotJarFile)
            throw new BuildException(
                "take multiple injar or libraryjar tasks to set a jar file in several ways!");
    }

    /**
     * Validates this subtask.
     *
     * @exception BuildException Validation not successful.
     */
    public void validate()
            throws BuildException {}
}
