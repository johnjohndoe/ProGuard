/* $Id: DataEntryWriter.java,v 1.3 2003/08/22 17:11:15 eric Exp $
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

import java.io.*;


/**
 * This interface provides methods for writing data entries, such as ZIP entries
 * of files. The implementation determines to which type of data entry the
 * data will be written.
 *
 * @author Eric Lafortune
 */
public interface DataEntryWriter
{
    /**
     * Finishes writing data entries.
     */
    public void close() throws IOException;


    /**
     * Prepares writing a data entry and returns a stream for writing the actual
     * data. The stream may be <code>null</code> to indicate that the data entry
     * should not be written.
     */
    public OutputStream openDataEntry(String name) throws IOException;


    /**
     * Closes the previously opened data entry.
     */
    public void closeDataEntry() throws IOException;
}
