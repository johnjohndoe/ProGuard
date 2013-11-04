/* $Id: JarWriter.java,v 1.11 2003/12/06 22:15:38 eric Exp $
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

import java.io.*;
import java.util.jar.*;
import java.util.*;


/**
 * This DataEntryWriter sends data entries to a given jar file.
 * The manifest and comment properties can optionally be set.
 *
 * @author Eric Lafortune
 */
public class JarWriter implements DataEntryWriter
{
    private JarOutputStream jarOutputStream;

    // The jar entries that are already in the jar.
    private Set jarEntryNames = new HashSet();


    /**
     * Creates a new JarWriter.
     * @param jarFile  the jar file to which all data entries will be written.
     * @param manifest an optional jar manifest.
     * @param comment  an optional zip comment.
     */
    public JarWriter(File     jarFile,
                     Manifest manifest,
                     String   comment) throws IOException
    {
        OutputStream outputStream =
            new BufferedOutputStream(
            new FileOutputStream(jarFile));

        jarOutputStream = manifest != null ?
            new JarOutputStream(outputStream, manifest) :
            new JarOutputStream(outputStream);

        if (comment != null)
        {
            jarOutputStream.setComment(comment);
        }
    }


    // Implementations for DataEntryWriter.

    public void close() throws IOException
    {
        if (jarOutputStream != null)
        {
            jarOutputStream.close();

            // Clear the references to the objects that we don't need anymore.
            jarOutputStream = null;
            jarEntryNames   = null;
        }
    }


    public OutputStream openDataEntry(String name) throws IOException
    {
        // We have to check if the name is already used, because ZipOutputStream
        // doesn't handle this case properly (it throws an exception which can
        // be caught, but the ZipEntry is remembered anyway).
        if (!jarEntryNames.add(name))
        {
            throw new IOException("Duplicate zip entry ["+name+"]");
        }

        jarOutputStream.putNextEntry(new JarEntry(name));

        return jarOutputStream;
    }


    public void closeDataEntry() throws IOException
    {
        jarOutputStream.closeEntry();
    }
}
