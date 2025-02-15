package xyz.nifeather.morph.skills.options;

import com.google.gson.annotations.Expose;
import org.bukkit.entity.EntityType;
import xyz.nifeather.morph.utilities.EntityTypeUtils;

public class SummonConfiguration
{
    @Expose
    private String name;

    public String getName()
    {
        return name;
    }

    public EntityType getEntityType()
    {
        var type = EntityTypeUtils.fromString(name);

        return type == EntityType.UNKNOWN ? null : type;
    }
}
