package xyz.nifeather.morph.backends.server.renderer.utilties;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HolderUtils
{
    private static final Logger log = LoggerFactory.getLogger(HolderUtils.class);
    private static ServerLevel level;

    private static void setupLevel()
    {
        if (level != null) return;

        level = ((CraftWorld)Bukkit.getWorlds().stream().findFirst().get()).getHandle();
    }

    private static <T> Registry<T> lookupRegistryOrThrow(ResourceKey<Registry<T>> registryKey)
    {
        setupLevel();

        return level.registryAccess().lookup(registryKey).orElseThrow();
    }

    public static <T> Holder<T> getHolderOrThrow(ResourceLocation location, ResourceKey<Registry<T>> registryKey)
    {
        setupLevel();

        var registry = lookupRegistryOrThrow(registryKey);

        var ref = registry.get(location).orElse(null);
        if (ref == null)
            throw new NullPointerException("Reference for key '%s' does not found in the registry '%s'.".formatted(location, registry.key()));

        if (!ref.isBound())
            throw new RuntimeException("Reference is not bound: Key '%s' in registry '%s'".formatted(location, registry.key()));

        return registry.wrapAsHolder(ref.value());
    }

    public static <T> Holder<T> getHolderOrThrow(ResourceKey<T> key, ResourceKey<Registry<T>> registryKey)
    {
        setupLevel();

        var registry = lookupRegistryOrThrow(registryKey);

        var ref = registry.get(key).orElse(null);
        if (ref == null)
            throw new NullPointerException("Reference for key '%s' does not found in the registry '%s'.".formatted(key, registry.key()));

        if (!ref.isBound())
            throw new RuntimeException("Reference is not bound: Key '%s' in registry '%s'".formatted(key, registry.key()));

        return registry.wrapAsHolder(ref.value());
    }
}
