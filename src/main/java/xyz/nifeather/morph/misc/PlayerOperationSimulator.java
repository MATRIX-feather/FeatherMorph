package xyz.nifeather.morph.misc;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;

import java.util.Map;
import java.util.Objects;

public class PlayerOperationSimulator extends MorphPluginObject
{
    private final Map<Player, BlockDestroyHandler> playerHandlerMap = new Object2ObjectOpenHashMap<>();

    @Initializer
    private void load(MorphConfigManager config)
    {
        this.addSchedule(this::update);

        config.bind(destroyTimeout, ConfigOption.MIRROR_DESTROY_TIMEOUT);
    }

    private final Bindable<Integer> destroyTimeout = new Bindable<>(40);

    private void update()
    {
        this.addSchedule(this::update, 5);

        var list = new Object2ObjectOpenHashMap<>(playerHandlerMap);

        var currentTick = plugin.getCurrentTick();

        list.forEach((p, i) ->
        {
            if (!p.isOnline())
            {
                playerHandlerMap.remove(p);
                return;
            }

            //移除不需要的Info
            if (currentTick - i.getLastUpdate() >= destroyTimeout.get())
            {
                i.setProgress(-1, currentTick);

                playerHandlerMap.remove(p);
            }
        });
    }

    private final float blockReachDistance = 4.5f;
    private final float entityReachDistance = 3f;

    private RayTraceResult rayTrace(Player player)
    {
        var eyeLoc = player.getEyeLocation();

        return player.getWorld().rayTrace(eyeLoc, eyeLoc.getDirection(), blockReachDistance,
                FluidCollisionMode.NEVER, false, 0d, e ->
                {
                    if (e instanceof Player p)
                        return e != player && p.getGameMode() != GameMode.SPECTATOR;
                    else
                        return true;
                });
    }

    /**
     * 模拟玩家左键
     *
     * @param player 目标玩家
     * @return 操作执行结果
     */
    public SimulateResult simulateLeftClick(Player player)
    {
        //logger.warn("模拟左键！");
        //Thread.dumpStack();

        //格挡时不要执行动作
        if (player.isBlocking()) return SimulateResult.fail();

        var nmsPlayer = ((CraftPlayer)player).getHandle();
        if (nmsPlayer.isUsingItem())
            return SimulateResult.fail();

        //获取正在看的方块或实体
        var traceResult = this.rayTrace(player);

        var targetBlock = traceResult == null ? null : traceResult.getHitBlock();
        var targetEntity = traceResult == null ? null : traceResult.getHitEntity();

        // 忽略超过实体reach范围的实体
        if (targetEntity != null && targetEntity.getLocation().distance(player.getLocation()) > entityReachDistance)
            targetEntity = null;

        //获取方块破坏信息
        var destroyHandler = playerHandlerMap.getOrDefault(player, null);

        //如果视野内有实体或者不再存在目标方块，那么使destroyHandler重置进度
        if ((targetBlock == null || targetEntity != null) && destroyHandler != null)
            destroyHandler.changeBlock(null);

        //优先攻击实体
        //如果目标实体不为null，则攻击该实体
        if (targetEntity != null)
        {
            if (player.getLocation().distance(targetEntity.getLocation()) > entityReachDistance)
                return SimulateResult.success(EquipmentSlot.HAND);

            player.attack(targetEntity);

            return SimulateResult.success(EquipmentSlot.HAND);
        }

        //没有实体也没有方块，视为对着空气空挥
        //此时不要做任何事情，只返回success来允许播放挥手动画
        if (targetBlock == null)
            return SimulateResult.success(EquipmentSlot.HAND);

        //如果玩家处于冒险模式并且无法破坏目标方块，也不要做任何事情，只返回success来允许播放动画
        if (player.getGameMode() == GameMode.ADVENTURE)
        {
            var item = net.minecraft.world.item.ItemStack.fromBukkitCopy(
                    Objects.requireNonNull(player.getEquipment(), "Null equipment?").getItemInMainHand());

            //todo: test this
            var canBreak = item.getComponents().get(DataComponents.CAN_BREAK);
            if (canBreak == null)
                return SimulateResult.fail();

            var craftBlock = ((CraftBlock) targetBlock);
            if (canBreak.predicates.stream()
                    .noneMatch(bp -> bp.matches(craftBlock.getHandle().getMinecraftWorld(), craftBlock.getPosition())))
            {
                return SimulateResult.success(EquipmentSlot.HAND);
            }
        }

        //=-=-=-=-=-=-=-=-=-=-=-=
        //
        // 在此开始对方块的处理
        //
        //=-=-=-=-=-=-=-=-=-=-=-=

        //初始化destoryInfo
        if (destroyHandler == null)
        {
            destroyHandler = new BlockDestroyHandler(targetBlock, 0, player);
            playerHandlerMap.put(player, destroyHandler);
        }

        //变更方块
        if (!targetBlock.equals(destroyHandler.getBlock()))
            destroyHandler.changeBlock(targetBlock);

        //NMS
        var nmsWorld = destroyHandler.getNmsWorld();
        var nmsBlock = destroyHandler.getNmsBlock();

        assert nmsBlock != null;
        assert nmsWorld != null;

        //AbstractBlockState#onBlockBreakStart(World world, BlockPos pos, PlayerEntity player)
        nmsBlock.attack(nmsWorld, ((CraftBlock) targetBlock).getPosition(), nmsPlayer);

        //AbstractBlockState#calcBlockBreakingDelta(PlayerEntity player, BlockView world, BlockPos pos)
        var delta = nmsBlock.getDestroyProgress(nmsPlayer, nmsWorld, ((CraftBlock) targetBlock).getPosition());

        //设置破坏进度
        var val = destroyHandler.getProgress();
        val += delta;

        destroyHandler.setProgress(val, plugin.getCurrentTick());

        return SimulateResult.success(EquipmentSlot.HAND);
    }

    /**
     * 模拟玩家右键
     *
     * @param player 目标玩家
     * @return 操作是否成功
     */
    public SimulateResult simulateRightClick(Player player)
    {
        //logger.warn("正在模拟右键！");

        var nmsPlayer = ((CraftPlayer)player).getHandle();
        if (nmsPlayer.isUsingItem())
        {
            nmsPlayer.releaseUsingItem();
            return SimulateResult.fail();
        }

        //获取正在看的方块或实体
        var traceResult = this.rayTrace(player);

        var targetBlock = traceResult == null ? null : traceResult.getHitBlock();
        var targetEntity = traceResult == null ? null : traceResult.getHitEntity();

        if (targetEntity != null && targetEntity.getLocation().distance(player.getLocation()) > entityReachDistance)
            targetEntity = null;

        var itemInMainHand = player.getEquipment().getItem(EquipmentSlot.HAND);
        var itemInOffHand = player.getEquipment().getItem(EquipmentSlot.OFF_HAND);

        //如果目标实体不是null，则和实体互动
        if (targetEntity != null)
        {
            //获取目标位置
            var hitPos = traceResult.getHitPosition();

            //获取互动位置
            hitPos.subtract(targetEntity.getLocation().toVector());

            //轮流尝试左右手
            if (this.tryUseItemOnEntity(player, targetEntity, itemInMainHand, InteractionHand.MAIN_HAND, hitPos))
                return SimulateResult.success(EquipmentSlot.HAND);

            return SimulateResult.of(this.tryUseItemOnEntity(player, targetEntity, itemInOffHand, InteractionHand.OFF_HAND, hitPos),
                                     EquipmentSlot.OFF_HAND);
        }

        //尝试和方块互动
        if (targetBlock != null)
        {
            var loc = ((CraftBlock) targetBlock).getPosition();

            Direction targetBlockDirection = null;
            var bukkitFace = player.getTargetBlockFace(5);
            bukkitFace = bukkitFace == null ? BlockFace.SELF : bukkitFace;

            //BlockFace(Bukkit)转BlockFace(Minecraft)
            switch (bukkitFace)
            {
                case DOWN -> targetBlockDirection = Direction.DOWN;
                case UP -> targetBlockDirection = Direction.UP;
                case NORTH -> targetBlockDirection = Direction.NORTH;
                case SOUTH -> targetBlockDirection = Direction.SOUTH;
                case WEST -> targetBlockDirection = Direction.WEST;
                case EAST -> targetBlockDirection = Direction.EAST;
                default -> logger.error("Unknown BlockFace: " + bukkitFace + ", will not attempt 'useItemOn'!");
            }

            if (targetBlockDirection != null)
            {
                var moving = new BlockHitResult(
                        new Vec3(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()),
                        targetBlockDirection, loc, false);

                if (this.tryUseItemOnBlock(player, itemInMainHand, InteractionHand.MAIN_HAND, moving))
                    return SimulateResult.success(EquipmentSlot.HAND, true);
                else if (this.tryUseItemOnBlock(player, itemInOffHand, InteractionHand.OFF_HAND, moving))
                    return SimulateResult.success(EquipmentSlot.OFF_HAND, true);
            }
        }

        if (this.tryUseItemOnSelf(player, itemInMainHand, InteractionHand.MAIN_HAND))
            return SimulateResult.success(EquipmentSlot.HAND);
        else if (this.tryUseItemOnSelf(player, itemInOffHand, InteractionHand.OFF_HAND))
            return SimulateResult.success(EquipmentSlot.OFF_HAND);

        return SimulateResult.fail();
    }

    private final PluginManager pluginManager = Bukkit.getPluginManager();

    /**
     * 尝试使某个玩家使用某个物品
     *
     * @param player 目标玩家
     * @param bukkitItem 物品
     * @param hand 要使用的 {@link InteractionHand}
     * @return 操作是否成功
     */
    private boolean tryUseItemOnSelf(Player player, ItemStack bukkitItem, InteractionHand hand)
    {
        var event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, bukkitItem, null, BlockFace.SELF);
        pluginManager.callEvent(event);

        if (event.useItemInHand() != Event.Result.DENY)
        {
            var record = NmsRecord.of(player);
            var manager = record.interactManager();

            //ServerPlayerGameMode.useItem()
            return manager.useItem(record.nmsPlayer(), record.nmsWorld(), CraftItemStack.asNMSCopy(bukkitItem), hand).consumesAction();
        }

        return false;
    }

    /**
     * 尝试使某个玩家对某个方块使用某个物品
     *
     * @param player 目标玩家
     * @param bukkitItem 物品
     * @param nmsHand 要使用的 {@link InteractionHand}
     * @param moving 要提供给NMS方法的 {@link BlockHitResult}
     * @return 操作是否成功
     */
    private boolean tryUseItemOnBlock(Player player,
                                      ItemStack bukkitItem, InteractionHand nmsHand,
                                      BlockHitResult moving)
    {
        var record = NmsRecord.of(player);

        ServerPlayerGameMode manager = record.interactManager();

        //ServerPlayerGameMode里已经调用了PlayerInteractEvent，因此不需要再重复调用一遍
        return manager.useItemOn(record.nmsPlayer(), record.nmsWorld(), CraftItemStack.asNMSCopy(bukkitItem), nmsHand, moving).consumesAction();
    }

    /**
     * 尝试使某个玩家在某个实体上使用某个物品
     *
     * @param player 目标玩家
     * @param targetEntity 目标实体
     * @param bukkitItem 物品
     * @param hand 要使用的 {@link InteractionHand}
     * @param hitPos 用于提供给{@link PlayerInteractAtEntityEvent}的位置
     * @return 操作是否成功
     */
    private boolean tryUseItemOnEntity(Player player, Entity targetEntity, ItemStack bukkitItem, InteractionHand hand, Vector hitPos)
    {
        var interactEntityEvent = new PlayerInteractEntityEvent(player, targetEntity);
        var interactAtEvent = new PlayerInteractAtEntityEvent(player, targetEntity, hitPos);
        pluginManager.callEvent(interactEntityEvent);
        pluginManager.callEvent(interactAtEvent);

        //先试试InteractAt
        //如果不成功，调用InteractOn
        //最后试试useItem
        if (!interactAtEvent.isCancelled() && !interactEntityEvent.isCancelled())
        {
            var record = NmsRecord.of(player, targetEntity);

            //EntityPlayer -> ServerPlayer in mojang mappings
            var playerHandle = record.nmsPlayer();

            //ServerLevel in mojang mappings
            var worldHandle = record.nmsWorld();
            var entityHandle = record.nmsEntity();
            var manager = record.interactManager();

            var vec = new Vec3(hitPos.getX(), hitPos.getY(), hitPos.getZ());

            assert entityHandle != null;
            return entityHandle.interactAt(playerHandle, vec, hand).consumesAction()
                    || playerHandle.interactOn(entityHandle, hand).consumesAction()
                    || manager.useItem(playerHandle, worldHandle, CraftItemStack.asNMSCopy(bukkitItem), hand).consumesAction();
        }

        return false;
    }

    /**
     * 操作模拟结果
     *
     * @param success 是否成功
     * @param hand 与 {@link InteractionHand} 对应的 {@link EquipmentSlot}
     */
    public record SimulateResult(boolean success, EquipmentSlot hand, boolean forceSwing)
    {
        public static SimulateResult success(EquipmentSlot hand)
        {
            return of(true, hand);
        }
        public static SimulateResult success(EquipmentSlot hand, boolean clickedOnBlock)
        {
            return of(true, hand, clickedOnBlock);
        }

        public static SimulateResult fail()
        {
            return of(false, null);
        }

        public static SimulateResult of(boolean success, EquipmentSlot hand)
        {
            return new SimulateResult(success, hand, false);
        }

        public static SimulateResult of(boolean success, EquipmentSlot hand, boolean clickedOnBlock)
        {
            return new SimulateResult(success, hand, clickedOnBlock);
        }
    }
}
