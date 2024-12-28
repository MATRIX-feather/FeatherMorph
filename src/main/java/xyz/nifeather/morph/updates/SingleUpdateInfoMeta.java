package xyz.nifeather.morph.updates;

import com.google.gson.annotations.SerializedName;
import xyz.nifeather.morph.FeatherMorphMain;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class SingleUpdateInfoMeta
{
    @SerializedName("id")
    public String id;

    @SerializedName("project_id")
    public String projectId;

    @SerializedName("author_id")
    public String authorId;

    @SerializedName("featured")
    public boolean featured;

    @SerializedName("name")
    public String displayName;

    @SerializedName("version_number")
    public String versionNumber;

    @SerializedName("changelog")
    public String changelog;

    @SerializedName("changelog_url")
    public String changelogUrl;

    @SerializedName("date_published")
    public String datePublished;

    @SerializedName("downloads")
    public double downloads;

    @SerializedName("version_type")
    public String versionType;

    @SerializedName("status")
    public String status;

    @SerializedName("requested_status")
    public String requestedStatus;

    @SerializedName("files")
    public List<Object> files;

    @SerializedName("dependencies")
    public List<Object> dependencies;

    @SerializedName("game_versions")
    public List<String> supportedVersions;

    @SerializedName("loaders")
    public List<String> supportedLoaders;

    public static SingleUpdateInfoMeta fromMap(Map<?, ?> map)
    {
        var instance = new SingleUpdateInfoMeta();
        var fields = instance.getClass().getDeclaredFields();

        for (Field field : fields)
        {
            var serializedName = field.isAnnotationPresent(SerializedName.class)
                                ? field.getAnnotation(SerializedName.class).value()
                                : field.getName();

            var mapValue = map.getOrDefault(serializedName, null);

            //MorphPlugin.getInstance().getSLF4JLogger()
            //        .info("Name: '%s', ST: '%s', VT: '%s'"
            //                .formatted(field.getName(), field.getType(), (mapValue == null ? "nil" : mapValue.getClass())));

            try
            {
                field.set(instance, mapValue);
            }
            catch (Throwable t)
            {
                FeatherMorphMain.getInstance().getSLF4JLogger().warn("Unable to set '%s' to '%s': %s".formatted(field.getName(), mapValue, t.getMessage()));
                //t.printStackTrace();
            }
        }

        return instance;
    }
}
