package xyz.nifeather.morph.misc.integrations.placeholderapi.builtin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.misc.integrations.placeholderapi.IPlaceholderProvider;

public class StateNameProvider extends MorphPluginObject implements IPlaceholderProvider
{
    @Override
    public @NotNull String getPlaceholderIdentifier()
    {
        return "state";
    }

    @Resolved
    private MorphManager morphs;

    @Override
    public @Nullable String resolvePlaceholder(Player player, String param)
    {
        var state = morphs.getDisguiseStateFor(player);

        switch (param)
        {
            case "id" ->
            {
                return state != null
                        ? state.getDisguiseIdentifier()
                        : "???";
            }

            case "name" ->
            {
                return state != null
                    ? PlainTextComponentSerializer.plainText().serialize(state.getServerDisplay())
                    : "???";
            }

            case "status" ->
            {
                return state != null ? "true" : "false";
            }
        }

        if (param.startsWith("provider_is"))
        {
            var spilt = param.split(":", 2);
            if (spilt.length < 2) return "false";

            var namespace = spilt[1];
            return state != null
                    ? namespace.equals(state.getProvider().getNameSpace()) ? "true" : "false"
                    : "false";
        }

        if (param.startsWith("id_is"))
        {
            var spilt = param.split(":", 2);
            if (spilt.length < 2) return "false";

            return state != null
                    ? state.getDisguiseIdentifier().equals(spilt[1]) ? "true" : "false"
                    : "false";
        }

        return "not_enough_param";
    }
}
