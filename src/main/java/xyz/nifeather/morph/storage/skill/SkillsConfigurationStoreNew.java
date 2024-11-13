package xyz.nifeather.morph.storage.skill;

import org.apache.commons.io.FileUtils;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xyz.nifeather.morph.abilities.AbilityType;
import xyz.nifeather.morph.abilities.options.ReduceDamageOption;
import xyz.nifeather.morph.skills.DefaultConfigGenerator;
import xyz.nifeather.morph.storage.DirectoryJsonBasedStorage;
import xyz.nifeather.morph.storage.MorphJsonBasedStorage;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class SkillsConfigurationStoreNew extends DirectoryJsonBasedStorage<SkillAbilityConfiguration>
{
    public SkillsConfigurationStoreNew()
    {
        super("skills");
    }

    @Initializer
    private void load()
    {
        var packageVersion = this.getPackageVersion();

        if (packageVersion < TARGET_PACKAGE_VERSION)
            update(packageVersion);

        if (packageVersion > TARGET_PACKAGE_VERSION)
            logger.warn("The package version is newer than our implementation! Errors may occur!");
    }

    private static final int TARGET_PACKAGE_VERSION = PackageVersions.WITHER_SKELETON_CHANGES;

    private void update(int currentVersion)
    {
        if (currentVersion < PackageVersions.INITIAL)
        {
            var legacySkillFile = new File(this.plugin.getDataFolder(), "skills.json");

            if (legacySkillFile.exists())
                migrateFromLegacyStorage();
            else
                saveDefaultGeneratedConfigurations();
        }

        if (currentVersion < PackageVersions.WITHER_SKELETON_CHANGES)
        {
            migrateWitherSkeleton();
        }

        setPackageVersion(TARGET_PACKAGE_VERSION);
    }

    private void migrateFromLegacyStorage()
    {
        try
        {
            logger.info("Migrating from legacy skill configuration...");
            var storage = new LegacyReadonlyConfigurationStorage();

            storage.initializeStorage();

            var file = storage.file();

            var storing = storage.getStoring();
            if (storing == null)
            {
                logger.warn("Can't migrate from legacy skill configuration: Null storing object, is everything all right?");
                return;
            }

            storing.configurations.forEach(this::save);

            var success = file.renameTo(new File(file.getParent(), "skills.json.old"));

            if (!success)
                logger.info("Can't rename 'skills.json' to 'skills.json.old', but it's not a big deal, I guess...");

            logger.info("Done migrating legacy skill configuration!");
        }
        catch (Throwable t)
        {
            logger.warn("Can't migrate from legacy skill configuration: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void migrateWitherSkeleton()
    {
        logger.info("Migrating new Wither Skeleton configuration");

        var configuration = this.get(EntityType.WITHER_SKELETON.key().asString());
        if (configuration == null)
        {
            logger.info("No configuration present for minecraft:wither_skeleton, skipping...");
            return;
        }

        configuration.addAbilityIdentifier(AbilityType.HAS_FIRE_RESISTANCE)
                .addAbilityIdentifier(AbilityType.REDUCES_WITHER_DAMAGE)
                .appendOption(AbilityType.REDUCES_WITHER_DAMAGE,
                        new ReduceDamageOption(1, true));

        configuration.legacy_MobID = EntityType.WITHER_SKELETON.key().asString();
        this.save(configuration);

        logger.info("Done Migrating new Wither Skeleton configuration");
    }

    private void saveDefaultGeneratedConfigurations()
    {
        logger.info("Saving default generated skill configurations...");

        var generatedConfiguration = DefaultConfigGenerator.createInstance().generateConfiguration();
        generatedConfiguration.forEach((id, config) ->
        {
            config.legacy_MobID = id;
            this.save(config);
        });

        logger.info("Done saving default generated skill configurations!");
    }

    public void save(SkillAbilityConfiguration configuration)
    {
        var identifier = configuration.legacy_MobID;

        if (identifier == null)
        {
            logger.warn("Found a configuration from legacy store that doesn't have a mobId! Ignoring...");
            return;
        }

        var path = this.getPath(identifier) + ".json";

        var file = this.directoryStorage.getFile(path, true);
        if (file == null)
        {
            logger.warn("Cannot save disguise configuration for " + identifier);
            return;
        }

        String json = gson.toJson(configuration);
        try
        {
            FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
        }
        catch (Throwable t)
        {
            logger.error("Can't write content to file: " + t.getMessage());
        }
    }

    private static final SkillAbilityConfiguration defaultConfig = new SkillAbilityConfiguration();

    @Override
    protected SkillAbilityConfiguration getDefault()
    {
        return defaultConfig;
    }

    private static class LegacyReadonlyConfigurationStorage extends MorphJsonBasedStorage<SkillAbilityConfigurationContainer>
    {
        @Override
        protected @NotNull String getFileName()
        {
            return "skills.json";
        }

        public File file()
        {
            return this.configurationFile;
        }

        @Nullable
        public SkillAbilityConfigurationContainer getStoring()
        {
            return this.storingObject;
        }

        @Override
        protected @NotNull SkillAbilityConfigurationContainer createDefault()
        {
            return new SkillAbilityConfigurationContainer();
        }

        @Override
        protected @NotNull String getDisplayName()
        {
            return "Legacy skill configuration store";
        }
    }

    public static class PackageVersions
    {
        public static final int INITIAL = 1;
        public static final int WITHER_SKELETON_CHANGES = 2;
    }
}
