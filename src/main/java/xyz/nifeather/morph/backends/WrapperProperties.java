package xyz.nifeather.morph.backends;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Optional;

public class WrapperProperties
{
    public static final SingleProperty<String> DISGUISE_ID = SingleProperty.of("wrapper_disguiseIdentifier", "nil");
    public static final SingleProperty<Optional<GameProfile>> PROFILE = SingleProperty.of("wrapper_profile", Optional.empty());
    public static final SingleProperty<CompoundTag> NBT = SingleProperty.of("wrapper_nbt", new CompoundTag());
    public static final SingleProperty<Boolean> DISPLAY_FAKE_EQUIP = SingleProperty.of("wrapper_display_fake_equip", false);
    public static final SingleProperty<String> DISGUISE_NAME = SingleProperty.of("wrapper_disguise_name", "");
    public static final SingleProperty<Boolean> SADDLED = SingleProperty.of("wrapper_saddled", false);
}
