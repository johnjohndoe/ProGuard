import proguard.annotation.*;

/**
 * This applet illustrates the use of annotations for configuring ProGuard.
 *
 * After having been compiled, it can be processed using:
 *     java -jar proguard.jar @examples.pro
 *
 * The annotation will preserve the class and its essential methods.
 */
@Keep
public class Applet extends java.applet.Applet
{
    // Implementations for Applet.

    public void init()
    {
        // ...
    }
}
