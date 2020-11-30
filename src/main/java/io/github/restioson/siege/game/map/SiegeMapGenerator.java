package io.github.restioson.siege.game.map;

import io.github.restioson.siege.Siege;
import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.SiegeTeams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateMetadata;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SiegeMapGenerator {
    private final SiegeMapConfig config;

    public SiegeMapGenerator(SiegeMapConfig config) {
        this.config = config;
    }

    public SiegeMap create() throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.id);

            SiegeMap map = new SiegeMap(template, this.config.attackerSpawnAngle);
            template.setBiome(BiomeKeys.PLAINS);

            BlockBounds waitingSpawn = template.getMetadata().getFirstRegionBounds("waiting_spawn");
            if (waitingSpawn == null) {
                throw new GameOpenException(new LiteralText("waiting_spawn region required but not found"));
            }

            map.setWaitingSpawn(waitingSpawn);

            this.addFlagsToMap(map, template.getMetadata());
            map.kitStands.addAll(this.collectKitStands(template));

            for (BlockPos pos : template.getBounds()) {
                BlockState state = template.getBlockState(pos);
                if (!state.isAir()) {
                    map.addProtectedBlock(pos.asLong());
                }
            }

            return map;
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText("Failed to load map"), e);
        }
    }

    private List<SiegeKitStandLocation> collectKitStands(MapTemplate template) {
        return template.getMetadata()
                .getRegions("kit_stand")
                .map(region -> {
                    CompoundTag data = region.getData();
                    GameTeam team = this.parseTeam(data);
                    SiegeKit type = this.parseKitStandType(data);

                    return new SiegeKitStandLocation(team, region.getBounds().getCenter(), type, data.getFloat("yaw"));
                })
                .collect(Collectors.toList());
    }

    private void addFlagsToMap(SiegeMap map, MapTemplateMetadata metadata) {
        Map<String, SiegeFlag> flags = new Object2ObjectOpenHashMap<>();

        metadata.getRegions("flag").forEach(region -> {
            BlockBounds bounds = region.getBounds();
            CompoundTag data = region.getData();
            String id = data.getString("id");
            String name = data.getString("name");
            GameTeam team = this.parseTeam(data);

            SiegeFlag flag = new SiegeFlag(id, name, team, bounds);
            if (data.contains("capturable") && !data.getBoolean("capturable")) {
                flag.capturable = false;
            }

            if (data.contains("plural") && data.getBoolean("plural")) {
                flag.pluralName = true;
            }

            flags.put(id, flag);
            map.flags.add(flag);
        });

        metadata.getRegions("flag").forEach(region -> {
            CompoundTag data = region.getData();
            String flagId = data.getString("id");

            SiegeFlag flag = flags.get(flagId);
            if (flag == null) {
                return;
            }

            ListTag prerequisiteFlagsList = data.getList("prerequisite_flags", NbtType.STRING);
            for (int i = 0; i < prerequisiteFlagsList.size(); i++) {
                String prerequisiteId = prerequisiteFlagsList.getString(i);

                SiegeFlag prerequisite = flags.get(prerequisiteId);
                if (prerequisite == null) {
                    Siege.LOGGER.error("Unknown flag \"{}}\"", prerequisiteId);
                    throw new GameOpenException(new LiteralText("unknown flag"));
                }

                flag.prerequisiteFlags.add(prerequisite);
            }
        });

        metadata.getRegions("respawn").forEach(region -> {
            CompoundTag data = region.getData();
            String flagId = data.getString("id");
            SiegeFlag flag = flags.get(flagId);
            if (flag != null) {
                flag.respawn = region.getBounds();
            } else {
                Siege.LOGGER.warn("Respawn attached to missing flag: {}", flagId);
            }
        });
    }

    private GameTeam parseTeam(CompoundTag data) {
        String teamName = data.getString("team");
        GameTeam team = SiegeTeams.byKey(teamName);
        if (team == null) {
            Siege.LOGGER.error("Unknown team \"{}\"", teamName);
            throw new GameOpenException(new LiteralText("unknown team"));
        }
        return team;
    }

    private SiegeKit parseKitStandType(CompoundTag data) {
        String kitName = data.getString("type");
        SiegeKit type;
        switch (kitName) {
            case "bow":
                type = SiegeKit.ARCHER;
                break;
            case "sword":
                type = SiegeKit.SOLDIER;
                break;
            case "shield":
                type = SiegeKit.SHIELD_BEARER;
                break;
            case "builder":
                type = SiegeKit.CONSTRUCTOR;
                break;
            default:
                Siege.LOGGER.error("Unknown kit \"" + kitName + "\"");
                throw new GameOpenException(new LiteralText("unknown kit"));
        }

        return type;
    }
}
