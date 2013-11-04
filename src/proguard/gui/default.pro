# The default configuration when starting up the GUI.

-libraryjars <java.home>/lib/rt.jar

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

-keepclasseswithmembernames class * {
    native <methods>;
}
