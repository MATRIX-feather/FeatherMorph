package xyz.nifeather.morph;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("UnstableApiUsage")
public class FeatherMorphBootstrap implements PluginBootstrap
{
    private final Logger logger = LoggerFactory.getLogger("FeatherMorph - Bootstrap");

    public boolean bootstrapLoaded = false;

    final AtomicBoolean pluginDisabled = new AtomicBoolean(false);

    @Override
    public void bootstrap(@NotNull BootstrapContext context)
    {
        logger.info("Loading bootstrap...");

        bootstrapLoaded = true;
        logger.info("Done!");
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context)
    {
        logger.info("Creating FeatherMorphMain...");
        return new FeatherMorphMain(this);
    }
}
