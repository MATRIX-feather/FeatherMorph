package xyz.nifeather.morph.network.server;

import xyz.nifeather.morph.MorphPlugin;

public class MessageChannel
{
    private static final String nameSpace = MorphPlugin.getMorphNameSpace();

    public static final String initializeChannel = nameSpace + ":init";
    public static final String versionChannel = nameSpace + ":version_v2";
    public static final String commandChannel = nameSpace + ":commands_v2";

    public static final String versionChannelLegacy = nameSpace + ":version";
    public static final String commandChannelLegacy = nameSpace + ":commands";
}
