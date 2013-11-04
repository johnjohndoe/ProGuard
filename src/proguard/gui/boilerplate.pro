# Basic - Applications. Keep all application classes that have a main method.
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Basic - Applets. Keep all extensions of java.applet.Applet.
-keep public class * extends java.applet.Applet

# Basic - Servlets. Keep all extensions of javax.servlet.Servlet.
-keep public class * extends javax.servlet.Servlet

# Basic - Midlets. Keep all extensions of javax.microedition.midlet.MIDlet.
-keep public class * extends javax.microedition.midlet.MIDlet

# Basic - Library. Keep all externally accessible classes, fields, and methods.
-keep public class * {
    public protected <fields>;
    public protected <methods>;
}

# Additional - Native method names. Keep all native class/method names.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Additional - Serialization code. Keep all fields and methods that are
# used for serialization.
-keepclassmembers class * extends java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Additional - BeanInfo classes. Keep all classes that implement the
# BeanInfo interface.
-keep class * implements java.beans.BeanInfo

# Additional - Bean classes. Keep all bean classes along with their getters
# and setters.
-keep class * {
    void set*(%);
    void set*(**);
    void set*(%[]);
    void set*(**[]);
    void set*(int, %);
    void set*(int, **);

    %    get*();
    **   get*();
    %[]  get*();
    **[] get*();
    %    get*(int);
    **   get*(int);
}

# Additional - RMI interfaces. Keep all Remote interfaces and their methods.
-keep interface * extends java.rmi.Remote {
    <methods>;
}

# Additional - RMI implementations. Keep all Remote implementations. This
# includes any explicit or implicit Activatable implementations with their
# two-argument constructors.
-keep class * implements java.rmi.Remote {
    <init>(java.rmi.activation.ActivationID, java.rmi.MarshalledObject);
}

# Additional - _class method names. Keep all .class method names. Useful for
# libraries that will be obfuscated again.
-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}
