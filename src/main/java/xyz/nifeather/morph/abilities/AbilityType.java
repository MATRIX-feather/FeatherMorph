package xyz.nifeather.morph.abilities;

import org.bukkit.NamespacedKey;

public class AbilityType
{
    private static final String nameSpace = "morph";

    public static final NamespacedKey CAN_FLY = new NamespacedKey(nameSpace, "can_fly");
    public static final NamespacedKey CAN_BREATHE_UNDER_WATER = new NamespacedKey(nameSpace, "breathe_under_water");
    public static final NamespacedKey HAS_FIRE_RESISTANCE = new NamespacedKey(nameSpace, "fire_resistance");
    public static final NamespacedKey BURNS_UNDER_SUN = new NamespacedKey(nameSpace, "burns_under_sun");
    public static final NamespacedKey TAKES_DAMAGE_FROM_WATER = new NamespacedKey(nameSpace, "takes_damage_from_water");
    public static final NamespacedKey ALWAYS_NIGHT_VISION = new NamespacedKey(nameSpace, "night_vision");
    public static final NamespacedKey HAS_JUMP_BOOST = new NamespacedKey(nameSpace, "normal_jump_boost");
    public static final NamespacedKey HAS_SMALL_JUMP_BOOST = new NamespacedKey(nameSpace, "small_jump_boost");

    @Deprecated
    public static final NamespacedKey HAS_SPEED_BOOST = new NamespacedKey(nameSpace, "speed");

    public static final NamespacedKey HAS_FEATHER_FALLING = new NamespacedKey(nameSpace, "feather_falling");
    public static final NamespacedKey NO_FALL_DAMAGE = new NamespacedKey(nameSpace, "no_fall_damage");
    public static final NamespacedKey SNOWY = new NamespacedKey(nameSpace, "snowy");
    public static final NamespacedKey WARDEN_LESS_AWARE = new NamespacedKey(nameSpace, "warden_less_aware");
    public static final NamespacedKey WARDEN = new NamespacedKey(nameSpace, "warden");
    public static final NamespacedKey CHAT_OVERRIDE = new NamespacedKey(nameSpace, "chat_override");
    public static final NamespacedKey BOSSBAR = new NamespacedKey(nameSpace, "bossbar");
    public static final NamespacedKey NO_SWEET_BUSH_DAMAGE = new NamespacedKey(nameSpace, "no_sweet_bush_damage");
    public static final NamespacedKey ATTRIBUTE = new NamespacedKey(nameSpace, "attribute_modify");
    public static final NamespacedKey HEALS_FROM_ENTITY = new NamespacedKey(nameSpace, "heals_from_entity");
    public static final NamespacedKey EXTRA_KNOCKBACK = new NamespacedKey(nameSpace, "extra_knockback");
    public static final NamespacedKey DRYOUT_IN_AIR = new NamespacedKey(nameSpace, "dryout_in_air");
    public static final NamespacedKey POTION_ON_ATTACK = new NamespacedKey(nameSpace, "potion_on_attack");
    public static final NamespacedKey SPIDER = new NamespacedKey(nameSpace, "spider");

    public static final NamespacedKey REDUCES_FALL_DAMAGE = new NamespacedKey(nameSpace, "reduce_fall_damage");
    public static final NamespacedKey REDUCES_MAGIC_DAMAGE = new NamespacedKey(nameSpace, "reduce_magic_damage");
    public static final NamespacedKey REDUCES_WITHER_DAMAGE = new NamespacedKey(nameSpace, "reduce_wither_damage");
}
