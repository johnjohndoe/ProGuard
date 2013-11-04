/* $Id: FilteredDataEntryWriter.java,v 1.3 2003/12/06 22:12:42 eric Exp $
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

import proguard.util.*;

import java.io.*;

/**
 * This DataEntryWriter delegates to one of two other DataEntryWriter instances,
 * depending on whether the name of the data entries match a given regular
 * expression or not.
 *
 * @author Eric Lafortune
 */
public class FilteredDataEntryWriter implements DataEntryWriter
{
    private RegularExpressionMatcher regularExpressionMatcher;
    private DataEntryWriter          matchingDataEntryWriter;
    private DataEntryWriter          nonMatchingDataEntryWriter;

    // A flag to remember to which DataEntryWriter the most recent entry was
    // sent, so we can close it properly.
    private boolean match;


    /**
     * Creates a new FilteredDataEntryWriter.
     * @param regularExpressionMatcher   the regular expression for the data entry
     *                                   names.
     * @param matchingDataEntryWriter    the DataEntryWriter to which the writing
     *                                   will be delegated if the data entry name
     *                                   matches the regular expression. May be
     *                                   <code>null</code>.
     * @param nonMatchingDataEntryWriter the DataEntryWriter to which the writing
     *                                   will be delegated if the data entry name
     *                                   does not match the regular expression.
     *                                   May be <code>null</code>.
     */
    public FilteredDataEntryWriter(RegularExpressionMatcher regularExpressionMatcher,
                                   DataEntryWriter          matchingDataEntryWriter,
                                   DataEntryWriter          nonMatchingDataEntryWriter)
    {
        this.regularExpressionMatcher   = regularExpressionMatcher;
        this.matchingDataEntryWriter    = matchingDataEntryWriter;
        this.nonMatchingDataEntryWriter = nonMatchingDataEntryWriter;
    }

    /**
     * Finishes writing data entries.
     */
    public void close()
    throws IOException
    {
        if (matchingDataEntryWriter != null)
        {
            matchingDataEntryWriter.close();
            matchingDataEntryWriter = null;
        }
        if (nonMatchingDataEntryWriter != null)
        {
            nonMatchingDataEntryWriter.close();
            nonMatchingDataEntryWriter = null;
        }
    }


    /**
     * Prepares writing a data entry and returns a stream for writing the actual
     * data. The stream may be <code>null</code> to indicate that the data entry
     * should not be written.
     */
    public OutputStream openDataEntry(String name)
    throws IOException
    {
        match = regularExpressionMatcher.matches(name);

        return match ?
            matchingDataEntryWriter != null ?
                matchingDataEntryWriter.openDataEntry(name) :
                null :
            nonMatchingDataEntryWriter != null ?
                nonMatchingDataEntryWriter.openDataEntry(name) :
                null;
    }


    /**
     * Closes the previously opened data entry.
     */
    public void closeDataEntry()
    throws IOException
    {
        if (match)
        {
            matchingDataEntryWriter.closeDataEntry();
        }
        else
        {
            nonMatchingDataEntryWriter.closeDataEntry();
        }
    }
}
