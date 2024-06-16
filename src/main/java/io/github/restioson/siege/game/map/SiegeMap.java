package io.github.restioson.siege.game.map;

import io.github.restioson.siege.Siege;
import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.SiegeSpawnLogic;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.active.SiegeActive;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.util.ArrayList;
import java.util.List;

public class SiegeMap {
    private final SiegeMapConfig config;
    private final MapTemplate template;
    public final List<SiegeFlag> flags = new ArrayList<>();
    public final List<SiegeKitStandLocation> kitStands = new ArrayList<>();
    public SiegeSpawn waitingSpawn = null;
    public List<BlockBounds> noBuildRegions = new ArrayList<>();
    public List<SiegeGate> gates = new ArrayList<>();
    public SiegeSpawn attackerFirstSpawn;
    public SiegeSpawn defenderFirstSpawn;
    public long time;

    private final LongSet protectedBlocks = new LongOpenHashSet();

    public SiegeMap(SiegeMapConfig config, MapTemplate template) {
        this.config = config;
        this.template = template;
        this.time = 1000;
    }

    public SiegeSpawn getFirstSpawn(GameTeam team) {
        if (team == SiegeTeams.ATTACKERS) {
            return this.attackerFirstSpawn;
        } else {
            return this.defenderFirstSpawn;
        }
    }

    public void setWaitingSpawn(SiegeSpawn spawn) {
        this.waitingSpawn = spawn;
    }

    public void addProtectedBlock(long pos) {
        this.protectedBlocks.add(pos);
    }

    public boolean isProtectedBlock(BlockPos pos) {
        return this.isProtectedBlock(pos.asLong());
    }

    public boolean isProtectedBlock(long pos) {
        return this.protectedBlocks.contains(pos);
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }

    public void spawnKitStands(SiegeActive active) {
        for (SiegeKitStandLocation stand : this.kitStands) {
            SiegeKitStandEntity standEntity = new SiegeKitStandEntity(active.world, active, stand);
            active.world.spawnEntity(standEntity);

            if (standEntity.controllingFlag != null) {
                standEntity.controllingFlag.kitStands.add(standEntity);
            }
        }
    }
}
