package xyz.nifeather.morph.misc.integrations.placeholderapi;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Managers.DependencyManager;
import xyz.nifeather.morph.misc.integrations.placeholderapi.builtin.AvaliableDisguisesProvider;
import xyz.nifeather.morph.misc.integrations.placeholderapi.builtin.StateNameProvider;

import java.util.List;

public class PlaceholderIntegration extends PlaceholderExpansion
{
    private static final List<IPlaceholderProvider> providers = new ObjectArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(PlaceholderIntegration.class);

    public PlaceholderIntegration(DependencyManager depManager)
    {
        addPlaceholders(ObjectArrayList.of(
                new StateNameProvider(),
                new AvaliableDisguisesProvider()
        ));
    }

    @Override
    public @NotNull String getIdentifier()
    {
        return "feathermorph";
    }

    @Override
    public @NotNull String getAuthor()
    {
        return "MATRIX-feather";
    }

    @Override
    public @NotNull String getVersion()
    {
        return "1.3.3";
    }

    private void addPlaceholders(List<IPlaceholderProvider> providerList)
    {
        providerList.forEach(this::addPlaceholderProvider);
    }

    /**
     * 添加一个Placeholder提供器
     * @param provider Placeholder提供器
     * @return 操作是否成功（是否已经注册过一个相同ID和匹配模式的提供器）
     */
    public boolean addPlaceholderProvider(IPlaceholderProvider provider)
    {
        if (providers.stream().anyMatch(p -> providerEquals(p, provider)))
            return false;

        providers.addFirst(provider);
        return true;
    }

    private boolean providerEquals(IPlaceholderProvider source, IPlaceholderProvider target)
    {
        if (source == null || target == null) return false;

        return source.getPlaceholderIdentifier().equals(target.getPlaceholderIdentifier());
    }

    private static final String defaultString = "???";

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String param)
    {
        if (player == null) return defaultString;

        param = param.replaceFirst(getIdentifier() + "_", "");
        var paramSpilt = param.split("_", 2);

        if (paramSpilt.length != 2)
            return null;

        var provider = providers.stream()
                .filter(p -> p.getPlaceholderIdentifier().equalsIgnoreCase(paramSpilt[0]))
                .findFirst().orElse(null);

        if (provider == null) return defaultString;

        return provider.resolvePlaceholder(player, paramSpilt[1]);
    }
}
