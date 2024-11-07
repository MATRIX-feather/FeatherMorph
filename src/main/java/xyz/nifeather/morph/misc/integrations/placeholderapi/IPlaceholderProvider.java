package xyz.nifeather.morph.misc.integrations.placeholderapi;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPlaceholderProvider
{
    /**
     * 获取此Placeholder提供器的ID
     *
     * @return 提供器ID
     */
    @NotNull
    public String getPlaceholderIdentifier();

    /**
     * 解析Placeholder
     * @param player 玩家
     * @param params 参数
     * @return 内容，为null则会自动返回"???"
     */
    @Nullable
    public String resolvePlaceholder(Player player, String params);
}
