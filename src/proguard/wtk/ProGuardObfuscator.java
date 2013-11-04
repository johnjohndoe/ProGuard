/* $Id: ProGuardObfuscator.java,v 1.5 2003/05/12 16:35:00 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.wtk;

import com.sun.kvem.environment.*;
import proguard.*;
import proguard.classfile.*;

import java.io.*;
import java.util.*;


/**
 * ProGuard plug-in for the J2ME Wireless Toolkit.
 * <p>
 * In order to integrate this plug-in in the toolkit, you'll have to put the
 * following lines in the file
 * {j2mewtk.dir}<code>/wtklib/Linux/ktools.properties</code> or
 * {j2mewtk.dir}<code>\wtklib\Windows\ktools.properties</code> (whichever is
 * applicable).
 * <p>
 * <pre>
 * obfuscator.runner.class.name: proguard.wtk.ProGuardObfuscator
 * obfuscator.runner.classpath: /usr/local/java/proguard1.6/lib/proguard.jar
 * </pre>
 * Please make sure the class path is set correctly for your system.
 *
 * @author Eric Lafortune
 */
public class ProGuardObfuscator implements Obfuscator
{
    // Implementations for Obfuscator

    public void createScriptFile(File jadFile,
                                 File projectDir)
    {
        // We don't really need to create a script file;
        // we'll just fill out all options in the run method.
    }


    public void run(File   obfuscatedJarFile,
                    String wtkBinDir,
                    String wtkLibDir,
                    String jarFileName,
                    String projectDirName,
                    String classPath,
                    String emptyAPI)
    throws IOException
    {
        // Create the ProGuard options.
        ProGuardOptions options = new ProGuardOptions();

        options.libraryJars = classPathElements(classPath);

        options.inJars = new ArrayList(1);
        options.inJars.add(jarFileName);

        options.resourceJars = new ArrayList(1);
        options.resourceJars.add(projectDirName + File.separator + "res");

        options.outJar = obfuscatedJarFile.getPath();

        // We want to keep all public MIDlets:
        // "-keep public class * extends javax.microedition.midlet.MIDlet".
        options.keepCommands = new ArrayList(1);
        options.keepCommands.add(new KeepCommand(ClassConstants.INTERNAL_ACC_PUBLIC,
                                                 0,
                                                 null,
                                                 "javax/microedition/midlet/MIDlet",
                                                 null,
                                                 true,
                                                 false,
                                                 false));

        // The preverify tool seems to unpack the resulting class files,
        // so we must not use mixed-case class names on Windows.
        options.useMixedCaseClassNames =
            !System.getProperty("os.name").regionMatches(true, 0, "windows", 0, 7);

        // We'll overload names with different return types.
        options.overloadAggressively = true;

        // We'll move all classes to the root package.
        options.defaultPackage = "";

        // Run ProGuard with these options.
        ProGuard proGuard = new ProGuard(options);
        proGuard.execute();
    }


    /**
     * Returns the individual elements of the given class path.
     */
    private List classPathElements(String classPath)
    {
        List list = new ArrayList();

        String separator = System.getProperty("path.separator");

        int index = 0;
        while (index < classPath.length())
        {
            int next_index = classPath.indexOf(separator, index);
            if (next_index < 0)
            {
                next_index = classPath.length();
            }

            list.add(classPath.substring(index, next_index));

            index = next_index + 1;
        }

        return list;
    }
}
