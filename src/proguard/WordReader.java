/* $Id: WordReader.java,v 1.3 2002/05/12 10:51:38 eric Exp $
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
package proguard;

import java.io.*;


/**
 * An abstract reader of words, with the possibility to include other readers.
 *
 * @author Eric Lafortune
 */
public abstract class WordReader
{
    private WordReader includeWordReader;
    private String     currentLine;
    private int        currentIndex;
    private String     currentWord;


    public void includeWordReader(WordReader newIncludeWordReader)
    {
        if (includeWordReader == null)
        {
            includeWordReader = newIncludeWordReader;
        }
        else
        {
            includeWordReader.includeWordReader(newIncludeWordReader);
        }
    }


    public String nextWord() throws IOException
    {
        // See if we have an included reader to produce a word.
        if (includeWordReader != null)
        {
            // Does the included word reader still produce a word?
            currentWord = includeWordReader.nextWord();
            if (currentWord != null)
            {
                // Return it if so.
                return currentWord;
            }

            // Otherwise ditch the word reader.
            includeWordReader = null;
        }

        // Get a word from this reader.

        // Skip leading whitespace.
        while (currentLine != null &&
               currentIndex < currentLine.length() &&
               Character.isWhitespace(currentLine.charAt(currentIndex)))
        {
            currentIndex++;
        }

        // Make sure we have a non-blank line.
        while (currentLine == null || currentIndex == currentLine.length())
        {
            currentLine = nextLine();
            if (currentLine == null)
            {
                return null;
            }

            // Skip leading whitespace.
            currentIndex = 0;
            while (currentIndex < currentLine.length() &&
                   Character.isWhitespace(currentLine.charAt(currentIndex)))
            {
                currentIndex++;
            }
        }

        // Find the word starting at the current index.
        int startIndex = currentIndex;
        if (isDelimiter(currentLine.charAt(startIndex)))
        {
            // The next word is a single delimiting character.
            currentIndex++;
        }
        else
        {
            // The next word is an actual character string.
            while (currentIndex < currentLine.length())
            {
                char currentCharacter = currentLine.charAt(currentIndex);
                if (isDelimiter(currentCharacter) ||
                    Character.isWhitespace(currentCharacter))
                {
                    break;
                }

                currentIndex++;
            }
        }

        // Remember and return the parsed word.
        currentWord = currentLine.substring(startIndex, currentIndex);

        return currentWord;
    }


    public String locationDescription()
    {
        return
            (includeWordReader == null ?
                (currentWord == null ?
                    "end of " :
                    "'" + currentWord + "' in " ) :
                (includeWordReader.locationDescription() + ",\n" +
                 "  included from ")) +
            lineLocationDescription();
    }


    protected abstract String nextLine() throws IOException;


    protected abstract String lineLocationDescription();


    private boolean isDelimiter(char character)
    {
        return character == '@' ||
               character == '{' ||
               character == '}' ||
               character == '(' ||
               character == ')' ||
               character == ',' ||
               character == ';';
    }
}
