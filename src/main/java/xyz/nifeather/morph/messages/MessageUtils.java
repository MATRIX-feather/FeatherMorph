package xyz.nifeather.morph.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.misc.NmsRecord;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Managers.DependencyManager;
import xiamomc.pluginbase.Messages.FormattableMessage;

public class MessageUtils extends MorphPluginObject
{
    private static void setupConfigManager()
    {
        if (pluginDepMgr == null)
            pluginDepMgr = DependencyManager.getInstance(FeatherMorphMain.getMorphNameSpace());

        config = pluginDepMgr.get(MorphConfigManager.class);
        plugin = FeatherMorphMain.getInstance();
    }

    private static DependencyManager pluginDepMgr;
    private static MorphConfigManager config;
    private static FeatherMorphMain plugin;

    public static Component prefixes(CommandSender sender, Component[] c)
    {
        if (config == null)
            setupConfigManager();

        if (!(sender instanceof Player))
            return Component.translatable("%s", c);

        var finalComponent = Component.empty();

        for (var cc : c)
            finalComponent = finalComponent.append(cc);

        var prefix = new FormattableMessage(plugin, config.getOrDefault(String.class, ConfigOption.PLUGIN_PREFIX));

        return prefix
                .withLocale(getLocale(sender))
                .resolve("message", finalComponent)
                .toComponent(null);
    }

    public static Component prefixes(CommandSender sender, String str)
    {
        return prefixes(sender, Component.text(str));
    }

    public static Component prefixes(CommandSender sender, Component c)
    {
        return prefixes(sender, new Component[]{c});
    }

    public static Component prefixes(CommandSender sender, FormattableMessage formattable)
    {
        if (formattable.getLocale() == null)
            formattable.withLocale(getLocale(sender));

        return prefixes(sender, formattable.toComponent(null));
    }

    @NotNull
    public static String getLocale(Player player)
    {
        if (isSingleLanguage())
            return getServerLocale();

        var nmsLocale = NmsRecord.ofPlayer(player).language;

        return nmsLocale == null ? getServerLocale() : nmsLocale.toLowerCase().replace('-', '_');
    }

    @NotNull
    public static String getLocaleOr(CommandSender sender, @NotNull String defaultValue)
    {
        var locale = getLocale(sender);
        return locale == null ? defaultValue : locale;
    }

    private static MorphConfigManager configManager;

    private static void initializeConfigManager()
    {
        if (configManager != null) return;

        var depMgr = DependencyManager.getInstance(FeatherMorphMain.getMorphNameSpace());
        var config = depMgr.get(MorphConfigManager.class);

        if (config != null)
        {
            config.bind(serverLocale, ConfigOption.LANGUAGE_CODE);
            config.bind(singleLanguage, ConfigOption.SINGLE_LANGUAGE);
        }

        configManager = config;
    }

    private final static Bindable<String> serverLocale = new Bindable<>("zh_cn");
    private final static Bindable<Boolean> singleLanguage = new Bindable<>(true);

    public static String getServerLocale()
    {
        initializeConfigManager();

        return serverLocale.get();
    }

    public static boolean isSingleLanguage()
    {
        initializeConfigManager();

        return singleLanguage.get();
    }

    @Nullable
    public static String getLocale(CommandSender sender)
    {
        if (sender instanceof Player player && !isSingleLanguage())
            return getLocale(player);
        else
            return getServerLocale();
    }

    public static String asParsedMiniMessageString(String locale, FormattableMessage formattable)
    {
        if (formattable.getLocale() == null)
            formattable.withLocale(locale);

        return MiniMessage.miniMessage().serialize(formattable.toComponent());
    }
}
