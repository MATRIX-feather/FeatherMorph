package xyz.nifeather.morph.commands.subcommands.plugin;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.SkinCacheStrings;
import xyz.nifeather.morph.misc.CapeURL;
import xyz.nifeather.morph.misc.DisguiseTypes;
import xyz.nifeather.morph.misc.MorphGameProfile;
import xyz.nifeather.morph.misc.MorphParameters;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.misc.skins.PlayerSkinProvider;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class SkinCacheSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "skin_cache";
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.ACCESS_SKIN_CACHE;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return new FormattableMessage(plugin, "skin_cache");
    }

    private final PlayerSkinProvider skinProvider = PlayerSkinProvider.getInstance();

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .then(
                                Commands.literal("list")
                                        .executes(ctx -> this.executeList(ctx, null))
                                        .then(
                                                Commands.argument("amount", StringArgumentType.string())
                                                        .suggests((ctx, builder) -> builder.suggest("all").buildFuture())
                                                        .executes(ctx -> this.executeList(ctx, StringArgumentType.getString(ctx, "amount")))
                                        )
                        ).then(
                                Commands.literal("drop")
                                        .then(
                                                Commands.argument("skin", StringArgumentType.string())
                                                        .suggests((ctx, builder) ->
                                                        {
                                                            var allSkin = skinProvider.getAllSkins();

                                                            return CompletableFuture.supplyAsync(() ->
                                                            {
                                                                var input = builder.getRemainingLowerCase();

                                                                allSkin.forEach(singleSkin ->
                                                                {
                                                                    var skinName = singleSkin.name;

                                                                    if (skinName.toLowerCase().contains(input))
                                                                        builder.suggest(skinName);
                                                                });

                                                                return builder.build();
                                                            });
                                                        })
                                                        .executes(this::executeDrop)
                                        )
                        ).then(
                                Commands.literal("cache")
                                        .then(
                                                Commands.argument("name", StringArgumentType.string())
                                                        .executes(this::executeCache)
                                        )
                        ).then(
                                Commands.literal("info")
                                        .then(
                                                Commands.argument("skin", StringArgumentType.string())
                                                        .suggests(this::filterSkinName)
                                                        .executes(this::executeInfo)
                                        )
                        ).then(
                                Commands.literal("disguise")
                                        .then(
                                                Commands.argument("skin", StringArgumentType.string())
                                                        .suggests(this::filterSkinName)
                                                        .executes(this::executeDisguise)
                                        )
                        ).then(
                                Commands.literal("copy")
                                        .then(
                                                Commands.argument("source", StringArgumentType.string())
                                                        .suggests(this::filterSkinName)
                                                        .then(
                                                                Commands.argument("target", StringArgumentType.string())
                                                                        .executes(this::executeCopy)
                                                        )
                                        )
                        ).then(
                                Commands.literal("rename")
                                        .then(
                                                Commands.argument("from", StringArgumentType.string())
                                                        .suggests(this::filterSkinName)
                                                        .then(
                                                                Commands.argument("to", StringArgumentType.string())
                                                                        .executes(this::executeRename)
                                                        )
                                        )
                        )
        );
    }

    private CompletableFuture<Suggestions> filterSkinName(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var targetName = suggestionsBuilder.getRemainingLowerCase();

        var allSkin = skinProvider.getAllSkins();

        return CompletableFuture.supplyAsync(() ->
        {
            allSkin.forEach(singleSkin ->
            {
                var skinName = singleSkin.name;

                if (skinName.toLowerCase().contains(targetName))
                    suggestionsBuilder.suggest(skinName);
            });

            return suggestionsBuilder.build();
        });
    }

    private int executeDrop(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();
        var targetName = StringArgumentType.getString(context, "skin");

        if (targetName.equals("*"))
        {
            var skinCount = skinProvider.getAllSkins().size();
            skinProvider.dropAll();

            sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.droppedAllSkins().resolve("count", skinCount + "")));
        }
        else
        {
            skinProvider.dropSkin(targetName);

            sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.droppedSkin().resolve("name", targetName)));
        }

        return 1;
    }

    private int executeList(CommandContext<CommandSourceStack> context, @Nullable String exInput)
    {
        var sender = context.getSource().getSender();
        var currentTime = System.currentTimeMillis();
        var skins = skinProvider.getAllSkins();
        var str = Component.empty();

        sender.sendMessage(
                MessageUtils.prefixes(sender, SkinCacheStrings.listHeader().resolve("count", skins.size() + ""))
        );

        int limit = 20;

        if (exInput != null)
        {
            try
            {
                if (exInput.equals("all"))
                    limit = Integer.MAX_VALUE;
                else
                    limit = Integer.parseInt(exInput);
            }
            catch (Throwable ignored)
            {
            }
        }

        limit = Math.min(1, limit);

        var current = 0;

        var overallLine = SkinCacheStrings.skinInfoOverallLine();
        var expiredString = SkinCacheStrings.skinExpired().toComponent(MessageUtils.getLocale(sender));

        overallLine.resolve("x_more", Component.empty());

        var it = skins.iterator();
        while (it.hasNext())
        {
            current++;

            var next = it.next();
            str = str.append(Component.text(next.name));

            if (currentTime > next.expiresAt)
                str = str.append(expiredString);

            if (it.hasNext() && !(current == limit))
                str = str.append(Component.text(", "));

            if (current == limit)
            {
                var remaining = skins.size() - current;
                overallLine.resolve("x_more",
                        SkinCacheStrings.andXMore()
                                .resolve("count", remaining + "")
                                .withLocale(MessageUtils.getLocale(sender)));

                break;
            }
        }

        overallLine.resolve("info_line", str);

        sender.sendMessage(MessageUtils.prefixes(sender, overallLine));

        return 1;
    }

    private int executeCache(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        var targetName = StringArgumentType.getString(context, "name");

        sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.fetchingSkin().resolve("name", targetName)));

        skinProvider.invalidate(targetName);
        skinProvider.fetchSkin(targetName)
                .thenAccept(optional ->
                {
                    optional.ifPresentOrElse(profile -> sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.fetchSkinSuccess().resolve("name", targetName))),
                            () -> sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.targetSkinNotFound())));
                });

        return 1;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();
        var targetName = StringArgumentType.getString(context, "skin");
        var skinMatch = skinProvider.getCachedProfile(targetName);

        if (skinMatch == null)
        {
            sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.targetSkinNotFound()));
            return 1;
        }

        var texDesc = "<Nil>";
        var capeDesc = "<Nil>";
        var tex = skinMatch.getProperties().get("textures").stream().findFirst().orElse(null);

        if (tex != null)
        {
            var playerProfile = CraftPlayerProfile.asBukkitCopy(skinMatch);

            var skinURL = playerProfile.getTextures().getSkin();
            if (skinURL != null)
                texDesc = skinURL.toString();

            var capeURL = playerProfile.getTextures().getCape();
            if (capeURL != null)
                capeDesc = capeURL.toString();
        }

        sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.infoLine().resolve("name", skinMatch.getName())));

        sender.sendMessage(
                MessageUtils.prefixes(
                        sender,
                        SkinCacheStrings.infoSkinLine().resolve(
                                "url",
                                Component.text(texDesc)
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, texDesc))
                                        .decorate(TextDecoration.UNDERLINED)
                        )
                )
        );

        sender.sendMessage(
                MessageUtils.prefixes(
                        sender,
                        SkinCacheStrings.infoCapeLine().resolve(
                                "cape",
                                CapeURL.findMatching(capeDesc).withLocale(MessageUtils.getLocale(sender))
                        )
                )
        );

        if (debug.get())
            sender.sendMessage("Cape " + capeDesc);

        return 1;
    }

    private int executeDisguise(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        if (!(sender instanceof Player player))
        {
            sender.sendMessage(
                    MessageUtils.prefixes(
                            sender,
                            CommandStrings.unknownOperation().resolve("operation", "disguise_from_skin_cache_in_console")
                    )
            );

            return 1;
        }

        var targetName = StringArgumentType.getString(context, "skin");
        var skinMatch = skinProvider.getCachedProfile(targetName);

        if (skinMatch == null)
        {
            sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.targetSkinNotFound()));
            return 1;
        }

        var parameters = MorphParameters
                .create(player, DisguiseTypes.PLAYER.toId(skinMatch.getName()))
                .setSource(sender)
                .setBypassAvailableCheck(true);

        morphManager.morph(parameters);

        return 1;
    }

    private int executeCopy(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        var sourceName = StringArgumentType.getString(context, "source");
        var targetName = StringArgumentType.getString(context, "target");

        copyOrMoveSkin(sender, sourceName, targetName, false);

        return 1;
    }

    private int executeRename(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        var sourceName = StringArgumentType.getString(context, "from");
        var targetName = StringArgumentType.getString(context, "to");

        copyOrMoveSkin(sender, sourceName, targetName, true);

        return 1;
    }

    private void copyOrMoveSkin(CommandSender sender, String sourceName, String targetName, boolean isMoveOperation)
    {
        FormattableMessage msg;
        var result = isMoveOperation ? this.moveSkin(sourceName, targetName) : this.copySkin(sourceName, targetName);
        switch (result)
        {
            case NO_SUCH_SKIN ->
            {
                msg = SkinCacheStrings.targetSkinNotFound();
            }
            case SUCCESS ->
            {
                msg = (isMoveOperation ? SkinCacheStrings.moveSuccess() : SkinCacheStrings.copySuccess())
                        .resolve("source", sourceName)
                        .resolve("target", targetName);
            }
            case TARGET_EXISTS ->
            {
                msg = SkinCacheStrings.copyMoveTargetExists();
            }
            default ->
            {
                // Make IDEA happy...
                msg = new FormattableMessage(plugin, "Nil!");
            }
        }
        sender.sendMessage(MessageUtils.prefixes(sender, msg));

    }

    private enum CopyMoveResult
    {
        NO_SUCH_SKIN,
        TARGET_EXISTS,
        SUCCESS
    }

    private CopyMoveResult copySkin(String sourceName, String targetName)
    {
        var sourceProfile = skinProvider.getCachedProfile(sourceName);
        if (sourceProfile == null)
            return CopyMoveResult.NO_SUCH_SKIN;

        var oTarget = skinProvider.getCachedProfile(targetName);
        if (oTarget != null)
            return CopyMoveResult.TARGET_EXISTS;

        var targetProfile = new MorphGameProfile(sourceProfile);
        targetProfile.setName(targetName);
        targetProfile.setUUID(sourceProfile.getId());

        var profile = CraftPlayerProfile.asBukkitCopy(targetProfile);
        if (profile.getId() == null)
        {
            logger.error("Null profile ID. " + targetProfile.getId());

            return CopyMoveResult.NO_SUCH_SKIN;
        }

        if (profile.getName() == null)
        {
            logger.error("Null profile Name. " + targetProfile.getName());
            return CopyMoveResult.NO_SUCH_SKIN;
        }

        skinProvider.cacheProfile(CraftPlayerProfile.asBukkitCopy(targetProfile));

        return CopyMoveResult.SUCCESS;
    }

    private CopyMoveResult moveSkin(String sourceName, String targetName)
    {
        var sourceProfile = skinProvider.getCachedProfile(sourceName);
        if (sourceProfile == null)
            return CopyMoveResult.NO_SUCH_SKIN;

        var oTarget = skinProvider.getCachedProfile(targetName);
        if (oTarget != null)
            return CopyMoveResult.TARGET_EXISTS;

        this.copySkin(sourceName, targetName);
        skinProvider.dropSkin(sourceName);

        return CopyMoveResult.SUCCESS;
    }

    @Resolved(shouldSolveImmediately = true)
    private MorphManager morphManager;

    private final Bindable<Boolean> debug = new Bindable<>(false);

    @Initializer
    private void load(MorphConfigManager config)
    {
        config.bind(debug, ConfigOption.DEBUG_OUTPUT);
    }
}
