/* $Id: MemberInfoObfuscator.java,v 1.4 2003/12/06 22:15:38 eric Exp $
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
package proguard.obfuscate;

import proguard.classfile.*;
import proguard.classfile.visitor.*;

import java.util.*;


/**
 * This ClassFileVisitor obfuscates all class members in the name spaces of all
 * visited class file. The class members must have been linked before applying this
 * visitor. The class file is typically a class file that is not being subclassed.
 *
 * @see MemberInfoLinker
 *
 * @author Eric Lafortune
 */
public class MemberInfoObfuscator
  implements ClassFileVisitor,
             MemberInfoVisitor
{
    private static final char UNIQUE_SUFFIX = '_';

    private boolean allowAggressiveOverloading;

    // Some objects that are reset and reused every time.

    // The main map: [class member descriptor - nested map]
    // The nested maps: [class member name - class member info]
    private final Map         descriptorMap     = new HashMap();

    private final NameFactory uniqueNameFactory = new NameFactory();
    private final NameFactory nameFactory       = new NameFactory();
    private final Set         namesToAvoid      = new HashSet();


    /**
     * Creates a new MemberObfuscator.
     * @param allowAggressiveOverloading a flag that specifies whether class
     *                                   members can be overloaded aggressively.
     */
    public MemberInfoObfuscator(boolean allowAggressiveOverloading)
    {
        this.allowAggressiveOverloading = allowAggressiveOverloading;
    }


    // Implementations for ClassFileVisitor.

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Collect method names in this class's name space.
        programClassFile.accept(new ClassFileUpDownTraveler(true, true, true, false,
                                new AllMemberInfoVisitor(this)));

        // Process the name space of each descriptor in turn.
        Iterator descriptorIterator = descriptorMap.values().iterator();
        while (descriptorIterator.hasNext())
        {
            Map memberNameMap = (Map)descriptorIterator.next();

            // First collect all names that are already in use in this name space.
            // Make sure no two class members already have the same name.
            Collection memberNames = memberNameMap.values();
            Iterator memberNameIterator = memberNames.iterator();
            while (memberNameIterator.hasNext())
            {
                MemberInfo memberInfo = (MemberInfo)memberNameIterator.next();

                // Does the class member already have a name?
                String newName = newMemberName(memberInfo);
                if (newName != null)
                {
                    // If so, is this name already being used by another class
                    // member in this name space?
                    if (namesToAvoid.contains(newName))
                    {
                        // We can't have that. Reassign a globally unique name.
                        String uniqueName =
                            uniqueNameFactory.nextName() + UNIQUE_SUFFIX;
                        setNewMemberName(memberInfo, uniqueName);
                    }
                    else
                    {
                        // Make sure this name won't be used as a new name.
                        namesToAvoid.add(newName);
                    }
                }
            }

            // Then assign unique names to class members that don't have a name yet.
            memberNameIterator = memberNames.iterator();
            while (memberNameIterator.hasNext())
            {
                MemberInfo memberInfo = (MemberInfo)memberNameIterator.next();

                // Does the class member already have a name?
                String newName = newMemberName(memberInfo);
                if (newName == null)
                {
                    // If not, find a locally unique new name.
                    do
                    {
                        newName = nameFactory.nextName();
                    }
                    while (namesToAvoid.contains(newName));

                    // Assign the new name.
                    setNewMemberName(memberInfo, newName);
                }
            }

            // Start with a fresh set of names for the next name space.
            nameFactory.reset();
            namesToAvoid.clear();
        }

        // Clean up for obfuscation of the next name space.
        descriptorMap.clear();
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
    }


    // Implementations for MemberInfoVisitor.

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo)
    {
        visitMemberInfo(programClassFile, programFieldInfo);
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        visitMethodInfo(programClassFile, programMethodInfo);
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
    {
        // Make sure the library field keeps its name.
        String name = libraryFieldInfo.getName(libraryClassFile);
        setNewMemberName(libraryFieldInfo, name);

        visitMemberInfo(libraryClassFile, libraryFieldInfo);
    }


    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
    {
        // Make sure the library method keeps its name.
        String name = libraryMethodInfo.getName(libraryClassFile);
        setNewMemberName(libraryMethodInfo, name);

        visitMethodInfo(libraryClassFile, libraryMethodInfo);
    }


    /**
     * Inserts the given method into the main map. Class initialization
     * methods and constructors are ignored.
     * @param classFile  the class file of the given method.
     * @param methodInfo the method to be linked.
     */
    private void visitMethodInfo(ClassFile classFile, MethodInfo methodInfo)
    {
        // Special cases: <clinit> and <init> are always kept unchanged.
        String name = methodInfo.getName(classFile);
        if (name.equals(ClassConstants.INTERNAL_METHOD_NAME_CLINIT) ||
            name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT))
        {
            return;
        }

        visitMemberInfo(classFile, methodInfo);
    }


    /**
     * Inserts the given class member into the main map.
     * @param classFile  the class file of the given member.
     * @param memberInfo the class member to be linked.
     */
    private void visitMemberInfo(ClassFile classFile, MemberInfo memberInfo)
    {
        // Get the member's original name and descriptor.
        String descriptor = memberInfo.getDescriptor(classFile);
        String name       = memberInfo.getName(classFile);

        // Put the [descriptor - name - member] triplet in the two-level map,
        // creating a new first-level map if necessary,
        // and overwriting a previous member if present.
        retrieveNameMap(descriptor).put(name, memberInfo);
    }


    // Small utility methods.

    /**
     * Gets the nested name map, based on the main map
     * [descriptor - nested map] and a given descriptor.
     * A new empty one is created if necessary.
     * @param descriptor the class member descriptor.
     * @return the nested map [name - class member info].
     */
    private Map retrieveNameMap(String descriptor)
    {
        // Check whether we're allowed to do aggressive overloading
        if (!allowAggressiveOverloading)
        {
            // Trim the return argument from the descriptor if not.
            // Works for fields and methods alike.
            descriptor = descriptor.substring(0, descriptor.indexOf(')')+1);
        }

        // See if we can find the nested map with this descriptor key.
        Map nameMap = (Map)descriptorMap.get(descriptor);

        // Create a new one if not.
        if (nameMap == null)
        {
            nameMap = new HashMap();
            descriptorMap.put(descriptor, nameMap);
        }

        return nameMap;
    }


    /**
     * Assigns a new name to the given class member.
     * @param memberInfo the given class member.
     * @param name       the new name.
     */
    static void setNewMemberName(MemberInfo memberInfo, String name)
    {
        MemberInfoLinker.lastMemberInfo(memberInfo).setVisitorInfo(name);
    }


    /**
     * Retrieves the new name of the given class member.
     * @param memberInfo the given class member.
     * @return the class member's new name, or <code>null</code> if it doesn't
     *         have one yet.
     */
    static String newMemberName(MemberInfo memberInfo)
    {
        return (String)MemberInfoLinker.lastMemberInfo(memberInfo).getVisitorInfo();
    }
}
