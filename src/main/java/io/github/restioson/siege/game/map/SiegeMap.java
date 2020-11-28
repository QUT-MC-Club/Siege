package io.github.restioson.siege.game.map;

import io.github.restioson.siege.Siege;
import io.github.restioson.siege.game.SiegeFlag;
import io.github.restioson.siege.game.SiegeTeam;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SiegeMap {
    private final MapTemplate template;
    public final List<SiegeFlag> flags;
    public final int attackerSpawnAngle;
    public final BlockBounds bounds;
    public final BlockBounds waitingSpawn;

    public SiegeMap(MapTemplate template, int attackerSpawnAngle) {
        this.template = template;
        this.attackerSpawnAngle = attackerSpawnAngle;
        this.waitingSpawn = template.getMetadata().getFirstRegionBounds("waiting_spawn");

        if (this.waitingSpawn == null) {
            Siege.LOGGER.error("waiting_spawn region required but not found");
            throw new GameOpenException(new LiteralText("waiting_spawn region required but not found"));
        }

        this.flags = template.getMetadata().getRegions("flag").map(region -> {
            BlockBounds bounds = region.getBounds();
            CompoundTag data = region.getData();
            String name = data.getString("name");
            String teamName = data.getString("team");

            SiegeTeam team;
            switch(teamName) {
                case "attackers":
                    team = SiegeTeam.ATTACKERS;
                    break;
                case "defenders":
                    team = SiegeTeam.DEFENDERS;
                    break;
                default:
                    Siege.LOGGER.error("Unknown team + \"" + teamName + "\"");
                    throw new GameOpenException(new LiteralText("unknown team"));
            }

            return new SiegeFlag(team, bounds, name, null);
        })
                .collect(Collectors.toList());

        template.getMetadata().getRegions("flag").forEach(region -> {
            CompoundTag data = region.getData();
            String flagName = data.getString("name");
            String[] flagNames = data.getString("prerequisite_flags").split(";");

            if (flagNames.length == 1 && flagNames[0].equals("")) {
                return;
            }

            List<SiegeFlag> prerequisites = Arrays.stream(flagNames)
                    .map(prerequisiteName -> {
                        Optional<SiegeFlag> flagOpt = this.flagWithName(prerequisiteName);
                        if (!flagOpt.isPresent()) {
                            Siege.LOGGER.error("Unknown flag \"" + prerequisiteName + "\"");
                            throw new GameOpenException(new LiteralText("unknown flag"));
                        }
                        return flagOpt.get();
                    })
                    .collect(Collectors.toList());

            this.flagWithName(flagName).ifPresent(siegeFlag -> siegeFlag.prerequisiteFlags = prerequisites);
        });

        this.bounds = template.getBounds();

    }

    private Optional<SiegeFlag> flagWithName(String name) {
        return this.flags.stream().filter(flag -> flag.name.equalsIgnoreCase(name)).findFirst();
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
