/* $Id: MultiJarReader.java,v 1.2 2002/11/03 13:30:14 eric Exp $
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

import java.io.IOException;
import java.util.Vector;
import java.util.jar.Manifest;


/**
 * This class can read a given set of jars, applying a given ZipEntryReader to
 * all ZIP entries it reads.
 *
 * @author Eric Lafortune
 */
public class MultiJarReader
{
    private Vector   jarFileNames;
    private Manifest manifest;


    public MultiJarReader(Vector jarFileNames)
    {
        this.jarFileNames = jarFileNames;
    }


    /**
     * Returns the Manifest from the first read jar file.
     */
    public Manifest getManifest()
    {
        return manifest;
    }


    /**
     * Reads the set of jars, applying the given ZipEntryReader to all ZIP
     * entries that are encountered.
     */
    public void readZipEntries(ZipEntryReader zipEntryReader)
    throws IOException
    {
        for (int index = 0; index < jarFileNames.size(); index++)
        {
            String jarFileName = (String)jarFileNames.elementAt(index);
            JarReader jarReader = new JarReader(jarFileName);

            jarReader.readZipEntries(zipEntryReader);

            if (manifest == null)
            {
                manifest = jarReader.getManifest();
            }
        }
    }
}
