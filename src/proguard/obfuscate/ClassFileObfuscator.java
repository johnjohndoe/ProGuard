/* $Id: ClassFileObfuscator.java,v 1.11 2002/11/03 13:30:14 eric Exp $
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
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassFileVisitor;

import java.util.Hashtable;


/**
 * This <code>ClassFileVisitor</code> comes up with obfuscated names for the
 * class files it visits, and for their class members. The actual renaming is
 * done afterward.
 *
 * @see ClassFileRenamer
 *
 * @author Eric Lafortune
 */
public class ClassFileObfuscator
  implements ClassFileVisitor
{
    private ClassPool programClassPool;
    private boolean   useMixedCaseClassNames;
    private String    defaultPackageName;

    // Hashtable: [package name - class name factory]
    private final Hashtable        packageHashtable = new Hashtable();
    private final NameFactory      defaultPackageClassNameFactory;
    private final MemberObfuscator memberObfuscator;


    /**
     * Creates a new ClassFileObfuscator.
     * @param programClassPool   the class pool in which class names have to be
     *                           unique.
     * @param defaultPackageName the package in which all classes that don't
     *                           have fixed names will be put, or <code>null</code>,
     *                           if all classes can remain in their original
     *                           packages.
     * @param allowAggressiveOverloading a flag that specifies whether class
     *                           members can be overloaded aggressively.
     */
    public ClassFileObfuscator(ClassPool programClassPool,
                               String    defaultPackageName,
                               boolean   useMixedCaseClassNames,
                               boolean   allowAggressiveOverloading)
    {
        this.programClassPool               = programClassPool;
        this.defaultPackageName             = defaultPackageName;
        this.useMixedCaseClassNames         = useMixedCaseClassNames;
        this.defaultPackageClassNameFactory = new NameFactory(useMixedCaseClassNames);
        this.memberObfuscator               = new MemberObfuscator(allowAggressiveOverloading);
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Does this class file still need a new name?
        if (newClassName(programClassFile) == null)
        {
            // Figure out a new name.
            String className   = programClassFile.getName();
            String packageName = ClassUtil.internalPackageName(className);

            String newPackageName = packageName;

            // Find the right name factory for this package, or use the default.
            NameFactory packageClassNameFactory = (NameFactory)packageHashtable.get(packageName);
            if (packageClassNameFactory == null)
            {
                // Do we have a default package name?
                if (defaultPackageName == null)
                {
                    // We haven't seen this package before. Create a new name factory
                    // for it.
                    packageClassNameFactory = new NameFactory(useMixedCaseClassNames);
                    packageHashtable.put(packageName, packageClassNameFactory);
                }
                else
                {
                    // Fall back on the default package class name factory and name.
                    packageClassNameFactory = defaultPackageClassNameFactory;
                    newPackageName          = defaultPackageName;
                }
            }

            // Come up with a unique class name.
            String newClassName;
            while (true)
            {
                // Let the factory produce a unique class name.
                newClassName = packageClassNameFactory.nextName();

                // We may have to add a package part to the class name.
                if (newPackageName.length() > 0)
                {
                    newClassName =
                        newPackageName +
                        ClassConstants.INTERNAL_PACKAGE_SEPARATOR +
                        newClassName;
                }

                // Isn't there a class file that has this name reserved?
                ClassFile otherClassFile = programClassPool.getClass(newClassName);
                if (otherClassFile == null ||
                    !newClassName.equals(newClassName(otherClassFile)))
                {
                    break;
                }
            }

            setNewClassName(programClassFile, newClassName);
        }

        // Is this a bottom class in the class hierarchy?
        if (programClassFile.subClasses == null)
        {
            // Then start obfuscating the class members in this name space.
            memberObfuscator.obfuscate(programClassFile);
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
    }


    // Small utility methods.

    static void setNewClassName(ClassFile classFile, String name)
    {
        classFile.setVisitorInfo(name);
    }


    static String newClassName(ClassFile classFile)
    {
        Object visitorInfo = classFile.getVisitorInfo();

        return visitorInfo instanceof String ?
            (String)visitorInfo :
            null;
    }
}
