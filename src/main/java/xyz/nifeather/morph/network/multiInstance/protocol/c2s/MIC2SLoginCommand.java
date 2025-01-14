package xyz.nifeather.morph.network.multiInstance.protocol.c2s;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.network.multiInstance.protocol.IClientHandler;
import xyz.nifeather.morph.network.multiInstance.protocol.ProtocolLevel;

public class MIC2SLoginCommand extends MIC2SCommand<String>
{
    private MIC2SLoginCommand(int ver, String secret)
    {
        super("login", "" + ver, secret);
    }

    public MIC2SLoginCommand(@NotNull ProtocolLevel protocolLevel, @NotNull String secret)
    {
        super("login", protocolLevel.versionString(), secret);
    }

    public int getVersion()
    {
        var argRaw = getArgumentAt(0, "0");
        int version = 0;

        try
        {
            version = Integer.parseInt(argRaw);
        }
        catch (Throwable t)
        {
            logger.warn("Can't get version from arg '%s': %s".formatted(argRaw, t.getMessage()));
        }

        return version;
    }

    @Nullable
    public String getSecret()
    {
        return getArgumentAt(1);
    }

    @Override
    public void onCommand(IClientHandler handler)
    {
        handler.onLoginCommand(this);
    }

    public static MIC2SLoginCommand from(String arg)
    {
        var args = arg.split(" ", 2);
        int ver = 0;

        try
        {
            ver = Integer.parseInt(args[0]);
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();
            logger.warn("Error occurred processing arguments: " + t.getMessage());
        }

        return new MIC2SLoginCommand(ver, args.length == 2 ? args[1] : "~NULL");
    }
}
