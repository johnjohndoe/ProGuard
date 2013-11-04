/* $Id: KeepClassFileOption.java,v 1.3 2003/12/06 22:12:42 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
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
package proguard;

import java.util.*;


/**
 * This class stores a specification of classes and possibly class members to be
 * kept from shrinking and/or obfuscation. The specification is template-based:
 * the class names and class member names and descriptors can contain wildcards.
 * Classes can be specified explicitly, or as extensions or implementations in
 * the class hierarchy.
 *
 * @author Eric Lafortune
 */
public class KeepClassFileOption implements Cloneable
{
    public int     requiredSetAccessFlags;
    public int     requiredUnsetAccessFlags;
    public String  className;
    public String  extendsClassName;
    public String  asClassName;
    public boolean markClassFiles;
    public boolean markConditionally;
    public boolean onlyKeepNames;
    public String  comments;

    public List    keepFieldOptions;
    public List    keepMethodOptions;


    /**
     * Creates a new option to keep all possible class(es).
     * The option doesn't have comments.
     */
    public KeepClassFileOption()
    {
        this(0,
             0,
             null,
             null,
             null,
             true,
             false,
             false);
    }


    /**
     * Creates a new option to keep the specified class(es).
     * The option doesn't have comments.
     *
     * @param requiredSetAccessFlags   the class access flags that must be set
     *                                 in order for the class to apply.
     * @param requiredUnsetAccessFlags the class access flags that must be unset
     *                                 in order for the class to apply.
     * @param className                the class name. The name may be null to
     *                                 specify any class, or it may contain
     *                                 "**", "*", or "?" wildcards.
     * @param extendsClassName         the name of the class that the class must
     *                                 extend or implement in order to apply.
     *                                 The name may be null to specify any class.
     * @param asClassName              the new name that the class should get.
     *                                 A value of null specifies the original
     *                                 name.
     * @param markClassFiles           specifies whether to mark the class files.
     *                                 If false, only class members are marked.
     *                                 If true, the class files are marked as
     *                                 well.
     * @param markConditionally        specifies whether to mark the class files
     *                                 and class members conditionally.
     *                                 If true, class files and class members
     *                                 are marked, on the condition that all
     *                                 specified class members are present.
     * @param onlyKeepNames            specifies whether the class files and class
     *                                 members need to be kept from obfuscation
     *                                 only.
     *                                 If true, the specified class class files
     *                                 and class members will be kept from
     *                                 obfuscation, but the may be removed in
     *                                 the shrinking phase.
     *                                 If false, they will be kept from
     *                                 shrinking and obfuscation alike.
     */
    public KeepClassFileOption(int     requiredSetAccessFlags,
                               int     requiredUnsetAccessFlags,
                               String  className,
                               String  extendsClassName,
                               String  asClassName,
                               boolean markClassFiles,
                               boolean markConditionally,
                               boolean onlyKeepNames)
    {
        this(requiredSetAccessFlags,
             requiredUnsetAccessFlags,
             className,
             extendsClassName,
             asClassName,
             markClassFiles,
             markConditionally,
             onlyKeepNames,
             null);
    }


    /**
     * Creates a new option to keep the specified class(es).
     *
     * @param requiredSetAccessFlags   the class access flags that must be set
     *                                 in order for the class to apply.
     * @param requiredUnsetAccessFlags the class access flags that must be unset
     *                                 in order for the class to apply.
     * @param className                the class name. The name may be null to
     *                                 specify any class, or it may contain
     *                                 "**", "*", or "?" wildcards.
     * @param extendsClassName         the name of the class that the class must
     *                                 extend or implement in order to apply.
     *                                 The name may be null to specify any class.
     * @param asClassName              the new name that the class should get.
     *                                 A value of null specifies the original
     *                                 name.
     * @param markClassFiles           specifies whether to mark the class files.
     *                                 If false, only class members are marked.
     *                                 If true, the class files are marked as
     *                                 well.
     * @param markConditionally        specifies whether to mark the class files
     *                                 and class members conditionally.
     *                                 If true, class files and class members
     *                                 are marked, on the condition that all
     *                                 specified class members are present.
     * @param onlyKeepNames            specifies whether the class files and class
     *                                 members need to be kept from obfuscation
     *                                 only.
     *                                 If true, the specified class class files
     *                                 and class members will be kept from
     *                                 obfuscation, but the may be removed in
     *                                 the shrinking phase.
     *                                 If false, they will be kept from
     *                                 shrinking and obfuscation alike.
     * @param comments                 provides optional comments on this option.
     */
    public KeepClassFileOption(int     requiredSetAccessFlags,
                               int     requiredUnsetAccessFlags,
                               String  className,
                               String  extendsClassName,
                               String  asClassName,
                               boolean markClassFiles,
                               boolean markConditionally,
                               boolean onlyKeepNames,
                               String  comments)
    {
        this.requiredSetAccessFlags   = requiredSetAccessFlags;
        this.requiredUnsetAccessFlags = requiredUnsetAccessFlags;
        this.className                = className;
        this.extendsClassName         = extendsClassName;
        this.asClassName              = asClassName;
        this.markClassFiles           = markClassFiles;
        this.markConditionally        = markConditionally;
        this.onlyKeepNames            = onlyKeepNames;
        this.comments                 = comments;
    }


    /**
     * Specifies to keep the specified field(s) of this option's class(es).
     *
     * @param keepFieldOption the field specification.
     */
    public void addField(KeepClassMemberOption keepFieldOption)
    {
        if (keepFieldOptions == null)
        {
            keepFieldOptions = new ArrayList();
        }

        keepFieldOptions.add(keepFieldOption);
    }


    /**
     * Specifies to keep the specified method(s) of this option's class(es).
     *
     * @param keepMethodOption the method specification.
     */
    public void addMethod(KeepClassMemberOption keepMethodOption)
    {
        if (keepMethodOptions == null)
        {
            keepMethodOptions = new ArrayList();
        }

        keepMethodOptions.add(keepMethodOption);
    }



    // Implementations for Object.

    public boolean equals(Object object)
    {
        if (this.getClass() != object.getClass())
        {
            return false;
        }

        KeepClassFileOption other = (KeepClassFileOption)object;
        return
            (this.requiredSetAccessFlags   == other.requiredSetAccessFlags  ) &&
            (this.requiredUnsetAccessFlags == other.requiredUnsetAccessFlags) &&
            (this.markClassFiles           == other.markClassFiles          ) &&
            (this.markConditionally        == other.markConditionally       ) &&
            (this.onlyKeepNames            == other.onlyKeepNames           ) &&
            (this.className         == null ? other.className         == null : this.className.equals(other.className))               &&
            (this.extendsClassName  == null ? other.extendsClassName  == null : this.extendsClassName.equals(other.extendsClassName)) &&
            (this.asClassName       == null ? other.asClassName       == null : this.asClassName.equals(other.asClassName))           &&
            (this.keepFieldOptions  == null ? other.keepFieldOptions  == null : this.keepFieldOptions.equals(other.keepFieldOptions)) &&
            (this.keepMethodOptions == null ? other.keepMethodOptions == null : this.keepMethodOptions.equals(other.keepMethodOptions));
    }

    public int hashCode()
    {
        return
            requiredSetAccessFlags                                         ^
            requiredUnsetAccessFlags                                       ^
            (className         == null ? 0 : className.hashCode()        ) ^
            (extendsClassName  == null ? 0 : extendsClassName.hashCode() ) ^
            (asClassName       == null ? 0 : asClassName.hashCode()      ) ^
            (markClassFiles            ? 0 : 1                           ) ^
            (markConditionally         ? 0 : 2                           ) ^
            (onlyKeepNames             ? 0 : 4                           ) ^
            (keepFieldOptions  == null ? 0 : keepFieldOptions.hashCode() ) ^
            (keepMethodOptions == null ? 0 : keepMethodOptions.hashCode());
    }

    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }
}
