/* $Id: ZipEntryClassFileWriter.java,v 1.2 2002/11/03 13:30:14 eric Exp $
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
import java.util.zip.ZipEntry;


/**
 * This ClassFileVisitor writes all ProgramClassFile objects it visits
 * to a given ZipEntryWriter.
 *
 * @author Eric Lafortune
 */
public class ZipEntryClassFileWriter implements ClassFileVisitor
{
    private ZipEntryWriter zipEntryWriter;


    public ZipEntryClassFileWriter(ZipEntryWriter zipEntryWriter)
    {
        this.zipEntryWriter = zipEntryWriter;
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        try
        {
            // Create a new ZIP entry.
            String className = programClassFile.getName() + ClassConstants.CLASS_FILE_EXTENSION;
            ZipEntry zipEntry = new ZipEntry(className);

            // Open the ZIP entry output stream.
            OutputStream outputStream = zipEntryWriter.openZipEntry(zipEntry);

            // Write the class file using a DataOutputStream.
            DataOutputStream classOutputStream = new DataOutputStream(outputStream);
            programClassFile.write(classOutputStream);
            classOutputStream.flush();

            // Close the ZIP entry.
            zipEntryWriter.closeZipEntry();
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
