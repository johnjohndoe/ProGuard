/* $Id: RegularExpressionMatcher.java,v 1.3 2003/02/11 18:06:45 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.classfile.util;

import proguard.classfile.ClassConstants;

import java.util.*;


/**
 * This utility class tests whether strings match a given regular expression.
 * Supported wildcards are
 * '*'  for any number of Java identifier characters,
 * '**' for any number of Java identifier characters plus '/', and
 * '?'  for a single Java identifier character.
 *
 * @author Eric Lafortune
 */
public class RegularExpressionMatcher
{
    private static final String MULTIPLE_CHARACTERS_WILDCARD1 = "*";
    private static final String MULTIPLE_CHARACTERS_WILDCARD2 = "**";
    private static final String SINGLE_CHARACTER_WILDCARD     = "?";

    private String[] expressionParts;


    /**
     * Creates a new RegularExpressionMatcher.
     * @param regularExpression the regular expression against which strings
     *                          will be matched.
     */
    public RegularExpressionMatcher(String regularExpression)
    {
        // Split the given regular expression into an array of parts: "*", "**",
        // "?", and simple text strings.

        // A List to collect the subsequent regular expression parts.
        List expressionPartsList = new ArrayList();

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
                    expressionPartsList.add(regularExpression.substring(previousIndex, index));
                }

                // Add the wildcard that we've found.
                expressionPartsList.add(wildcard);

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
            expressionPartsList.add(regularExpression.substring(previousIndex));
        }

        // Copy the List into the array.
        expressionParts = new String[expressionPartsList.size()];
        expressionPartsList.toArray(expressionParts);
    }


    /**
     * Tries to match the given string.
     * @param string the string to match.
     * @return a boolean indicating whether the regular expression matches the
     *         given string.
     */
    public boolean matches(String string)
    {
        return matches(string, 0, 0);
    }


    /**
     * Tries to match the given string, starting at the given index, with the
     * regular expression parts starting at the given index.
     */
    private boolean matches(String string,
                            int    stringStartIndex,
                            int    expressionIndex)
    {
        // Are we out of expression parts?
        if (expressionIndex == expressionParts.length)
        {
            // There's a match, at least if we're at the end of the string as well.
            return stringStartIndex == string.length();
        }

        String expressionPart = expressionParts[expressionIndex];

        // Did we get a wildcard of some sort?
        if (expressionPart.equals(MULTIPLE_CHARACTERS_WILDCARD1))
        {
            // Try out all possible matches for '*', not matching the package
            // separator.
            for (int stringEndIndex = stringStartIndex;
                 stringEndIndex <= string.length();
                 stringEndIndex++)
            {
                // Are we matching some characters already?
                if (stringEndIndex > stringStartIndex)
                {
                    // Make sure we don't start matching non-Java identifier
                    // characters.
                    char lastCharacter = string.charAt(stringEndIndex-1);
                    if (!Character.isJavaIdentifierPart(lastCharacter))
                    {
                        // We can never get a match.
                        return false;
                    }
                }

                // Continue looking for a match of the next expression part,
                // starting from the end index.
                if (matches(string, stringEndIndex, expressionIndex + 1))
                {
                    return true;
                }
            }

            // We could get a match for '*', but not for the rest of the
            // expression parts.
            return false;
        }
        else if (expressionPart.equals(MULTIPLE_CHARACTERS_WILDCARD2))
        {
            // Try out all possible matches for '**'.
            for (int stringEndIndex = stringStartIndex;
                 stringEndIndex <= string.length();
                 stringEndIndex++)
            {
                // Are we matching some characters already?
                if (stringEndIndex > stringStartIndex)
                {
                    // Make sure we don't start matching non-Java identifier
                    // characters, except '/'.
                    char lastCharacter = string.charAt(stringEndIndex-1);
                    if (!(Character.isJavaIdentifierPart(lastCharacter) ||
                          lastCharacter == ClassConstants.INTERNAL_PACKAGE_SEPARATOR))
                    {
                        // We can never get a match.
                        return false;
                    }
                }

                // Continue looking for a match of the next expression part,
                // starting from this index.
                if (matches(string, stringEndIndex, expressionIndex + 1))
                {
                    return true;
                }
            }

            // We could get a match for '**', but not for the rest of the
            // expression parts.
            return stringStartIndex == string.length();
        }
        else if (expressionPart.equals(SINGLE_CHARACTER_WILDCARD))
        {
            // Do we have any characters left to match?
            if (stringStartIndex == string.length())
            {
                // We've run out of characters.
                return false;
            }

            // Skip a single character (not '/' or ';') and check if the rest
            // of the expression parts match.
            char matchedCharacter = string.charAt(stringStartIndex);
            return
                matchedCharacter != ClassConstants.INTERNAL_PACKAGE_SEPARATOR &&
                matchedCharacter != ClassConstants.INTERNAL_TYPE_CLASS_END &&
                matches(string, stringStartIndex + 1, expressionIndex + 1);
        }
        else
        {
            // The expression part is a simple text string. Check if it matches,
            // and if the rest of the expression parts match.
            int expressionPartLength = expressionPart.length();
            return
                string.regionMatches(stringStartIndex, expressionPart, 0, expressionPartLength) &&
                matches(string, stringStartIndex + expressionPartLength, expressionIndex + 1);
        }
    }


    /**
     * A main method for testing string matching.
     */
    private static void main(String[] args)
    {
        try
        {
            System.out.println("Regular expression ["+args[0]+"]");
            RegularExpressionMatcher matcher = new RegularExpressionMatcher(args[0]);
            for (int index = 1; index < args.length; index++)
            {
                String string = args[index];
                System.out.print("String             ["+string+"]");
                System.out.println(" -> match = "+matcher.matches(args[1]));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
