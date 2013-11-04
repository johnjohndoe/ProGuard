/* $Id: KeepClassSpecification.java,v 1.6 2003/05/01 18:05:53 eric Exp $
 *
 * ProGuard - integration into Ant.
 *
 * Copyright (c) 2003 Dirk Schnelle (dirk.schnelle@web.de)
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
package proguard.ant;

import org.apache.tools.ant.*;
import proguard.*;
import proguard.classfile.*;
import proguard.classfile.util.*;

import java.util.*;

/**
 * Keep a class specification of any type.
 *
 * @author Dirk Schnelle
 */
public class KeepClassSpecification implements Subtask, AccessContainer
{
    private final static String CLASS_KEYWORD = "class";
    private final static String INTERFACE_KEYWORD = "interface";
    private final static String NOT_INTERFACEKEYWORD = "!interface";
    private final static String ANY_CLASS_KEYWORD = "*";

    /** The access parser to be use. */
    private final static AccessParser ACCESS_PARSER;

    /** The command specification. */
    private KeepCommand keepCommand;
    /** All access flags to be set. */
    private int accessFlags;
    /** All access flags to be unset. */
    private int unsetAccessFlags;
    /** Is it a class or an interface. */
    private String type = CLASS_KEYWORD;
    /** Name of the class that this class inherits from. */
    private String extendsClassName;
    /** Name of the class or interface. */
    private String name;
    /** Nested access tasks. */
    private Collection nestedAccessTasks;
    /** Nested access flags- */
    private Collection nestedAccessFlags;

    private boolean markClassFiles;
    private boolean markClassFilesConditionally;
    private boolean onlyKeepNames;

    /** members of the class to be kept. */
    private Collection keepMembers;


    static
    {
        Collection accessMapping = new java.util.ArrayList();
        accessMapping.add(ClassConstants.EXTERNAL_ACC_PUBLIC);
        accessMapping.add(ClassConstants.EXTERNAL_ACC_FINAL);
        accessMapping.add(ClassConstants.EXTERNAL_ACC_STATIC);

        ACCESS_PARSER = new AccessParser(accessMapping);
    }


    /**
     * Defaults constructor.
     */
    public KeepClassSpecification()
    {

    }


    /**
     * Initializes this class definition
     *
     * @param markClassFiles              Mark the class files
     * @param markClassFilesConditionally Mark the class files conditionally
     * @param onlyKeepNames               Only keep the names.
     */
    public void init(boolean markClassFiles,
                     boolean markClassFilesConditionally,
                     boolean onlyKeepNames)
    {
        this.markClassFiles = markClassFiles;
        this.markClassFilesConditionally = markClassFilesConditionally;
        this.onlyKeepNames = onlyKeepNames;

        keepMembers = new java.util.ArrayList();
        nestedAccessTasks = new java.util.ArrayList();

        nestedAccessFlags = null;
    }


    public void setMarkClassFiles(boolean on)
    {
        this.markClassFiles = on;
    }


    public void setMarkClassFilesConditionally(boolean on)
    {
        this.markClassFilesConditionally = on;
    }


    public void setOnlyKeepNames(boolean on)
    {
        this.onlyKeepNames = on;
    }


    /**
     * Adds a parsed field to this keep command.
     * @param requiredSetAccessFlags   Access flags to be set.
     * @param requiredUnsetAccessFlags Access flags to be unset.
     * @param name                     Name of the field
     * @param descriptor               Descriptor of the field
     * @param asName                   Not used.
     */
    public void keepField(int requiredSetAccessFlags,
                          int requiredUnsetAccessFlags,
                          String name,
                          String descriptor,
                          String asName)
    {
        keepCommand.keepField(requiredSetAccessFlags,
                              requiredUnsetAccessFlags,
                              name,
                              descriptor,
                              asName);
    }


    /**
     * Adds a nested field subtask
     * @param field Handler for the nested task.
     */
    public void addField(KeepField field)
    {
        keepMembers.add(field);
    }


    /**
     * Adds a parsed method descriptor.
     * @param requiredSetAccessFlags   Access flags to be set.
     * @param requiredUnsetAccessFlags Access flags to be unset.
     * @param name                     Name of the field
     * @param descriptor               Descriptor of the field
     * @param asName                   Not used.
     */
    public void keepMethod(int requiredSetAccessFlags,
                           int requiredUnsetAccessFlags,
                           String name,
                           String descriptor,
                           String asName)
    {
        keepCommand.keepMethod(requiredSetAccessFlags,
                               requiredUnsetAccessFlags,
                               name,
                               descriptor,
                               asName);

    }


    /**
     * Adds a nested method subtask
     * @param method Handler for the nested task.
     */
    public void addMethod(KeepMethod method)
    {
        keepMembers.add(method);
    }


    /**
     * Adds a nested access subtask
     * @param access Handler for the nested task.
     */
    public void addAccess(Access access)
    {
        nestedAccessTasks.add(access);
    }


    /**
     * Sets the access for this class definition.
     * @param access String of access flags
     * @throws BuildException
     *         Usage of an unknown flag.
     */
    public void setAccess(String access) throws BuildException
    {
        if (!nestedAccessTasks.isEmpty())
        {
            throw new BuildException("Access cannot be set directly and in nested access tasks");
        }

        accessFlags = ACCESS_PARSER.getAccessFlags(access);
        unsetAccessFlags = ACCESS_PARSER.getUnsetAccessFlags(access);
    }


    /**
     * Adds the given access string to the list of configured access flags.
     * @param access Name of the access flag.
     * @exception BuildException
     *            Error adding the access string
     */
    public void addAccess(String access) throws BuildException
    {
        if (nestedAccessFlags == null)
        {
            nestedAccessFlags = new java.util.ArrayList();
        }

        nestedAccessFlags.add(access);
    }


    /**
     * Sets the type of this class definition.
     * @param type One of class, interface, !interface
     * @throws BuildException
     *         Unknown type.
     */
    public void setType(String type) throws BuildException
    {
        if (CLASS_KEYWORD.equalsIgnoreCase(type))
        {
            this.type = CLASS_KEYWORD;
        }
        else if (INTERFACE_KEYWORD.equalsIgnoreCase(type))
        {
            this.type = type;
        }
        else if (NOT_INTERFACEKEYWORD.equalsIgnoreCase(type))
        {
            this.type = type;
        }
        else
        {
            throw new BuildException("Type for a class must match any of the following: class, interface, !interface");
        }
    }


    /**
     * Sets the name of the class which this class extends.
     * @param extendsClassName Name of the extender class.
     */
    public void setExtends(String extendsClassName)
    {
        this.extendsClassName = ClassUtil.internalClassName(extendsClassName);
    }


    /**
     * Sets the name of the interface which this class implements.
     * @param implementsClassName Name of the interface.
     */
    public void setImplements(String implementsClassName)
    {
        setExtends(implementsClassName);
    }


    /**
     * Sets the name of this class.
     * @param name Name of this class
     */
    public void setName(String name)
    {
        if (!ANY_CLASS_KEYWORD.equals(name))
        {
            this.name = ClassUtil.internalClassName(name);
        }
    }


    /**
     * Validates this subtask.
     */
    public void validate() throws BuildException
    {

    }


    /**
     * Executes this subtask for the given parent task.
     * @param parent Parent task object.
     */
    public void execute(ProGuardTask parent)
    {
        evalNestedAccess();

        keepCommand = new KeepCommand(accessFlags,
                                      unsetAccessFlags,
                                      name,
                                      extendsClassName,
                                      null,
                                      markClassFiles,
                                      markClassFilesConditionally,
                                      onlyKeepNames);

        parent.addKeepCommand(keepCommand);

        evalNestedMembers();
    }


    /**
     * Evaluates nested access tasks.
     * @exception BuildException
     *             Validation of nested access not successful.
     */
    private void evalNestedAccess() throws BuildException
    {
        Iterator iterator = nestedAccessTasks.iterator();
        while (iterator.hasNext())
        {
            Access access = (Access)iterator.next();
            access.validate();
            access.execute(this);
        }

        if (nestedAccessFlags != null)
        {
            accessFlags = ACCESS_PARSER.getAccessFlags(nestedAccessFlags);
            unsetAccessFlags = ACCESS_PARSER.getUnsetAccessFlags(nestedAccessFlags);
        }
    }


    /**
     * Evaluates nested members.
     */
    private void evalNestedMembers()
    {
        Iterator iterator = keepMembers.iterator();
        while (iterator.hasNext())
        {
            KeepClassMember member = (KeepClassMember)iterator.next();
            member.execute(this);
        }
    }
}