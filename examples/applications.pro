#
# This ProGuard configuration file illustrates how to process applications.
#

# Specify the library jars, input jars, and output jar.

-libraryjars <java.home>/lib/rt.jar
#-libraryjars junit.jar
#-libraryjars servlet.jar
#-libraryjars jai_core.jar
#...

-injars      in.jar
-outjar      out.jar


# Preserve all public applications, and print out which ones.

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}


# Print out a list of what we're preserving.

-printseeds


# Preserve all native method names.

-keepclassmembernames class * {
    native <methods>;
}


# Preserve all serializable class names for backward compatibility with any
# previously saved serialization data.

-keepnames class * implements java.io.Serializable

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    Object writeReplace();
    Object readResolve();
}


# Some jars may contain more items that need to be preserved, e.g.:
# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
