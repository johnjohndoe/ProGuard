/* $Id: KeepField.java,v 1.7 2003/08/04 08:46:45 eric Exp $
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

import proguard.classfile.*;
import proguard.classfile.util.*;

import java.util.*;

/**
 * Keep a field.
 *
 * @author Dirk SChnelle
 */
public class KeepField extends KeepClassMember
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

        ACCESS_PARSER = new AccessParser(accessMapping);
    }


    /**
     * Defaults constructor.
     */
    public KeepField()
    {
        super();
    }


    /**
     * Gets the access parser for the access attributes of the member.
     * @return AccessParser.
     */
    protected AccessParser getAccessParser()
    {
        return ACCESS_PARSER;
    }

    /**
     * Get the default type.
     */
    protected String getDefaultType()
    {
       return null;
    }


    /**
     * Executes this subtask for the given parent task.
     * @param parent Parent task object.
      */
    public void execute(KeepClassSpecification parent)
    {
        evalNestedAccess();

        parent.keepField(accessFlags,
                         unsetAccessFlags,
                         name,
                         type == null ? null : ClassUtil.internalType(type),
                         null);
    }
}