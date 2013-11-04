#
# This ProGuard configuration file illustrates how to process the ReTrace tool.
# Configuration files for typical applications will be very similar.
# Usage:
#     java -jar proguard.jar @retrace.pro
#

-libraryjars <java.home>/lib/rt.jar

# We'll filter out the Ant and WTK classes, keeping everything else.

-injars      retrace.jar
-injars      proguard.jar(!proguard/ant/**,!proguard/wtk/**,**)
-outjar      retrace_out.jar

# If we wanted to reuse the previously obfuscated proguard_out.jar, we could
# perform incremental obfuscation based on its mapping file, and only keep the
# additional ReTrace files instead of all files.

#-applymapping proguard.map
#-outjar       retrace_out.jar(proguard/retrace/**)


# Allow methods with the same signature, except for the return type,
# to get the same obfuscation name.

-overloadaggressively


# The main seed: ReTrace and its main method.

-keep public class proguard.retrace.ReTrace {
    public static void main(java.lang.String[]);
}
