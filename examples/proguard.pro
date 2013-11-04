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


# Put all obfuscated classes into the nameless root package.

-defaultpackage ''


# The main seeds: ProGuard and its companion tool ReTrace.

-keep public class proguard.ProGuard {
    public static void main(java.lang.String[]);
}

-keep public class proguard.ReTrace {
    public static void main(java.lang.String[]);
}
