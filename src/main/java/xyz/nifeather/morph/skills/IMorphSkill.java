package xyz.nifeather.morph.skills;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.SkillStrings;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.storage.skill.ISkillOption;
import xyz.nifeather.morph.storage.skill.SkillAbilityConfiguration;

public interface IMorphSkill<T extends ISkillOption>
{
    /**
     * 执行伪装的主动技能
     * @param player 玩家
     * @param state {@link DisguiseState}
     * @param configuration 此技能的整体配置，包括ID、冷却等
     * @param option 此技能的详细设置
     * @return 执行后的冷却长度
     */
    public int executeSkill(Player player, DisguiseState state, SkillAbilityConfiguration configuration, T option);

    /**
     * Called when this skill gets equipped
     * @implNote We don't suggest sending data to the client in this method, do that in {@link IMorphSkill#applyToClient(DisguiseState)} instead
     * @param state {@link DisguiseState}
     */
    public default void onInitialEquip(DisguiseState state)
    {
    }

    /**
     * Apply data to the client
     * @param state {@link DisguiseState}
     */
    public default void applyToClient(DisguiseState state)
    {
    }

    /**
     * Called when this skill gets de-equipped
     * @param state {@link DisguiseState}
     */
    public default void onDeEquip(DisguiseState state)
    {
    }

    /**
     * 内部轮子
     */
    @ApiStatus.Internal
    public default int executeSkillGeneric(Player player, DisguiseState state, SkillAbilityConfiguration config, ISkillOption option)
    {
        T castedOption;

        try
        {
            castedOption = (T) option;
        }
        catch (ClassCastException e)
        {
            player.sendMessage(MessageUtils.prefixes(player, SkillStrings.exceptionOccurredString()));
            return 20;
        }

        return executeSkill(player, state, config, castedOption);
    }

    /**
     * 获取要应用的技能ID
     * @return 技能ID
     */
    @NotNull
    public NamespacedKey getIdentifier();

    /**
     * 获取和此技能对应的{@link ISkillOption}实例
     *
     * @return {@link ISkillOption}
     */
    public T getOptionInstance();
}
