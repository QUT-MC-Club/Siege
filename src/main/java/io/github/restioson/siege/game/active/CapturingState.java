package io.github.restioson.siege.game.active;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum CapturingState {
    CAPTURING(new LiteralText("Capturing..").formatted(Formatting.GOLD)),
    CONTESTED(new LiteralText("Contested!").formatted(Formatting.GRAY)),
    SECURING(new LiteralText("Securing..").formatted(Formatting.AQUA)),
    PREREQUISITE_REQUIRED(new LiteralText("Cannot capture yet!").formatted(Formatting.RED));

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
