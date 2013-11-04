/* $Id: FilteredDataEntryReader.java,v 1.4 2003/12/06 22:15:38 eric Exp $
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

import proguard.util.*;

import java.io.*;
import java.util.zip.*;


/**
 * This DataEntryReader delegates to one of two other DataEntryReader instances,
 * depending on whether the name of the data entries match a given regular
 * expression or not.
 *
 * @author Eric Lafortune
 */
public class FilteredDataEntryReader implements DataEntryReader
{
    private RegularExpressionMatcher regularExpressionMatcher;
    private DataEntryReader          matchingDataEntryReader;
    private DataEntryReader          nonMatchingDataEntryReader;


    /**
     * Creates a new FilteredDataEntryReader.
     * @param regularExpressionMatcher   the regular expression for the data entry
     *                                   names.
     * @param matchingDataEntryWriter    the DataEntryReader to which the reading
     *                                   will be delegated if the data entry name
     *                                   matches the regular expression. May be
     *                                   <code>null</code>.
     * @param nonMatchingDataEntryWriter the DataEntryReader to which the reading
     *                                   will be delegated if the data entry name
     *                                   does not match the regular expression.
     *                                   May be <code>null</code>.
     */
    public FilteredDataEntryReader(RegularExpressionMatcher regularExpressionMatcher,
                                   DataEntryReader          matchingDataEntryReader,
                                   DataEntryReader          nonMatchingDataEntryReader)
    {
        this.regularExpressionMatcher   = regularExpressionMatcher;
        this.matchingDataEntryReader    = matchingDataEntryReader;
        this.nonMatchingDataEntryReader = nonMatchingDataEntryReader;
    }


    // Implementations for DataEntryReader.

    public void readZipEntry(ZipEntry    inEntry,
                             InputStream inputStream)
    throws IOException
    {
        if (regularExpressionMatcher.matches(inEntry.getName()))
        {
            if (matchingDataEntryReader != null)
            {
                matchingDataEntryReader.readZipEntry(inEntry, inputStream);
            }
        }
        else
        {
            if (nonMatchingDataEntryReader != null)
            {
                nonMatchingDataEntryReader.readZipEntry(inEntry, inputStream);
            }
        }
    }


    public void readFile(File file,
                         File directory)
    throws IOException
    {
        // Chop the directory name from the file name.
        String name = file.getPath()
            .substring(directory.getPath().length() + File.separator.length());

        if (regularExpressionMatcher.matches(name))
        {
            if (matchingDataEntryReader != null)
            {
                matchingDataEntryReader.readFile(file, directory);
            }
        }
        else
        {
            if (nonMatchingDataEntryReader != null)
            {
                nonMatchingDataEntryReader.readFile(file, directory);
            }
        }
    }
}
