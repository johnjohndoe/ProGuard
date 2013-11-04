import proguard.annotation.KeepApplication;

/**
 * This application illustrates the use of annotations for configuring ProGuard.
 *
 * After having been compiled, it can be processed using:
 *     java -jar proguard.jar @examples.pro
 *
 * The annotation will preserve the class and its main method.
 */
@KeepApplication
public class Application
{
    public static void main(String[] args)
    {
        System.out.println("The answer is 42");
    }
}
