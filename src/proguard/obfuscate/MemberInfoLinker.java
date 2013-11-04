/* $Id: MemberInfoLinker.java,v 1.7 2004/08/15 12:39:30 eric Exp $
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

import proguard.classfile.*;
import proguard.classfile.visitor.*;

import java.util.*;


/**
 * This ClassFileVisitor links all class members that should get the same names
 * in the name spaces of all visited class files. A class file's name space
 * encompasses all of its subclasses and interfaces. It is typically a class file
 * that is not being subclassed. Chains of links that have been created in
 * previous invocations are merged with new chains of links, in order to create
 * a consistent set of chains. Class initialization methods and constructors are
 * ignored.
 *
 * @see MemberInfoObfuscator
 *
 * @author Eric Lafortune
 */
public class MemberInfoLinker
  implements ClassFileVisitor,
             MemberInfoVisitor
{
    // An object that is reset and reused every time.
    // The map: [class member name+descriptor - class member info]
    private final Map memberInfoMap = new HashMap();


    // Implementations for ClassFileVisitor.

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Collect all members in this class's name space.
        programClassFile.hierarchyAccept(true, true, true, false,
                                         new AllMemberInfoVisitor(this));

        // Clean up for obfuscation of the next name space.
        memberInfoMap.clear();
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
        visitMemberInfo(libraryClassFile, libraryFieldInfo);
    }


    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
    {
        visitMethodInfo(libraryClassFile, libraryMethodInfo);
    }


    /**
     * Links the given method into the chains of links. Class initialization
     * methods and constructors are ignored.
     * @param classFile  the class file of the given method.
     * @param methodInfo the method to be linked.
     */
    private void visitMethodInfo(ClassFile classFile, MethodInfo methodInfo)
    {
        // Special cases: <clinit> and <init> are always kept unchanged.
        // We can ignore them here.
        String name = methodInfo.getName(classFile);
        if (name.equals(ClassConstants.INTERNAL_METHOD_NAME_CLINIT) ||
            name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT))
        {
            return;
        }

        visitMemberInfo(classFile, methodInfo);
    }


    /**
     * Links the given class member into the chains of links.
     * @param classFile  the class file of the given member.
     * @param memberInfo the class member to be linked.
     */
    private void visitMemberInfo(ClassFile classFile, MemberInfo memberInfo)
    {
        // Get the member's original name and descriptor.
        String descriptor = memberInfo.getDescriptor(classFile);
        String name       = memberInfo.getName(classFile);

        // Get the last member in the chain.
        MemberInfo thisLastMemberInfo = lastMemberInfo(memberInfo);

        // See if we've already come across a member with the same name and
        // descriptor.
        String key = name + descriptor;
        MemberInfo otherMemberInfo = (MemberInfo)memberInfoMap.get(key);

        if (otherMemberInfo == null)
        {
            // Store the new class member info in the map.
            memberInfoMap.put(key, thisLastMemberInfo);
        }
        else
        {
            // Get the last member in the other chain.
            MemberInfo otherLastMemberInfo = lastMemberInfo(otherMemberInfo);

            // Check if both link chains aren't already ending in the same element.
            if (thisLastMemberInfo != otherLastMemberInfo)
            {
                // Merge the two chains, making sure LibraryMemberInfo elements,
                // if any, are at the end of the resulting chain.
                if (thisLastMemberInfo instanceof LibraryMemberInfo)
                {
                    // This class member chain ends with a library class member.
                    // Link this chain to the end of the other one.
                    otherLastMemberInfo.setVisitorInfo(thisLastMemberInfo);
                }
                /* We can skip this test and go straight to the final case.
                else if (otherLastVisitorAccepter instanceof LibraryMemberInfo)
                {
                    // The other member chain ends with a library class member.
                    // Link the other chain to the end of this one.
                    thisLastVisitorAccepter.setVisitorInfo(otherLastVisitorAccepter);
                }
                */
                else
                {
                    // We have two non-library class members. Link their chains
                    // one way or another.
                    thisLastMemberInfo.setVisitorInfo(otherLastMemberInfo);
                }
            }
        }
    }


    // Small utility methods.

    /**
     * Finds the last class member in the linked list of class members.
     * @param memberInfo the given class member.
     * @return the last class member in the linked list.
     */
    static MemberInfo lastMemberInfo(MemberInfo memberInfo)
    {
        VisitorAccepter lastVisitorAccepter = memberInfo;
        while (lastVisitorAccepter.getVisitorInfo() != null &&
               lastVisitorAccepter.getVisitorInfo() instanceof VisitorAccepter)
        {
            lastVisitorAccepter = (VisitorAccepter)lastVisitorAccepter.getVisitorInfo();
        }

        return (MemberInfo)lastVisitorAccepter;
    }
}
