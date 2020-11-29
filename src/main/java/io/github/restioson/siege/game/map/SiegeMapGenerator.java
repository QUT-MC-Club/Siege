package io.github.restioson.siege.game.map;

import com.google.common.collect.ImmutableList;
import io.github.restioson.siege.Siege;
import io.github.restioson.siege.game.SiegeFlag;
import io.github.restioson.siege.game.SiegeKitStandEntity;
import io.github.restioson.siege.game.SiegeTeams;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
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
            map.flags.addAll(this.collectFlags(template));
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
                    SiegeKitStandEntity.KitType type = this.parseKitStandType(data);

                    return new SiegeKitStandLocation(team, region.getBounds().getCenter(), type);
                })
                .collect(Collectors.toList());
    }

    private Collection<SiegeFlag> collectFlags(MapTemplate template) {
        Map<String, SiegeFlag> flags = new Object2ObjectLinkedOpenHashMap<>();

        template.getMetadata().getRegions("flag").forEach(region -> {
            BlockBounds bounds = region.getBounds();
            CompoundTag data = region.getData();
            String name = data.getString("name");
            GameTeam team = this.parseTeam(data);
            flags.put(name, new SiegeFlag(team, bounds, name, ImmutableList.of()));
        });

        template.getMetadata().getRegions("flag").forEach(region -> {
            CompoundTag data = region.getData();
            String flagName = data.getString("name");
            String[] flagNames = data.getString("prerequisite_flags").split(";");

            if (flagNames.length == 1 && flagNames[0].equals("")) {
                return;
            }

            List<SiegeFlag> prerequisites = Arrays.stream(flagNames)
                    .map(prerequisiteName -> {
                        SiegeFlag flag = flags.get(prerequisiteName);
                        if (flag == null) {
                            Siege.LOGGER.error("Unknown flag \"" + prerequisiteName + "\"");
                            throw new GameOpenException(new LiteralText("unknown flag"));
                        }
                        return flag;
                    })
                    .collect(Collectors.toList());

            SiegeFlag flag = flags.get(flagName);
            if (flag != null) {
                flag.prerequisiteFlags = prerequisites;
            }
        });

        return flags.values();
    }

    private GameTeam parseTeam(CompoundTag data) {
        String teamName = data.getString("team");
        GameTeam team;
        switch (teamName) {
            case "attackers":
                team = SiegeTeams.ATTACKERS;
                break;
            case "defenders":
                team = SiegeTeams.DEFENDERS;
                break;
            default:
                Siege.LOGGER.error("Unknown team + \"" + teamName + "\"");
                throw new GameOpenException(new LiteralText("unknown team"));
        }

        return team;
    }

    private SiegeKitStandEntity.KitType parseKitStandType(CompoundTag data) {
        String kitName = data.getString("type");
        SiegeKitStandEntity.KitType type;
        switch (kitName) {
            case "bow":
                type = SiegeKitStandEntity.KitType.ARCHER;
                break;
            case "sword":
                type = SiegeKitStandEntity.KitType.SOLDIER;
                break;
            case "shield":
                type = SiegeKitStandEntity.KitType.SHIELD_BEARER;
                break;
            case "builder":
                type = SiegeKitStandEntity.KitType.CONSTRUCTOR;
                break;
            default:
                Siege.LOGGER.error("Unknown kit + \"" + kitName + "\"");
                throw new GameOpenException(new LiteralText("unknown kit"));
        }

        return type;
    }
}
