#
# This ProGuard configuration file illustrates how to process ProGuard itself.
# Configuration files for typical applications will be very similar.
#

-libraryjars <java.home>/lib/rt.jar
-injars      proguard.jar
-outjar      proguard_out.jar


# Allow methods with the same signature, except for the return type,
# to get the same obfuscation name.

-overloadaggressively


# Put all obfuscated classes into a single package 'pro'.

-defaultpackage pro


# The main seed.

-keep public class proguard.ProGuard {
    public static void main(java.lang.String[]);
}
