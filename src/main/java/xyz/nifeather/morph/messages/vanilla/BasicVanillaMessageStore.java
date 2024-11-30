package xyz.nifeather.morph.messages.vanilla;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.MorphPlugin;
import xiamomc.pluginbase.Messages.IStrings;
import xiamomc.pluginbase.Messages.MessageStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class BasicVanillaMessageStore extends MessageStore<MorphPlugin>
{
    @NotNull
    protected abstract String getLocaleCode();

    @Override
    protected @NotNull String getFileName()
    {
        return "mclang/" + getLocaleCode() + ".json";
    }

    @Override
    protected List<Class<? extends IStrings>> getStrings()
    {
        return new ArrayList<>();
    }

    @Override
    protected String getPluginNamespace()
    {
        return MorphPlugin.getMorphNameSpace();
    }

    protected final String langDirUri = plugin.getDataFolder().toURI() + "/mclang";
    protected final File langDir = new File(URI.create(langDirUri));

    private final String urlPattern = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/"
            + Bukkit.getMinecraftVersion()
            + "/assets/minecraft/lang/";

    @Override
    public void initializeStorage()
    {
        super.initializeStorage();

        if (storingObject.isEmpty())
            downloadLanguage();
    }

    protected void downloadLanguage()
    {
        logger.info("Downloading Minecraft localization for language " + getLocaleCode() + "...");

        if (!langDir.exists())
        {
            var result = langDir.mkdirs();

            if (!result)
            {
                logger.error("Unable to create directory '" + langDir + "', download cancelled.");
                return;
            }
        }

        var targetFile = new File(URI.create(langDirUri + "/" + getLocaleCode() + ".json"));

        targetFile.delete();

        if (targetFile.exists() && !storingObject.isEmpty())
            return;

        this.addSchedule(() ->
        {
            try
            {
                var req = new URL(urlPattern + getLocaleCode() + ".json");
                var con = (HttpURLConnection) req.openConnection();

                logger.info("Downloading from " + req);

                con.setRequestMethod("GET");
                con.setInstanceFollowRedirects(true);
                con.setReadTimeout(3000);
                con.setConnectTimeout(3000);

                String str;
                StringBuilder builder = new StringBuilder();
                var reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                while ((str = reader.readLine()) != null)
                    builder.append(str).append("\n");

                var success = con.getResponseCode() == 200;

                con.disconnect();

                if (success)
                {
                    try (var stream = new FileOutputStream(targetFile))
                    {
                        stream.write(builder.toString().getBytes());
                        this.addSchedule(this::reloadConfiguration);
                        logger.info("Successfully downloaded Minecraft localization for language " + getLocaleCode() + "!");
                    }
                    catch (Throwable t)
                    {
                        logger.error("Error occurred while writing data for language " + getLocaleCode() + ": " + t.getMessage());
                        t.printStackTrace();
                    }
                }
                else
                {
                    logger.warn("Unable to download Minecraft localization for language" + getLocaleCode() + ": " + con.getResponseCode() + " :: " + con.getRequestMethod());
                }
            }
            catch (Throwable t)
            {
                logger.error("Unable to create request for language " + getLocaleCode() + ": " + t.getMessage());
            }
        }, 0, true);
    }
}
