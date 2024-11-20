package xyz.nifeather.morph.messages;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.MorphPlugin;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Messages.IStrings;
import xiamomc.pluginbase.Messages.MessageStore;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MorphMessageStore extends MessageStore<MorphPlugin>
{
    private final List<Class<? extends IStrings>> strings = ObjectList.of(
            CommonStrings.class,
            CommandStrings.class,
            HelpStrings.class,
            MorphStrings.class,
            RequestStrings.class,
            SkillStrings.class,
            CommandNameStrings.class,
            HintStrings.class,
            TypesString.class,
            StatStrings.class,
            UpdateStrings.class,
            SkinCacheStrings.class,
            CapeStrings.class
    );

    private final Map<String, MorphMessageSubStore> subStores = new Object2ObjectOpenHashMap<>();

    private final Bindable<String> serverLanguage = new Bindable<>();

    public File getFile(String relativePath, boolean nullIfNotFound)
    {
        if (relativePath == null || relativePath.isBlank() || relativePath.isEmpty()) return null;

        var dataFolder = plugin.getDataFolder().toURI();
        var file = new File(URI.create("" + dataFolder + "/" + relativePath));

        if (!nullIfNotFound) return file;

        if (file.exists()) return file;
        return null;
    }

    @Nullable
    @Contract("null -> null; !null -> !null")
    public String getAbsolutePath(String relativePath)
    {
        if (relativePath == null || relativePath.isBlank() || relativePath.isEmpty()) return null;

        return plugin.getDataFolder().toURI().getPath() + "/" + relativePath;
    }

    @Initializer
    private void load(MorphConfigManager config)
    {
        var legacyDefaultFile = this.getFile("messages/default.json", true);

        if (legacyDefaultFile != null)
        {
            var targetFile = this.getFile(this.getFileName(), false);

            if (targetFile.exists()) targetFile.delete();

            try
            {
                Files.move(legacyDefaultFile.toPath(), targetFile.toPath());
                this.addSchedule(super::reloadConfiguration);
            }
            catch (Throwable t)
            {
                logger.error("Unable to update builtin message store: %s".formatted(t));
            }
        }

        config.bind(serverLanguage, ConfigOption.LANGUAGE_CODE);
    }

    private MorphMessageSubStore getOrCreateSubStore(String locale)
    {
        MorphMessageSubStore store;

        synchronized (subStores)
        {
            store = subStores.getOrDefault(locale, null);

            if (store != null) return store;

            store = new MorphMessageSubStore(locale, strings, this);
            store.initializeStorage();
            store.saveConfiguration();

            subStores.put(locale, store);
        }

        return store;
    }

    public Map<String, String> getAllMessages()
    {
        return storingObject;
    }

    @Override
    protected @NotNull String getFileName()
    {
        return "messages/template.json";
    }

    @Override
    protected @NotNull Map<String, String> createDefault()
    {
        // Use Object2ObjectAVLTreeMap from fastutil can stuck folia threads... why?
        return new ConcurrentHashMap<>();
    }

    @Override
    public String get(String key, @Nullable String defaultValue, @Nullable String locale)
    {
        var serverLanguage = this.serverLanguage.get();

        if (locale == null || locale.isBlank() || locale.isEmpty())
        {
            logger.warn("Resolving message key " + key + " for null or empty locale");
            locale = serverLanguage;
        }

        var messageStores = ObjectArrayList.of(
                this.getOrCreateSubStore("override"),
                this.getOrCreateSubStore(locale)
        );

        if (!locale.equals(serverLanguage))
            messageStores.add(this.getOrCreateSubStore(serverLanguage));

        messageStores.add(this.getOrCreateSubStore("en_us"));

        for (var store : messageStores)
        {
            var msg = store.get(key, "NIL", null);

            // Since we use ConcurrentHashMap, we no longer can use NULL as the default value
            // So this might be the only way. Solving this would require a PluginBase update.
            if (msg.equals("NIL")) msg = null;

            if (msg != null) return msg;
        }

        return defaultValue == null ? "%s@%s".formatted(key, locale) : defaultValue;
    }

    public boolean reloadOverwriteNonDefault()
    {
        this.oWNonDef = true;

        return reloadConfiguration();
    }

    private boolean oWNonDef;

    @Override
    public boolean reloadConfiguration()
    {
        var allSuccess = new AtomicBoolean(true);

        subStores.forEach((l, s) ->
        {
            if (oWNonDef)
                s.overWriteNonDefaultAfterReload();

            if (!s.reloadConfiguration()) allSuccess.set(false);
        });

        oWNonDef = false;

        return super.reloadConfiguration() && allSuccess.get();
    }

    @Override
    protected List<Class<? extends IStrings>> getStrings()
    {
        return strings;
    }

    @Override
    protected String getPluginNamespace()
    {
        return MorphPlugin.getMorphNameSpace();
    }
}
