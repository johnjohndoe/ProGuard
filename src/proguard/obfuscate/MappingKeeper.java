/* $Id: MappingKeeper.java,v 1.3 2003/12/06 22:15:38 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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


/**
 * This MappingKeeper applies the mappings that it receives to its class pool,
 * so these mappings are ensured in a subsequent obfuscation step.
 *
 * @author Eric Lafortune
 */
public class MappingKeeper implements MappingProcessor
{
    private ClassPool programClassPool;

    // A field acting as a parameter
    private ProgramClassFile programClassFile;


    /**
     * Creates a new ClassFileObfuscator.
     * @param programClassPool   the class pool in which class names have to be
     *                           unique.
     */
    public MappingKeeper(ClassPool programClassPool)
    {
        this.programClassPool = programClassPool;
    }


    // Implementations for MappingProcessor.

    public boolean processClassFileMapping(String className,
                                           String newClassName)
    {
        // Find the class.
        String name = ClassUtil.internalClassName(className);

        programClassFile = (ProgramClassFile)programClassPool.getClass(name);
        if (programClassFile != null)
        {
            // Make sure the mapping name will be kept.
            String newName = ClassUtil.internalClassName(newClassName);
            
            ClassFileObfuscator.setNewClassName(programClassFile, newName);

            // The class members have to be kept as well.
            return true;
        }

        return false;
    }


    public void processFieldMapping(String className,
                                    String fieldType,
                                    String fieldName,
                                    String newFieldName)
    {
        // Find the field.
        String name       = fieldName;
        String descriptor = ClassUtil.internalType(fieldType);

        FieldInfo fieldInfo = programClassFile.findField(name, descriptor);
        if (fieldInfo != null)
        {
            // Make sure the mapping name will be kept.
            MemberInfoObfuscator.setNewMemberName(fieldInfo, newFieldName);
        }
    }


    public void processMethodMapping(String className,
                                     int    firstLineNumber,
                                     int    lastLineNumber,
                                     String methodReturnType,
                                     String methodNameAndArguments,
                                     String newMethodName)
    {
        // Find the method.
        String name       = ClassUtil.externalMethodName(methodNameAndArguments);
        String descriptor = ClassUtil.internalMethodDescriptor(methodReturnType,
                                                               methodNameAndArguments);

        MethodInfo methodInfo = programClassFile.findMethod(name, descriptor);
        if (methodInfo != null)
        {
            // Make sure the mapping name will be kept.
            MemberInfoObfuscator.setNewMemberName(methodInfo, newMethodName);
        }
    }
}
