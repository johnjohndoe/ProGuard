/* $Id: DataEntryReader.java,v 1.3 2003/03/25 20:08:53 eric Exp $
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
 * This interface provides methods for reading data entries, i.e. ZIP entries
 * and files. The implementation determines what to do with the read data,
 * if anything.
 *
 * @author Eric Lafortune
 */
public interface DataEntryReader
{
    /**
     * Reads the given ZIP entry, originating from the given input stream.
     */
    public void readZipEntry(ZipEntry    zipEntry,
                             InputStream inputStream)
    throws IOException;


    /**
     * Reads the given file, originating from the hierarchy of the given base
     * directory.
     */
    public void readFile(File file,
                         File directory)
    throws IOException;
}
