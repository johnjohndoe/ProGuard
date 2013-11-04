/* $Id: FileWordReader.java,v 1.4 2003/02/09 15:22:27 eric Exp $
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
package proguard;

import java.io.*;


/**
 * A <code>WordReader</code> that returns words from a file.
 * Comments (starting with '#' and ending with a newline) are removed.
 *
 * @author Eric Lafortune
 */
public class FileWordReader extends WordReader
{
    private static final char COMMENT_CHARACTER = '#';

    private String           fileName;
    private LineNumberReader reader;


    /**
     * Creates a new FileWordReader for the given file name.
     */
    public FileWordReader(String fileName) throws IOException
    {
        this.fileName = fileName;
        this.reader   = new LineNumberReader(
                        new BufferedReader(
                        new FileReader(fileName)));
    }


    // Implementations for WordReader

    protected String nextLine() throws IOException
    {
        String line = reader.readLine();
        if (line == null)
        {
            return null;
        }

        // Trim off any comments.
        int comments_start = line.indexOf(COMMENT_CHARACTER);
        if (comments_start >= 0)
        {
           line = line.substring(0, comments_start);
        }

        return line;
    }


    protected String lineLocationDescription()
    {
        return "line " + reader.getLineNumber() + " of file '" + fileName + "'";
    }
}
