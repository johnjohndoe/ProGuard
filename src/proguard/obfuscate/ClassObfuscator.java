/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
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
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.ClassVisitor;

import java.util.*;

/**
 * This <code>ClassVisitor</code> comes up with obfuscated names for the
 * classes it visits, and for their class members. The actual renaming is
 * done afterward.
 *
 * @see ClassRenamer
 *
 * @author Eric Lafortune
 */
public class ClassObfuscator
extends      SimplifiedVisitor
implements   ClassVisitor,
             AttributeVisitor,
             InnerClassesInfoVisitor,
             ConstantVisitor
{
    private final boolean useMixedCaseClassNames;
    private final String  flattenPackageHierarchy;
    private final String  repackageClasses;
    private final boolean allowAccessModification;

    private final Set classNamesToAvoid                  = new HashSet();

    // Map: [package prefix - new package prefix]
    private final Map packagePrefixMap                   = new HashMap();

    // Map: [package prefix - package name factory]
    private final Map packagePrefixPackageNameFactoryMap = new HashMap();

    // Map: [package prefix - class name factory]
    private final Map packagePrefixClassNameFactoryMap   = new HashMap();

    // A field acting as a temporary variable and as a return value for names
    // of outer classes.
    private String newClassName;


    /**
     * Creates a new ClassObfuscator.
     * @param programClassPool        the class pool in which class names
     *                                have to be unique.
     * @param useMixedCaseClassNames  specifies whether obfuscated packages
     *                                and classes can get mixed-case names.
     * @param flattenPackageHierarchy the base package if the obfuscated
     *                                package hierarchy is to be flattened.
     * @param repackageClasses        the base package if the obfuscated
     *                                classes are to be repackaged.
     * @param allowAccessModification specifies whether obfuscated classes
     *                                can be freely moved between packages.
     */
    public ClassObfuscator(ClassPool programClassPool,
                           boolean   useMixedCaseClassNames,
                           String    flattenPackageHierarchy,
                           String    repackageClasses,
                           boolean   allowAccessModification)
    {
        // First append the package separator if necessary.
        if (flattenPackageHierarchy != null &&
            flattenPackageHierarchy.length() > 0)
        {
            flattenPackageHierarchy += ClassConstants.INTERNAL_PACKAGE_SEPARATOR;
        }

        // First append the package separator if necessary.
        if (repackageClasses != null &&
            repackageClasses.length() > 0)
        {
            repackageClasses += ClassConstants.INTERNAL_PACKAGE_SEPARATOR;
        }

        this.useMixedCaseClassNames  = useMixedCaseClassNames;
        this.flattenPackageHierarchy = flattenPackageHierarchy;
        this.repackageClasses        = repackageClasses;
        this.allowAccessModification = allowAccessModification;

        // Map the root package onto the root package.
        packagePrefixMap.put("", "");

        // Collect all names that have been taken already.
        programClassPool.classesAccept(new MyKeepCollector());
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // Does this class still need a new name?
        newClassName = newClassName(programClass);
        if (newClassName == null)
        {
            // Make sure the outer class has a name, if it exists. The name will
            // be stored as the new class name, as a side effect, so we'll be
            // able to use it as a prefix.
            programClass.attributesAccept(this);

            // Figure out a package prefix. The package prefix may actually be
            // the outer class prefix, if any, or it may be the fixed base
            // package, if classes are to be repackaged.
            String newPackagePrefix = newClassName != null ?
                newClassName + ClassConstants.INNER_CLASS_SEPARATOR :
                newPackagePrefix(ClassUtil.internalPackagePrefix(programClass.getName()));

            // Come up with a new class name.
            newClassName = generateUniqueClassName(newPackagePrefix);

            setNewClassName(programClass, newClassName);
        }
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        // Make sure the outer classes have a name, if they exist.
        innerClassesAttribute.innerClassEntriesAccept(clazz, this);
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        // Make sure the enclosing class has a name.
        enclosingMethodAttribute.referencedClassAccept(this);
    }


    // Implementations for InnerClassesInfoVisitor.

    public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
    {
        // Make sure the outer class has a name, if it exists.
        int innerClassIndex = innerClassesInfo.u2innerClassIndex;
        int outerClassIndex = innerClassesInfo.u2outerClassIndex;
        if (innerClassIndex != 0 &&
            outerClassIndex != 0 &&
            clazz.getClassName(innerClassIndex).equals(clazz.getName()))
        {
            clazz.constantPoolEntryAccept(outerClassIndex, this);
        }
    }


    // Implementations for ConstantVisitor.

    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Make sure the outer class has a name.
        classConstant.referencedClassAccept(this);
    }


    /**
     * This ClassVisitor collects package names and class names that have to
     * be kept.
     */
    private class MyKeepCollector implements ClassVisitor
    {
        public void visitProgramClass(ProgramClass programClass)
        {
            // Does the class already have a new name?
            String newClassName = newClassName(programClass);
            if (newClassName != null)
            {
                // Remember not to use this name.
                classNamesToAvoid.add(mixedCaseClassName(newClassName));

                // Are we not aggressively repackaging all obfuscated classes?
                if (repackageClasses == null ||
                    !allowAccessModification)
                {
                    String className = programClass.getName();

                    // Keep the package name for all other classes in the same
                    // package. Do this resursively if we're not doing any
                    // repackaging.
                    mapPackageName(className,
                                   newClassName,
                                   repackageClasses        == null &&
                                   flattenPackageHierarchy == null);
                }
            }
        }


        public void visitLibraryClass(LibraryClass libraryClass)
        {
        }


        /**
         * Makes sure the package name of the given class will always be mapped
         * consistently with its new name.
         */
        private void mapPackageName(String  className,
                                    String  newClassName,
                                    boolean recursively)
        {
            String packagePrefix    = ClassUtil.internalPackagePrefix(className);
            String newPackagePrefix = ClassUtil.internalPackagePrefix(newClassName);

            // Put the mapping of this package prefix, and possibly of its
            // entire hierarchy, into the package prefix map.
            do
            {
                packagePrefixMap.put(packagePrefix, newPackagePrefix);

                if (!recursively)
                {
                    break;
                }

                packagePrefix    = ClassUtil.internalPackagePrefix(packagePrefix);
                newPackagePrefix = ClassUtil.internalPackagePrefix(newPackagePrefix);
            }
            while (packagePrefix.length()    > 0 &&
                   newPackagePrefix.length() > 0);
        }
    }


    // Small utility methods.

    /**
     * Finds or creates the new package prefix for the given package.
     */
    private String newPackagePrefix(String packagePrefix)
    {
        // Doesn't the package prefix have a new package prefix yet?
        String newPackagePrefix = (String)packagePrefixMap.get(packagePrefix);
        if (newPackagePrefix == null)
        {
            // Are we forcing a new package prefix?
            if (repackageClasses != null)
            {
                return repackageClasses;
            }

            // Are we forcing a new superpackage prefix?
            // Othewrise figure out the new superpackage prefix, recursively.
            String newSuperPackagePrefix = flattenPackageHierarchy != null ?
                flattenPackageHierarchy :
                newPackagePrefix(ClassUtil.internalPackagePrefix(packagePrefix));

            // Come up with a new package prefix.
            newPackagePrefix = generateUniquePackagePrefix(newSuperPackagePrefix);

            // Remember to use this mapping in the future.
            packagePrefixMap.put(packagePrefix, newPackagePrefix);
        }

        return newPackagePrefix;
    }


    /**
     * Creates a new package prefix in the given new superpackage.
     */
    private String generateUniquePackagePrefix(String newSuperPackagePrefix)
    {
        // Find the right name factory for this package.
        NameFactory packageNameFactory =
            (NameFactory)packagePrefixPackageNameFactoryMap.get(newSuperPackagePrefix);
        if (packageNameFactory == null)
        {
            // We haven't seen packages in this superpackage before. Create
            // a new name factory for them.
            packageNameFactory = new SimpleNameFactory(useMixedCaseClassNames);
            packagePrefixPackageNameFactoryMap.put(newSuperPackagePrefix,
                                                   packageNameFactory);
        }

        // Come up with package names until we get an original one.
        String newPackagePrefix;
        do
        {
            // Let the factory produce a package name.
            newPackagePrefix = newSuperPackagePrefix +
                               packageNameFactory.nextName() +
                               ClassConstants.INTERNAL_PACKAGE_SEPARATOR;
        }
        while (packagePrefixMap.containsValue(newPackagePrefix));

        return newPackagePrefix;
    }


    /**
     * Creates a new class name in the given new package.
     */
    private String generateUniqueClassName(String newPackagePrefix)
    {
        // Find the right name factory for this package.
        NameFactory classNameFactory =
            (NameFactory)packagePrefixClassNameFactoryMap.get(newPackagePrefix);
        if (classNameFactory == null)
        {
            // We haven't seen classes in this package before. Create a new name
            // factory for them.
            classNameFactory = new SimpleNameFactory(useMixedCaseClassNames);
            packagePrefixClassNameFactoryMap.put(newPackagePrefix,
                                                 classNameFactory);
        }

        // Come up with class names until we get an original one.
        String newClassName;
        do
        {
            // Let the factory produce a class name.
            newClassName = newPackagePrefix +
                           classNameFactory.nextName();
        }
        while (classNamesToAvoid.contains(mixedCaseClassName(newClassName)));

        return newClassName;
    }

    /**
     * Returns the given class name, unchanged if mixed-case class names are
     * allowed, or the lower-case version otherwise.
     */
    private String mixedCaseClassName(String className)
    {
        return useMixedCaseClassNames ?
            className :
            className.toLowerCase();
    }


    /**
     * Assigns a new name to the given class.
     * @param clazz the given class.
     * @param name  the new name.
     */
    static void setNewClassName(Clazz clazz, String name)
    {
        clazz.setVisitorInfo(name);
    }


    /**
     * Retrieves the new name of the given class.
     * @param clazz the given class.
     * @return the class's new name, or <code>null</code> if it doesn't
     *         have one yet.
     */
    static String newClassName(Clazz clazz)
    {
        Object visitorInfo = clazz.getVisitorInfo();

        return visitorInfo instanceof String ?
            (String)visitorInfo :
            null;
    }
}
