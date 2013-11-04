/* $Id: KeepClassMember.java,v 1.7 2003/08/04 08:46:45 eric Exp $
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
import proguard.classfile.*;

import java.util.*;

/**
 * Base for all subtasks that can take a class member.
 *
 * @author Dirk Schnelle
 */
public abstract class KeepClassMember implements AccessContainer
{
    private final static String ANY_CLASS_MEMBER_KEYWORD = "*";
    private final static String ANY_FIELD_KEYWORD = "<fields>";
    private final static String ANY_METHOD_KEYWORD = "<methods>";

    /** All access flags to be set.. */
    protected int accessFlags;
    /** All access flags to be unset. */
    protected int unsetAccessFlags;
    /** Type. */
    protected String type;
    /** Name of the class member. */
    protected String name;
    /** Nested access tasks. */
    private Collection nestedAccessTasks;
    /** Nested access flags- */
    private Collection nestedAccessFlags;


    /**
     * Defaults constructor
     */
    public KeepClassMember()
    {
        nestedAccessTasks = new java.util.ArrayList();

        nestedAccessFlags = null;

        this.type = getDefaultType();
    }


    /**
     * Gets the access parser for the access attributes of the member.
     * @return AccessParser.
     */
    protected abstract AccessParser getAccessParser();


    /**
     * Get the default type. This method is only called once when creating
     * new objects.
     */
    protected abstract String getDefaultType();

    /**
     * Sets the access flags in a string.
     * @param access Access flags to be set.
     */
    public void setAccess(String access)
    {
        if (!nestedAccessTasks.isEmpty())
        {
            throw new BuildException("Access cannot be set directly and in nested access tasks");
        }

        accessFlags = getAccessParser().getAccessFlags(access);
        unsetAccessFlags = getAccessParser().getUnsetAccessFlags(access);
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
            nestedAccessFlags = new ArrayList();
        }

        nestedAccessFlags.add(access);
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
     * Sets the type of the member
     * @param type Type of the member.
     */
    public void setType(String type)
    {
        if (ANY_CLASS_MEMBER_KEYWORD.equals(type))
        {
           this.type = null;
        }
        else
        {
           this.type = type;
        }
    }


    /**
     * Sets the name of the member. ANY-things are ignored.
     * @param name Name of the member.
     */
    public void setName(String name)
    {
        if (!ANY_CLASS_MEMBER_KEYWORD.equals(name) &&
            !ANY_FIELD_KEYWORD.equalsIgnoreCase(name) &&
            !ANY_METHOD_KEYWORD.equalsIgnoreCase(name))
        {
            this.name = name;
        }
    }


    /**
     * Executes this subtask for the given parent task.
     * @param parent Parent task object.
     */
    public abstract void execute(KeepClassSpecification parent);


    /**
     * Evaluates nested access tasks.
     */
    protected void evalNestedAccess() throws BuildException
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
            accessFlags = getAccessParser().getAccessFlags(nestedAccessFlags);
            unsetAccessFlags = getAccessParser().getUnsetAccessFlags(nestedAccessFlags);
        }
    }
}