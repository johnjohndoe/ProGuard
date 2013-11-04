/* $Id: Command.java,v 1.4 2002/05/19 15:53:37 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard;

import proguard.classfile.*;


/**
 * This interface represents any ProGuard command. A command is
 * executed in subsequent phases, by calling the <code>{@link
 * #execute(int,ClassPool,ClassPool) execute}</code> method
 * with the different <code>phase</code> constants.
 *
 * @author Eric Lafortune
 */
public interface Command
{
    public final static int PHASE_INITIALIZE = 0;
    public final static int PHASE_CHECK      = 1;
    public final static int PHASE_SHRINK     = 2;
    public final static int PHASE_OBFUSCATE  = 3;
    public final static int PHASE_WRITE      = 4;


    /**
     * Executes this command in the given phase.
     * @param phase the execution phase, expressed by any of the
     *              following constants:
     * <ol>
     * <li><code>{@link #PHASE_INITIALIZE PHASE_INITIALIZE}</code>
     * <li><code>{@link #PHASE_CHECK      PHASE_CHECK     }</code>
     * <li><code>{@link #PHASE_SHRINK     PHASE_SHRINK    }</code>
     * <li><code>{@link #PHASE_OBFUSCATE  PHASE_OBFUSCATE }</code>
     * <li><code>{@link #PHASE_WRITE      PHASE_WRITE     }</code>
     * </ol>
     * @param libraryClassPool the class pool containing library
     *                         class files.
     * @param programClassPool the class pool containing program
     *                         class files.
     */
    public void execute(int       phase,
                        ClassPool programClassPool,
                        ClassPool libraryClassPool);
}
