/* $Id: ZipEntryCopier.java,v 1.2 2002/11/03 13:30:14 eric Exp $
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
import java.util.zip.ZipEntry;


/**
 * This ZipEntryReader writes the ZIP entries that it reads to a given
 * ZipEntryWriter.
 *
 * @author Eric Lafortune
 */
public class ZipEntryCopier implements ZipEntryReader
{
    private static final int BUFFER_SIZE = 1024;

    private ZipEntryWriter zipEntryWriter;
    private byte[]         buffer = new byte[BUFFER_SIZE];



    public ZipEntryCopier(ZipEntryWriter zipEntryWriter)
    {
        this.zipEntryWriter = zipEntryWriter;
    }


    // Implementations for ZipEntryReader

    public void readZipEntry(ZipEntry    inEntry,
                             InputStream inputStream)
    throws IOException
    {
        // Create a new ZIP entry.
        ZipEntry outEntry = new ZipEntry(inEntry);

        // Open the ZIP entry output stream.
        OutputStream outputStream = zipEntryWriter.openZipEntry(outEntry);

        // Copy the data from the input stream to the output stream.
        copyData(inputStream, outputStream);

        // Close the ZIP entry.
        zipEntryWriter.closeZipEntry();
    }


    // Small utility methods.

    private void copyData(InputStream  inputStream,
                          OutputStream outputStream)
    throws IOException
    {
        while (true)
        {
            int count = inputStream.read(buffer);
            if (count < 0)
            {
                break;
            }
            outputStream.write(buffer, 0, count);
        }

        outputStream.flush();
    }
}
