/* $Id: DataEntryCopier.java,v 1.9 2003/12/06 22:15:38 eric Exp $
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

import java.io.*;
import java.util.zip.*;


/**
 * This DataEntryReader writes the ZIP entries and files that it reads to a
 * given DataEntryWriter.
 *
 * @author Eric Lafortune
 */
public class DataEntryCopier implements DataEntryReader
{
    private static final int BUFFER_SIZE = 1024;

    private DataEntryWriter dataEntryWriter;
    private byte[]          buffer = new byte[BUFFER_SIZE];



    public DataEntryCopier(DataEntryWriter dataEntryWriter)
    {
        this.dataEntryWriter = dataEntryWriter;
    }


    // Implementations for DataEntryReader.

    public void readZipEntry(ZipEntry    zipEntry,
                             InputStream inputStream)
    throws IOException
    {
        String name = zipEntry.getName();

        try
        {
            // Open the data entry output stream.
            OutputStream outputStream = dataEntryWriter.openDataEntry(name);
            if (outputStream != null)
            {
                // Copy the data from the input stream to the output stream.
                copyData(inputStream, outputStream);

                // Close the data entry.
                dataEntryWriter.closeDataEntry();
            }
        }
        catch (IOException ex)
        {
            System.err.println("Warning: can't write resource zip entry [" + name + "] (" + ex.getMessage() + ")");
        }
    }


    public void readFile(File file,
                         File directory)
    throws IOException
    {
        // Chop the directory name from the file name and get the right separators.
        String name = file.getPath()
            .substring(directory.getPath().length() + File.separator.length())
            .replace(File.separatorChar, ClassConstants.INTERNAL_PACKAGE_SEPARATOR);

        // Open the file input stream.
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        try
        {
            // Open the data entry output stream.
            OutputStream outputStream = dataEntryWriter.openDataEntry(name);
            if (outputStream != null)
            {
                // Copy the data from the input stream to the output stream.
                copyData(inputStream, outputStream);

                // Close the data entry.
                dataEntryWriter.closeDataEntry();
            }
        }
        catch (IOException ex)
        {
            System.err.println("Warning: can't write resource file [" + name + "] (" + ex.getMessage() + ")");
        }
        finally
        {
            // Close the file input stream.
            inputStream.close();
        }
    }


    // Small utility methods.

    /**
     * Copies all data that it can read from the given input stream to the
     * given output stream.
     */
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
