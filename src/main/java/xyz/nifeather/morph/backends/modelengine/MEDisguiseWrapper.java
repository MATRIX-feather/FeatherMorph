package xyz.nifeather.morph.backends.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagTypes;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.backends.DisguiseBackend;
import xyz.nifeather.morph.backends.DisguiseWrapper;
import xyz.nifeather.morph.backends.WrapperEvent;
import xyz.nifeather.morph.misc.DisguiseEquipment;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MEDisguiseWrapper extends DisguiseWrapper<MEDisguiseInstance>
{
    private static final Logger log = LoggerFactory.getLogger(MEDisguiseWrapper.class);

    public MEDisguiseWrapper(@NotNull MEDisguiseInstance meDisguiseInstance, DisguiseBackend<MEDisguiseInstance, ? extends DisguiseWrapper<MEDisguiseInstance>> backend)
    {
        super(meDisguiseInstance, backend);

        ActiveModel activeModel = null;

        var modelId = instance.modelId;
        try
        {
            this.activeModel = ModelEngineAPI.createActiveModel(modelId);
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();

            logger.error("Failed to create active model for ID '%s': '%s'".formatted(modelId, t.getMessage()));
            t.printStackTrace();
        }
    }

    /**
     * Gets current displaying equipment
     *
     * @return A {@link EntityEquipment} that presents the fake equipment
     */
    @Override
    public EntityEquipment getFakeEquipments()
    {
        return equipment;
    }

    private final DisguiseEquipment equipment = new DisguiseEquipment();

    /**
     * Sets displaying equipment to the giving value
     *
     * @param newEquipment A {@link EntityEquipment} that presents the new equipment
     */
    @Override
    public void setFakeEquipments(@NotNull EntityEquipment newEquipment)
    {
        this.equipment.setArmorContents(newEquipment.getArmorContents());

        this.equipment.setHandItems(newEquipment.getItemInMainHand(), newEquipment.getItemInOffHand());
    }

    /**
     * Sets the state of server-side SelfView for the underlying disguise instance
     *
     * @param enabled Whether server-side SelfView should be turned on
     */
    @Override
    public void setServerSelfView(boolean enabled)
    {
    }

    /**
     * Gets current entity type for this wrapper
     *
     * @return A value that presents the current {@link EntityType}.
     */
    @Override
    public EntityType getEntityType()
    {
        return EntityType.UNKNOWN;
    }

    /**
     * Clone the underlying disguise instance
     *
     * @return A new instance cloned from the underlying disguise
     */
    @Override
    public MEDisguiseInstance copyInstance()
    {
        return null;
    }

    /**
     * Clone this wrapper
     *
     * @return A new wrapper cloned from this instance, everything in the new instance should not have any reference with this wrapper
     */
    @Override
    public DisguiseWrapper<MEDisguiseInstance> clone()
    {
        return null;
    }

    @Override
    public boolean isBaby()
    {
        return false;
    }

    /**
     * Actions when we finished constructing disguise
     *
     * @param state        A {@link DisguiseState} that handles the current wrapper
     * @param targetEntity The targeted entity (If there is any)
     */
    @Override
    public void onPostConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
    }

    /**
     * Updates the underlying disguise instance
     *
     * @param state  {@link DisguiseState}
     * @param player The player who owns the provided state
     */
    @Override
    public void update(DisguiseState state, Player player)
    {
    }

    /**
     * Merge NBT to the underlying instance
     *
     * @param compound {@link CompoundTag}
     */
    @Override
    public void mergeCompound(CompoundTag compound)
    {
    }

    /**
     * Gets a value from current compound
     *
     * @param path NBT Path
     * @param type {@link TagType}, check {@link TagTypes} for more information
     * @return A NBT tag, null if not found
     */
    @Override
    public <R extends Tag> @Nullable R getTag(String path, TagType<R> type)
    {
        return null;
    }

    /**
     * Returns a copy of the existing compound.
     */
    @Override
    public CompoundTag getCompound()
    {
        return null;
    }

    /**
     * Gets network id of this disguise displayed to other players
     *
     * @return The network id of this disguise
     */
    @Override
    public int getNetworkEntityId()
    {
        return getBindingPlayer() == null ? -1 : getBindingPlayer().getEntityId();
    }

    private final Map<SingleProperty<?>, Object> disguiseProperties = new ConcurrentHashMap<>();

    @Override
    public <X> void writeProperty(SingleProperty<X> property, X value)
    {
        disguiseProperties.put(property, value);
    }

    @Override
    public <X> @NotNull X readProperty(SingleProperty<X> property)
    {
        return this.readPropertyOr(property, property.defaultVal());
    }

    @Override
    public <X> X readPropertyOr(SingleProperty<X> property, X defaultVal)
    {
        return (X) disguiseProperties.getOrDefault(property, defaultVal);
    }

    @Override
    public <X> X readPropertyOrThrow(SingleProperty<X> property)
    {
        var val = disguiseProperties.getOrDefault(property, null);
        if (val == null) throw new NullDependencyException("The requested property '%s' was not found in %s".formatted(property.id(), this));

        return (X) val;
    }

    @Override
    public Map<SingleProperty<?>, Object> getProperties()
    {
        return new Object2ObjectOpenHashMap<>(this.disguiseProperties);
    }

    @Override
    public <T> void subscribeEvent(Object source, WrapperEvent<T> wrapperEvent, Consumer<T> c)
    {
    }

    @Override
    public void unSubscribeEvent(Object source, WrapperEvent<?> wrapperEvent)
    {
    }

    private ActiveModel activeModel;

    @Nullable
    public ActiveModel getActiveModel()
    {
        return activeModel;
    }

    private final AtomicReference<Player> bindingPlayer = new AtomicReference<>();

    private final AtomicReference<ModeledEntity> modeled = new AtomicReference<ModeledEntity>();

    @Nullable
    public ModeledEntity getModeled()
    {
        return modeled.get();
    }

    public void bindPlayer(Player player)
    {
        this.bindingPlayer.set(player);

        this.modeled.set(ModelEngineAPI.createModeledEntity(player));
    }

    public String getModelID()
    {
        return instance.modelId;
    }

    @Nullable
    public Player getBindingPlayer()
    {
        return bindingPlayer.get();
    }

    public static MEDisguiseWrapper clone(MEDisguiseWrapper wrapper, MEBackend backend)
    {
        var newInstance = new MEDisguiseWrapper(new MEDisguiseInstance(wrapper.getModelID()), backend);

        newInstance.disguiseProperties.putAll(wrapper.getProperties());

        return newInstance;
    }

    public static MEDisguiseWrapper cloneOther(DisguiseWrapper<?> other, MEBackend backend)
    {
        var newInstance = new MEDisguiseWrapper(new MEDisguiseInstance("_fallback"), backend);

        newInstance.disguiseProperties.putAll(other.getProperties());

        return newInstance;
    }
}
