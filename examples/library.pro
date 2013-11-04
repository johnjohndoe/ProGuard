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


# Preserve all native method names and .class method names.

-keepclassmembernames class * {
    native <methods>;
    static Class class$(java.lang.String);
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
