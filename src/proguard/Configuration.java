/* $Id: Configuration.java,v 1.1 2003/08/22 17:11:15 eric Exp $
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
package proguard;

import java.util.*;


/**
 * The ProGuard configuration.
 *
 * @see ProGuard
 *
 * @author Eric Lafortune
 */
public class Configuration
{
    public ClassPath libraryJars;
    public ClassPath inJars;
    public ClassPath resourceJars;
    public ClassPath outJars;
    public List      keepClassFileOptions;
    public List      keepAttributes;
    public String    newSourceFileAttribute;
    public String    printSeeds;
    public String    printUsage;
    public String    printMapping;
    public String    applyMapping;
    public boolean   verbose;
    public String    dump;
    public boolean   ignoreWarnings;
    public boolean   warn                        = true;
    public boolean   note                        = true;
    public boolean   shrink                      = true;
    public boolean   obfuscate                   = true;
    public boolean   useMixedCaseClassNames      = true;
    public boolean   overloadAggressively;
    public String    defaultPackage;
    public boolean   skipNonPublicLibraryClasses = true;
}
