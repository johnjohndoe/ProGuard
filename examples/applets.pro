#
# This ProGuard configuration file illustrates how to process applets.
# Usage:
#     java -jar proguard.jar @applets.pro
#

# Specify the library jars, input jars, and output jar.

-libraryjars <java.home>/lib/rt.jar
-injars      in.jar
-outjar      out.jar


# Preserve all public applets.

-keep public class * extends java.applet.Applet


# Print out a list of what we're preserving.

-printseeds


# Your application may contain more items that need to be preserved;
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
