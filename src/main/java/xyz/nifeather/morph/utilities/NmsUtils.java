package xyz.nifeather.morph.utilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.FeatherMorphMain;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NmsUtils
{
    private static final Logger log = LoggerFactory.getLogger(NmsUtils.class);

    public static Entity spawnEntity(EntityType bukkitType, World targetWorld, Location location)
    {
        var nmsType = EntityTypeUtils.getNmsType(bukkitType);

        if (nmsType == null)
            throw new IllegalArgumentException("No NMS EntityType for bukkit type '%s'".formatted(bukkitType));

        var nmsWorld = ((CraftWorld) targetWorld).getHandle();
        var nmsEntity = nmsType.create(nmsWorld, EntitySpawnReason.COMMAND);

        if (nmsEntity == null)
            throw new IllegalArgumentException("Unable to spawn entity");

        nmsEntity.setPos(new Vec3(location.x(), location.y(), location.z()));

        return nmsEntity.getBukkitEntity();
    }

    public static ServerLevel getNmsLevel(World world)
    {
        return ((CraftWorld)world).getHandle();
    }

    private final static Map<EntityType, List<String>> syncableAttributesMap = new Object2ObjectArrayMap<>();

    public static List<AttributeInstance> getValidAttributes(EntityType targetType, AttributeMap mapToLookup)
    {
        var validAttributes = getSyncableAttributeListFor(targetType);

        if (validAttributes.isEmpty()) return new ObjectArrayList<>();

        var existing = new ObjectArrayList<>(mapToLookup.getSyncableAttributes());
        existing.removeIf(instance -> validAttributes.stream().noneMatch(s -> s.equals(instance.getAttribute().getRegisteredName())));

        return existing;
    }

    @Unmodifiable
    public static List<String> getSyncableAttributeListFor(EntityType bukkitType)
    {
        var logger = FeatherMorphMain.getInstance().getSLF4JLogger();
        var cached = syncableAttributesMap.getOrDefault(bukkitType, null);
        if (cached != null)
            return cached;

        if (!bukkitType.isAlive())
            return List.of();

        var nmsType = EntityTypeUtils.getNmsType(bukkitType);

        if (nmsType == null || !DefaultAttributes.hasSupplier(nmsType))
            return List.of();

        // 因为AttributeMap是lazy init，所以我们需要先遍历一遍才能知道哪些属性可以同步
        var lists = ReflectionUtils.getFields(new Attributes(), Holder.class, false);
        var holders = lists.stream()
                .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()))
                .toList();

        // 遍历Attributes中已知的所有属性
        // 没有使用BuiltInRegistries是因为我不知道怎么做... :(

        Map<Holder<Attribute>, AttributeInstance> attributesTempMap = new Object2ObjectOpenHashMap<>();

        holders.forEach(f ->
        {
            Holder<?> holder;

            try
            {
                // 获取值，并检查其是否符合我们想要的类型（Holder<Attribute>）
                // 如果不是，说明我们获取到了错误的地方，立马抛出异常！
                var obj = f.get(null);

                if (obj instanceof Holder<?> holderInstance)
                {
                    if (holderInstance.value() instanceof Attribute)
                        holder = holderInstance;
                    else
                        throw new IllegalArgumentException("The type of the holder is not a attribute! Got '%s'".formatted(holderInstance.value().getClass()));
                }
                else
                    throw new IllegalArgumentException("The field '%s' is not a Holder type! Got '%s'".formatted(f.getName(), f.getType()));

                // 激活属性map
                var supplier = DefaultAttributes.getSupplier((net.minecraft.world.entity.EntityType<? extends LivingEntity>) nmsType);
                attributesTempMap.computeIfAbsent((Holder<Attribute>) holder, holderx -> supplier.createInstance(mod -> {}, holderx));
            }
            catch (Throwable t)
            {
                logger.error("Can't activate attribute: " + t.getMessage());
            }
        });

        var validAttributes = attributesTempMap.values().stream()
                .filter(instance ->
                {
                    return instance.getAttribute().value().isClientSyncable()
                            && !instance.getAttribute().getRegisteredName().equals("[unregistered]");
                })
                .map(instance -> instance.getAttribute().getRegisteredName())
                .collect(Collectors.toCollection(ObjectArrayList::new));

        syncableAttributesMap.put(bukkitType, ObjectLists.unmodifiable(validAttributes));

        return validAttributes;
    }
}
