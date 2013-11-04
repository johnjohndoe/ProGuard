#
# This ProGuard configuration file illustrates how to process servlets.
# Usage:
#     java -jar proguard.jar @servlets.pro
#

# Specify the input jars, output jars, and library jars.

-injars  in.jar
-outjars out.jar

-libraryjars <java.home>/lib/rt.jar
-libraryjars /usr/local/java/servlet/servlet.jar

# Preserve all public servlets.

-keep public class * implements javax.servlet.Servlet

# Print out a list of what we're preserving.

-printseeds

# Your application may contain more items that need to be preserved;
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
