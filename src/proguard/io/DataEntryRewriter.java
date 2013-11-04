/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.io;

import proguard.classfile.*;

import java.io.*;

/**
 * This DataEntryReader writes the resource data entries that it reads to a
 * given DataEntryWriter, updating their contents based on the renamed classes
 * in the given ClassPool.
 *
 * @author Eric Lafortune
 */
public class DataEntryRewriter implements DataEntryReader
{
    private final ClassPool       classPool;
    private final DataEntryWriter dataEntryWriter;


    /**
     * Creates a new DataEntryRewriter.
     */
    public DataEntryRewriter(ClassPool       classPool,
                             DataEntryWriter dataEntryWriter)
    {
        this.classPool       = classPool;
        this.dataEntryWriter = dataEntryWriter;
    }


    // Implementations for DataEntryReader.

    public void read(DataEntry dataEntry) throws IOException
    {
        try
        {
            // Get the output entry corresponding to this input entry.
            OutputStream outputStream = dataEntryWriter.getOutputStream(dataEntry);
            if (outputStream != null)
            {
                InputStream inputStream = dataEntry.getInputStream();

                // Copy the data from the input entry to the output entry.
                copyData(inputStream, outputStream);

                // Close the data entries.
                dataEntry.closeInputStream();
            }
        }
        catch (IOException ex)
        {
            System.err.println("Warning: can't write resource [" + dataEntry.getName() + "] (" + ex.getMessage() + ")");
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
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        StringBuffer word = new StringBuffer();

        while (true)
        {
            int i = reader.read();
            if (i < 0)
            {
                break;
            }

            // Is the character part of a word?
            char c = (char)i;
            if (Character.isJavaIdentifierPart(c) ||
                c == '.' ||
                c == '-')
            {
                // Collect the characters in this word.
                word.append(c);
            }
            else
            {
                // Write out the updated word, if any.
                writeUpdatedWord(writer, word.toString());
                word.setLength(0);

                // Write out the character that terminated it.
                writer.write(c);
            }
        }

        // Write out the final word.
        writeUpdatedWord(writer, word.toString());

        writer.flush();
        outputStream.flush();
    }


    /**
     * Writes the given word to the given writer, after having adapted it,
     * based on the renamed class names.
     */
    private void writeUpdatedWord(Writer writer, String word)
    throws IOException
    {
        if (word.length() > 0)
        {
            String newWord = word;

            boolean containsDots = word.indexOf('.') >= 0;

            // Replace dots by forward slashes.
            String className = containsDots ?
                word.replace('.', ClassConstants.INTERNAL_PACKAGE_SEPARATOR) :
                word;

            // Find the class corrsponding to the word.
            Clazz clazz = classPool.getClass(className);
            if (clazz != null)
            {
                // Update the word if necessary.
                String newClassName = clazz.getName();
                if (!className.equals(newClassName))
                {
                    // Replace forward slashes by dots.
                    newWord = containsDots ?
                        newClassName.replace(ClassConstants.INTERNAL_PACKAGE_SEPARATOR, '.') :
                        newClassName;
                }
            }

            writer.write(newWord);
        }
    }
}
