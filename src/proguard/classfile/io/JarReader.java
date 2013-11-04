/* $Id: JarReader.java,v 1.7 2003/12/06 22:15:38 eric Exp $
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
import java.util.zip.*;


/**
 * This DataEntryPump can read a given jar, applying a given DataEntryReader to
 * all ZIP entries it reads.
 *
 * @author Eric Lafortune
 */
public class JarReader implements DataEntryPump
{
    private File jarFile;


    public JarReader(File jarFile)
    {
        this.jarFile = jarFile;
    }


    // Implementation for DataEntryPump.

    public void pumpDataEntries(DataEntryReader dataEntryReader)
    throws IOException
    {
        ZipInputStream zipInputStream = null;

        try
        {
            zipInputStream = new ZipInputStream(
                             new BufferedInputStream(
                             new FileInputStream(jarFile)));

            // Get all entries from the input jar.
            while (true)
            {
                // Can we get another entry?
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                if (zipEntry == null)
                {
                    break;
                }

                // Delegate the actual reading to the data entry reader.
                dataEntryReader.readZipEntry(zipEntry, zipInputStream);

                // Close the entry, so we can continue with the next one.
                zipInputStream.closeEntry();
            }
        }
        finally
        {
            if (zipInputStream != null)
            {
                try
                {
                    zipInputStream.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }
}
