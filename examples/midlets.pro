#
# This ProGuard configuration file illustrates how to process J2ME midlets.
# Usage:
#     java -jar proguard.jar @midlets.pro
#
# You should still apply the preverify tool after having processed your code.

# Specify the library jars, input jars, and output jar.

-libraryjars /usr/local/java/wtk104/lib/midpapi.zip
-injars      in.jar
-outjar      out.jar


# Allow methods with the same signature, except for the return type,
# to get the same obfuscation name.

-overloadaggressively


# Put all obfuscated classes into the nameless root package.

-defaultpackage ''


# On Windows, you can't use mixed case class names,
# for the sake of the preverify tool.
#
# -dontusemixedcaseclassnames


# Preserve all public midlets.

-keep public class * extends javax.microedition.midlet.MIDlet


# Print out a list of what we're preserving.

-printseeds


# Your midlet may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
