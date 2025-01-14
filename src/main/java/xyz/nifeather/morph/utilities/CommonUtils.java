package xyz.nifeather.morph.utilities;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.attribute.Attribute;

import java.util.Arrays;

public class CommonUtils
{
    private static Attribute[] cachedAttributes;

    public static Attribute[] getAvailableAttributes()
    {
        if (cachedAttributes != null)
            return Arrays.copyOf(cachedAttributes, cachedAttributes.length);

        var value = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).stream().toArray(Attribute[]::new);
        cachedAttributes = value;

        return value;
    }
}
