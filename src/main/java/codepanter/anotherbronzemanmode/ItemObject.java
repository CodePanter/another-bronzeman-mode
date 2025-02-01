package codepanter.anotherbronzemanmode;
import lombok.Value;
import net.runelite.client.util.AsyncBufferedImage;

@Value
public class ItemObject
{
    int id;
    String name;
    boolean tradeable;
    AsyncBufferedImage icon;
}
