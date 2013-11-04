/* $Id: KeepAttribute.java,v 1.7 2003/12/19 04:17:03 eric Exp $
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
 * ANY WARRANTY; without even the implied warranty of MERCHAntABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.ant;

import org.apache.tools.ant.*;


/**
 * Collect the attributes to keep.
 *
 * @author Dirk Schnelle
 */
public class KeepAttribute
        implements Subtask
{
    /** Name of the attribute to add. */
    private String attribute = null;

    /** Flag if the name of the attribute is set. */
    private boolean nameSet = false;

    /** Any attribute should be set. */
    private final static String ANY_ATTRIBUTE_KEYWORD = "*";

    /**
     * Defaults constructor.
     */
    public KeepAttribute() {}

    /**
     * Adds an attribute.
     *
     * @param attribute Name of the attribute to add.
     */
    public void setName(String attribute)
    {
        if (!ANY_ATTRIBUTE_KEYWORD.equals(attribute))
        {
            this.attribute = attribute;
        }

        nameSet = true;
    }

    /**
     * Validates this subtask.
     *
     * @exception BuildException Name is not set.
     */
    public void validate()
            throws BuildException
    {
        if (!nameSet)
        {
            throw new BuildException(
                "name is required for the keepattribute task");
        }
    }

    /**
     * Executes this subtask for the given parent task.
     *
     * @param parent Parent task object.
     */
    public void execute(ProGuardConfigurationTask parent)
    {
        parent.addKeepattribute(attribute);
    }
}
