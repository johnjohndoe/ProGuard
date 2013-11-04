/* $Id: JarReader.java,v 1.5 2002/05/12 13:33:41 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.classfile.visitor;

import proguard.classfile.*;

import java.io.*;
import java.util.zip.*;
import java.util.jar.*;


/**
 * This class can read a given jar, applying a given ClassFileVisitor to all
 * class files it reads. The visitor can for instance collect the class files,
 * or perform some operation on them.
 * <p>
 * Class files are read as ProgramClassFile objects or LibraryClassFile objects,
 * depending on the <code>isLibrary</code> flag. In case of libraries, only
 * public class files are considered.
 *
 * @author Eric Lafortune
 */
public class JarReader
{
    private String   jarFileName;
    private boolean  isLibrary;
    private Manifest manifest;


    public JarReader(String jarFileName, boolean isLibrary)
    {
        this.jarFileName = jarFileName;
        this.isLibrary   = isLibrary;
    }


    public void setJarFileName(String jarFileName)
    {
        this.jarFileName = jarFileName;
    }

    public String getJarFileName()
    {
        return jarFileName;
    }


    public void setLibrary(boolean isLibrary)
    {
        this.isLibrary = isLibrary;
    }

    public boolean isLibrary()
    {
        return isLibrary;
    }


    /**
     * Returns the Manifest from the most recently read jar file.
     */
    public Manifest getManifest()
    {
        return manifest;
    }


    /**
     * Reads the given jar, applying the given ClassFileVisitor to all class
     * files that are read.
     */
    public void classFilesAccept(ClassFileVisitor classFileVisitor)
    throws IOException
    {
        if (isLibrary)
        {
            // We filter library class files, only keeping public ones.
            // E.g. about 60% of all rt.jar classes.
            classFileVisitor = new FilteredClassFileVisitor(classFileVisitor,
                                                            ClassConstants.INTERNAL_ACC_PUBLIC,
                                                            0);
        }

        JarInputStream jarInputStream = null;

        try
        {
            jarInputStream = new JarInputStream(
                             new BufferedInputStream(
                             new FileInputStream(
                             new File(jarFileName))));

            // Get all entries from the input jar.
            while (true)
            {
                // Is there another file?
                ZipEntry inEntry = jarInputStream.getNextEntry();
                if (inEntry == null)
                {
                    break;
                }

                // Is it a class file?
                String name = inEntry.getName();
                if (name.endsWith(ClassConstants.CLASS_FILE_EXTENSION))
                {
                    // Create a full internal representation of the class file
                    DataInputStream inStream = new DataInputStream(jarInputStream);

                    try
                    {
                        // Create a ClassFile representation.
                        ClassFile classFile = isLibrary ?
                            (ClassFile)LibraryClassFile.create(inStream) :
                            (ClassFile)ProgramClassFile.create(inStream);

                        // Apply the visitor.
                        classFile.accept(classFileVisitor);
                    }
                    catch (Exception ex)
                    {
                      ex.printStackTrace();
                      System.err.println("Corrupt class ["+name+"] in jar ["+jarFileName+"]");
                    }
                }

                jarInputStream.closeEntry();
            }

            manifest = jarInputStream.getManifest();
        }
        finally
        {
            if (jarInputStream != null)
            {
                try
                {
                    jarInputStream.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }
}
