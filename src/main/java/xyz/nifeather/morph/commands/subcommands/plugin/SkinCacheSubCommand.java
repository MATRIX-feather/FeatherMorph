package xyz.nifeather.morph.commands.subcommands.plugin;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import xiamomc.pluginbase.Command.ISubCommand;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.subcommands.plugin.skincache.cmdTree.CommandBuilder;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.SkinCacheStrings;
import xyz.nifeather.morph.misc.CapeURL;
import xyz.nifeather.morph.misc.DisguiseTypes;
import xyz.nifeather.morph.misc.MorphGameProfile;
import xyz.nifeather.morph.api.v0.disguise.MorphParameters;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.misc.skins.PlayerSkinProvider;

import java.util.List;

public class SkinCacheSubCommand extends MorphPluginObject implements ISubCommand
{
    @Override
    public @NotNull String getCommandName()
    {
        return "skin_cache";
    }

    @Override
    public @Nullable String getPermissionRequirement()
    {
        return CommonPermissions.ACCESS_SKIN_CACHE;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return new FormattableMessage(plugin, "skin_cache");
    }

    private final PlayerSkinProvider skinProvider = PlayerSkinProvider.getInstance();

    private List<ISubCommand> genSubCmd()
    {
        return CommandBuilder.builder()
                .startNew()
                .name("list")
                //.permission("dddd")
                .onFilter(args -> List.of("all"))
                .executes((sender, args) ->
                {
                    var currentTime = System.currentTimeMillis();
                    var skins = skinProvider.getAllSkins();
                    var str = Component.empty();

                    sender.sendMessage(
                            MessageUtils.prefixes(sender, SkinCacheStrings.listHeader().resolve("count", skins.size() + ""))
                    );

                    var limit = args.isEmpty()
                            ? 20
                            : args.get(0).equalsIgnoreCase("all")
                                ? Integer.MAX_VALUE
                                : 20;

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

                    return true;
                })

                .startNew()
                .name("drop")
                //.permission("dddd")
                .onFilter(args ->
                {
                    var stream = skinProvider.getAllSkins()
                            .stream()
                            .map(sk -> sk.name);

                    var filterName = args.isEmpty() ? "" : args.get(0);

                    var list = new ObjectArrayList<>(stream
                            .filter(name -> name.toLowerCase().contains(filterName.toLowerCase()))
                            .toList());

                    if (filterName.isBlank())
                        list.add("*");

                    return list;
                })
                .executes((sender, args) ->
                {
                    if (args.isEmpty())
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.listNoEnoughArguments()));
                        return true;
                    }

                    var targetName = args.get(0);

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

                    return true;
                })

                .startNew()
                .name("cache")
                //.permission("dddd")
                .executes((sender, args) ->
                {
                    if (args.isEmpty())
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.listNoEnoughArguments()));
                        return true;
                    }

                    var targetName = args.get(0);

                    sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.fetchingSkin().resolve("name", targetName)));

                    skinProvider.invalidate(targetName);
                    skinProvider.fetchSkin(targetName)
                            .thenAccept(optional ->
                            {
                                optional.ifPresentOrElse(profile -> sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.fetchSkinSuccess().resolve("name", targetName))),
                                        () -> sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.targetSkinNotFound())));
                            });

                    return true;
                })

                .startNew()
                .name("info")
                .onFilter(this::filterSkinName)
                .executes((sender, args) ->
                {
                    if (args.isEmpty())
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.listNoEnoughArguments()));
                        return true;
                    }

                    var targetName = args.get(0);
                    var skinMatch = skinProvider.getCachedProfile(targetName);

                    if (skinMatch == null)
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.targetSkinNotFound()));
                        return true;
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

                    return true;
                })

                .startNew()
                .name("disguise")
                .onFilter(this::filterSkinName)
                .executes((sender, args) ->
                {
                    if (args.isEmpty())
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.listNoEnoughArguments()));
                        return true;
                    }

                    if (!(sender instanceof Player player))
                    {
                        sender.sendMessage(
                                MessageUtils.prefixes(
                                        sender,
                                        CommandStrings.unknownOperation().resolve("operation", "disguise_from_skin_cache_in_console")
                                )
                        );

                        return true;
                    }

                    var targetName = args.get(0);
                    var skinMatch = skinProvider.getCachedProfile(targetName);

                    if (skinMatch == null)
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, SkinCacheStrings.targetSkinNotFound()));
                        return true;
                    }

                    var parameters = MorphParameters
                            .create(player, DisguiseTypes.PLAYER.toId(skinMatch.getName()))
                            .setSource(sender)
                            .setBypassAvailableCheck(true);

                    morphManager.morph(parameters);

                    return true;
                })

                .startNew()
                .name("copy")
                .onFilter(this::filterSkinName)
                .executes((sender, args) ->
                {
                    if (args.size() < 2)
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.listNoEnoughArguments()));
                        return true;
                    }

                    var sourceName = args.get(0);
                    var targetName = args.get(1);

                    copyOrMoveSkin(sender, sourceName, targetName, false);

                    return true;
                })

                .startNew()
                .name("rename")
                .onFilter(this::filterSkinName)
                .executes((sender, args) ->
                {
                    if (args.size() < 2)
                    {
                        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.listNoEnoughArguments()));
                        return true;
                    }

                    var sourceName = args.get(0);
                    var targetName = args.get(1);

                    copyOrMoveSkin(sender, sourceName, targetName, true);

                    return true;
                })

                .buildAll();
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

    private List<String> filterSkinName(List<String> args)
    {
        var targetName = args.isEmpty() ? "" : args.get(0);

        if (args.size() > 1) return List.of();

        return skinProvider.getAllSkins()
                .stream()
                .map(ss -> ss.name)
                .filter(name -> name.toLowerCase().contains(targetName.toLowerCase()))
                .toList();
    }

    @Resolved(shouldSolveImmediately = true)
    private MorphManager morphManager;

    private final Bindable<Boolean> debug = new Bindable<>(false);

    @Initializer
    private void load(MorphConfigManager config)
    {
        config.bind(debug, ConfigOption.DEBUG_OUTPUT);
    }

    private List<ISubCommand> getSubCmd()
    {
        if (subCommands == null)
            subCommands = genSubCmd();

        return subCommands;
    }

    @Nullable
    private List<ISubCommand> subCommands;

    @Override
    public @Nullable List<ISubCommand> getSubCommands()
    {
        return getSubCmd();
    }
}
