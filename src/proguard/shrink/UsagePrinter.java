/* $Id: UsagePrinter.java,v 1.9 2002/05/23 19:19:58 eric Exp $
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
package proguard.shrink;

import proguard.classfile.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

import java.io.*;


/**
 * This ClassFileVisitor prints out the class files and class members that have been
 * marked as being used (or not used).
 *
 * @see UsageMarker
 *
 * @author Eric Lafortune
 */
public class UsagePrinter
  implements ClassFileVisitor,
             MemberInfoVisitor
{
    private boolean     printUnusedItems;
    private PrintStream ps;

    // A field to remember the class name, if a header is needed for class members.
    private String      className;


    /**
     * Creates a new UsagePrinter that prints to <code>System.out</code>.
     * @param printUsedItems a flag that indicates whether only unused items
     * should be printed, or alternatively, only used items.
     */
    public UsagePrinter(boolean printUnusedItems)
    {
        this(printUnusedItems, System.out);
    }


    /**
     * Creates a new UsagePrinter that prints to the given stream.
     * @param printUsedItems a flag that indicates whether only unused items
     * should be printed, or alternatively, only used items.
     * @param printStream the stream to which to print
     */
    public UsagePrinter(boolean printUnusedItems, PrintStream printStream)
    {
        this.printUnusedItems = printUnusedItems;
        this.ps               = printStream;
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        if (UsageMarker.isUsed(programClassFile))
        {
            if (printUnusedItems)
            {
                className = programClassFile.getName();

                programClassFile.fieldsAccept(this);
                programClassFile.methodsAccept(this);

                className = null;
            }
            else
            {
                ps.println(ClassUtil.externalClassName(programClassFile.getName()));
            }
        }
        else
        {
            if (printUnusedItems)
            {
                ps.println(ClassUtil.externalClassName(programClassFile.getName()));
            }
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
    }


    // Implementations for MemberInfoVisitor

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programfieldInfo)
    {
        if (UsageMarker.isUsed(programfieldInfo) ^ printUnusedItems)
        {
            printClassNameHeader();

            ps.println("    " +
                       lineNumberRange(programClassFile, programfieldInfo) + ":" +
                       ClassUtil.externalFullFieldDescription(
                           programfieldInfo.getAccessFlags(),
                           programfieldInfo.getName(programClassFile),
                           programfieldInfo.getDescriptor(programClassFile)));
        }
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        if (UsageMarker.isUsed(programMethodInfo) ^ printUnusedItems)
        {
            printClassNameHeader();

            ps.println("    " +
                       lineNumberRange(programClassFile, programMethodInfo) + ":" +
                       ClassUtil.externalFullMethodDescription(
                           programClassFile.getName(),
                           programMethodInfo.getAccessFlags(),
                           programMethodInfo.getName(programClassFile),
                           programMethodInfo.getDescriptor(programClassFile)));
        }
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}


    // Small utility methods.

    /**
     * Prints the class name field. The field is then cleared, so it is not
     * printed again.
     */
    private void printClassNameHeader()
    {
        if (className != null)
        {
            ps.println(ClassUtil.externalClassName(className) + ":");
            className = null;
        }
    }


    /**
     * Returns the line number range of the given class member as a String,
     * if it can find it.
     */
    private String lineNumberRange(ProgramClassFile programClassFile, ProgramMemberInfo programMemberInfo)
    {
        CodeAttrInfo codeAttribute =
            (CodeAttrInfo)programMemberInfo.getAttribute(programClassFile,
                                                     ClassConstants.ATTR_Code);
        if (codeAttribute == null)
        {
            return "0:0";
        }

        LineNumberTableAttrInfo lineNumberTableAttribute =
            (LineNumberTableAttrInfo)codeAttribute.getAttribute(programClassFile,
                                                                ClassConstants.ATTR_LineNumberTable);
        if (lineNumberTableAttribute == null)
        {
            return "0:0";
        }

        return "" +
               lineNumberTableAttribute.getLineNumber(0) +
               ":" +
               lineNumberTableAttribute.getLineNumber(Integer.MAX_VALUE);
    }
}
