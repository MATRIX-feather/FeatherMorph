package xyz.nifeather.morph.storage.skill;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xyz.nifeather.morph.abilities.impl.AttributeModifyingAbility;
import xyz.nifeather.morph.abilities.options.AttributeModifyOption;
import xyz.nifeather.morph.skills.DefaultConfigGenerator;
import xyz.nifeather.morph.storage.DirectoryJsonBasedStorage;
import xyz.nifeather.morph.storage.MorphJsonBasedStorage;

import java.io.File;
import java.nio.charset.StandardCharsets;

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
    }

    private static final int TARGET_PACKAGE_VERSION = PackageVersions.ATTRIBUTE_NAME_CHANGED;

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

        if (currentVersion < PackageVersions.ATTRIBUTE_NAME_CHANGED)
            migrate_attribute();

        setPackageVersion(TARGET_PACKAGE_VERSION);
    }

    private void migrate_attribute()
    {
        logger.info("Migrating attribute names...");
        var files = directoryStorage.getFiles(".*\\.json$");

        var abilityInstance = new AttributeModifyingAbility();

        for (File file : files)
        {
            var config = this.loadFrom(file);
            if (config == null)
            {
                logger.warn("Can't load SkillAbilityConfiguration from '%s', see errors above.".formatted(file.toString()));
                continue;
            }

            var targetOption = config.getAbilityOptions(abilityInstance);

            if (targetOption == null) continue;

            var key = getKeyFromFile(file);
            config.legacy_MobID = key;
            logger.info("Migrating " + key);

            for (AttributeModifyOption.AttributeInfo attributeInfo : targetOption.modifiers)
                attributeInfo.attributeName = attributeInfo.attributeName.replace("generic.", "");

            config.setOption(abilityInstance.getIdentifier().asString(), targetOption);

            this.save(config);
        }

        logger.info("Done.");
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
        public static final int ATTRIBUTE_NAME_CHANGED = 2;
    }
}
