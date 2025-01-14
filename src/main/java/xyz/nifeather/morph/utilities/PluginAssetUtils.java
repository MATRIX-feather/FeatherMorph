package xyz.nifeather.morph.utilities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.FeatherMorphMain;

import java.nio.charset.StandardCharsets;

public class PluginAssetUtils
{
    /**
     * 通过给定的路径获取资源内容
     * @param path 目标路径
     * @return 原始数据，返回null则未找到或出现异常
     */
    public static byte @Nullable [] getFileBytes(String path)
    {
        var plugin = FeatherMorphMain.getPlugin(FeatherMorphMain.class);

        var stream = plugin.getResource(path);

        if (stream == null) return null;

        try
        {
            return stream.readAllBytes();
        }
        catch (Throwable ignored)
        {
        }

        return null;
    }

    /**
     * 以字符串的形式获取资源内容
     * @param path 资源路径
     * @return 文件资源内容，返回空则未找到或出现异常
     */
    public static String getFileStrings(String path)
    {
        var bytes = getFileBytes(path);

        if (bytes == null) return "";

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static final String assetPath = "assets/feathermorph";

    public static String langPath(@NotNull String languageCode)
    {
        return assetPath + "/lang/" + languageCode + ".json";
    }
}
