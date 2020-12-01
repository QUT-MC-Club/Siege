package io.github.restioson.siege.game.map;

import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class SiegeGate {
    public final SiegeFlag flag;
    public final BlockBounds gateOpen;
    public final BlockBounds portcullis;
    public final int retractHeight;

    public final GateSlider slider;

    private int openSlide;

    public SiegeGate(SiegeFlag flag, BlockBounds gateOpen, BlockBounds portcullis, int retractHeight) {
        this.flag = flag;
        this.gateOpen = gateOpen;
        this.portcullis = portcullis;
        this.retractHeight = retractHeight;

        this.openSlide = 0;

        this.slider = new GateSlider(portcullis, retractHeight);
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
