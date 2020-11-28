package io.github.restioson.siege.game;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum SiegeTeam {
    ATTACKERS("Attackers", Formatting.RED),
    DEFENDERS("Defenders", Formatting.AQUA);

    private final Text name;

    SiegeTeam(String name, Formatting color) {
        this.name = new LiteralText(name).formatted(color);
    }

    public Text getName() {
        return this.name;
    }
}
