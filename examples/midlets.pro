#
# This ProGuard configuration file illustrates how to process J2ME midlets.
#

# Specify the library jars, input jars, and output jar.

-libraryjars /usr/local/java/wtk104/lib/midpapi.zip
-injars      in.jar
-outjar      out.jar


# Preserve all public midlets.

-keep public class * extends javax.microedition.MIDlet


# Print out a list of what we're preserving.

-printseeds


# Your midlet may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
