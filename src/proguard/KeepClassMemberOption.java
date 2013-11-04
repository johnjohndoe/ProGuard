/* $Id: KeepClassMemberOption.java,v 1.2 2003/12/06 22:12:42 eric Exp $
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


/**
 * This class stores a specification of class members to be kept from shrinking
 * and/or obfuscation. The specification is template-based: the class member
 * names and descriptors can contain wildcards.
 *
 * @author Eric Lafortune
 */
public class KeepClassMemberOption
{
    public int    requiredSetAccessFlags;
    public int    requiredUnsetAccessFlags;
    public String name;
    public String descriptor;
    public String asName;


    /**
     * Creates a new option to keep the specified class member(s).
     */
    public KeepClassMemberOption()
    {
        this(0,
             0,
             null,
             null,
             null);
    }


    /**
     * Creates a new option to keep the specified class member(s).
     *
     * @param requiredSetAccessFlags   the class access flags that must be set
     *                                 in order for the class to apply.
     * @param requiredUnsetAccessFlags the class access flags that must be unset
     *                                 in order for the class to apply.
     * @param name                     the class member name. The name may be
     *                                 null to specify any class member or it
     *                                 may contain "*" or "?" wildcards.
     * @param descriptor               the class member descriptor. The
     *                                 descriptor may be null to specify any
     *                                 class member or it may contain
     *                                 "**", "*", or "?" wildcards.
     * @param asName                   the new name that the field should get.
     *                                 A value of null specifies the original
     *                                 name.
     */
    public KeepClassMemberOption(int    requiredSetAccessFlags,
                                 int    requiredUnsetAccessFlags,
                                 String name,
                                 String descriptor,
                                 String asName)
    {
        this.requiredSetAccessFlags   = requiredSetAccessFlags;
        this.requiredUnsetAccessFlags = requiredUnsetAccessFlags;
        this.name                     = name;
        this.descriptor               = descriptor;
        this.asName                   = asName;
    }



    // Implementations for Object.

    public boolean equals(Object object)
    {
        if (this.getClass() != object.getClass())
        {
            return false;
        }

        KeepClassMemberOption other = (KeepClassMemberOption)object;
        return
            (this.requiredSetAccessFlags   == other.requiredSetAccessFlags  ) &&
            (this.requiredUnsetAccessFlags == other.requiredUnsetAccessFlags) &&
            (this.name       == null ? other.name       == null : this.name.equals(other.name)            ) &&
            (this.descriptor == null ? other.descriptor == null : this.descriptor.equals(other.descriptor)) &&
            (this.asName     == null ? other.asName     == null : this.asName.equals(other.asName)        );
    }

    public int hashCode()
    {
        return
            requiredSetAccessFlags                           ^
            requiredUnsetAccessFlags                         ^
            (name       == null ? 0 : name.hashCode()      ) ^
            (descriptor == null ? 0 : descriptor.hashCode()) ^
            (asName     == null ? 0 : asName.hashCode()    );
    }

}
