package net.ttzplayz.create_wizardry.advancement;

import com.google.common.collect.Sets;
import com.simibubi.create.AllItems;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ttzplayz.create_wizardry.item.CWItems;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.*;

public class CWAdvancements implements DataProvider {
    public static final List<CWAdvancement> ENTRIES = new ArrayList<>();
    public static final CWAdvancement START = null,
            ROOT = create("root", b -> b.icon(itemFromRegistry(CreateWizardry.id("lightning_bucket")))
                    .title("Welcome to Create: Wizardry")
                    .description("Mechanical Sorcery")
                    .awardedForFree()
                    .special(CWAdvancement.TaskType.SILENT)),
    // CHANNELER/LIGHTNING
            CHANNELER = create("channeler", b -> b.icon(itemFromRegistry(CreateWizardry.id("channeler")))
                    .title("Harvest the Heavens")
                    .description("Craft and Place Down a Channeler to harvest Liquid Lightning.")
                    .special(CWAdvancement.TaskType.NORMAL)
                    .whenIconCollected()
                    .after(ROOT)),
            SHOCKING = create("shocking", b -> b.icon(itemFromRegistry(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "lightning_bottle")))
                    .title("A *Shocking* Discovery")
                    .description("Yeah, SHOCKING...")
                    .after(CHANNELER)
                    .special(CWAdvancement.TaskType.SECRET)),
            UNLIMITED_POWER = create("unlimited_power", b -> b.icon(itemFromRegistry(ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "lightning_bucket")))
                    .title("UNLIMITED POWER!!!")
                    .description("Safe? No. Fun? Yes.")
                    .special(CWAdvancement.TaskType.SUPER_SECRET)
                    .after(SHOCKING)),
    // BLAZE CASTER
            I_CAST_FIREBALL = create("i_cast_fireball", b -> b.icon(itemFromRegistry(CreateWizardry.id("blaze_caster")))
                    .title("I CAST FIREBALL!!!")
                    .description("Forge the Blaze Caster. And uh, you don't care how big the room is.")
                    .whenIconCollected()
                    .special(CWAdvancement.TaskType.NOISY)
                    .after(ROOT)),
            MEGA_LASER = create("mega_laser", b -> b.icon(itemFromRegistry(CreateWizardry.id("blaze_caster")))
                    .title("AND MEGA LASER!")
                    .description("Cast Black Hole with the Blaze Caster.")
                    .special(CWAdvancement.TaskType.SUPER_SECRET)
                    .after(I_CAST_FIREBALL)),
    // MANA MANIPULATION
            SPLOINK = create("sploink", b -> b.icon(itemFromRegistry(CreateWizardry.id("arcane_sheet")))
                    .title("Sploink.")
                    .description("Obtain a ** magical ** Arcane Sheet.")
                    .special(CWAdvancement.TaskType.NOISY)
                    .after(ROOT)),
            ENLIGHTENMENT_AGE = create("enlightenment_age", b -> b.icon(itemFromRegistry(CreateWizardry.id("arcane_casing")))
                    .title("The Enlightenment Age")
                    .description("Make an Arcane Casing.")
                    .special(CWAdvancement.TaskType.NORMAL)
                    .after(ROOT)),
            SPLASH_GUARD_ON = create("splash_guard_on", b -> b.icon(itemFromRegistry(CreateWizardry.id("arcane_pipe")))
                    .title("Insulated")
                    .description("Insulate pipes with Arcane Sheets to prevent Mana loss.")
                    .special(CWAdvancement.TaskType.NOISY)
                    .after(SPLOINK)),
            DO_YOU_FEEL_DIFFERENT = create("do_you_feel_different", b -> b.icon(itemFromRegistry(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_spell_book")))
                    .title("Do You Feel Different?")
                    .description("Convert a mob to its casting counterpart.")
                    .special(CWAdvancement.TaskType.SECRET)
                    .after(ROOT)),
            AURA_MONSTER = create("aura_monster", b -> b.icon(itemFromRegistry(CreateWizardry.id("mana_siphon")))
                    .title("Aura MONSTER")
                    .description("Create a Mana Siphon to harness your innate AURA (mana).")
                    .special(CWAdvancement.TaskType.NORMAL)
                    .after(SPLASH_GUARD_ON)),
            YOUR_SOUL_IS_MINE = create("your_soul_is_mine", b -> b.icon(itemFromRegistry(CreateWizardry.id("mana_bucket")))
                    .title("Your Soul Is Mine!")
                    .description("Siphon Mana from a spellcasting mob.")
                    .special(CWAdvancement.TaskType.EXPERT)
                    .after(AURA_MONSTER)),
            BABY_BLUE = create("baby_blue", b -> b.icon(itemFromRegistry(ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "arcane_essence")))
                    .title("99.7% Pure")
                    .description("Harvest Mana Crystals from a Mana Siphon for (almost) free Arcane Essence.")
                    .special(CWAdvancement.TaskType.EXPERT)
                    .after(AURA_MONSTER)),
            A_DEVASTATING_LOSS = create("a_devastating_loss", b -> b.icon(itemFromRegistry(CreateWizardry.id("arcane_pipe")))
                    .title("I II II L")
                    .description("Pump Mana across <20 blocks of pipes...and be devastated by the result. Please insulate, for Herobrine's sake.")
                    .special(CWAdvancement.TaskType.SUPER_SECRET)
                    .after(ROOT)),
    // GENERAL/MISC
            VAMPIRE_SHOWER = create("vampire_shower", b -> b.icon(itemFromRegistry(ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "blood_bucket")))
                    .title("Vampire Shower")
                    .description("Bathe in the blood of your enemies.")
                    .special(CWAdvancement.TaskType.SECRET)
                    .after(ROOT)),
            ALCH_101 = create("alch_101", b -> b.icon(itemFromRegistry(ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "legendary_ink")))
                    .title("Alchemy 101")
                    .description("Synthesize Arcane Essence with Lapis, Amethyst, and Exp.")
                    .special(CWAdvancement.TaskType.NORMAL)
                    .after(ROOT)),
            INDUSTRIAL_INK = create("industrial_ink", b -> b.icon(itemFromRegistry(ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "legendary_ink")))
                    .title("Industrial Ink")
                    .description("Mix ink using Create contraptions.")
                    .special(CWAdvancement.TaskType.NOISY)
                    .after(ALCH_101));








    private static CWAdvancement create(String id, UnaryOperator<CWAdvancement.Builder> b) {
        return new CWAdvancement(id, b) {
            @Override
            protected DeferredHolder<CriterionTrigger<?>, CWBuiltInTrigger> add(ResourceLocation id) {
                return CWBuiltInTriggers.register(id);
            }
        };
    }

    public static void registerTriggers() {
        for (CWAdvancement advancement : ENTRIES) {
            if (advancement.hasBuiltinTrigger()) {
                CWBuiltInTriggers.register(advancement.getId());
            }
        }
    }

    // Datagen

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public CWAdvancements(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }



    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.registries.thenCompose(provider -> {
            PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
            List<CompletableFuture<?>> futures = new ArrayList<>();

            Set<ResourceLocation> set = Sets.newHashSet();
            Consumer<AdvancementHolder> consumer = (advancement) -> {
                ResourceLocation id = advancement.id();
                if (!set.add(id))
                    throw new IllegalStateException("Duplicate advancement " + id);
                Path path = pathProvider.json(id);
                LOGGER.info("Saving advancement {}", id);
                futures.add(DataProvider.saveStable(cache, provider, Advancement.CODEC, advancement.value(), path));
            };

            for (CWAdvancement advancement : ENTRIES)
                advancement.save(consumer, provider);

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Create: Wizardry Advancements";
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        for (CWAdvancement advancement : ENTRIES)
            advancement.provideLang(consumer);
    }

    private static java.util.function.Function<HolderLookup.Provider, ItemStack> itemFromRegistry(ResourceLocation id) {
        return provider -> new ItemStack(
                provider.lookupOrThrow(Registries.ITEM)
                        .getOrThrow(ResourceKey.create(Registries.ITEM, id))
                        .value());
    }
}
