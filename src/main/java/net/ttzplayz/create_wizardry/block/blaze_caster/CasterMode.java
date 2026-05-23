package net.ttzplayz.create_wizardry.block.blaze_caster;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum CasterMode implements StringRepresentable {
    SENTRY, IMPULSE;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
