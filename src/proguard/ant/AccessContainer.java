/* $Id: AccessContainer.java,v 1.4 2003/12/19 04:17:03 eric Exp $
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
 * Interface for all tasks that handle nested access tasks.
 *
 * @author Dirk Schnelle
 */
public interface AccessContainer
{
    /**
     * Adds the given access string to the list of configured access flags.
     *
     * @param access Name of the access flag.
     *
     * @exception BuildException Error adding the access string
     */
    public void addAccess(String access)
            throws BuildException;
}
