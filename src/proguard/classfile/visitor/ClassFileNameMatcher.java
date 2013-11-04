/* $Id: ClassFileNameMatcher.java,v 1.1 2002/09/01 16:41:35 eric Exp $
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
package proguard.classfile.visitor;

import proguard.classfile.*;

import java.util.Vector;


/**
 * This utility class matches class names with a given regular expression.
 * The class names are assumed to be in internal format (with slashes, not dots).
 *
 * @author Eric Lafortune
 */
class ClassFileNameMatcher
{
    private static final String MULTIPLE_CHARACTERS_WILDCARD1 = "*";
    private static final String MULTIPLE_CHARACTERS_WILDCARD2 = "**";
    private static final String SINGLE_CHARACTER_WILDCARD     = "?";

    private String[] expressionParts;


    /**
     * Creates a new ClassFileNameMatcher.
     * @param regularExpression the regular expression against which class names
     *                          will be matched.
     */
    public ClassFileNameMatcher(String regularExpression)
    {
        // Split the given regular expression into an array of parts: "*", "**",
        // "?", and simple text strings.

        // A Vector to collect the subsequent regular expression parts.
        Vector expressionPartsVector = new Vector();

        String wildcard      = null;
        int    previousIndex = 0;
        int    index         = 0;
        int    regularExpressionLength = regularExpression.length();
        while (index < regularExpressionLength)
        {
            wildcard =
                regularExpression.regionMatches(index, MULTIPLE_CHARACTERS_WILDCARD2, 0, MULTIPLE_CHARACTERS_WILDCARD2.length()) ? MULTIPLE_CHARACTERS_WILDCARD2 :
                regularExpression.regionMatches(index, MULTIPLE_CHARACTERS_WILDCARD1, 0, MULTIPLE_CHARACTERS_WILDCARD1.length()) ? MULTIPLE_CHARACTERS_WILDCARD1 :
                regularExpression.regionMatches(index, SINGLE_CHARACTER_WILDCARD,     0, SINGLE_CHARACTER_WILDCARD.length()) ?     SINGLE_CHARACTER_WILDCARD     :
                                                                                                                                    null;
            if (wildcard != null)
            {
                // Add the simple text string that we've skipped.
                if (previousIndex < index)
                {
                    expressionPartsVector.addElement(regularExpression.substring(previousIndex, index));
                }

                // Add the wildcard that we've found.
                expressionPartsVector.addElement(wildcard);

                // We'll continue parsing after this wildcard.
                index += wildcard.length();
                previousIndex = index;
            }
            else
            {
                // We'll continue parsing at the next character.
                index++;
            }
        }

        // Add the final simple text string that we've skipped, if any.
        if (wildcard == null)
        {
            expressionPartsVector.addElement(regularExpression.substring(previousIndex));
        }

        // Copy the Vector into the array.
        expressionParts = new String[expressionPartsVector.size()];
        expressionPartsVector.copyInto(expressionParts);
    }


    /**
     * Tries to match the given name.
     * @boolean a boolean indicating whether the regular expression matches the
     *          given name.
     */
    public boolean matches(String name)
    {
        return matches(name, 0, 0);
    }


    /**
     * Tries to match the given name, starting at the given index, with the
     * regular expression parts starting at the given index.
     */
    private boolean matches(String name,
                            int    nameIndex,
                            int    expressionIndex)
    {
        // Are we out of expression parts?
        if (expressionIndex == expressionParts.length)
        {
            // There's a match, at least if we're at the end of the name as well.
            return nameIndex == name.length();
        }

        String expressionPart = expressionParts[expressionIndex];

        // Did we get two subsequent wildcards?
        if (expressionPart.equals(MULTIPLE_CHARACTERS_WILDCARD1))
        {
            // Try out all possible matches for '*', not matching the package
            // separator.
            for (int nextNameIndex = nameIndex;
                 nextNameIndex == nameIndex ||
                 (nextNameIndex <= name.length() &&
                  name.charAt(nextNameIndex-1) != ClassConstants.INTERNAL_PACKAGE_SEPARATOR);
                 nextNameIndex++)
            {
                // Continue looking for a match of the next expression part,
                // starting from this index.
                if (matches(name, nextNameIndex, expressionIndex + 1))
                {
                    return true;
                }
            }

            // We couldn't get a match for '*' and the rest of the expression
            // parts.
            return false;
        }
        else if (expressionPart.equals(MULTIPLE_CHARACTERS_WILDCARD2))
        {
            // Try out all possible matches for '**'.
            for (int nextNameIndex = nameIndex;
                 nextNameIndex <= name.length();
                 nextNameIndex++)
            {
                // Continue looking for a match of the next expression part,
                // starting from this index.
                if (matches(name, nextNameIndex, expressionIndex + 1))
                {
                    return true;
                }
            }

            // We couldn't get a match for '**' and the rest of the expression
            // parts.
            return nameIndex == name.length();
        }
        else if (expressionPart.equals(SINGLE_CHARACTER_WILDCARD))
        {
            // Skip a single character (not a package separator) and check if
            // the rest of the expression parts match.
            return
                nameIndex < name.length() &&
                name.charAt(nameIndex) != ClassConstants.INTERNAL_PACKAGE_SEPARATOR &&
                matches(name, nameIndex + 1, expressionIndex + 1);
        }
        else
        {
            // The expression part is a simple text string. Check if it matches,
            // and if the rest of the expression parts match.
            int expressionPartLength = expressionPart.length();
            return
                name.regionMatches(nameIndex, expressionPart, 0, expressionPartLength) &&
                matches(name, nameIndex + expressionPartLength, expressionIndex + 1);
        }
    }


    /**
     * A main method for testing class name matching.
     */
    private static void main(String[] args)
    {
        try
        {
            System.out.println("Regular expression  ["+args[0]+"]");
            System.out.println("Internal class name ["+args[1]+"]");
            ClassFileNameMatcher matcher = new ClassFileNameMatcher(args[0]);
            System.out.println(" -> match = "+matcher.matches(args[1]));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
