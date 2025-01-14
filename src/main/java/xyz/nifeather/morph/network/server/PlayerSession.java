package xyz.nifeather.morph.network.server;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.network.ConnectionState;
import xiamomc.morph.network.InitializeState;
import xiamomc.morph.network.PlayerOptions;

public class PlayerSession
{
    public PlayerSession(Player bindingPlayer, boolean isLegacyPacketBuf)
    {
        options = new PlayerOptions<>(bindingPlayer);
        this.isLegacyPacketBuf = isLegacyPacketBuf;
    }

    public final PlayerOptions<Player> options;
    public boolean isLegacyPacketBuf;

    @NotNull
    public InitializeState initializeState = InitializeState.NOT_CONNECTED;

    @NotNull
    public ConnectionState connectionState = ConnectionState.NOT_CONNECTED;

    public static final class SessionBuilder
    {
        public static SessionBuilder builder(Player player)
        {
            return new SessionBuilder(player);
        }

        private final Player bindingPlayer;
        private boolean isLegacy;

        public SessionBuilder(Player bindingPlayer)
        {
            this.bindingPlayer = bindingPlayer;
        }

        public SessionBuilder isLegacy(boolean isLegacy)
        {
            this.isLegacy = isLegacy;
            return this;
        }

        public PlayerSession build()
        {
            return new PlayerSession(
                    bindingPlayer,
                    isLegacy
            );
        }
    }
}
