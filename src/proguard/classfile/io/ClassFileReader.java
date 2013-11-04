/* $Id: ClassFileReader.java,v 1.2 2003/02/09 15:22:28 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
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
 * This class applies a given ClassFileVisitor to the class file definitions
 * entries that it reads.
 * <p>
 * Class files are read as ProgramClassFile objects or LibraryClassFile objects,
 * depending on the <code>isLibrary</code> flag.
 * <p>
 * In case of libraries, only public class files may be considered, depending on
 * the <code>skipNonPublicLibraryClasses</code> flag.
 *
 * @author Eric Lafortune
 */
public class ClassFileReader
{
    private ClassFileVisitor classFileVisitor;
    private boolean          isLibrary;
    private boolean          skipNonPublicLibraryClasses;


    /**
     * Creates a new DataEntryClassFileFilter for reading ProgramClassFile objects.
     */
    public ClassFileReader(ClassFileVisitor classFileVisitor)
    {
        this(classFileVisitor, false, false);
    }


    /**
     * Creates a new DataEntryClassFileFilter for reading the specified
     * ClassFile objects.
     */
    public ClassFileReader(ClassFileVisitor classFileVisitor,
                           boolean          isLibrary,
                           boolean          skipNonPublicLibraryClasses)
    {
        this.classFileVisitor            = classFileVisitor;
        this.isLibrary                   = isLibrary;
        this.skipNonPublicLibraryClasses = skipNonPublicLibraryClasses;
    }


    /**
     * Reads a ClassFile from the given input stream and applies the
     * ClassFileVisitor to it.
     */
    public void readData(String name, InputStream inputStream)
    throws IOException
    {
        try
        {
            // Get the proper type of input stream.
            DataInputStream inStream = new DataInputStream(inputStream);

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
