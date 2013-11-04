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

import java.io.IOException;

/**
 * This DataEntryReader delegates to another DataEntryReader, renaming the
 * data entries based on the renamed classes in the given ClassPool.
 *
 * @author Eric Lafortune
 */
public class DataEntryRenamer implements DataEntryReader
{
    private final ClassPool       classPool;
    private final DataEntryReader dataEntryReader;


    public DataEntryRenamer(ClassPool       classPool,
                            DataEntryReader dataEntryReader)
    {
        this.classPool       = classPool;
        this.dataEntryReader = dataEntryReader;
    }


    // Implementations for DataEntryReader.

    public void read(DataEntry dataEntry) throws IOException
    {
        // Delegate to the actual data entry reader.
        dataEntryReader.read(renamedDataEntry(dataEntry));
    }


    /**
     * Create a renamed data entry, if possible.
     */
    private DataEntry renamedDataEntry(DataEntry dataEntry)
    {
        String dataEntryName = dataEntry.getName();

        // Try to find a corresponding class name by removing increasingly
        // long suffixes,
        for (int suffixIndex = dataEntryName.length() - 1;
             suffixIndex > 0;
             suffixIndex--)
        {
            char c = dataEntryName.charAt(suffixIndex);
            if (!Character.isJavaIdentifierPart(c))
            {
                // Stop looking at the first package separator.
                if (c == ClassConstants.INTERNAL_PACKAGE_SEPARATOR)
                {
                    break;
                }

                // Chop off the suffix.
                String className = dataEntryName.substring(0, suffixIndex);

                // Is there a class corresponding to the data entry?
                Clazz clazz = classPool.getClass(className);
                if (clazz != null)
                {
                    // Did the class get a new name?
                    String newClassName = clazz.getName();
                    if (!className.equals(newClassName))
                    {
                        // Return a renamed data entry.
                        String newDataEntryName =  suffixIndex > 0 ?
                            newClassName + dataEntryName.substring(suffixIndex) :
                            newClassName;

                        return new RenamedDataEntry(dataEntry, newDataEntryName);
                    }

                    // Otherwise stop looking.
                    break;
                }
            }
        }

        return dataEntry;
    }
}
