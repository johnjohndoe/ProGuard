/* $Id: SwitchInstruction.java,v 1.5 2003/02/09 15:22:28 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.classfile.instruction;



/**
 * This interface describes a switch instruction (either a table switch or
 * a lookup switch).
 *
 * @author Eric Lafortune
 */
public interface SwitchInstruction
{
    /**
     * Gets the switch's default offset.
     */
    public int getDefaultSwitchOffset();

    /**
     * Gets the switch's number of cases (not including the default one).
     */
    public int getSwitchCount();

    /**
     * Gets the switch's offset specified by the given index.
     * @param index the offset index, in the range from 0 to switchCount-1.
     */
    public int geSwitchOffset(int index);

    /**
     * Gets the switch's case value specified by the given index.
     * @param index the case value index, in the range from 0 to switchCount-1.
     */
    public int geSwitchCaseValue(int index);
}
