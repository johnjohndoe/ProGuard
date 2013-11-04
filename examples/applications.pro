#
# This ProGuard configuration file illustrates how to process applications.
# Usage:
#     java -jar proguard.jar @applications.pro
#

# Specify the library jars, input jars, and output jar.

-libraryjars <java.home>/lib/rt.jar
#-libraryjars junit.jar
#-libraryjars servlet.jar
#-libraryjars jai_core.jar
#...

-injars      in.jar
-outjar      out.jar


# Preserve all public applications.

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}


# Print out a list of what we're preserving.

-printseeds


# Preserve all native method names and the names of their classes.

-keepclasseswithmembernames class * {
    native <methods>;
}


# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
# You can comment this out if your application doesn't use serialization.
# If your code contains serializable classes that have to be backward 
# compatible, please refer to the manual.

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    Object writeReplace();
    Object readResolve();
}


# Your application may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
