package xyz.nifeather.morph.providers.disguise;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.RevealingHandler;
import xyz.nifeather.morph.abilities.AbilityManager;
import xyz.nifeather.morph.backends.DisguiseBackend;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphStrings;
import xyz.nifeather.morph.misc.DisguiseState;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.network.server.ServerSetEquipCommand;
import xyz.nifeather.morph.skills.MorphSkillHandler;
import xyz.nifeather.morph.skills.SkillType;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.MessageStore;

import java.util.List;

/**
 * 提供一个默认的DisguiseProvider
 * 包括自动设置Bossbar、技能等
 */
public abstract class DefaultDisguiseProvider extends DisguiseProvider
{
    @Resolved
    private MorphSkillHandler skillHandler;

    @Resolved
    private AbilityManager abilityHandler;

    @Resolved
    private MorphClientHandler clientHandler;

    @Resolved
    private MessageStore<?> messageStore;

    @Resolved
    private RevealingHandler revealingHandler;

    @Override
    public @NotNull DisguiseBackend<?, ?> getPreferredBackend()
    {
        return getMorphManager().getDefaultBackend();
    }

    private record MessageConfiguration(
            short statusBit,
            Component display,
            String locale
    )
    {
        public static final MessageConfiguration DEFAULT = new MessageConfiguration
                (
                        (short) -1,
                        MiniMessage.miniMessage().deserialize("<yellow>missingno"),
                        "missingno"
                );

        public MessageConfiguration withBit(short bit)
        {
            return new MessageConfiguration(bit, display, locale);
        }

        public MessageConfiguration withDisplay(Component display)
        {
            return new MessageConfiguration(statusBit, display, locale);
        }

        public MessageConfiguration withLocale(String newLocale)
        {
            return new MessageConfiguration(statusBit, display, newLocale);
        }
    }

    @Override
    public boolean updateDisguise(Player player, DisguiseState state)
    {
        var disguise = state.getDisguiseWrapper();
        var option = clientHandler.getPlayerOption(player, true);

        var haveSkill = state.haveSkill();

        if (option.displayDisguiseOnHUD && plugin.getCurrentTick() % (haveSkill ? 2 : 5) == 0)
        {
            var locale = MessageUtils.getLocale(player);

            short bit = 0;

            if (haveSkill)
                bit |= 1;

            if (state.getSkillCooldown() <= 0)
                bit |= 2;
            else
                bit |= 4;

            var revLevel = revealingHandler.getRevealingLevel(player);
            switch (revLevel)
            {
                case SAFE -> bit |= 8;
                case SUSPECT -> bit |= 16;
                case REVEALED -> bit |= 32;
            }

            var msgConfig = state.getSessionData("MESSAGE_CONFIG", MessageConfiguration.class);
            if (msgConfig == null) msgConfig = MessageConfiguration.DEFAULT;

            short stateBit = msgConfig.statusBit();

            if (stateBit != bit || !msgConfig.locale.equals(locale))
            {
                //更新actionbar信息
                var msg = haveSkill
                        ? (state.getSkillCooldown() <= 0
                            ? MorphStrings.disguisingWithSkillAvaliableString()
                            : MorphStrings.disguisingWithSkillPreparingString())
                        : MorphStrings.disguisingAsString();

                var disguiseRevealed = revLevel == RevealingHandler.RevealingLevel.REVEALED || revLevel == RevealingHandler.RevealingLevel.SUSPECT;
                var display = disguiseRevealed
                        ? Component.empty()
                                .append(state.getPlayerDisplay())
                                .append((revLevel == RevealingHandler.RevealingLevel.REVEALED ? MorphStrings.revealed() : MorphStrings.partialRevealed()).toComponent(locale))
                        : state.getPlayerDisplay();

                msgConfig = msgConfig
                        .withDisplay(msg.resolve("what", display).toComponent(locale, messageStore))
                        .withBit(bit)
                        .withLocale(locale);

                state.setSessionData("MESSAGE_CONFIG", msgConfig);
            }

            player.sendActionBar(msgConfig.display);
        }

        try
        {
            disguise.update(state, player);
        }
        catch (Throwable t)
        {
            logger.error("Error occurred while updating disguise!");
            t.printStackTrace();

            return false;
        }

        return true;
    }

    @Override
    @NotNull
    public List<AbstractS2CCommand<?>> getInitialSyncCommands(DisguiseState state)
    {
        //logger.info("SID: " + state.getSkillLookupIdentifier() + " :: DID: " + state.getDisguiseIdentifier());
        if (skillHandler.hasSpeficSkill(state.skillLookupIdentifier(), SkillType.INVENTORY))
        {
            var eqiupment = state.getDisguisedItems();

            var list = new ObjectArrayList<AbstractS2CCommand<?>>();

            this.addIfPresents(eqiupment, list, EquipmentSlot.HAND);
            this.addIfPresents(eqiupment, list, EquipmentSlot.OFF_HAND);

            this.addIfPresents(eqiupment, list, EquipmentSlot.HEAD);
            this.addIfPresents(eqiupment, list, EquipmentSlot.CHEST);
            this.addIfPresents(eqiupment, list, EquipmentSlot.LEGS);
            this.addIfPresents(eqiupment, list, EquipmentSlot.FEET);

            return list;
        }

        return List.of();
    }

    private void addIfPresents(EntityEquipment equipment, ObjectArrayList<AbstractS2CCommand<?>> list, EquipmentSlot slot)
    {
        var item = equipment.getItem(slot);

        if (item.getType() != Material.AIR)
            list.add(new ServerSetEquipCommand(item, slot));
    }

    @Override
    public void postConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
        //被动技能
        var abilities = abilityHandler.getAbilitiesFor(state.skillLookupIdentifier());
        state.getAbilityUpdater().setAbilities(abilities);

        var abilityOptions = abilityHandler.getOptionsFor(state.skillLookupIdentifier());
        abilityOptions.forEach((id, config) -> state.getAbilityUpdater().setAbilityConfig(id.asString(), config));

        var skillEntry = skillHandler.getSkillEntry(state.skillLookupIdentifier());
        if (skillEntry != null)
            state.setSkill(skillEntry.value(), skillEntry.key());
    }
}
