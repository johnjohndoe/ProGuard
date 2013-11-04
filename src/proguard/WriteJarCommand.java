/* $Id: WriteJarCommand.java,v 1.6 2002/05/19 15:53:37 eric Exp $
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
import proguard.classfile.visitor.*;

import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.io.*;


/**
 * This <code>Command</code> reads a jar and adds its class files to either the
 * library class pool or the program class pool.
 *
 * @author Eric Lafortune
 */
public class WriteJarCommand implements Command
{
    private JarWriter jarWriter;


    public WriteJarCommand(String jarFileName)
    {
        jarWriter = new JarWriter(jarFileName);
    }


    // Implementations for Command

    public void execute(int       phase,
                        ClassPool programClassPool,
                        ClassPool libraryClassPool)
    {
        if (phase == PHASE_WRITE)
        {
            executeWritingPhase(programClassPool);
        }
    }


    private void executeWritingPhase(ClassPool classPool)
    {
        System.out.println("Writing output jar [" + jarWriter.getJarFileName() + "]...");

        jarWriter.setManifest(classPool.getManifest());
        jarWriter.setComment(ProGuard.VERSION);

        try
        {
            jarWriter.open();
            classPool.classFilesAccept(jarWriter);
        }
        catch (Exception ex)
        {
            System.err.println("Can't write [" + jarWriter.getJarFileName() + "]");
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                jarWriter.close();
            }
            catch (IOException ex)
            {
            }
        }
    }
}
