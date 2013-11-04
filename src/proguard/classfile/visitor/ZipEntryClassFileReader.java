/* $Id: ZipEntryClassFileReader.java,v 1.3 2003/01/09 19:48:24 eric Exp $
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
 * This ZipEntryReader applies a given ClassFileVisitor to the class file ZIP
 * entries that it reads. Other ZIP entries are ignored.
 * <p>
 * Class files are read as ProgramClassFile objects or LibraryClassFile objects,
 * depending on the <code>isLibrary</code> flag.
 * <p>
 * In case of libraries, only public class files may be considered, depending on
 * the <code>skipNonPublicLibraryClasses</code> flag.
 *
 * @author Eric Lafortune
 */
public class ZipEntryClassFileReader implements ZipEntryReader
{
    private ClassFileVisitor classFileVisitor;
    private boolean          isLibrary;
    private boolean          skipNonPublicLibraryClasses;


    /**
     * Creates a new ZipEntryClassFileReader for reading ProgramClassFile objects.
     */
    public ZipEntryClassFileReader(ClassFileVisitor classFileVisitor)
    {
        this(classFileVisitor, false, false);
    }


    /**
     * Creates a new ZipEntryClassFileReader for reading the specified
     * ClassFile objects.
     */
    public ZipEntryClassFileReader(ClassFileVisitor classFileVisitor,
                                   boolean          isLibrary,
                                   boolean          skipNonPublicLibraryClasses)
    {
        this.classFileVisitor            = classFileVisitor;
        this.isLibrary                   = isLibrary;
        this.skipNonPublicLibraryClasses = skipNonPublicLibraryClasses;
    }


    // Implementations for ZipEntryReader

    public void readZipEntry(ZipEntry    zipEntry,
                             InputStream inputStream)
    throws IOException
    {
        // Is it a class file?
        String name = zipEntry.getName();
        if (!zipEntry.isDirectory() &&
            name.endsWith(ClassConstants.CLASS_FILE_EXTENSION))
        {
            // Create a full internal representation of the class file
            DataInputStream inStream = new DataInputStream(inputStream);

            try
            {
                // Create a ClassFile representation.
                ClassFile classFile = isLibrary ?
                    (ClassFile)LibraryClassFile.create(inStream, skipNonPublicLibraryClasses) :
                    (ClassFile)ProgramClassFile.create(inStream);

                // Apply the visitor.
                if (classFile != null)
                {
                    classFile.accept(classFileVisitor);
                }
            }
            catch (Exception ex)
            {
                throw new IOException("Can't process class file ["+name+"] ("+ex.getMessage()+")");
            }
        }
    }
}
