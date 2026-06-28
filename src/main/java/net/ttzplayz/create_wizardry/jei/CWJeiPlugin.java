package net.ttzplayz.create_wizardry.jei;//package net.ttzplayz.create_wizardry.jei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.MillingCategory;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.JeiPlugin;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.ttzplayz.create_wizardry.item.CWItems.*;

@JeiPlugin
public class CWJeiPlugin implements IModPlugin {

//    look at create jei implementation /TODO

//    private final List<CreateRecipeCategory<?>> categories = new ArrayList<>();
//    private IIngredientManager ingredientManager;
//
//    public static IJeiRuntime runtime;
//
//    private void loadCategories() {
//        categories.clear();
//
//        CreateRecipeCategory<?>
//
//                milling = builder(AbstractCrushingRecipe.class)
//                .addTypedRecipes(AllRecipeTypes.MILLING)
//                .catalyst(AllBlocks.MILLSTONE::get)
//                .doubleItemIcon(AllBlocks.MILLSTONE.get(), AllItems.WHEAT_FLOUR.get())
//                .emptyBackground(177, 53)
//                .build("milling", MillingCategory::new);
//    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.parse("create_wizardry:information");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(List.of(new ItemStack(LIGHTNING_BUCKET)), VanillaTypes.ITEM_STACK, Component.translatable("jei.create_wizardry.liquid_lightning"));
        registration.addIngredientInfo(List.of(new ItemStack(MANA_BUCKET)), VanillaTypes.ITEM_STACK, Component.translatable("jei.create_wizardry.mana"));
        registration.addIngredientInfo(List.of(new ItemStack(BLOOD_BUCKET)), VanillaTypes.ITEM_STACK, Component.translatable("jei.create_wizardry.blood"));
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        // Hide our internal CW Blood fluid from JEI; it's plumbing for blood spells, not a
        // standalone ingredient players interact with directly.
        // Only the source fluid is listed as a JEI ingredient (flowing fluids aren't), so removing
        // just it avoids "ingredient not in list" warnings.
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                NeoForgeTypes.FLUID_STACK,
                List.of(new FluidStack(CWFluidRegistry.BLOOD_SOURCE.get(), 1000)));
    }

}