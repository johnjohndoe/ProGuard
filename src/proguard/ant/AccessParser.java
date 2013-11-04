/* $Id: AccessParser.java,v 1.10 2003/08/04 08:46:45 eric Exp $
 *
 * ProGuard - integration into Ant.
 *
 * Copyright (c) 2003 Dirk Schnelle (dirk.schnelle@web.de)
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
package proguard.ant;

import org.apache.tools.ant.*;
import proguard.classfile.*;

import java.util.*;

/**
 * Parses a string with access strings of a method, a class or a field.
 *
 * @author Dirk Schnelle
 */
class AccessParser
{
    /** Known access attributes. */
    private final static Map ACCESS_MAPPING;
    /** Valid access strings for this parser. */
    private final Collection accessStrings;


    static
    {
        ACCESS_MAPPING = new HashMap();
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_PUBLIC,
                           new Integer(ClassConstants.INTERNAL_ACC_PUBLIC));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_PRIVATE,
                           new Integer(ClassConstants.INTERNAL_ACC_PRIVATE));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_PROTECTED,
                           new Integer(ClassConstants.INTERNAL_ACC_PROTECTED));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_STATIC,
                           new Integer(ClassConstants.INTERNAL_ACC_STATIC));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_FINAL,
                           new Integer(ClassConstants.INTERNAL_ACC_FINAL));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_SUPER,
                           new Integer(ClassConstants.INTERNAL_ACC_SUPER));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_SYNCHRONIZED,
                           new Integer(ClassConstants.INTERNAL_ACC_SYNCHRONIZED));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_VOLATILE,
                           new Integer(ClassConstants.INTERNAL_ACC_VOLATILE));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_TRANSIENT,
                           new Integer(ClassConstants.INTERNAL_ACC_TRANSIENT));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_NATIVE,
                           new Integer(ClassConstants.INTERNAL_ACC_NATIVE));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_INTERFACE,
                           new Integer(ClassConstants.INTERNAL_ACC_INTERFACE));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_ABSTRACT,
                           new Integer(ClassConstants.INTERNAL_ACC_ABSTRACT));
        ACCESS_MAPPING.put(ClassConstants.EXTERNAL_ACC_STRICT,
                           new Integer(ClassConstants.INTERNAL_ACC_STRICT));
    }


    /**
     * Creates a new access parser object with the given valid access strings.
     * @param accessStrings Valid access strings.
     */
    public AccessParser(Collection accessStrings)
    {
        this.accessStrings = accessStrings;
    }


    /**
     * Gets the configured access flags masked as a bit pattern.
     * @param str The string to parse.
     * @return Access flags.
     * @throws BuildException
     *         Discovered invalid access flag.
     */
    int getAccessFlags(String str) throws BuildException
    {
        return parseAccessFlags(asCollection(str), true);
    }


    /**
     * Gets the configured access flags masked as a bit pattern.
     * @param accessFlags The string to parse.
     * @return Access flags.
     * @throws BuildException
     *         Discovered invalid access flag.
     */
    int getAccessFlags(Collection accessFlags) throws BuildException
    {
        return parseAccessFlags(accessFlags, true);
    }


    /**
     * Gets the configured unset access flags masked as a bit pattern.
     * @param str The string to parse.
     * @return Access flags to be unset.
     * @throws BuildException
     *         Discovered invalid access flag.
     */
    int getUnsetAccessFlags(String str) throws BuildException
    {
        return parseAccessFlags(asCollection(str), false);
    }


    /**
     * Gets the configured access flags masked as a bit pattern.
     * @param accessFlags The string to parse.
     * @return Access flags.
     * @throws BuildException
     *         Discovered invalid access flag.
     */
    int getUnsetAccessFlags(Collection accessFlags) throws BuildException
    {
        return parseAccessFlags(accessFlags, false);
    }


    /**
     * Gets a collection of access flags. The flags are just separated but
     * not evaluated.
     * @param str The flag to be parsed
     * @return Collection of access flags
     */
    private Collection asCollection(String str)
    {
        StringTokenizer tokenizer = new StringTokenizer(str);

        Collection accessStrings = new ArrayList(tokenizer.countTokens());

        while (tokenizer.hasMoreElements())
        {
            String next = tokenizer.nextToken();
            accessStrings.add(next);
        }

        return accessStrings;
    }


    /**
     * Parses the given string for access flags and mask them as a bit pattern.
     * @param accessFlagsCollection The access flags to be parsed.
     * @param set <code>true</code>, if access flags to be set should be parsed,
     *            <code>false</code> if access flags to be unset should be
     *            parsed.
     * @return Discovered access flags.
     * @throws BuildException
     *         Discovered invalid access flag.
     */
    private int parseAccessFlags(Collection accessFlagsCollection, boolean set)
        throws BuildException
    {
        int accessFlags = 0;

        Iterator iterator = accessFlagsCollection.iterator();

        while (iterator.hasNext())
        {
            String next = (String)iterator.next();
            if (next.startsWith("!") != set)
            {
                Integer nextFlag = (Integer)ACCESS_MAPPING.get(next);
                if (nextFlag == null)
                {
                    throw new BuildException("Unknown access flag: " + next);
                }

                accessFlags |= nextFlag.intValue();
            }
        }

        return accessFlags;
    }
}