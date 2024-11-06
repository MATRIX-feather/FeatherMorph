package xyz.nifeather.morph.misc.integrations.modelengine;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.backends.modelengine.MEBackend;
import xyz.nifeather.morph.api.v0.events.lifecycle.ManagerFinishedInitializeEvent;
import xyz.nifeather.morph.providers.disguise.ModelEngineProvider;

public class ModelEngineHelper extends MorphPluginObject implements Listener
{
    @EventHandler
    public void onInitDone(ManagerFinishedInitializeEvent e)
    {
        var manager = e.manager;

        try
        {
            manager.registerBackend(new MEBackend());
        }
        catch (Throwable t)
        {
            logger.error("Can't register Model Engine as one of the backends: " + t.getMessage());
            t.printStackTrace();

            return;
        }

        try
        {
            manager.registerDisguiseProvider(new ModelEngineProvider());
        }
        catch (Throwable t)
        {
            logger.error("Can't register Model Engine Provider as one of the providers: " + t.getMessage());
            t.printStackTrace();

            return;
        }

        logger.info("Successfully registered ModelEngine as a disguise provider and backend!");
    }
}
