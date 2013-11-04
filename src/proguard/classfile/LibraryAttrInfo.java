/* $Id: LibraryAttrInfo.java,v 1.8 2002/11/03 13:30:13 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 1999 Mark Welsh (markw@retrologic.com)
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile;

import java.io.*;

/**
 * Representation of an attribute. Specific attributes have their representations
 * sub-classed from this.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class LibraryAttrInfo
{
    /**
     * Skips LibraryAttrInfo from the data passed.
     *
     * @throws IOException if class file is corrupt or incomplete
     */
    public static void skip(DataInput din) throws IOException
    {
        // Instantiate based on attribute name
        int u2attrNameIndex = din.readUnsignedShort();
        int u4attrLength    = din.readInt();
        din.skipBytes(u4attrLength);
    }
}
