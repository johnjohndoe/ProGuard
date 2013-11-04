/* $Id: ZipEntryWriter.java,v 1.2 2002/11/03 13:30:14 eric Exp $
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
 * This interface provides methods for writing ZIP entries.
 *
 * @author Eric Lafortune
 */
public interface ZipEntryWriter
{
    /**
     * Writes the given ZIP entry header and returns a stream for writing
     * the actual data.
     */
    public OutputStream openZipEntry(ZipEntry zipEntry) throws IOException;


    /**
     * Closes the previously opened ZIP entry.
     */
    public void closeZipEntry() throws IOException;
}
