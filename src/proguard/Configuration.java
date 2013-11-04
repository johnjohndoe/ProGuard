/* $Id: Configuration.java,v 1.10 2004/08/28 17:03:30 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
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
    /**
     * A list of input and output entries (jars, wars, ears, zips, and directories).
     */
    public ClassPath programJars;

    /**
     * A list of library entries (jars, wars, ears, zips, and directories).
     */
    public ClassPath libraryJars;

    /**
     * A list of ClassPathSpecification instances, whose class names and class
     * member names are to be kept from shrinking, optimization, and obfuscation.
     */
    public List      keep;

    /**
     * A list of ClassPathSpecification instances, whose class names and class
     * member names are to be kept from obfuscation.
     */
    public List      keepNames;

    /**
     * A list of ClassPathSpecification instances, whose methods are assumed to
     * have no side effects.
     */
    public List      assumeNoSideEffects;

    /**
     * A list of String instances specifying optional attributes to be kept.
     * A <code>null</code> list means no attributes. An empty list means all
     * attributes.
     */
    public List      keepAttributes;

    public String    newSourceFileAttribute;
    public String    printSeeds;
    public String    printUsage;
    public String    printMapping;
    public String    applyMapping;
    public String    dump;
    public boolean   verbose                     = false;
    public boolean   ignoreWarnings              = false;
    public boolean   warn                        = true;
    public boolean   note                        = true;
    public boolean   shrink                      = true;
    public boolean   optimize                    = true;
    public boolean   obfuscate                   = true;
    public boolean   allowAccessModification     = false;
    public boolean   useMixedCaseClassNames      = true;
    public boolean   overloadAggressively        = false;
    public String    defaultPackage;
    public boolean   skipNonPublicLibraryClasses = true;
}
