package io.github.restioson.siege.game.active;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum CapturingState {
    CAPTURING(Text.literal("Capturing..").formatted(Formatting.GOLD)),
    CONTESTED(Text.literal("Contested!").formatted(Formatting.GRAY)),
    SECURING(Text.literal("Securing..").formatted(Formatting.AQUA)),
    RECAPTURE_DISABLED(Text.literal("Recapture is disabled for this game!").formatted(Formatting.RED), false),
    PREREQUISITE_REQUIRED(Text.literal("Cannot capture yet!").formatted(Formatting.RED), false);

    private final Text name;
    private final boolean hasAlert;

    CapturingState(Text name) {
        this.name = name;
        this.hasAlert = true;
    }

    CapturingState(Text name, boolean hasAlert) {
        this.name = name;
        this.hasAlert = hasAlert;
    }


    public Text getName() {
        return this.name;
    }

    public boolean hasAlert() {
        return this.hasAlert;
    }
}
