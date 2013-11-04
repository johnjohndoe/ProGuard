/* $Id: JarWriter.java,v 1.3 2002/05/12 13:33:41 eric Exp $
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
 * This ClassFileVisitor can write all ProgramClassFile objects it visits to a
 * given jar file.
 *
 * @author Eric Lafortune
 */
public class JarWriter implements ClassFileVisitor
{
    private String          jarFileName;
    private Manifest        manifest;
    private String          comment;
    private JarOutputStream jarOutputStream;


    public JarWriter(String jarFileName)
    {
        this.jarFileName = jarFileName;
    }


    public void setJarFileName(String jarFileName)
    {
        this.jarFileName = jarFileName;
    }

    public String getJarFileName()
    {
        return jarFileName;
    }


    public void setManifest(Manifest manifest)
    {
        this.manifest = manifest;
    }

    public Manifest getManifest()
    {
        return manifest;
    }


    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getComment()
    {
        return comment;
    }


    public void open() throws IOException
    {
        jarOutputStream = new JarOutputStream(
                          new BufferedOutputStream(
                          new FileOutputStream(
                          new File(jarFileName))),
                          manifest);

        jarOutputStream.setComment(comment);
    }


    public void close() throws IOException
    {
        if (jarOutputStream != null)
        {
            jarOutputStream.close();
        }

        jarOutputStream = null;
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        try
        {
            String className = programClassFile.getName() + ClassConstants.CLASS_FILE_EXTENSION;

            ZipEntry outEntry = new ZipEntry(className);
            jarOutputStream.putNextEntry(outEntry);

            // Write the class file using a DataOutputStream.
            DataOutputStream classOutputStream = new DataOutputStream(jarOutputStream);
            programClassFile.write(classOutputStream);
            classOutputStream.flush();

            jarOutputStream.closeEntry();
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
    }
}
