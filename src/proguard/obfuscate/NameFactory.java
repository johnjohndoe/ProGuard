/* $Id: NameFactory.java,v 1.12 2004/08/15 12:39:30 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
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
package proguard.obfuscate;

import java.util.*;


/**
 * A <code>NameFactory</code> generates unique names using mixed-case characters,
 * or lower-case characters only.
 *
 * @author Eric Lafortune
 */
class NameFactory
{
    private static final int CHARACTER_COUNT = 26;

    private static final List cachedMixedCaseNames = new ArrayList();
    private static final List cachedLowerCaseNames = new ArrayList();

    private boolean generateMixedCaseNames;
    private int     index = 0;


    /**
     * Creates a new <code>NameFactory</code> that generates mixed-case names.
     */
    public NameFactory()
    {
        this(true);
    }


    /**
     * Creates a new <code>NameFactory</code>.
     * @param generateMixedCaseNames a flag to indicate whether the generated
     *                               names will be mixed-case, or lower-case only.
     */
    public NameFactory(boolean generateMixedCaseNames)
    {
        this.generateMixedCaseNames = generateMixedCaseNames;
    }


    public void reset()
    {
        index = 0;
    }


    public String nextName()
    {
        return name(index++);
    }


    private String name(int index)
    {
        // Which cache do we need?
        List cachedNames = generateMixedCaseNames ?
            cachedMixedCaseNames :
            cachedLowerCaseNames;

        // Do we have the name in the cache?
        if (index < cachedNames.size())
        {
            return (String)cachedNames.get(index);
        }

        // Create a new name and cache it.
        String name = newName(index);
        cachedNames.add(index, name);

        return name;
    }


    private String newName(int index)
    {
        // If we're allowed to generate mixed-case names, we can use twice as
        // many characters.
        int totalCharacterCount = generateMixedCaseNames ?
            2 * CHARACTER_COUNT :
            CHARACTER_COUNT;

        int baseIndex = index / totalCharacterCount;
        int offset    = index % totalCharacterCount;

        char newChar = charAt(offset);

        String newName = baseIndex == 0 ?
            new String(new char[] { newChar }) :
            (name(baseIndex-1) + newChar);

        return newName;
    }


    private char charAt(int index)
    {
        return (char)((index < CHARACTER_COUNT ? 'a' - 0               :
                                                 'A' - CHARACTER_COUNT) + index);
    }


    public static void main(String[] args)
    {
        System.out.println("Some mixed-case names:");
        printNameSamples(new NameFactory(true), 60);
        System.out.println("Some lower-case names:");
        printNameSamples(new NameFactory(false), 60);
        System.out.println("Some more mixed-case names:");
        printNameSamples(new NameFactory(true), 80);
        System.out.println("Some more lower-case names:");
        printNameSamples(new NameFactory(false), 80);
    }


    private static void printNameSamples(NameFactory factory, int count)
    {
        for (int counter = 0; counter < count; counter++)
        {
            System.out.println("  ["+factory.nextName()+"]");
        }
    }
}
