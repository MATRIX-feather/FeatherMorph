package xiamomc.morph.backends;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.MorphPluginObject;

public abstract class DisguiseBackend<TInstance, TWrapper extends DisguiseWrapper<TInstance>> extends MorphPluginObject
{
    /**
     * Gets the identifier of this backend.
     * @return An identifier of this backend.
     */
    public abstract String getIdentifier();

    /**
     * Creates a disguise from the giving entity
     * @param targetEntity The entity used to construct disguise
     * @return A wrapper that handles the constructed disguise
     */
    public abstract DisguiseWrapper<TInstance> createInstance(@NotNull Entity targetEntity);

    /**
     * Creates a disguise by the giving type
     * @param entityType Target entity type
     * @return A wrapper that handles the constructed disguise
     */
    public abstract DisguiseWrapper<TInstance> createInstance(EntityType entityType);

    /**
     * Creates a player disguise by the giving name
     * @param targetPlayerName Target player name
     * @return A wrapper that handles the constructed disguise
     */
    public abstract DisguiseWrapper<TInstance> createPlayerInstance(String targetPlayerName);

    /**
     * Creates a disguise instance directly from the entity
     * @param entity The entity used to construct disguise
     * @return The constructed instance
     */
    public abstract TInstance createRawInstance(Entity entity);

    /**
     * Checks whether an entity is disguised by this backend
     * @param target The entity to check
     * @return Whether this entity is disguised by this backend
     */
    public abstract boolean isDisguised(Entity target);

    /**
     * Gets the wrapper that handles the target entity's disguise instance
     * @param target The entity to lookup
     * @return The wrapper that handles the entity's disguise. Null if it's not disguised.
     */
    @Nullable
    public abstract TWrapper getDisguise(Entity target);

    /**
     * 将某一玩家伪装成给定Wrapper中的实例
     * @param player 目标玩家
     * @param wrapper 目标Wrapper
     * @return 操作是否成功
     * @apiNote 传入的wrapper可能不是此后端产出的Wrapper
     */
    public abstract boolean disguise(Player player, DisguiseWrapper<?> wrapper);

    /**
     * Undisguise a player
     * @param player The player to undisguise
     * @return Whether the operation was successful
     */
    public abstract boolean unDisguise(Player player);

    /**
     * Deserialize a wrapper instance from the giving parameter
     * @param offlineParameter The parameter to deserialize
     * @return A wrapper that presents the giving parameter.
     *         null if invalid or illegal
     * @apiNote The format for the input string is undefined and may looks like one of these three formats: "id|content", "id|*empty*", "*empty*"
     */
    @Nullable
    public abstract TWrapper fromOfflineSave(String offlineParameter);

    /**
     * Serialize a wrapper instance to a string that can be saved in the Offline Storage
     * @param wrapper The target wrapper to save
     * @return A serialized string that can be deserialized to a wrapper in the future.
     *         Null if the giving wrapper is not supported by this backend.
     */
    @Nullable
    public abstract String toOfflineSave(DisguiseWrapper<?> wrapper);
}
