#
# This ProGuard configuration file illustrates how to process a program
# library, such that it remains usable as a library.
#

# Specify the library jars, input jars, and output jar.
# In this case, the input jar is the program library that we want to process.

-libraryjars <java.home>/lib/rt.jar
-injars      in.jar
-outjar      out.jar

# Preserve all public classes, and their public and protected fields and
# methods.

-keep public class * {
    public protected *;
}

# Preserve all native methods.

-keepclassmembers class * {
    native <methods>;
}

# Preserve all serialization members.

-keepclassmembers class * implements java.io.Serializable {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    Object writeReplace();
    Object readResolve();
    static final long serialVersionUID;
}

# Some jars may contain more items that need to be preserved, e.g.:
# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
