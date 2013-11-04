/* $Id: KeepAttribute.java,v 1.5 2003/03/03 19:11:45 eric Exp $
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

/**
 * Collect the attributes to keep.
 *
 * @author Dirk Schnelle
 */
public class KeepAttribute implements Subtask
{
    /** Name of the attribute to add. */
    private String attribute;


    /**
     * Defaults constructor.
     */
    public KeepAttribute()
    {
    }


    /**
     * Adds an attribute.
     * @param attribute Name of the attribute to add.
     */
    public void setName(String attribute)
    {
        this.attribute = attribute;
    }


    /**
     * Validates this subtask.
     */
    public void validate() throws BuildException
    {
        if (attribute == null)
        {
            throw new BuildException("name is a required attribute for the keepattribute task");
        }
    }


    /**
     * Executes this subtask for the given parent task.
     * @param parent Parent task object.
     */
    public void execute(ProGuardTask parent)
    {
        parent.addKeepattribute(attribute);
    }
}