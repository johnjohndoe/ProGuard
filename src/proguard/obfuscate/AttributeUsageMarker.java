/* $Id: AttributeUsageMarker.java,v 1.9 2002/07/28 16:57:22 eric Exp $
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
 * This ClassFileVisitor marks all attributes that should be kept in the classes
 * it visits.
 *
 * @see AttributeShrinker
 *
 * @author Eric Lafortune
 */
public class AttributeUsageMarker
  implements ClassFileVisitor,
             MemberInfoVisitor,
             AttrInfoVisitor,
             InnerClassesInfoVisitor
{
    // A visitor info flag to indicate the attribute is being used.
    private static final Object USED = new Object();


    // Flags to specify whether optional attributes should be kept anyway.
    private boolean keepAllAttributes;
    private boolean keepAllUnknownAttributes;
    private boolean keepAllKnownAttributes;
    private Set     keepUnknownAttributes;
    private boolean keepInnerClassNameAttribute;
    private boolean keepLineNumberTableAttribute;
    private boolean keepLocalVariableTableAttribute;
    private boolean keepSourceFileAttribute;
    private boolean keepDeprecatedAttribute;
    private boolean keepSyntheticAttribute;


    /**
     * Specifies to keep all optional attributes.
     */
    public void keepAllAttributes()
    {
        keepAllAttributes = true;
    }

    /**
     * Specifies to keep all unknown attributes.
     */
    public void keepAllUnknownAttributes()
    {
        keepAllUnknownAttributes = true;
    }

    /**
     * Specifies to keep all known attributes.
     */
    public void keepAllKnownAttributes()
    {
        keepAllKnownAttributes = true;
    }


    /**
     * Specifies to keep optional attributes with the given names.
     */
    public void keepAttributes(String[] attributeNames)
    {
        for (int index = 0; index < attributeNames.length; index++)
        {
            keepAttribute(attributeNames[index]);
        }
    }


    /**
     * Specifies to keep optional attributes with the given name.
     */
    public void keepAttribute(String attributeName)
    {
        if      (attributeName.equals(ClassConstants.ATTR_InnerClasses))
        {
            keepInnerClassNameAttribute = true;
        }
        else if (attributeName.equals(ClassConstants.ATTR_LineNumberTable))
        {
            keepLineNumberTableAttribute = true;
        }
        else if (attributeName.equals(ClassConstants.ATTR_LocalVariableTable))
        {
            keepLocalVariableTableAttribute = true;
        }
        else if (attributeName.equals(ClassConstants.ATTR_SourceFile))
        {
            keepSourceFileAttribute = true;
        }
        else if (attributeName.equals(ClassConstants.ATTR_Deprecated))
        {
            keepDeprecatedAttribute = true;
        }
        else if (attributeName.equals(ClassConstants.ATTR_Synthetic))
        {
            keepSyntheticAttribute = true;
        }
        else
        {
            if (keepUnknownAttributes == null)
            {
                keepUnknownAttributes = new HashSet();
            }

            keepUnknownAttributes.add(attributeName);
        }
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Mark the class member attributes that should be kept.
        programClassFile.fieldsAccept(this);
        programClassFile.methodsAccept(this);

        // Mark the class attributes that should be kept.
        programClassFile.attributesAccept(this);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
    }


    // Implementations for MemberInfoVisitor

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programfieldInfo)
    {
        visitMemberInfo(programClassFile, programfieldInfo);
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        visitMemberInfo(programClassFile, programMethodInfo);
    }


    private void visitMemberInfo(ProgramClassFile programClassFile, ProgramMemberInfo programMemberInfo)
    {
        // Mark the class member attributes that should be kept.
        programMemberInfo.attributesAccept(programClassFile, this);
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}


    // Implementations for AttrInfoVisitor

    public void visitUnknownAttrInfo(ClassFile classFile, UnknownAttrInfo unknownAttrInfo)
    {
        if (keepAllAttributes ||
            keepAllUnknownAttributes ||
            (keepUnknownAttributes != null &&
             keepUnknownAttributes.contains(unknownAttrInfo.getAttributeName(classFile))))
        {
            markAsUsed(unknownAttrInfo);
        }
    }


    public void visitInnerClassesAttrInfo(ClassFile classFile, InnerClassesAttrInfo innerClassesAttrInfo)
    {
        markAsUsed(innerClassesAttrInfo);

        if (!keepAllAttributes &&
            !keepAllKnownAttributes &&
            !keepInnerClassNameAttribute)
        {
            // Clear references to the original inner class names.
            innerClassesAttrInfo.innerClassEntriesAccept(classFile, this);
        }
    }


    public void visitConstantValueAttrInfo(ClassFile classFile, ConstantValueAttrInfo constantValueAttrInfo)
    {
        markAsUsed(constantValueAttrInfo);
    }


    public void visitExceptionsAttrInfo(ClassFile classFile, ExceptionsAttrInfo exceptionsAttrInfo)
    {
        markAsUsed(exceptionsAttrInfo);
    }


    public void visitCodeAttrInfo(ClassFile classFile, CodeAttrInfo codeAttrInfo)
    {
        markAsUsed(codeAttrInfo);

        // Mark the code attributes that should be kept.
        codeAttrInfo.attributesAccept(classFile, this);
    }


    public void visitLineNumberTableAttrInfo(ClassFile classFile, LineNumberTableAttrInfo lineNumberTableAttrInfo)
    {
        if (keepAllAttributes ||
            keepAllKnownAttributes ||
            keepLineNumberTableAttribute)
        {
            markAsUsed(lineNumberTableAttrInfo);
        }
    }


    public void visitLocalVariableTableAttrInfo(ClassFile classFile, LocalVariableTableAttrInfo localVariableTableAttrInfo)
    {
        if (keepAllAttributes ||
            keepAllKnownAttributes ||
            keepLocalVariableTableAttribute)
        {
            markAsUsed(localVariableTableAttrInfo);
        }
    }


    public void visitSourceFileAttrInfo(ClassFile classFile, SourceFileAttrInfo sourceFileAttrInfo)
    {
        if (keepAllAttributes ||
            keepAllKnownAttributes ||
            keepSourceFileAttribute)
        {
            markAsUsed(sourceFileAttrInfo);
        }
    }


    public void visitDeprecatedAttrInfo(ClassFile classFile, DeprecatedAttrInfo deprecatedAttrInfo)
    {
        if (keepAllAttributes ||
            keepAllKnownAttributes ||
            keepDeprecatedAttribute)
        {
            markAsUsed(deprecatedAttrInfo);
        }
    }


    public void visitSyntheticAttrInfo(ClassFile classFile, SyntheticAttrInfo syntheticAttrInfo)
    {
        if (keepAllAttributes ||
            keepAllKnownAttributes ||
            keepSyntheticAttribute)
        {
            markAsUsed(syntheticAttrInfo);
        }
    }


    // Implementations for InnerClassesInfoVisitor

    public void visitInnerClassesInfo(ClassFile classFile, InnerClassesInfo innerClassesInfo)
    {
        // Clear the reference to the original inner class name, as used in
        // the source code.
        innerClassesInfo.u2innerNameIndex = 0;
    }


    // Small utility methods.

    /**
     * Marks the given VisitorAccepter as being used (or useful).
     * In this context, the VisitorAccepter will be an AttrInfo object.
     */
    private static void markAsUsed(VisitorAccepter visitorAccepter)
    {
        visitorAccepter.setVisitorInfo(USED);
    }


    /**
     * Returns whether the given VisitorAccepter has been marked as being used.
     * In this context, the VisitorAccepter will be an AttrInfo object.
     */
    static boolean isUsed(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter.getVisitorInfo() == USED;
    }
}
