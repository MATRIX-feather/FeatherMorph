package xiamomc.morph.backends.server.renderer.network.registries;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import xiamomc.morph.misc.DisguiseEquipment;

import java.util.UUID;

public class CustomEntries
{
    public static final RegistryKey<GameProfile> PROFILE = RegistryKey.of("profile", new GameProfile(UUID.randomUUID(), "sample"));
    public static final RegistryKey<Boolean> PROFILE_LISTED = RegistryKey.of("profile_listed", false);

    public static final RegistryKey<String> DISGUISE_NAME = RegistryKey.of("disguise_name", "").doRequireNonNull();

    public static final RegistryKey<DisguiseEquipment> EQUIPMENT = RegistryKey.of("equip", new DisguiseEquipment());
    public static final RegistryKey<Boolean> DISPLAY_FAKE_EQUIPMENT = RegistryKey.of("display_fake_equip", false);

    /**
     * @deprecated Use {@link CustomEntries#SPAWN_ID} instead
     */
    @Deprecated(since = "1.2.6", forRemoval = true)
    public static final RegistryKey<UUID> TABLIST_UUID = RegistryKey.of("tablist_uuid", Util.NIL_UUID);

    public static final RegistryKey<Boolean> WARDEN_CHARGING_ATTACK = RegistryKey.of("warden_charging_attack", false);
    public static final RegistryKey<Boolean> ATTACK_ANIMATION = RegistryKey.of("attack_animation", false);

    public static final RegistryKey<Integer> SLIME_SIZE_REAL = RegistryKey.of("slime_size_real", 0);

    public static final RegistryKey<String> ANIMATION = RegistryKey.of("animation", "morph:unknown");

    public static final RegistryKey<UUID> SPAWN_UUID = RegistryKey.of("spawn_uuid", Util.NIL_UUID);
    public static final RegistryKey<Integer> SPAWN_ID = RegistryKey.of("spawn_id", -1);
}
