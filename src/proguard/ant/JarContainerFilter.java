/* $Id: JarContainerFilter.java,v 1.1 2003/12/19 04:17:03 eric Exp $
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
 * Specify those parts of a jar container that should be filtered.
 *
 * @author Dirk Schnelle
 */
public abstract class JarContainerFilter
{
    /** Reference to the jar container. */
    protected JarContainer parent;

    /**
     * Default constructor.
     */
    public JarContainerFilter() {}

    /**
     * Set the jar container.
     *
     * @param parent The parent jar container.
     */
    void setParent(JarContainer parent)
    {
        this.parent = parent;
    }

    /**
     * Specify a filter pattern.
     *
     * @param name Filter.
     */
    public void setName(String name)
    {
        applyFilter(name);
    }

    /**
     * Apply the filter to the jar container.
     *
     * @param filter Filter to be applied.
     */
    protected abstract void applyFilter(String filter);
}

