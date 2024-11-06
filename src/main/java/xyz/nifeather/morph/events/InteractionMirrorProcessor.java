package xyz.nifeather.morph.events;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.S2C.set.S2CSetSneakingCommand;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.api.v0.events.gameplay.PlayerJoinedWithDisguiseEvent;
import xyz.nifeather.morph.api.v0.events.gameplay.PlayerMorphEvent;
import xyz.nifeather.morph.api.v0.events.gameplay.PlayerUnMorphEvent;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.DisguiseTypes;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.misc.PlayerOperationSimulator;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.storage.DirectoryStorage;
import xyz.nifeather.morph.storage.mirrorlogging.MirrorSingleEntry;
import xyz.nifeather.morph.storage.mirrorlogging.OperationType;
import xyz.nifeather.morph.utilities.DisguiseUtils;
import xyz.nifeather.morph.utilities.ItemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class InteractionMirrorProcessor extends MorphPluginObject implements Listener
{
    private final Map<Player, String> mirrorMap = new ConcurrentHashMap<>();

    @Resolved
    private MorphClientHandler clientHandler;

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent e)
    {
        if (!allowSneak.get()) return;

        var player = e.getPlayer();

        var playerInf = getMirrorTarget(player);
        var targetPlayer = playerInf.target;

        if (!playerInDistance(player, playerInf) || targetPlayer.isSneaking() == e.isSneaking()) return;

        targetPlayer.setSneaking(e.isSneaking());
        clientHandler.sendCommand(targetPlayer, new S2CSetSneakingCommand(e.isSneaking()));

        logOperation(e.getPlayer(), targetPlayer, OperationType.ToggleSneak);
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent e)
    {
        if (!allowSwap.get()) return;

        var player = e.getPlayer();
        var playerInf = getMirrorTarget(player);

        if (!playerInDistance(player, playerInf)) return;

        var targetPlayer = playerInf.target;
        assert targetPlayer != null;

        var equipment = targetPlayer.getEquipment();

        var mainHandItem = equipment.getItemInMainHand();
        var offhandItem = equipment.getItemInOffHand();

        equipment.setItemInMainHand(offhandItem);
        equipment.setItemInOffHand(mainHandItem);

        logOperation(player, targetPlayer, OperationType.SwapHand);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e)
    {
        if (!allowDrop.get()) return;

        var player = e.getPlayer();
        var playerInf = getMirrorTarget(player);

        if (!playerInDistance(player, playerInf) || !player.isSneaking()) return;

        var target = playerInf.target;
        assert target != null;

        if (!target.getEquipment().getItemInMainHand().getType().isAir())
        {
            target.dropItem(false);
            target.swingHand(EquipmentSlot.HAND);

            logOperation(player, target, OperationType.ItemDrop);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHotbarChange(PlayerItemHeldEvent e)
    {
        if (!allowHotBar.get()) return;

        var player = e.getPlayer();
        var inf = getMirrorTarget(player);

        if (!playerInDistance(player, inf)) return;

        var targetPlayer = inf.target;
        assert targetPlayer != null;

        targetPlayer.getInventory().setHeldItemSlot(e.getNewSlot());
        logOperation(player, targetPlayer, OperationType.HotbarChange);
    }

    @Resolved(shouldSolveImmediately = true)
    private MorphConfigManager config;

    @EventHandler
    public void onPlayerStopUsingItem(PlayerStopUsingItemEvent e)
    {
        var player = e.getPlayer();
        var inf = getMirrorTarget(player);

        if (!playerInDistance(player, inf)) return;

        var targetPlayer = inf.target;
        assert targetPlayer != null;

        //如果目标玩家正在使用的物品和我们当前释放的物品一样，并且释放的物品拥有使用动画，那么调用releaseUsingItem
        var ourHandItem = e.getItem().getType();
        var nmsPlayer = NmsRecord.ofPlayer(targetPlayer);

        if (nmsPlayer.isUsingItem()
                && ItemUtils.isContinuousUsable(ourHandItem)
                && nmsPlayer.getUseItem().getBukkitStack().getType() == ourHandItem)
        {
            nmsPlayer.releaseUsingItem();
            logOperation(player, targetPlayer, OperationType.ReleaseUsingItem);
        }
    }

    @EventHandler
    public void onPlayerHurtEntity(EntityDamageByEntityEvent e)
    {
        if (!allowSimulation.get()) return;

        if (e.getDamager() instanceof Player damager)
        {
            var inf = getMirrorTarget(damager);

            if (!playerInDistance(damager, inf)) return;

            var targetPlayer = inf.target;
            assert targetPlayer != null;

            simulateOperationAsync(Action.LEFT_CLICK_AIR, targetPlayer, damager, success -> {});
            logOperation(damager, targetPlayer, OperationType.LeftClick);

            //如果伪装的玩家想攻击本体，取消事件
            if (e.getEntity() instanceof Player hurtedPlayer && hurtedPlayer.equals(targetPlayer))
            {
                e.setCancelled(true);

                return;
            }

            // 如果不是目标玩家的TickThread，那么就没有必要检查是否要攻击本体
            // 因为这个事件本来就是攻击者的TickThread上发起，因此没必要检查是否是发起者的TickThread
            if (TickThread.isTickThreadFor(NmsRecord.ofPlayer(targetPlayer)))
            {
                var damagerLookingAt = damager.getTargetEntity(5);
                var playerLookingAt = targetPlayer.getTargetEntity(5);

                //如果伪装的玩家想攻击的实体和被伪装的玩家一样，模拟左键并取消事件
                if (damagerLookingAt != null && damagerLookingAt.equals(playerLookingAt))
                    e.setCancelled(true);
            }
        }
    }

    @Resolved
    private PlayerTracker tracker;

    /**
     * todo: SwingEvent应当只相应方块破坏
     */
    @EventHandler
    public void onPlayerSwing(PlayerArmSwingEvent e)
    {
        if (!allowSimulation.get()) return;

        var player = e.getPlayer();

        var inf = getMirrorTarget(player);
        var targetPlayer = inf.target;
        if (targetPlayer == null) return;

        var playerInDistance = playerInDistance(player, inf);

        //取消一定条件下源玩家的挥手动画
        if (targetPlayer.getLocation().getWorld() == player.getLocation().getWorld()
                && Math.abs(targetPlayer.getLocation().distance(player.getLocation())) <= 6
                && playerInDistance)
        {
            var theirTarget = targetPlayer.getTargetEntity(5);
            var ourTarget = player.getTargetEntity(5);

            if ((ourTarget != null || theirTarget != null)
                    && (ourTarget == targetPlayer || ourTarget == theirTarget || theirTarget == player))
            {
                e.setCancelled(true);
            }
        }

        if (!playerInDistance || simStack.contains(targetPlayer)) return;

        //若源玩家正在丢出物品，不要处理
        //检查玩家在此tick内是否存在互动以避免重复镜像
        if (tracker.droppingItemThisTick(player)
            || tracker.interactingThisTick(player))
        {
            return;
        }

        var lastAction = tracker.getLastInteractAction(player);

        //如果此时玩家没有触发Interaction, 那么默认设置为左键空气
        if (!tracker.interactingThisTick(player))
            lastAction = PlayerTracker.InteractType.LEFT_CLICK_AIR;

        if (lastAction == null) return;

        //旁观者模式下左键方块不会产生Interact事件，我们得猜这个玩家现在是左键还是右键
        if (player.getGameMode() == GameMode.SPECTATOR)
        {
            if (lastAction.isRightClick())
                lastAction = PlayerTracker.InteractType.LEFT_CLICK_BLOCK;
        }

        simulateOperationAsync(lastAction.toBukkitAction(), targetPlayer, player, success -> {});
        logOperation(player, targetPlayer, lastAction.isLeftClick() ? OperationType.LeftClick : OperationType.RightClick);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        var player = e.getPlayer();
        var action = e.getAction();

        if (action == Action.PHYSICAL) return;

        //Sometimes right click fires PlayerInteractEvent for both left and right hand.
        //This prevents us from simulating the same operation twice.
        if (tracker.isDuplicatedRightClick(player))
        {
            if (debugOutput.get())
                logger.info("Interact, Duplicated RC " + System.currentTimeMillis());

            return;
        }

        var inf = getMirrorTarget(player);

        if (!playerInDistance(player, inf))
            return;

        var targetPlayer = inf.target;
        assert targetPlayer != null;

        simulateOperationAsync(e.getAction(), targetPlayer, player, success -> {});
        logOperation(player, targetPlayer, e.getAction().isLeftClick() ? OperationType.LeftClick : OperationType.RightClick);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e)
    {
        var player = e.getPlayer();
        if (tracker.isDuplicatedRightClick(player))
        {
            if (debugOutput.get())
                logger.info("InteractAt, Duplicated RC: " + System.currentTimeMillis());

            return;
        }

/*
        // No idea why this is here, let's comment-out for now.
        // 2023-11-13

        var equipment = e.getPlayer().getEquipment();
        if (!equipment.getItem(e.getHand()).getType().isAir())
        {
            logger.info("InteractAt, Item not air");
            return;
        }
*/

        var inf = getMirrorTarget(player);

        if (!playerInDistance(player, inf)) return;
        var targetPlayer = inf.target;
        assert targetPlayer != null;

        simulateOperationAsync(Action.RIGHT_CLICK_AIR, targetPlayer, player, success -> {});
        logOperation(player, targetPlayer, OperationType.RightClick);
    }

    @Resolved
    private PlayerOperationSimulator operationSimulator;

    /**
     * 某一模拟进程内的模拟顺序
     */
    private final Stack<Player> simStack = new Stack<>();

    private void clearStackIfPossible(Player player, boolean forceClear)
    {
        //logger.info("Call clear if possible");
        //如果不成功，同样清空栈
        if (forceClear)
        {
            //logger.info("Force clear");
            simStack.clear();
            return;
        }

        // 如果目标实体不是玩家，那么清空simStack
        var targetedEntity = player.getTargetEntity(5);
        if (!(targetedEntity instanceof Player targetedPlayer))
        {
            simStack.clear();
            return;
        }

        //by_name: 直接清空
        //by_sight: 如果下个不是目标，那么清空
        var mode = selectionMode.get();
        if (mode.equalsIgnoreCase(InteractionMirrorSelectionMode.BY_NAME))
        {
            simStack.clear();
        }
        else
        {
            //logger.info("Is by sight");
            var nextSelection = getMirrorTarget(player);
            //logger.info("Next sel:" + nextSelection);

            // 如果玩家面向的目标不是下一个mirror对象，那么清空栈
            if (!targetedPlayer.equals(nextSelection.target))
            {
                //logger.info("Target not equal, clear");
                simStack.clear();
                return;
            }

            var targetPlayerTarget = targetedPlayer.getTargetEntity(5);

            //if (debugOutput.get())
            //    logger.info("Target player target:" + targetPlayerTarget);

            // 如果面向的玩家没有面向任何东西，那么也清空栈
            if (targetPlayerTarget == null)
            {
                //logger.info("Null target clear");
                simStack.clear();
                return;
            }

            // 如果玩家面向的目标也在面向自己，那么清空栈
            if (player.equals(targetPlayerTarget))
            {
                //logger.info("Circular clear");
                simStack.clear();
                return;
            }

            //logger.info("All passes failed");
        }
    }

    private void simulateOperationAsync(Action action, Player targetPlayer, Player source, Consumer<Boolean> callback)
    {
        AtomicBoolean success = new AtomicBoolean(false);
        targetPlayer.getScheduler().run(plugin, task ->
        {
            success.set(simulateOperation(action, targetPlayer, source));
            callback.accept(success.get());
        }, () -> { /* retired */ });
    }

    /**
     * 模拟玩家操作
     *
     * @param action 操作类型
     * @param targetPlayer 目标玩家
     * @return 操作是否成功
     */
    private boolean simulateOperation(Action action, Player targetPlayer, Player source)
    {
        if (!allowSimulation.get()) return false;

        // 如果栈内包含目标玩家，或者此玩家这个tick已经和环境互动过了一次，那么忽略此操作
        if (simStack.contains(targetPlayer) || tracker.interactingThisTick(targetPlayer)) return false;

        // 向栈推送此玩家
        simStack.push(source);

        if (debugOutput.get())
        {
            var builder = new StringBuilder();

            simStack.forEach(p -> builder.append(p.getName()).append(" -> "));
            builder.append("[Not Contained] ").append(targetPlayer.getName());

            logger.info("SimStack: %s :: Tick %s".formatted(builder, plugin.getCurrentTick()));
        }

        var isRightClick = action.isRightClick();
        var result = isRightClick
                ? operationSimulator.simulateRightClick(targetPlayer)
                : operationSimulator.simulateLeftClick(targetPlayer);

        boolean success = false;

        if (result.success())
        {
            var itemInUse = targetPlayer.getEquipment().getItem(result.hand()).getType();

            if (!isRightClick || !ItemUtils.isContinuousUsable(itemInUse) || result.forceSwing())
            {
                //如果栈上最后的玩家不是目标，那么使其挥手
                if (!simStack.empty() && !simStack.peek().equals(targetPlayer))
                    targetPlayer.swingHand(result.hand());
            }

            success = true;
        }

        clearStackIfPossible(targetPlayer, !result.success());

        return success;
    }

    public static class InteractionMirrorSelectionMode
    {
        public static final String BY_NAME = "BY_NAME";
        public static final String BY_SIGHT = "BY_SIGHT";
    }

    @Resolved
    private MorphManager manager;

    public record PlayerInfo(@Nullable Player target, @NotNull String targetName)
    {
        public static final String notSetStr = "~NOTSET";
    }

    /**
     * Search for a player that matches the target name.
     * @param player The {@link Player} who triggered this operation
     * @return A player who matches the target name
     * @apiNote If {@link ConfigOption#MIRROR_SELECTION_MODE} is set to {@link InteractionMirrorSelectionMode#BY_SIGHT},
     *          the returned value might be a player who disguised as our searching target.
     */
    @NotNull
    private PlayerInfo getMirrorTarget(Player player)
    {
        var targetName = mirrorMap.getOrDefault(player, null);
        if (targetName == null) return new PlayerInfo(null, PlayerInfo.notSetStr);

        PlayerInfo info;

        if (selectionMode.get().equalsIgnoreCase(InteractionMirrorSelectionMode.BY_SIGHT))
        {
            var targetEntity = player.getTargetEntity(5);

            if (!(targetEntity instanceof Player targetPlayer))
                return new PlayerInfo(null, targetName);

            var state = manager.getDisguiseStateFor(targetPlayer);

            if (state != null && state.getDisguiseIdentifier().equals("player:" + targetName))
                info = new PlayerInfo(targetPlayer, targetName);
            else if (targetPlayer.getName().equals(targetName) && state == null)
                info = new PlayerInfo(targetPlayer, targetName);
            else
                info = new PlayerInfo(null, targetName);
        }
        else
        {
            var targetPlayer = Bukkit.getPlayerExact(targetName);

            if (targetPlayer == null || !playerNotDisguised(targetPlayer))
                info = new PlayerInfo(null, targetName);
            else
                info = new PlayerInfo(targetPlayer, targetName);
        }

        //忽略在交互栈上的玩家
        if (simStack.contains(info.target))
            info = new PlayerInfo(null, targetName);

        return info;
    }

    /**
     * Check whether this player passes the disguise check for InteractionMirror
     * @param player The {@link Player} to check
     * @return True if this player is not disguised or {@link ConfigOption#MIRROR_IGNORE_DISGUISED} is off,
     *         otherwise false.
     */
    private boolean playerNotDisguised(Player player)
    {
        //ignoreDisguised.get() ? false : !isDisguised;
        return !ignoreDisguised.get() || manager.getDisguiseStateFor(player) == null;
    }

    private boolean playerInDistance(Player source, PlayerInfo inf)
    {
        return playerInDistance(source, inf.target);
    }

    @Contract("_, null-> false; _, !null -> _")
    private boolean playerInDistance(@NotNull Player source, @Nullable Player target)
    {
        if (target == null
                || !source.hasPermission(CommonPermissions.MIRROR) //检查来源是否有权限进行操控
                || target.hasPermission(CommonPermissions.MIRROR_IMMUNE) //检查目标是否免疫操控
                || target.getOpenInventory().getType() != InventoryType.CRAFTING //检查目标是否正和容器互动
                || target.isSleeping() //检查目标是否正在睡觉
                || target.isDead() //检查目标是否已经死亡
                || !DisguiseUtils.gameModeMirrorable(target)) //检查目标游戏模式是否满足操控条件
        {
            return false;
        }

        var isInSameWorld = target.getWorld().equals(source.getWorld());
        var normalDistance = config.getOrDefault(Integer.class, ConfigOption.MIRROR_CONTROL_DISTANCE);

        //normalDistance为-1，总是启用，为0则禁用
        return normalDistance == -1
                || (normalDistance != 0 && isInSameWorld && target.getLocation().distance(source.getLocation()) <= normalDistance);
    }

    private final Bindable<Boolean> allowSimulation = new Bindable<>(false);
    private final Bindable<Boolean> allowSneak = new Bindable<>(false);
    private final Bindable<Boolean> allowSwap = new Bindable<>(false);
    private final Bindable<Boolean> allowDrop = new Bindable<>(false);
    private final Bindable<Boolean> allowHotBar = new Bindable<>(false);
    private final Bindable<Boolean> ignoreDisguised = new Bindable<>(false);
    private final Bindable<String> selectionMode = new Bindable<>(InteractionMirrorSelectionMode.BY_NAME);
    private final Bindable<Boolean> logOperations = new Bindable<>(false);
    private final Bindable<Integer> cleanUpDate = new Bindable<>(3);
    private final Bindable<Boolean> debugOutput = new Bindable<>(false);

    @Initializer
    private void load()
    {
        this.addSchedule(this::update);

        config.bind(allowSimulation, ConfigOption.MIRROR_BEHAVIOR_DO_SIMULATION);
        config.bind(allowSneak, ConfigOption.MIRROR_BEHAVIOR_SNEAK);
        config.bind(allowSwap, ConfigOption.MIRROR_BEHAVIOR_SWAP_HAND);
        config.bind(allowDrop, ConfigOption.MIRROR_BEHAVIOR_DROP);
        config.bind(allowHotBar, ConfigOption.MIRROR_BEHAVIOR_HOTBAR);
        config.bind(ignoreDisguised, ConfigOption.MIRROR_IGNORE_DISGUISED);

        config.bind(selectionMode, ConfigOption.MIRROR_SELECTION_MODE);

        config.bind(logOperations, ConfigOption.MIRROR_LOG_OPERATION);
        config.bind(cleanUpDate, ConfigOption.MIRROR_LOG_CLEANUP_DATE);

        config.bind(debugOutput, ConfigOption.DEBUG_OUTPUT);
    }

    private void update()
    {
        this.addSchedule(this::update);

        if (plugin.getCurrentTick() % (5 * 20) == 0)
            pushToLoggingBase();
    }

    //region Morph events

    @EventHandler
    public void onPlayerExit(PlayerQuitEvent e)
    {
        mirrorMap.remove(e.getPlayer());
    }

    @EventHandler
    public void onPlayerMorph(PlayerMorphEvent e)
    {
        addOrRemoveFromMirrorMap(e.state, e.getPlayer());
    }

    @EventHandler
    public void onPlayerUnMorph(PlayerUnMorphEvent e)
    {
        mirrorMap.remove(e.getPlayer());
    }

    @EventHandler
    public void onJoinedWithState(PlayerJoinedWithDisguiseEvent e)
    {
        addOrRemoveFromMirrorMap(e.state, e.getPlayer());
    }

    private void addOrRemoveFromMirrorMap(DisguiseState state, Player player)
    {
        var id = state.getDisguiseIdentifier();

        if (DisguiseTypes.fromId(id) == DisguiseTypes.PLAYER)
            mirrorMap.put(player, DisguiseTypes.PLAYER.toStrippedId(id));
        else
            mirrorMap.remove(player);
    }

    //endregion Morph events

    //region Operation Logging

    private final DirectoryStorage logStore = new DirectoryStorage("logs");

    private final Map<Player, Stack<MirrorSingleEntry>> tempEntries = new Object2ObjectOpenHashMap<>();

    private final SimpleDateFormat logFileTimeFormat = new SimpleDateFormat("yyyy-MM-dd");

    private File loggingTargetFile;

    private String currentLogDate = "0000-00-00";

    private void cleanUpLogFiles(int days)
    {
        if (days <= 0) return;

        var files = logStore.getFiles("mirror-[0-9]{4}-[0-9]{2}-[0-9]{2}.log");
        var calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -days);

        //todo: Replace this with a nicer implementation
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        var targetDate = calendar.getTime();

        for (File file : files)
        {
            //[0:mirror] [1:YYYY] [2:MM] [3:dd] [4:.log]
            var splitName = file.getName().split("-");
            if (splitName.length < 4) continue;

            var formattedName = "%s-%s-%s".formatted(splitName[1], splitName[2], splitName[3]);

            if (formattedName.equals(currentLogDate)) continue;

            Date date;

            try
            {
                date = logFileTimeFormat.parse(formattedName);
            }
            catch (Throwable t)
            {
                logger.error("Unable to determine creation date for InteractionMirror log file '%s': '%s'".formatted(file.getName(), t.getLocalizedMessage()));
                t.printStackTrace();
                continue;
            }

            if (date.after(targetDate)) continue;

            try
            {
                logger.info("Removing InteractionMirror log '%s' as it's older than %s day(s)".formatted(file.getName(), days));

                if (!file.delete())
                    logger.warn("Unable to remove file: Unknown error");
            }
            catch (Throwable t)
            {
                logger.error("Unable to remove file: %s".formatted(t.getLocalizedMessage()));
                t.printStackTrace();
            }
        }
    }

    private void updateTargetFile()
    {
        cleanUpLogFiles(cleanUpDate.get());

        var targetLogDate = logFileTimeFormat.format(new Date(System.currentTimeMillis()));
        var createNew = !targetLogDate.equals(currentLogDate);

        if (!createNew) return;

        this.currentLogDate = targetLogDate;
        this.loggingTargetFile = logStore.getFile("mirror-%s.log".formatted(targetLogDate), true);
    }

    public void pushToLoggingBase()
    {
        if (logStore.initializeFailed())
            return;

        if (loggingTargetFile == null)
            updateTargetFile();

        synchronized (tempEntries)
        {
            var dateFormat = new SimpleDateFormat("HH:mm:ss");

            if (tempEntries.isEmpty())
                return;

            try (var stream = new FileOutputStream(this.loggingTargetFile, true))
            {
                tempEntries.forEach((p, stack) ->
                {
                    for (var entry : stack)
                    {
                        String msg = "";

                        msg += "[%s] %s triggered operation %s for player %s repeating %s time(s).\n"
                                .formatted(dateFormat.format(new Date(entry.timeMills())),
                                        entry.playerName(), entry.operationType(),
                                        entry.targetPlayerName(), entry.repeatingTimes());

                        try
                        {
                            stream.write(msg.getBytes());
                        }
                        catch (IOException e)
                        {
                            logger.error("Error occurred while saving logs: " + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
            catch (Throwable throwable)
            {
                logger.error("Error occurred while saving logs: " + throwable.getLocalizedMessage());
                throwable.printStackTrace();
            }

            this.tempEntries.clear();
        }
    }

    @NotNull
    private MirrorSingleEntry getOrCreateEntryFor(Player player, Player targetPlayer, OperationType type)
    {
        synchronized (tempEntries)
        {
            var playerStack = tempEntries.getOrDefault(player, null);

            if (playerStack == null)
            {
                playerStack = new Stack<>();
                tempEntries.put(player, playerStack);
            }

            MirrorSingleEntry entry = null;

            if (playerStack.size() > 0)
            {
                var peek = playerStack.peek();
                if (peek.uuid().equals(player.getUniqueId().toString())
                        && peek.targetPlayerName().equals(targetPlayer.getName())
                        && peek.operationType() == type)
                {
                    entry = peek;
                }
            }

            if (entry != null) return entry;

            entry = new MirrorSingleEntry(player.getName(), player.getUniqueId().toString(), targetPlayer.getName(), type, 0, System.currentTimeMillis());
            playerStack.push(entry);

            return entry;
        }
    }

    private void logOperation(Player source, Player targetPlayer, OperationType type)
    {
        if (!logOperations.get()) return;

        var entry = getOrCreateEntryFor(source, targetPlayer, type);
        entry.increaseRepeatingTimes();
    }

    //endregion Operation Logging
}
