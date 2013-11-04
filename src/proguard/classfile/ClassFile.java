/* $Id: ClassFile.java,v 1.11 2002/07/04 16:16:58 eric Exp $
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

import proguard.classfile.visitor.*;


/**
 * This interface provides access to the data in a Java class file (*.class).
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public interface ClassFile extends VisitorAccepter
{
    /**
     * Returns the access flags of this class.
     * @see ClassConstants
     */
    public int getAccessFlags();

    /**
     * Returns the full internal name of this class.
     */
    public String getName();

    /**
     * Returns the full internal name of the super class of this class, or
     * null if this class represents java.lang.Object.
     */
    public String getSuperName();

    /**
     * Returns the tag value of the CpEntry at the specified index.
     */
    public int getCpTag(int cpIndex);

    /**
     * Returns the String value of the StringCpEntry at the specified index.
     */
    public String getCpString(int cpIndex);

    /**
     * Returns the class name of ClassCpEntry at the specified index.
     */
    public String getCpClassNameString(int cpIndex);

    /**
     * Returns the name of the NameAndTypeCpEntry at the specified index.
     */
    public String getCpNameString(int cpIndex);

    /**
     * Returns the type of the NameAndTypeCpEntry at the specified index.
     */
    public String getCpTypeString(int cpIndex);


    // Methods pertaining to related class files.

    /**
     * Notifies this ClassFile that it is being subclassed by another class.
     */
    public void addSubClass(ClassFile classFile);

    /**
     * Returns the super class of this class.
     */
    public ClassFile getSuperClass();


    // Methods for getting specific class members.

    /**
     * Returns the field with the given name and descriptor.
     */
    FieldInfo findField(String name, String descriptor);

    /**
     * Returns the method with the given name and descriptor.
     */
    MethodInfo findMethod(String name, String descriptor);


    // Methods for accepting various types of visitors.

    /**
     * Accepts the given class file visitor.
     */
    public void accept(ClassFileVisitor classFileVisitor);

    /**
     * Lets the given constant pool entry visitor visit all constant pool entries
     * of this class.
     */
    public void constantPoolEntriesAccept(CpInfoVisitor cpInfoVisitor);

    /**
     * Lets the given constant pool entry visitor visit the constant pool entry
     * at the specified index.
     */
    public void constantPoolEntryAccept(CpInfoVisitor cpInfoVisitor, int index);

    /**
     * Lets the given member info visitor visit all fields of this class.
     */
    public void fieldsAccept(MemberInfoVisitor memberInfoVisitor);

    /**
     * Lets the given member info visitor visit the specified field.
     */
    public void fieldAccept(MemberInfoVisitor memberInfoVisitor, String name, String descriptor);

    /**
     * Lets the given member info visitor visit all methods of this class.
     */
    public void methodsAccept(MemberInfoVisitor memberInfoVisitor);

    /**
     * Lets the given member info visitor visit the specified method.
     */
    public void methodAccept(MemberInfoVisitor memberInfoVisitor, String name, String descriptor);

    /**
     * Lets the given attribute info visitor visit all attributes of this class.
     */
    public void attributesAccept(AttrInfoVisitor attrInfoVisitor);
}
