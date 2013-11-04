/* $Id: Access.java,v 1.7 2003/12/19 04:17:03 eric Exp $
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

import org.apache.tools.ant.*;


/**
 * Access flag.
 *
 * @author Dirk Schnelle
 */
public class Access
{
    /** Name of the access flag. */
    private String access;

    /**
     * Defaults constructor.
     */
    public Access() {}

    /**
     * Adds an access flag.
     *
     * @param access Name of the access flag to add.
     */
    public void setName(String access)
    {
        this.access = access;
    }

    /**
     * Validates this subtask.
     *
     * @exception BuildException Validation not successful.
     */
    public void validate()
            throws BuildException
    {
        if (access == null)
        {
            throw new BuildException(
                "name is a required attribute for the access task!");
        }
    }

    /**
     * Executes this subtask for the given access container.
     *
     * @param accessContainer Parent task object.
     */
    public void execute(AccessContainer accessContainer)
    {
        accessContainer.addAccess(access);
    }
}
