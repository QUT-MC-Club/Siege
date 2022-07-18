package io.github.restioson.siege.game.active;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum CapturingState {
    CAPTURING(Text.literal("Capturing..").formatted(Formatting.GOLD)),
    CONTESTED(Text.literal("Contested!").formatted(Formatting.GRAY)),
    SECURING(Text.literal("Securing..").formatted(Formatting.AQUA)),
    PREREQUISITE_REQUIRED(Text.literal("Cannot capture yet!").formatted(Formatting.RED));

    private final Text name;

    CapturingState(Text name) {
        this.name = name;
    }

    public Text getName() {
        return this.name;
    }

    public boolean hasAlert() {
        return this != PREREQUISITE_REQUIRED;
    }
}
