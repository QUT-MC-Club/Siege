package io.github.restioson.siege.game.map;

import io.github.restioson.siege.game.active.SiegeActive;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class SiegeGate {
    public final String id;
    public final SiegeFlag flag;
    public final BlockBounds gateOpen;
    public final BlockBounds portcullis;
    public final int retractHeight;

    public final GateSlider slider;
    public boolean bashedOpen = false;
    public int health;
    public int repairedHealthThreshold;
    public int maxHealth;
    public int openSlide;
    public long timeOfLastBash;

    @Nullable
    public BlockBounds brace;

    public SiegeGate(String id, SiegeFlag flag, BlockBounds gateOpen, BlockBounds portcullis, @Nullable BlockBounds brace, int retractHeight, int repairedHealthThreshold, int maxHealth) {
        this.id = id;
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

    public void broadcastHealth(ServerPlayerEntity initiator, SiegeActive active, ServerWorld world) {
        Text text = Text.literal("Gate health: ").append(Integer.toString(this.health)).formatted(Formatting.DARK_GREEN);
        initiator.sendMessage(text, true);
        for (PlayerRef ref : active.participants.keySet()) {
            ref.ifOnline(world, p -> {
                if (this.gateOpen.contains(p.getBlockPos()) && p != initiator) {
                    p.sendMessage(text, true);
                }
            });
        }
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
