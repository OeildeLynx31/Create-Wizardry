package net.ttzplayz.create_wizardry.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.ttzplayz.create_wizardry.advancement.CWAdvancements;

public class CWLangProvider extends LanguageProvider {
    public CWLangProvider(PackOutput output, String locale) {
        super(output, CreateWizardry.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        CWAdvancements.provideLang(this::add);
        add("fluid_type.create_wizardry.lightning_type", "Liquid Lightning");
        add("fluid_type.create_wizardry.mana_type", "Mana");
        add("fluid_type.create_wizardry.fire_ale_type", "Fire Ale");
        add("fluid_type.create_wizardry.netherward_tincture_type", "Netherward Tincture");
        add("item.create_wizardry.arcane_sheet", "Arcane Sheet");
        add("block.create_wizardry.arcane_pipe", "Arcane Pipe");
        add("block.create_wizardry.smart_arcane_pipe", "Smart Arcane Pipe");
        add("block.create_wizardry.glass_arcane_pipe", "Glass Arcane Pipe");
        add("block.create_wizardry.encased_arcane_pipe", "Encased Arcane Pipe");
        add("block.create_wizardry.arcane_pump", "Arcane Pump");
        add("item.create_wizardry.crushed_mithril", "Crushed Raw Mithril");
        add("item.create_wizardry.mithril_nugget", "Mithril Nugget");
        add("item.create_wizardry.mana_bucket", "Bucket of Mana");
        add("item.create_wizardry.mana_bottle", "Bottle of Mana");
        add("item.create_wizardry.lightning_bucket", "Bucket o' Liquid Lightning");
        add("item.create_wizardry.blood_bucket", "Bucket o' Blood");
        add("item.create_wizardry.channeler", "Channeler");
        add("block.create_wizardry.channeler", "Channeler");
        add("jei.create_wizardry.liquid_lightning",
                "An extremely volatile fluid that is charged with electric energy. Can be obtained by channeling lightning bolts using the Channeler block or by extracting from charged creepers in a bottle.");
        add("jei.create_wizardry.mana",
                "The liquid essence of magic. It is an extremely volatile substance, rapidly leaking from pipes that are un-insulated with arcane material. Can be extracted from players, spells, and mobs with the Mana Siphon, and can also be crystallized into Arcane Essence by placing a crystalline block above a filled mana Siphon.");
        add("jei.create_wizardry.blood",
                "The lifeblood of all organisms. Can be obtained by compressing meat or boiling mobs.");
        add("item.create_wizardry.incomplete_blaze_caster", "Incomplete Blaze Caster");
        add("item.create_wizardry.casters_scone", "Caster's Scone");
        add("itemGroup.create_wizardry.main", "Create: Wizardry");
        add("block.create_wizardry.blaze_caster", "Blaze Caster");
        add("block.create_wizardry.arcane_casing", "Arcane Casing");
        add("block.create_wizardry.arcane_block", "Arcane Block");
        add("block.create_wizardry.mana_siphon", "Mana Siphon");
        add("create_wizardry.tooltip.mana_siphon.confined", "Confined");
        add("create_wizardry.tooltip.mana_siphon.expanded", "Expanded");
        add("block.create_wizardry.arcane_essence_cluster", "Arcane Essence Cluster");
        add("effect.create_wizardry.mana_depletion", "Depletion");
        add("effect.create_wizardry.siphon_lock", "Siphon Lock");
        add("gamerule.siphonTransformationDamage", "Mana Siphon Transformation Damage");
        add("create_wizardry.tooltip.must_be_superheated", "(Must be superheated)");
        add("create_wizardry.tooltip.spell", "Spell: %s");
        add("create_wizardry.tooltip.no_scroll", "No scroll equipped");
        add("create_wizardry.tooltip.hat", "Hat: %s");
        add("create_wizardry.tooltip.mode.sentry", "Mode: Sentry");
        add("create_wizardry.tooltip.mode.impulse", "Mode: Impulse");
        add("create_wizardry.tooltip.mode.impulse.locked", "Impulse (Locked)");
        add("create_wizardry.tooltip.mode.impulse.unlocked", "Impulse (Unlocked)");
        add("create_wizardry.tooltip.cooldown", "Cooldown: %ss");
        add("create_wizardry.tooltip.ready", "Ready");
        add("create_wizardry.tooltip.not_enough_mana", "Not enough Mana!");
        add("create_wizardry.tooltip.casting", "Casting...");
        add("create_wizardry.tooltip.creative_mode", "Creative Mode: ON (right-click with Creative Blaze Cake to toggle)");
        add("create_wizardry.tooltip.superheated", "Superheated: %ss");
        add("create_wizardry.tooltip.superheated_permanent", "Superheated");
        add("create_wizardry.tooltip.spell_incompatible", "(Incompatible with Blaze Caster)");
        add("create_wizardry.message.spell_incompatible", "This spell cannot be cast from the Blaze Caster");
        add("create_wizardry.tooltip.hat.spell_power_boost", "+5%% Spell Power");
        add("create_wizardry.tooltip.hat.mana_boost", "+1,250 Mana");
        add("create_wizardry.tooltip.hat.school_power_boost", "+10%% %s Spell Power");
        add("create_wizardry.tooltip.hat.tarnished_penalty", "-15% Spell Power");
        add("create_wizardry.tooltip.hat.tarnished_mana", "+1,500 Mana");
        add("create_wizardry.tooltip.hat.mana_cost", "-25% Mana Cost");
        //TODO: make easier in a single method type
    }
}
