/* $Id: NameFactory.java,v 1.3 2002/05/12 13:33:42 eric Exp $
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
package proguard.obfuscate;

import proguard.classfile.*;
import proguard.classfile.visitor.*;

import java.util.*;


/**
 * A <code>NameFactory</code> generates unique names using lower-case characters,
 * and upper-case characters
 *
 * @author Eric Lafortune
 */
class NameFactory
{
    private static final int CHARACTER_COUNT1 = 26;
    private static final int CHARACTER_COUNT2 = 26 + CHARACTER_COUNT1;

    private static final Vector cachedNames = new Vector();

    private int index = 0;


    public void reset()
    {
        index = 0;
    }


    public String nextName()
    {
        return nameAt(index++);
    }


    private String nameAt(int index)
    {
        return index >= cachedNames.size() ?
            newNameAt(index) :
            oldNameAt(index);
    }


    private String newNameAt(int index)
    {
        int baseIndex = index / CHARACTER_COUNT2;
        int offset    = index % CHARACTER_COUNT2;

        char newChar = charAt(offset);

        String newName = baseIndex == 0 ?
            new String(new char[] { newChar }) :
            (nameAt(baseIndex) + newChar);

        cachedNames.add(index, newName);

        return newName;
    }


    private String oldNameAt(int index)
    {
        return (String)cachedNames.elementAt(index);
    }


    private char charAt(int index)
    {
        return (char)((index < CHARACTER_COUNT1 ? 'a' - 0               :
                                                  'A' - CHARACTER_COUNT1) + index);
    }
}
