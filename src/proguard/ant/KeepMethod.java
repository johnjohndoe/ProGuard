/* $Id: KeepMethod.java,v 1.8 2003/12/19 04:17:03 eric Exp $
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
 * ANY WARRAntY; without even the implied warranty of MERCHAntABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.ant;

import java.util.*;

import proguard.classfile.*;
import proguard.classfile.util.*;


/**
 * Keep a method.
 *
 * @author Dirk Schnelle
 */
public class KeepMethod
        extends KeepClassMember
{
    /** Access parser to be used. */
    private final static AccessParser ACCESS_PARSER;

    static
    {
        Collection accessMapping = new java.util.ArrayList();
        accessMapping.add(ClassConstants.EXTERNAL_ACC_PUBLIC);
        accessMapping.add(ClassConstants.EXTERNAL_ACC_PROTECTED);
        accessMapping.add(ClassConstants.EXTERNAL_ACC_PRIVATE);
        accessMapping.add(ClassConstants.EXTERNAL_ACC_STATIC);
        accessMapping.add(ClassConstants.EXTERNAL_ACC_VOLATILE);
        accessMapping.add(ClassConstants.EXTERNAL_ACC_TRANSIENT);

        ACCESS_PARSER = new AccessParser(accessMapping);
    }

    /** <code>true</code> if this method specifies a constructor. */
    private boolean constructor;

    /** Parameters of the method. */
    private String param;

    /**
     * Default constructor.
     */
    public KeepMethod()
    {
        super();

        this.constructor = false;
    }

    /**
     * Modificator for the constructor property.
     *
     * @param constructor <code>true</code> if this method specifies a
     *        constructor.
     */
    void setConstructor(boolean constructor)
    {
        this.constructor = constructor;
    }

    /**
     * Sets the parameters of this method.
     *
     * @param param The parameters of this method.
     */
    public void setParam(String param)
    {
        this.param = param;
    }

    /**
     * Gets the access parser for the access attributes of the member.
     *
     * @return AccessParser.
     */
    protected AccessParser getAccessParser()
    {
        return ACCESS_PARSER;
    }

    /**
     * Get the default type.
     *
     * @return Default type.
     */
    protected String getDefaultType()
    {
        return ClassConstants.EXTERNAL_TYPE_VOID;
    }

    /**
     * Executes this subtask for the given parent task.
     *
     * @param parent Parent task object.
     */
    public void execute(KeepClassSpecification parent)
    {
        evalNestedAccess();

        parent.keepMethod(accessFlags, unsetAccessFlags, getInternalName(),
            getDescriptor(), null);
    }

    /**
     * Gets the name to be passed to the keep method.
     *
     * @return Internal representation of the name of the method.
     */
    private String getInternalName()
    {
        // The internal name is always <init> for aconstructor.
        if (constructor)
        {
            return "<init>";
        }

        if (name == null)
        {
            return null;
        }

        int index = name.indexOf("(");

        if (index == -1)
        {
            return name;
        }

        return name.substring(0, index);
    }

    /**
     * Gets the descriptor to be passed to the keep method.
     *
     * @return Internal representation of the descriptor of the method.
     */
    private String getDescriptor()
    {
        if (name == null)
        {
            return null;
        }

        String paramSource = ((param == null)
            ? name
            : param);
        StringTokenizer tokenizer = new StringTokenizer(paramSource, "(), \t");

        if (!tokenizer.hasMoreTokens())
        {
            return null;
        }

        // Skip the name, if parsing from the name attribute.
        if (paramSource != param)
        {
            tokenizer.nextToken();
        }

        List args = new ArrayList();

        while (tokenizer.hasMoreTokens())
        {
            args.add(tokenizer.nextToken());
        }

        return ClassUtil.internalMethodDescriptor(type, args);
    }
}
