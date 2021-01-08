package io.github.restioson.siege.game.map;

import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class SiegeGate {
    public final SiegeFlag flag;
    public final BlockBounds gateOpen;
    public final BlockBounds portcullis;
    public final int retractHeight;

    public final GateSlider slider;
    public boolean bashedOpen = false;
    public int health;
    public int repairedHealthThreshold;
    public int maxHealth;
    private int openSlide;
    public long timeOfLastBash;

    @Nullable
    public BlockBounds brace;

    public SiegeGate(SiegeFlag flag, BlockBounds gateOpen, BlockBounds portcullis, @Nullable BlockBounds brace, int retractHeight, int repairedHealthThreshold, int maxHealth) {
        this.flag = flag;
        this.gateOpen = gateOpen;
        this.portcullis = portcullis;
        this.retractHeight = retractHeight;

        this.openSlide = 0;
        this.health = repairedHealthThreshold;
        this.repairedHealthThreshold = repairedHealthThreshold;
        this.maxHealth = maxHealth;

        this.slider = new GateSlider(portcullis, retractHeight);

        this.brace = brace;
    }

    public boolean tickOpen(ServerWorld world) {
        if (this.openSlide >= this.slider.getMaxOffset()) {
            return false;
        }
        this.slider.set(world, ++this.openSlide);
        return true;
    }

    public boolean tickClose(ServerWorld world) {
        if (this.openSlide <= 0) {
            return false;
        }
        this.slider.set(world, --this.openSlide);
        return true;
    }
}
