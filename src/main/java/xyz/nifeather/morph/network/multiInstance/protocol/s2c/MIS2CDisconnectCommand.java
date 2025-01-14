package xyz.nifeather.morph.network.multiInstance.protocol.s2c;

import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.network.multiInstance.protocol.IMasterHandler;

public class MIS2CDisconnectCommand extends MIS2CCommand<String>
{
    public MIS2CDisconnectCommand(int reasonCode)
    {
        this(reasonCode, "<No details>");
    }

    public MIS2CDisconnectCommand(int reasonCode, String detail)
    {
        super("deny", "" + reasonCode, detail);
    }

    @Override
    public void onCommand(IMasterHandler handler)
    {
        handler.onDisconnectCommand(this);
    }

    public int getReasonCode()
    {
        var str = getArgumentAt(0, "0");

        try
        {
            return Integer.parseInt(str);
        }
        catch (Throwable t)
        {
            logger.warn("Unable to parse integer for deny command: " + t.getMessage());
        }

        return -2;
    }

    public String getDetails()
    {
        return getArgumentAt(1, "<No details>");
    }

    public static MIS2CDisconnectCommand from(String text)
    {
        var args = text.split(" ", 2);
        int reasonCode = -2;

        try
        {
            reasonCode = Integer.parseInt(args[0]);
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();

            logger.warn("Can't parse disconnect reason code from the server command");
        }

        if (args.length == 2)
            return new MIS2CDisconnectCommand(reasonCode, args[1]);
        else
            return new MIS2CDisconnectCommand(reasonCode);
    }
}
