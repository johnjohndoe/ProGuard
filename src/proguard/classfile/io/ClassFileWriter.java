/* $Id: ClassFileWriter.java,v 1.4 2003/12/06 22:15:38 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.classfile.io;

import proguard.classfile.*;
import proguard.classfile.visitor.*;

import java.io.*;


/**
 * This ClassFileVisitor writes all ProgramClassFile objects it visits
 * to a given DataEntryWriter.
 *
 * @author Eric Lafortune
 */
public class ClassFileWriter implements ClassFileVisitor
{
    private DataEntryWriter dataEntryWriter;


    public ClassFileWriter(DataEntryWriter dataEntryWriter)
    {
        this.dataEntryWriter = dataEntryWriter;
    }


    // Implementations for ClassFileVisitor.

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        String name = programClassFile.getName() + ClassConstants.CLASS_FILE_EXTENSION;

        try
        {
            // Open the ZIP entry output stream.
            OutputStream outputStream = dataEntryWriter.openDataEntry(name);
            if (outputStream != null)
            {
                // Write the class file using a DataOutputStream.
                DataOutputStream classOutputStream =
                    new DataOutputStream(outputStream);
                programClassFile.write(classOutputStream);
                classOutputStream.flush();

                // Close the data entry.
                dataEntryWriter.closeDataEntry();
            }
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException("Can't write class file [" + name + "] (" + ex.getMessage() + ")");
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
    }
}
