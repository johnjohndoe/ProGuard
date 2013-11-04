/* $Id: JarWriter.java,v 1.6 2002/11/03 13:30:14 eric Exp $
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

import java.io.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;


/**
 * This ZipEntryWriter writes its entries to a given jar file that can be
 * opened and closed.
 * <p>
 * The jar file name must be set and the <code>open</code> method
 * must be called before visiting. The <code>close</code> method must be called
 * after visiting. The manifest and comment properties can optionally be set.
 *
 * @author Eric Lafortune
 */
public class JarWriter implements ZipEntryWriter
{
    private String          jarFileName;
    private Manifest        manifest;
    private String          comment;
    private JarOutputStream jarOutputStream;


    public JarWriter(String   jarFileName,
                     Manifest manifest,
                     String   comment)
    {
        this.jarFileName = jarFileName;
        this.manifest    = manifest;
        this.comment     = comment;
    }


    public void open() throws IOException
    {
        OutputStream outputStream =
            new BufferedOutputStream(
            new FileOutputStream(
            new File(jarFileName)));

        jarOutputStream = manifest != null ?
            new JarOutputStream(outputStream, manifest) :
            new JarOutputStream(outputStream);

        if (comment != null)
        {
            jarOutputStream.setComment(comment);
        }
    }


    public void close() throws IOException
    {
        if (jarOutputStream != null)
        {
            jarOutputStream.close();
        }

        jarOutputStream = null;
    }


    // Implementations for ZipEntryWriter

    public OutputStream openZipEntry(ZipEntry zipEntry) throws IOException
    {
        jarOutputStream.putNextEntry(zipEntry);

        return jarOutputStream;
    }


    public void closeZipEntry() throws IOException
    {
        jarOutputStream.closeEntry();
    }
}
