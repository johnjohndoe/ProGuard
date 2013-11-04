/* $Id: JarContainerInclude.java,v 1.1 2003/12/19 04:17:03 eric Exp $
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

/**
 * Specify those parts of a jar container that should be included.
 *
 * @author Dirk Schnelle
 */
public class JarContainerInclude
        extends JarContainerFilter
{
    /**
     * Default constructor.
     */
    public JarContainerInclude() {}

    /**
     * Apply the filter to the jar container.
     *
     * @param filter Filter to be applied.
     */
    protected void applyFilter(String filter)
    {
        parent.include(filter);
    }
}
