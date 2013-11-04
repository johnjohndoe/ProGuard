#
# This ProGuard configuration file illustrates how to process ProGuard itself.
# Configuration files for typical applications will be very similar.
# Usage:
#     java -jar proguard.jar @proguard.pro
#

-libraryjars <java.home>/lib/rt.jar
-injars      proguard.jar
-outjar      proguard_out.jar

# Disregard warnings about missing classes, in case we don't have
# the Ant or J2ME libraries.

-ignorewarnings


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


# If you want to preserve the Ant task as well, you'll have to specify the
# main ant.jar.

#-libraryjars /usr/local/java/ant1.5.3/lib/ant.jar
#-keep public class proguard.ant.* {
#    public void set*(boolean);
#    public void set*(**);
#    public void add*(**);
#}


# If you want to preserve the WTK obfuscation plug-in, you'll have to specify
# the kenv.zip.

#-libraryjars /usr/local/java/j2me2.0beta/wtklib/kenv.zip
#-keep public class proguard.wtk.ProGuardObfuscator
