package xyz.nifeather.morph.messages;

import xiamomc.pluginbase.Messages.FormattableMessage;

public class MorphStrings extends AbstractMorphStrings
{
    public static FormattableMessage morphSuccessString()
    {
        return getFormattable(getKey("morph_success"),
                "成功伪装为<what>！");
    }

    public static FormattableMessage morphVisibleAfterStandup()
    {
        return getFormattable(getKey("morph_visible_after_standup"),
                "伪装将在起身后对自己可见");
    }

    public static FormattableMessage disguisingAsString()
    {
        return getFormattable(getKey("disguising_as"),
                "正伪装为<what>");
    }

    public static FormattableMessage morphUnlockedString()
    {
        return getFormattable(getKey("morph_unlocked"),
                "<color:green>✔ 已解锁<what>的伪装！");
    }

    public static FormattableMessage morphLockedString()
    {
        return getFormattable(getKey("morph_locked"),
                "<color:red>❌ 已失去<what>的伪装！");
    }

    public static FormattableMessage disguisingWithSkillPreparingString()
    {
        return getFormattable(getKey("disguising_as_skill_preparing"),
                "<color:#eeb565>正伪装为<what>");
    }

    public static FormattableMessage disguisingWithSkillAvaliableString()
    {
        return getFormattable(getKey("disguising_as_skill_avaliable"),
                "<color:#8fe98d>正伪装为<what>");
    }

    public static FormattableMessage unMorphSuccessString()
    {
        return getFormattable(getKey("unmorph_success"),
                "已取消伪装");
    }


    public static FormattableMessage invalidIdentityString()
    {
        return getFormattable(getKey("invalid_id"),
                "<color:red>此ID不能用于伪装");
    }

    public static FormattableMessage noSuchLocalDisguiseString()
    {
        return getFormattable(getKey("no_such_local_disguise"),
                "<color:red>未找到和此ID匹配的本地伪装");
    }

    public static FormattableMessage parseErrorString()
    {
        return getFormattable(getKey("parse_error"),
                "<color:red>未能解析<id>");
    }

    public static FormattableMessage morphNotOwnedString()
    {
        return getFormattable(getKey("morph_not_owned"),
                "<color:red>你尚未拥有此伪装");
    }

    public static FormattableMessage disguiseCoolingDownString()
    {
        return getFormattable(getKey("cooling_down"),
                "<color:red>请等一会再进行伪装");
    }

    public static FormattableMessage disguiseNotDefinedString()
    {
        return getFormattable(getKey("disguise_not_defined"),
                "<color:red>未指定伪装");
    }


    public static FormattableMessage headDisguiseDisabledString()
    {
        return getFormattable(getKey("head_morph_disabled"),
                "<color:red>此功能已禁用");
    }

    public static FormattableMessage invalidSkinString()
    {
        return getFormattable(getKey("invalid_skin"),
                "<color:red>无效的皮肤");
    }

    public static FormattableMessage resetString()
    {
        return getFormattable(getKey("state.reset"),
                "相关功能已禁用，正在取消您的伪装...");
    }

    public static FormattableMessage recoverString()
    {
        return getFormattable(getKey("state.recover_online"),
                "相关功能已重置，正在恢复您的伪装...");
    }

    public static FormattableMessage stateRecoverReasonString()
    {
        return getFormattable(getKey("state.recover_reason"),
                "自上次离线后相关功能已被重置");
    }

    public static FormattableMessage recoveringStateString()
    {
        return getFormattable(getKey("state.recovering"),
                "我们正在恢复您的伪装");
    }

    public static FormattableMessage recoveringStateLimitedString()
    {
        return getFormattable(getKey("state.recovering_limited"),
                "我们正从有限副本中恢复您的伪装");
    }

    public static FormattableMessage recoveringStateLimitedHintString()
    {
        return getFormattable(getKey("state.recovering_limited_hint"),
                "<italic>恢复后的伪装可能和之前的不一样");
    }

    public static FormattableMessage recoveringFailedString()
    {
        return getFormattable(getKey("state.recovering_failed"),
                "<color:red>我们无法恢复您的伪装 :(");
    }


    public static FormattableMessage selfVisibleOnString()
    {
        return getFormattable(getKey("self_visible_on"),
                "<color:green>已切换自身可见");
    }

    public static FormattableMessage selfVisibleOffString()
    {
        return getFormattable(getKey("self_visible_off"),
                "<color:red>已切换自身可见");
    }

    public static FormattableMessage disguiseBannedOrNotSupportedString()
    {
        return getFormattable(getKey("disguise_banned_or_not_supported"),
                "<color:red>服务器不支持或已禁用此伪装");
    }

    public static FormattableMessage errorWhileDisguising()
    {
        return getFormattable(getKey("error_while_disguising"),
                "<color:red>伪装时出现问题");
    }

    public static FormattableMessage errorWhileUpdatingDisguise()
    {
        return getFormattable(getKey("error_while_updating_disguise"),
                "<color:red>更新伪装状态时遇到了意外，正在取消伪装");
    }

    public static FormattableMessage clientVersionMismatchString()
    {
        return getFormattable(getKey("client_version_mismatch"),
                "<color:red>客户端模组版本不匹配，将不会启用增强功能！");
    }

    public static FormattableMessage clientVersionMismatchKickString()
    {
        return getFormattable(getKey("client_version_mismatch_kick"),
                "<color:red>客户端模组版本不匹配");
    }

    public static FormattableMessage unsupportedClientBehavior()
    {
        return getFormattable(getKey("unsupported_client_behavior"),
                "[Fallback] <color:red>检测到不受支持的客户端行为，将不会启用增强功能！");
    }

    public static FormattableMessage noEnoughSpaceString()
    {
        return getFormattable(getKey("no_enough_space"),
                "<color:red>空间太过狭小，无法进行伪装！");
    }

    public static FormattableMessage speedingDetectedString()
    {
        return getFormattable(getKey("speeding"),
                "检测到可能的超速驾驶，正在取消伪装...");
    }

    public static FormattableMessage operationCancelledString()
    {
        return getFormattable(getKey("operation_cancelled"),
                "<red>操作被其他来源取消");
    }

    public static FormattableMessage revealed()
    {
        return getFormattable(getKey("disguise_revealed"),
                "（伪装已暴露）");
    }

    public static FormattableMessage partialRevealed()
    {
        return getFormattable(getKey("partial_damaged"),
                "（部分暴露）");
    }

    private static String getKey(String key)
    {
        return "morph." + key;
    }
}
