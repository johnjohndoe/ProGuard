#
# This ProGuard configuration file illustrates how to process the ProGuard GUI.
# Configuration files for typical applications will be very similar.
# Usage:
#     java -jar proguard.jar @proguardgui.pro
#

-libraryjars <java.home>/lib/rt.jar

# We'll filter out the Ant and WTK classes, keeping everything else.

-injars      proguardgui.jar
-injars      proguard.jar(!proguard/ant/**,!proguard/wtk/**,**)
-outjar      proguardgui_out.jar

# If we wanted to reuse the previously obfuscated proguard_out.jar, we could
# perform incremental obfuscation based on its mapping file, and only keep the
# additional GUI files instead of all files.

#-applymapping proguard.map
#-outjar       proguardgui_out.jar(proguard/gui/**)


# Allow methods with the same signature, except for the return type,
# to get the same obfuscation name.

-overloadaggressively


# The main seed: ProGuardGUI and its main method.

-keep public class proguard.gui.ProGuardGUI {
    public static void main(java.lang.String[]);
}
