package xyz.nifeather.morph.config;

import org.apache.commons.lang3.RandomStringUtils;
import xiamomc.pluginbase.Configuration.ConfigNode;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.events.InteractionMirrorProcessor;
import xyz.nifeather.morph.utilities.NbtUtils;

import java.util.ArrayList;

public enum ConfigOption
{
    PLUGIN_PREFIX(ConfigNode.create().append("message_pattern"), "<color:#dddddd>≡ FM » <message>"),

    ALLOW_HEAD_MORPH(ConfigNode.create().append("allowHeadMorph"), true),
    ALLOW_CHAT_OVERRIDE(ConfigNode.create().append("chatOverride").append("allowOverride"), false),
    CHAT_OVERRIDE_USE_CUSTOM_RENDERER(ConfigNode.create().append("chatOverride").append("UseCustomRenderer"), true),
    CHAT_OVERRIDE_DEFAULT_PATTERN(ConfigNode.create().append("chatOverride").append("default_pattern"), "<<who>> <message>"),

    SKILL_COOLDOWN_ON_DAMAGE(ConfigNode.create().append("cooldown_on_damage"), 15),

    @Deprecated(forRemoval = true)
    ACTION_ITEM(ConfigNode.create().append("action_item"), "", true),

    @Deprecated(forRemoval = true, since = "1.3.0")
    SKILL_ITEM(ConfigNode.create().append("skill_item"), "", true),

    //@Deprecated(forRemoval = true)
    //SKILL_ITEM_USE_COMPONENT(ConfigNode.create().append("skill_item_use_component_detection"), true, true),

    ARMORSTAND_SHOW_ARMS(ConfigNode.create().append("armorstand_show_arms"), true),

    MODIFY_BOUNDING_BOX(boundingBoxNode().append("modify_boxes"), false),
    CHECK_AVAILABLE_SPACE(boundingBoxNode().append("check_space"), true),

    @Deprecated(forRemoval = true)
    MODIFY_BOUNDING_BOX_LEGACY(ConfigNode.create().append("modify_bounding_boxes"), false, true),

    UNMORPH_ON_DEATH(ConfigNode.create().append("unmorph_on_death"), true),

    ALLOW_CLIENT(clientNode().append("allow_client_mod"), true),
    //FORCE_CLIENT(clientNode().append("force_client"), false),
    FORCE_TARGET_VERSION(clientNode().append("force_version"), false),

    LOG_INCOMING_PACKETS(clientNode().append("log_incoming_packets"), false),
    LOG_OUTGOING_PACKETS(clientNode().append("log_outgoing_packets"), false),

    USE_CLIENT_RENDERER(clientNode().append("client_renderer"), true),

    MIRROR_CONTROL_DISTANCE(interactionMirrorNode().append("normalDistance"), -1),
    MIRROR_IGNORE_DISGUISED(interactionMirrorNode().append("ignore_disguised"), true),
    MIRROR_DESTROY_TIMEOUT(interactionMirrorNode().append("destroy_timeout"), 40),

    MIRROR_BEHAVIOR_DO_SIMULATION(interactionMirrorBehaviorNode().append("simulate_interactions"), false),
    MIRROR_BEHAVIOR_SNEAK(interactionMirrorBehaviorNode().append("sneak"), false),
    MIRROR_BEHAVIOR_SWAP_HAND(interactionMirrorBehaviorNode().append("swap_hands"), false),
    MIRROR_BEHAVIOR_DROP(interactionMirrorBehaviorNode().append("allow_drop"), false),
    MIRROR_BEHAVIOR_HOTBAR(interactionMirrorBehaviorNode().append("hotbar"), false),

    MIRROR_LOG_OPERATION(interactionMirrorNode().append("log_operations"), false),
    MIRROR_LOG_CLEANUP_DATE(interactionMirrorNode().append("log_cleanup"), 7),

    MIRROR_SELECTION_MODE(interactionMirrorNode().append("selection_mode"), InteractionMirrorProcessor.InteractionMirrorSelectionMode.BY_NAME),

    BANNED_DISGUISES(ConfigNode.create().append("bannedDisguises"), new ArrayList<String>()),

    PIGLIN_BRUTE_IGNORE_DISGUISES(ConfigNode.create().append("piglin_brute_ignore_disguises"), true),
    HEALTH_SCALE(healthScaleNode().append("enabled"), true),
    HEALTH_SCALE_MAX_HEALTH(healthScaleNode().append("max_health"), 60),

    DISPLAY_BOSSBAR(bossbarNode().append("enabled"), true),

    FLYABILITY_EXHAUSTION_BASE(flyAbilityNode().append("exhaustion_base"), 200d),
    FLYABILITY_IDLE_CONSUME(flyAbilityNode().append("idle_consumption"), true),
    FLYABILITY_DISALLOW_FLY_IN_WATER(flyAbilityNode().append("disallow_in_water"), new ArrayList<String>()),
    FLYABILITY_DISALLOW_FLY_IN_LAVA(flyAbilityNode().append("disallow_in_lava"), new ArrayList<String>()),

    @Deprecated(since = "1.2.2", forRemoval = true)
    FLYABILITY_NO_LIQUID(flyAbilityNode().append("no_fly_in_liquid"), true, true),

    LANGUAGE_CODE(languageNode().append("code"), "en_us"),
    SINGLE_LANGUAGE(languageNode().append("single_language"), true),

    BLACKLIST_PATTERNS(nbtBlacklistNode().append("patterns"), new ArrayList<>(NbtUtils.defaultBlacklistedPatterns)),

    BLACKLIST_TAGS(nbtBlacklistNode().append("names"), new ArrayList<>(NbtUtils.defaultBlacklistedTags)),

    AMBIENT_FREQUENCY(ambientSoundNode().append("frequency"), 1D),

    DEBUG_OUTPUT(ConfigNode.create().append("debug_output"), false, true),

    REVEALING(ConfigNode.create().append("revealing"), false),

    CHECK_UPDATE(ConfigNode.create().append("check_update"), true),

    ALLOW_ACQUIRE_MORPHS(ConfigNode.create().append("allow_acquire_morphs"), true),

    FORCED_DISGUISE(ConfigNode.create().append("forced_disguise"), MorphManager.forcedDisguiseNoneId),

    ALLOW_FLIGHT(ConfigNode.create().append("allow_flight"), true),

    NOFLY_WORLDS(ConfigNode.create().append("nofly_worlds"), new ArrayList<String>()),

    UUID_RANDOM_BASE(ConfigNode.create().append("uuid_random_base"), RandomStringUtils.randomAlphabetic(8)),

    ENABLE_MULTIINSTANCE(multiInstanceNode().append("enabled"), false),

    MASTER_ADDRESS(multiInstanceNode().append("master_address"), "0.0.0.0:39210"),

    IS_MASTER(multiInstanceNode().append("is_master_service"), false),

    MASTER_SECRET(multiInstanceNode().append("secret"), RandomStringUtils.randomAlphabetic(12)),

    DO_CHECK_ABILITY_PERMISSIONS(ConfigNode.create().append("check_ability_permissions"), true),

    DO_MODIFY_AI(ConfigNode.create().append("modify_ai"), true),

    GUI_PATTERN(ConfigNode.create().append("gui_pattern"), new ArrayList<String>()),

    //ANIM_SELECT_PATTERN(ConfigNode.create().append("anim_select_pattern"), new ArrayList<String>()),

    HIDE_DISGUISED_PLAYERS_IN_TAB(ConfigNode.create().append("hide_disguised_players_in_tab"), false),

    // SRR -> ServerRenderer
    SR_SHOW_PLAYER_DISGUISES_IN_TAB(serverRendererNode().append("show_player_disguises_in_tab"), false),

    TOWNY_ALLOW_FLY_IN_WILDERNESS(townyNode().append("allow_fly_in_wilderness"), false),

    DISGUISE_DISABLED_WORLDS(worldOptionNode().append("disabled_worlds"), new ArrayList<String>()),

    VERSION(ConfigNode.create().append("version"), 0);

    public final ConfigNode node;
    public final Object defaultValue;
    public final boolean excludeFromInit;

    private ConfigOption(ConfigNode node, Object defaultValue, boolean excludeFromInit)
    {
        this.node = node;
        this.defaultValue = defaultValue;
        this.excludeFromInit = excludeFromInit;
    }

    private ConfigOption(ConfigNode node, Object defaultValue)
    {
        this(node, defaultValue, false);
    }

    @Override
    public String toString()
    {
        return node.toString();
    }

    public static ConfigNode interactionMirrorNode()
    {
        return ConfigNode.create().append("interactionMirror");
    }

    private static ConfigNode nbtBlacklistNode()
    {
        return ConfigNode.create().append("nbt_blacklist");
    }

    private static ConfigNode interactionMirrorBehaviorNode()
    {
        return interactionMirrorNode().append("behaviors");
    }

    private static ConfigNode bossbarNode()
    {
        return ConfigNode.create().append("bossbar");
    }

    private static ConfigNode languageNode()
    {
        return ConfigNode.create().append("language");
    }

    private static ConfigNode clientNode()
    {
        return ConfigNode.create().append("client");
    }
    private static ConfigNode healthScaleNode()
    {
        return ConfigNode.create().append("health_scale");
    }
    private static ConfigNode ambientSoundNode()
    {
        return ConfigNode.create().append("ambient_sounds");
    }
    private static ConfigNode flyAbilityNode()
    {
        return ConfigNode.create().append("flying");
    }
    private static ConfigNode boundingBoxNode()
    {
        return ConfigNode.create().append("bounding_boxes");
    }
    private static ConfigNode multiInstanceNode()
    {
        return ConfigNode.create().append("multi_instance");
    }
    private static ConfigNode serverRendererNode()
    {
        return ConfigNode.create().append("server_renderer");
    }
    private static ConfigNode integrationNode()
    {
        return ConfigNode.create().append("integrations");
    }
    public static ConfigNode townyNode()
    {
        return integrationNode().append("towny");
    }
    private static ConfigNode worldOptionNode()
    {
        return ConfigNode.create().append("world_option");
    }
}
