package xyz.nifeather.morph;

import com.ticxo.modelengine.api.ModelEngineAPI;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.ApiStatus;
import xiamomc.pluginbase.ScheduleInfo;
import xyz.nifeather.morph.abilities.AbilityManager;
import xyz.nifeather.morph.commands.MorphCommandManager;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.events.*;
import xyz.nifeather.morph.interfaces.IManagePlayerData;
import xyz.nifeather.morph.interfaces.IManageRequests;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphMessageStore;
import xyz.nifeather.morph.messages.vanilla.VanillaMessageStore;
import xyz.nifeather.morph.misc.NetworkingHelper;
import xyz.nifeather.morph.misc.PlayerOperationSimulator;
import xyz.nifeather.morph.misc.integrations.towny.TownyAdapter;
import xyz.nifeather.morph.misc.recipe.RecipeManager;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.gui.IconLookup;
import xyz.nifeather.morph.misc.integrations.modelengine.ModelEngineHelper;
import xyz.nifeather.morph.misc.integrations.placeholderapi.PlaceholderIntegration;
import xyz.nifeather.morph.misc.integrations.residence.ResidenceEventProcessor;
import xyz.nifeather.morph.network.multiInstance.MultiInstanceService;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.skills.MorphSkillHandler;
import xyz.nifeather.morph.storage.skill.SkillsConfigurationStoreNew;
import xyz.nifeather.morph.transforms.Transformer;
import xyz.nifeather.morph.updates.UpdateHandler;
import xiamomc.pluginbase.Command.CommandHelper;
import xiamomc.pluginbase.Messages.MessageStore;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.Arrays;

public final class MorphPlugin extends XiaMoJavaPlugin
{
    private static MorphPlugin instance;

    /**
     * 仅当当前对象无法继承MorphPluginObject或不需要完全继承MorphPluginObject时使用
     * @return 插件的实例
     */
    @Deprecated
    public static MorphPlugin getInstance()
    {
        return instance;
    }

    public MorphPlugin()
    {
        instance = this;
    }

    public static String getMorphNameSpace()
    {
        return "morphplugin";
    }

    @Override
    public String getNamespace()
    {
        return getMorphNameSpace();
    }

    private CommandHelper<MorphPlugin> cmdHelper;

    private MorphManager morphManager;

    private PluginManager pluginManager;

    private MorphSkillHandler skillHandler;

    private AbilityManager abilityManager;

    private VanillaMessageStore vanillaMessageStore;

    private MorphMessageStore messageStore;

    private PlaceholderIntegration placeholderIntegration;

    private MorphClientHandler clientHandler;

    private Metrics metrics;

    private InteractionMirrorProcessor mirrorProcessor;

    private MultiInstanceService instanceService;

    private EntityProcessor entityProcessor;

    private static final String noticeHeaderFooter = "- x - x - x - x - x - x - x - x - x - x - x - x -";
    private void printImportantWarning(boolean critical, String... warnings)
    {
        if (critical)
        {
            logger.error(noticeHeaderFooter);
            logger.error("");

            for (String warning : warnings)
                logger.error(warning);

            logger.error("");
            logger.error(noticeHeaderFooter);
        }
        else
        {
            logger.warn(noticeHeaderFooter);
            logger.warn("");

            for (String warning : warnings)
                logger.warn(warning);

            logger.warn("");
            logger.warn(noticeHeaderFooter);
        }
    }

    @Override
    protected void enable()
    {
        super.enable();

        pluginManager = Bukkit.getPluginManager();
        var bukkitVersion = Bukkit.getMinecraftVersion();

        String primaryVersion = "1.21.1";
        String[] compatVersions = new String[] { primaryVersion, "1.21" };
        if (Arrays.stream(compatVersions).noneMatch(bukkitVersion::equals))
        {
            printImportantWarning(
                    true,
                    "This version of Minecraft (%s) is not supported!".formatted(bukkitVersion),
                    "Please use %s instead!".formatted(primaryVersion)
            );

            pluginManager.disablePlugin(this);
            return;
        }

        if (!bukkitVersion.equals(primaryVersion))
        {
            printImportantWarning(
                    false,
                    "Minecraft %s is not primary supported!".formatted(bukkitVersion),
                    "We suggest to use %s instead!".formatted(primaryVersion)
            );
        }

        this.metrics = new Metrics(this, 18062);

        this.registerListener(softDeps);

        var playerTracker = new PlayerTracker();

        softDeps.setHandle("PlaceholderAPI", p ->
        {
            logger.info("Registering Placeholders...");
            placeholderIntegration = new PlaceholderIntegration(dependencyManager);
            placeholderIntegration.register();
        }, true);

        softDeps.setHandle("Residence", r ->
        {
            logger.info("Residence detected, applying integrations...");
            this.registerListener(new ResidenceEventProcessor());
        }, true);

        softDeps.setHandle("Towny", plugin ->
        {
            logger.info("Towny detected, applying integrations...");
            this.registerListener(new TownyAdapter());
        }, true);

        softDeps.setHandle("ModelEngine", r ->
        {
            try
            {
                ModelEngineAPI.getAPI();
            }
            catch (Throwable t)
            {
                logger.info("Error occurred activating model engine support: " + t.getMessage());
                t.printStackTrace();
                return;
            }

            logger.info("Activating model engine support...");
            this.registerListener(new ModelEngineHelper());
        }, true);

        //缓存依赖
        dependencyManager.cache(this);
        dependencyManager.cache(clientHandler = new MorphClientHandler());
        dependencyManager.cache(new NetworkingHelper());

        dependencyManager.cache(morphManager = new MorphManager());
        dependencyManager.cache(skillHandler = new MorphSkillHandler());
        dependencyManager.cache(abilityManager = new AbilityManager());
        dependencyManager.cache(new RevealingHandler());
        dependencyManager.cache(new Transformer());

        dependencyManager.cache(vanillaMessageStore = new VanillaMessageStore());

        dependencyManager.cacheAs(MessageStore.class, messageStore = new MorphMessageStore());
        dependencyManager.cacheAs(MiniMessage.class, MiniMessage.miniMessage());
        dependencyManager.cacheAs(IManagePlayerData.class, morphManager);
        dependencyManager.cacheAs(IManageRequests.class, new RequestManager());
        dependencyManager.cacheAs(Scoreboard.class, Bukkit.getScoreboardManager().getMainScoreboard());
        dependencyManager.cacheAs(MorphConfigManager.class, new MorphConfigManager(this));
        dependencyManager.cache(playerTracker);

        dependencyManager.cache(cmdHelper = new MorphCommandManager());

        dependencyManager.cache(new SkillsConfigurationStoreNew());

        dependencyManager.cache(new MessageUtils());

        dependencyManager.cache(new PlayerOperationSimulator());

        var updateHandler = new UpdateHandler();
        dependencyManager.cache(updateHandler);

        dependencyManager.cache(instanceService = new MultiInstanceService());

        dependencyManager.cache(DisguiseProperties.INSTANCE);

        dependencyManager.cache(new RecipeManager());

        mirrorProcessor = new InteractionMirrorProcessor();

        //注册EventProcessor
        this.schedule(() ->
        {
            registerListeners(new Listener[]
                    {
                            playerTracker,
                            mirrorProcessor,
                            new CommonEventProcessor(),
                            new RevealingEventProcessor(),
                            new DisguiseAnimationProcessor(),
                            new ForcedDisguiseProcessor(),
                            new PlayerSkinProcessor(),
                            entityProcessor = new EntityProcessor()
                    });

            clientHandler.sendReAuth(Bukkit.getOnlinePlayers());
        });

        //Init GUI IconLookup
        IconLookup.instance();
    }

    @Override
    public void disable()
    {
        if (!getServer().isStopping())
        {
            printImportantWarning(true,
                    "HEY, THERE!",
                    "Are you doing a hot reload?",
                    "Note that FeatherMorph does NOT support doing such!",
                    "Before you open any issues, please do a FULL RESTART for your server! We will NOT provide any support after the hot reload!");
        }

        //调用super.onDisable后依赖管理器会被清空
        //需要在调用前先把一些东西处理好
        try
        {
            if (entityProcessor != null)
                entityProcessor.recoverGoals();

            if (morphManager != null)
                morphManager.onPluginDisable();

            if (placeholderIntegration != null)
                placeholderIntegration.unregister();

            if (clientHandler != null)
                clientHandler.getConnectedPlayers().forEach(clientHandler::disconnect);

            if (metrics != null)
                metrics.shutdown();

            if (mirrorProcessor != null)
                mirrorProcessor.pushToLoggingBase();

            if (instanceService != null)
                instanceService.onDisable();

            var messenger = this.getServer().getMessenger();

            messenger.unregisterOutgoingPluginChannel(this);
        }
        catch (Exception e)
        {
            logger.warn("Error occurred while disabling: " + e.getMessage());
            e.printStackTrace();
        }

        super.disable();
    }

    private void registerListeners(Listener[] listeners)
    {
        for (Listener l : listeners)
        {
            registerListener(l);
        }
    }

    private void registerListener(Listener l)
    {
        pluginManager.registerEvents(l, this);
    }

    /**
     * Can be removed in PluginBase 0.0.30
     */
    @Override
    public boolean acceptSchedules()
    {
        return true;
    }

    @Override
    public void startMainLoop(Runnable r)
    {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, o -> r.run(), 1, 1);
    }

    @Override
    public void runAsync(Runnable r)
    {
        Bukkit.getAsyncScheduler().runNow(this, o -> r.run());
    }

    @Override
    protected int getExceptionLimit()
    {
        return 3;
    }
}
