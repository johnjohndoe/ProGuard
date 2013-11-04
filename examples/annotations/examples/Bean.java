import proguard.annotation.*;

/**
 * This bean illustrates the use of annotations for configuring ProGuard.
 *
 * After having been compiled, it can be processed using:
 *     java -jar proguard.jar @examples.pro
 *
 * The annotations will preserve the class and its public getters and setters.
 */
@Keep
@KeepPublicGettersSetters
public class Bean
{
    public boolean booleanProperty;
    public int     intProperty;
    public String  stringProperty;


    public boolean isBooleanProperty()
    {
        return booleanProperty;
    }


    public void setBooleanProperty(boolean booleanProperty)
    {
        this.booleanProperty = booleanProperty;
    }


    public int getIntProperty()
    {
        return intProperty;
    }


    public void setIntProperty(int intProperty)
    {
        this.intProperty = intProperty;
    }


    public String getStringProperty()
    {
        return stringProperty;
    }


    public void setStringProperty(String stringProperty)
    {
        this.stringProperty = stringProperty;
    }
}
