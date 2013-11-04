#
# This ProGuard configuration file illustrates how to process ProGuard itself.
# Configuration files for typical applications will be very similar.
#

-libraryjars <java.home>/lib/rt.jar
-injars      proguard.jar
-outjar      proguard_out.jar

-keep public class proguard.ProGuard {
    public static void main(java.lang.String[]);
}
