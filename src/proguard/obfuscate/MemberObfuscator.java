/* $Id: MemberObfuscator.java,v 1.14 2002/08/29 18:02:25 eric Exp $
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
import proguard.classfile.visitor.*;

import java.util.*;


/**
 * This ClassFileVisitor visits all the class members of visited class files,
 * collecting their names. After having visited any number of class files,
 * it can generate obfuscated class member names that are not conflicting.
 *
 * @author Eric Lafortune
 */
class      MemberObfuscator
implements ClassFileVisitor,
           MemberInfoVisitor
{
    private static final char UNIQUE_SUFFIX = '_';

    private boolean allowAggressiveOverloading;

    // Some objects that are reset and reused every time.

    // The main hashtable: [class member descriptor - nested hashtable]
    // The nested hashtables: [class member name - class member info]
    private final Hashtable descriptorHashtable = new Hashtable();

    private final NameFactory uniqueNameFactory = new NameFactory();
    private final NameFactory nameFactory       = new NameFactory();
    private final HashSet     namesToAvoid      = new HashSet();


    /**
     * Creates a new MemberObfuscator.
     * @param allowAggressiveOverloading a flag that specifies whether class
     *                                   members can be overloaded aggressively.
     */
    public MemberObfuscator(boolean allowAggressiveOverloading)
    {
        this.allowAggressiveOverloading = allowAggressiveOverloading;
    }


    /**
     * Creates a set of obfuscated names for all members in the name space of
     * the given class file.
     */
    public void obfuscate(ClassFile classFile)
    {
        // Collect method names in this class's name space.
        classFile.accept(new ClassFileUpDownTraveler(true, true, true, false,
                                                     this));

        // Process the method name space of each descriptor in turn.
        Enumeration descriptorEnumeration = descriptorHashtable.elements();
        while (descriptorEnumeration.hasMoreElements())
        {
            Hashtable memberNameHashtable = (Hashtable)descriptorEnumeration.nextElement();

            // First collect all names that are already set in this name space.
            // Make sure no two members already have the same name.
            Enumeration memberNameEnumeration = memberNameHashtable.elements();
            while (memberNameEnumeration.hasMoreElements())
            {
                VisitorAccepter memberInfo = (VisitorAccepter)memberNameEnumeration.nextElement();
                memberInfo = lastVisitorAccepter(memberInfo);

                // Is it a library member?
                if (memberInfo instanceof LibraryMemberInfo)
                {
                    // Make sure its name won't be used as a new name.
                    namesToAvoid.add(((LibraryMemberInfo)memberInfo).getName(null));
                }
                else
                {
                    // Does the class member have a name?
                    String name = newMemberName(memberInfo);
                    if (name != null)
                    {
                        // Only deal with it if it's not <clinit> or <init>.
                        if (!(name.equals(ClassConstants.INTERNAL_METHOD_NAME_CLINIT) ||
                              name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT)))
                        {
                            // Is this name already being used by another method
                            // in this name space?
                            if (namesToAvoid.contains(name))
                            {
                                // We can't have that. Reassign a globally unique name.
                                String newName = uniqueNameFactory.nextName() + UNIQUE_SUFFIX;
                                setNewMemberName(memberInfo, newName);
                            }
                            else
                            {
                                // Make sure this name won't be used as a new name.
                                namesToAvoid.add(name);
                            }
                        }
                    }
                }
            }

            // Then assign unique names to class members that didn't have a name yet.
            memberNameEnumeration = memberNameHashtable.elements();
            while (memberNameEnumeration.hasMoreElements())
            {
                VisitorAccepter programMemberInfo = (VisitorAccepter)memberNameEnumeration.nextElement();
                programMemberInfo = lastVisitorAccepter(programMemberInfo);

                // Is it a library member?
                if (!(programMemberInfo instanceof LibraryMemberInfo))
                {
                    String name = newMemberName(programMemberInfo);
                    if (name == null)
                    {
                        // Find a locally unique new name.
                        String newName;
                        do
                        {
                            newName = nameFactory.nextName();
                        }
                        while (namesToAvoid.contains(newName));

                        // Assign the new name.
                        setNewMemberName(programMemberInfo, newName);
                    }
                }
            }

            // Start with a fresh set of names for the next name space.
            nameFactory.reset();
            namesToAvoid.clear();
        }

        // Clean up for obfuscation of the next name space.
        descriptorHashtable.clear();
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Collect the names of all class members in the hashtable.
        programClassFile.fieldsAccept(this);
        programClassFile.methodsAccept(this);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        // Collect the names of all class members in the hashtable.
        libraryClassFile.fieldsAccept(this);
        libraryClassFile.methodsAccept(this);
    }


    // Implementations for MemberInfoVisitor

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo)
    {
        visitMemberInfo(programClassFile, programFieldInfo);
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        String name = programMethodInfo.getName(programClassFile);

        // Special case: <clinit> is always kept unchanged.
        if (name.equals(ClassConstants.INTERNAL_METHOD_NAME_CLINIT))
        {
            return;
        }

        // Special case: <init> is always kept unchanged,
        // irrespective of the descriptor.
        if (name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT))
        {
            // The descriptor may have to be updated later on though,
            // so we mark the method name as if it is new.
            setNewMemberName(programMethodInfo, ClassConstants.INTERNAL_METHOD_NAME_INIT);
            return;
        }

        visitMemberInfo(programClassFile, programMethodInfo);
    }


    private void visitMemberInfo(ProgramClassFile programClassFile, ProgramMemberInfo programMemberInfo)
    {
        // Get the member's original name and descriptor.
        String descriptor = programMemberInfo.getDescriptor(programClassFile);
        String name       = programMemberInfo.getName(programClassFile);

        // Get any new name already assigned to this member in this class, earlier.
        VisitorAccepter thisLastVisitorAccepter = lastVisitorAccepter(programMemberInfo);

        // Get any name already assigned to this class member in the other classes.
        Hashtable memberNameHashtable = retrieveNameHashtable(descriptor);

        VisitorAccepter otherMemberInfo = (VisitorAccepter)memberNameHashtable.get(name);

        if (otherMemberInfo == null)
        {
            // Store the new class member info in the hashtable.
            memberNameHashtable.put(name, thisLastVisitorAccepter);
        }
        else
        {
            VisitorAccepter otherLastVisitorAccepter = lastVisitorAccepter(otherMemberInfo);

            // Check if both members are already guarantueed to get the same name.
            if (thisLastVisitorAccepter != otherLastVisitorAccepter)
            {
                if (thisLastVisitorAccepter instanceof LibraryMemberInfo)
                {
                    // This class member chain ends with a library class member.
                    // Make sure we continue to use this class member's name.
                    otherLastVisitorAccepter.setVisitorInfo(thisLastVisitorAccepter);
                }
                else if (otherLastVisitorAccepter instanceof LibraryMemberInfo)
                {
                    // The other member chain ends with a library class member.
                    // Make sure we continue to use that class member's name.
                    thisLastVisitorAccepter.setVisitorInfo(otherLastVisitorAccepter);
                }
                else
                {
                    // We have two non-library class members.
                    // Check if either class member already has an assigned name.
                    String thisName  = newMemberName(thisLastVisitorAccepter);
                    String otherName = newMemberName(otherLastVisitorAccepter);

                    if (thisName == null)
                    {
                        // This class member chain doesn't have an assigned name.
                        // Make sure we continue to use the other member chain's name.
                        thisLastVisitorAccepter.setVisitorInfo(otherLastVisitorAccepter);
                    }
                    else if (otherName == null)
                    {
                        // The other class member chain doesn't have an assigned name.
                        // Make sure we continue to use this member chain's name.
                        otherLastVisitorAccepter.setVisitorInfo(thisLastVisitorAccepter);
                    }
                    else if (!thisName.equals(otherName))
                    {
                        // Both chains already have assigned names, and they're
                        // conflicting. This should be pretty rare. We'll
                        // assign a new name that is guarantueed to be globally
                        // unique.
                        String newName = uniqueNameFactory.nextName() + UNIQUE_SUFFIX;
                        setNewMemberName(thisLastVisitorAccepter, newName);

                        // Make sure we continue to use this member chain's name.
                        otherLastVisitorAccepter.setVisitorInfo(thisLastVisitorAccepter);
                    }
                }
            }
        }
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
    {
        visitLibraryMemberInfo(libraryClassFile, libraryFieldInfo);
    }


    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
    {
        // Special cases: <clinit> and <init> are always kept unchanged.
        // We can ignore them here.
        String name = libraryMethodInfo.getName(libraryClassFile);
        if (name.equals(ClassConstants.INTERNAL_METHOD_NAME_CLINIT) ||
            name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT))
        {
            return;
        }

        visitLibraryMemberInfo(libraryClassFile, libraryMethodInfo);
    }


    private void visitLibraryMemberInfo(LibraryClassFile libraryClassFile, LibraryMemberInfo libraryMemberInfo)
    {
        // Get the class member's original name and descriptor.
        String descriptor = libraryMemberInfo.getDescriptor(libraryClassFile);
        String name       = libraryMemberInfo.getName(libraryClassFile);

        // Get any name already assigned to this class member in the other classes.
        Hashtable memberNameHashtable = retrieveNameHashtable(descriptor);

        VisitorAccepter otherMemberInfo = (VisitorAccepter)memberNameHashtable.get(name);

        if (otherMemberInfo == null)
        {
            // Store the new class member info in the hashtable.
            memberNameHashtable.put(name, libraryMemberInfo);
        }
        else
        {
            VisitorAccepter otherLastVisitorAccepter = lastVisitorAccepter(otherMemberInfo);

            // Check if both class members are already guarantueed to get the same name.
            if (!(otherLastVisitorAccepter instanceof LibraryMemberInfo))
            {
                // Make sure the other class member also gets this library class member's name.
                otherLastVisitorAccepter.setVisitorInfo(libraryMemberInfo);
            }
        }
    }


    /**
     * Based on the main hashtable [descriptor - nested hashtable] and a given
     * descriptor, get the nested name hashtable. A new empty one is created if
     * necessary.
     * @param descriptor the class member descriptor
     * @return the nested hashtable [name - class member info]
     */
    private Hashtable retrieveNameHashtable(String descriptor)
    {
        // Check whether we're allowed to do aggressive overloading
        if (!allowAggressiveOverloading)
        {
            // Trim the return argument from the descriptor if not.
            // Works for fields and methods alike.
            descriptor = descriptor.substring(0, descriptor.indexOf(')')+1);
        }

        // See if we can find the nested hashtable with this descriptor key.
        Hashtable nameHashtable = (Hashtable)descriptorHashtable.get(descriptor);

        // Create a new one if not.
        if (nameHashtable == null)
        {
            nameHashtable = new Hashtable();
            descriptorHashtable.put(descriptor, nameHashtable);
        }

        return nameHashtable;
    }


    static void setNewMemberName(VisitorAccepter visitorAccepter, String name)
    {
        lastVisitorAccepter(visitorAccepter).setVisitorInfo(name);
    }


    static String newMemberName(VisitorAccepter visitorAccepter)
    {
        Object visitorInfo = lastVisitorInfo(visitorAccepter);

        return visitorInfo != null &&
               visitorInfo instanceof String ?
            (String)visitorInfo :
            null;
    }


    static Object lastVisitorInfo(VisitorAccepter visitorAccepter)
    {
        return lastVisitorAccepter(visitorAccepter).getVisitorInfo();
    }


    /**
     * Traverses the linked list of class members to find the last one.
     * The visitor info fields are used as pointers to the next class members.
     */
    private static VisitorAccepter lastVisitorAccepter(VisitorAccepter visitorAccepter)
    {
        VisitorAccepter lastVisitorAccepter = visitorAccepter;
        while (lastVisitorAccepter.getVisitorInfo() != null &&
               lastVisitorAccepter.getVisitorInfo() instanceof VisitorAccepter)
        {
            lastVisitorAccepter = (VisitorAccepter)lastVisitorAccepter.getVisitorInfo();
        }

        return lastVisitorAccepter;
    }
}
