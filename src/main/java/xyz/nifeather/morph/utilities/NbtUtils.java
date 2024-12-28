package xyz.nifeather.morph.utilities;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.Util;
import net.minecraft.nbt.*;
import net.minecraft.server.commands.data.EntityDataAccessor;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.misc.MorphGameProfile;

import java.util.List;
import java.util.UUID;

public class NbtUtils
{
    public static CompoundTag writeGameProfile(CompoundTag nbt, GameProfile profile)
    {
        if (!profile.getName().isEmpty())
            nbt.putString("Name", profile.getName());

        if (!profile.getId().equals(Util.NIL_UUID))
            nbt.putUUID("Id", profile.getId());

        if (profile.getProperties().isEmpty())
            return nbt;

        var propertiesCompound = new CompoundTag();

        for (var key : profile.getProperties().keySet())
        {
            var list = new ListTag();

            CompoundTag childCompound;
            for (var property : profile.getProperties().get(key))
            {
                childCompound = new CompoundTag();
                childCompound.putString("Value", property.value());

                var sign = property.signature();
                if (sign != null)
                    childCompound.putString("Signature", sign);

                list.add(childCompound);
            }

            propertiesCompound.put(key, list);
        }

        nbt.put("Properties", propertiesCompound);

        return nbt;
    }

    @javax.annotation.Nullable
    public static GameProfile readGameProfile(String snbt)
    {
        CompoundTag compound = null;

        try
        {
            compound = TagParser.parseTag(snbt);
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();

            logger.warn("Unable to parse GameProfile: " + t.getMessage());
            logger.warn("Raw profile: '%s'".formatted(snbt));

            return null;
        }

        String name = "NIL";
        if (compound.contains("Name", Tag.TAG_STRING))
            name = compound.getString("Name");

        UUID uuid = Util.NIL_UUID;
        if (compound.hasUUID("Id"))
            uuid = compound.getUUID("Id");

        var profile = new MorphGameProfile(new GameProfile(uuid, name));

        if (!compound.contains("Properties", Tag.TAG_COMPOUND)) return profile;

        try
        {
            var propertiesCompound = compound.getCompound("Properties");

            for (var key : propertiesCompound.getAllKeys())
            {
                var list = propertiesCompound.getList(key, Tag.TAG_COMPOUND);

                for (int i = 0; i < list.size(); i++)
                {
                    var childCompound = list.getCompound(i);
                    var value = childCompound.getString("Value");

                    if (childCompound.contains("Signature", Tag.TAG_STRING))
                        profile.getProperties().put(key, new Property(key, value, childCompound.getString("Signature")));
                    else
                        profile.getProperties().put(key, new Property(key, value));
                }
            }
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();

            logger.warn("Can't parse profile properties: " + t.getMessage());
            t.printStackTrace();
        }

        return profile;
    }

    public static CompoundTag toCompoundTag(GameProfile profile)
    {
        var compound = new CompoundTag();
        return writeGameProfile(compound, profile);
    }

    /**
     * 获取目标实体的 {@link CompoundTag}
     * @param entity 目标实体
     * @return 此实体的NBT数据，当实体为null或不为 {@link CraftEntity} 的实例时返回null
     */
    @NotNull
    public static CompoundTag getRawTagCompound(Entity entity)
    {
        if (entity instanceof CraftEntity craftEntity)
        {
            var nmsEntity = craftEntity.getHandle();

            var entityDataObject = new EntityDataAccessor(nmsEntity);

            return entityDataObject.getData();
        }

        return new CompoundTag();
    }

    /**
     * 将目标NBT序列化为字符串
     * @param compound 目标NBT
     * @return 由此NBT序列化的字符串，当compound为null时返回null
     */
    public static String getCompoundString(CompoundTag compound)
    {
        if (compound == null) return null;

        //StringNbtWriter
        var visitor = new StringTagVisitor();

        //StringNbtWriter#apply(NbtElement)
        return visitor.visit(compound);
    }

    /**
     *
     * @param input The input NBT string
     * @return Null if invalid of illegal
     */
    @Nullable
    public static CompoundTag toCompoundTag(@Nullable String input)
    {
        if (input == null || input.isEmpty()) return null;

        try
        {
            return TagParser.parseTag(input);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    @Nullable
    @Contract("_, false -> !null; _, true -> _")
    public static CompoundTag toCompoundTag(@Nullable String input, boolean nullIfInvalid)
    {
        var result = toCompoundTag(input);

        if (result != null) return result;

        return nullIfInvalid ? null : new CompoundTag();
    }

    public static boolean isBabyForType(EntityType type, CompoundTag compoundTag)
    {
        var ageable = EntityTypeUtils.hasBabyVariant(type);

        if (!ageable) return false;

        if (EntityTypeUtils.isZombie(type) || type == EntityType.PIGLIN)
            return compoundTag.getBoolean("IsBaby");

        var val = compoundTag.getInt("Age");

        return val < 0;
    }

    public static List<String> defaultBlacklistedPatterns = List.of(
            //Common
            "[pP]urpur.*", "[pP]aper.*", "[sS]pigot.*", "[bB]ukkit.*",

            //Player
            "Xp.*", "food.*",

            //Misc
            "Death.*", "Spawn.*"
    );

    public static List<String> defaultBlacklistedTags = List.of(
            //Common
            "UUID", "data", "Brain", "Motion", "palette", "Attributes",
            "Invulnerable",

            //Armor stand
            "DisabledSlots", "ArmorItems", "HandItems",

            //Player
            "Tags", "recipes", "Inventory", "abilities", "recipeBook",
            "EnderItems", "warden_spawn_tracker", "previousPlayerGameType",
            "LastDeathLocation", "playerGameType", "seenCredits", "Score",

            //Villager
            "Offers", "LastRestock", "RestocksToday",

            //Misc
            "Pos", "Owner", "WorldUUIDLeast", "WorldUUIDMost",
            "Rotation", "listener", "ActiveEffects", "ArmorDropChances",
            "PersistenceRequired", "SelectedItem"
    );
}
