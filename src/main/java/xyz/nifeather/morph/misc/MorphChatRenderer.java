package xyz.nifeather.morph.misc;

import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

public class MorphChatRenderer extends MorphPluginObject implements ChatRenderer
{
    private Component message;

    private Component messageRevealed;

    @Resolved(shouldSolveImmediately = true)
    private MorphManager morphManager;

    @Resolved(shouldSolveImmediately = true)
    private MorphConfigManager config;

    public MorphChatRenderer(FormattableMessage formattable)
    {
        if (formattable != null)
            this.formattable = formattable;
    }

    private FormattableMessage formattable = null;

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName,
                                     @NotNull Component incomingMessage, @NotNull Audience viewer)
    {
        if (formattable == null)
        {
            var option = ConfigOption.CHAT_OVERRIDE_DEFAULT_PATTERN;
            formattable = new FormattableMessage(plugin,
                    config.getOrDefault(String.class, option, (String)option.defaultValue));
        }

        if (this.message == null)
        {
            var state = morphManager.getDisguiseStateFor(source);

            if (state != null)
                sourceDisplayName = state.getServerDisplay();

            try
            {
                this.message = buildMessage(sourceDisplayName, incomingMessage, source);
                this.messageRevealed = buildMessage(sourceDisplayName.append(Component.text("(" + source.getName() + ")")), incomingMessage, source);
            }
            catch (Throwable t)
            {
                logger.error("Error occurred while formatting message: " + t.getMessage());
                t.printStackTrace();

                this.message = this.message == null ? Component.empty() : this.message;
                this.messageRevealed = this.messageRevealed == null ? Component.empty() : this.messageRevealed;
            }
        }

        // var v = viewer.toString();
        // var p = (viewer instanceof Permissible permissible ? "%s".formatted(permissible.hasPermission(CommonPermissions.CHAT_OVERRIDE_REVEAL)) : "NOTAPERM");

        // logger.info("V" + v + " P " + p);

        if (!(viewer instanceof Permissible permissible))
        {
            if (!incompatableChat)
            {
                logger.warn("It seems that this server is using a chat plugin that doesn't compatible with us.");
                logger.warn("ChatOverride will not work for this situation and will always show the revealed message.");
                logger.warn("");
                logger.warn("If you believe this is an error, please open an issue on our GitHub repository!");
            }

            incompatableChat = true;
            return messageRevealed;
        }

        return permissible.hasPermission(CommonPermissions.CHAT_OVERRIDE_REVEAL)
                ? this.messageRevealed
                : this.message;
    }

    private static boolean incompatableChat = false;

    private Component buildMessage(Component displayName, Component msg, Player player)
    {
        var locale = MessageUtils.getLocale(player);

        return new FormattableMessage(FeatherMorphMain.getMorphNameSpace(), formattable.getKey(), formattable.getDefaultString())
                .withLocale(locale)
                .resolve("who", displayName)
                .resolve("message", msg)
                .toComponent();
    }
}
