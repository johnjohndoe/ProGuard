/* $Id: ClassPool.java,v 1.8 2002/11/03 13:30:13 eric Exp $
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

import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.*;

import java.util.*;
import java.util.jar.Manifest;

/**
 * This is a set of representations of class files. They can be enumerated or
 * retrieved by name. They can also be accessed by means of class file visitors.
 * <p>
 * In addition, a ClassPool can have a manifest.
 *
 * @author Eric Lafortune
 */
public class ClassPool
{
    private Hashtable classFiles = new Hashtable();
    private Manifest  manifest;

    /**
     * Adds the given ClassFile to the class pool.
     */
    public void addClass(ClassFile classFile)
    {
        Object previousClassFile = classFiles.put(classFile.getName(),
                                                  classFile);
        if (previousClassFile != null)
        {
            System.err.println("Warning: duplicated input class ["+classFile.getName()+"]");

            // We'll put the original one back.
            classFiles.put(((ClassFile)previousClassFile).getName(),
                           previousClassFile);
        }
    }


    /**
     * Removes the given ClassFile from the class pool.
     */
    public void removeClass(ClassFile classFile)
    {
        classFiles.remove(classFile.getName());
    }


    /**
     * Returns a ClassFile from the class pool based on its name. Returns
     * <code>null</code> if the class with the given name is not in the class
     * pool. Returns the base class if the class name is an array type, and the
     * <code>java.lang.Object</code> class if that base class is a primitive type.
     */
    public ClassFile getClass(String className)
    {
        return (ClassFile)classFiles.get(ClassUtil.internalClassNameFromType(className));
    }


    /**
     * Returns an Enumeration of all ClassFile objects in the class pool.
     */
    public Enumeration elements()
    {
        return classFiles.elements();
    }


    /**
     * Returns the number of class files in the class pool.
     */
    public int size()
    {
        return classFiles.size();
    }


    /**
     * Applies the given ClassPoolVisitor to the class pool.
     */
    public void accept(ClassPoolVisitor classPoolVisitor)
    {
        classPoolVisitor.visitClassPool(this);
    }


    /**
     * Applies the given ClassFileVisitor to all classes in the class pool,
     * in random order.
     */
    public void classFilesAccept(ClassFileVisitor classFileVisitor)
    {
        Enumeration enumeration = classFiles.elements();
        while (enumeration.hasMoreElements())
        {
            ClassFile classFile = (ClassFile)enumeration.nextElement();
            classFile.accept(classFileVisitor);
        }
    }


    /**
     * Applies the given ClassFileVisitor to all classes in the class pool,
     * in sorted order.
     */
    public void classFilesAcceptAlphabetically(ClassFileVisitor classFileVisitor)
    {
        TreeMap sortedClassFiles = new TreeMap(classFiles);
        Iterator iterator = sortedClassFiles.values().iterator();
        while (iterator.hasNext())
        {
            ClassFile classFile = (ClassFile)iterator.next();
            classFile.accept(classFileVisitor);
        }
    }


    /**
     * Applies the given ClassFileVisitor to the class with the given name,
     * if it is present in the class pool.
     */
    public void classFileAccept(ClassFileVisitor classFileVisitor, String className)
    {
        ClassFile classFile = getClass(className);
        if (classFile != null)
        {
            classFile.accept(classFileVisitor);
        }
    }


    public void setManifest(Manifest manifest)
    {
        this.manifest = manifest;
    }

    public Manifest getManifest()
    {
        return manifest;
    }
}
