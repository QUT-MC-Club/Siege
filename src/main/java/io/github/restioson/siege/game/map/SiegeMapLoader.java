package io.github.restioson.siege.game.map;

import com.google.common.base.Strings;
import io.github.restioson.siege.Siege;
import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.SiegeTeams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.*;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SiegeMapLoader {
    private final SiegeMapConfig config;

    public SiegeMapLoader(SiegeMapConfig config) {
        this.config = config;
    }

    public SiegeMap create(MinecraftServer server) throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, this.config.templateId());
            MapTemplateMetadata metadata = template.getMetadata();

            SiegeMap map = new SiegeMap(template, this.config.attackerSpawnAngle());

            NbtCompound mapData = metadata.getData();
            String biomeId = mapData.getString("biome");
            if (!Strings.isNullOrEmpty(biomeId)) {
                template.setBiome(RegistryKey.of(Registry.BIOME_KEY, new Identifier(biomeId)));
            } else {
                template.setBiome(BiomeKeys.PLAINS);
            }

            if (mapData.contains("time")) {
                map.time = mapData.getLong("time");
            }

            TemplateRegion waitingSpawn = metadata.getFirstRegion("waiting_spawn");
            if (waitingSpawn == null) {
                throw new GameOpenException(new LiteralText("waiting_spawn region required but not found"));
            }

            map.setWaitingSpawn(new SiegeSpawn(waitingSpawn.getBounds(), waitingSpawn.getData().getFloat("yaw")));

            this.addFlagsToMap(map, metadata);
            map.kitStands.addAll(this.collectKitStands(map.flags, template));

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

    private List<SiegeKitStandLocation> collectKitStands(List<SiegeFlag> flags, MapTemplate template) {
        return template.getMetadata()
                .getRegions("kit_stand")
                .map(region -> {
                    NbtCompound data = region.getData();
                    GameTeam team = null;
                    if (data.contains("team")) {
                        team = this.parseTeam(data);
                    }

                    SiegeFlag flag = null;
                    if (data.contains("flag")) {
                        flag = flags.stream().filter(f -> f.id.equalsIgnoreCase(data.getString("flag"))).findAny().orElse(null);

                        if (flag == null) {
                            Siege.LOGGER.error("Unknown flag \"{}\"", data.getString("flag"));
                            throw new GameOpenException(new LiteralText("unknown flag"));
                        }
                    }

                    SiegeKit type = this.parseKitStandType(data);

                    return new SiegeKitStandLocation(team, flag, region.getBounds().center(), type, data.getFloat("yaw"));
                })
                .collect(Collectors.toList());
    }

    private void addFlagsToMap(SiegeMap map, MapTemplateMetadata metadata) {
        Map<String, SiegeFlag> flags = new Object2ObjectOpenHashMap<>();

        metadata.getRegions("flag").forEach(region -> {
            BlockBounds bounds = region.getBounds();
            NbtCompound data = region.getData();
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

            if (data.contains("icon")) {
                String icon = data.getString("icon");
                flag.icon = new ItemStack(Registry.ITEM.get(new Identifier(icon)));
            }

            flags.put(id, flag);
            map.flags.add(flag);
        });

        map.flags.sort(Comparator.comparing(flag -> flag.name));

        metadata.getRegions("flag").forEach(region -> {
            NbtCompound data = region.getData();
            String flagId = data.getString("id");

            SiegeFlag flag = flags.get(flagId);
            if (flag == null) {
                return;
            }

            NbtList prerequisiteFlagsList = data.getList("prerequisite_flags", NbtType.STRING);
            for (int i = 0; i < prerequisiteFlagsList.size(); i++) {
                String prerequisiteId = prerequisiteFlagsList.getString(i);

                SiegeFlag prerequisite = flags.get(prerequisiteId);
                if (prerequisite == null) {
                    Siege.LOGGER.error("Unknown flag \"{}\"", prerequisiteId);
                    throw new GameOpenException(new LiteralText("unknown flag"));
                }

                flag.prerequisiteFlags.add(prerequisite);
            }

            NbtList recapturePrerequisites = data.getList("recapture_prerequisites", NbtType.STRING);
            for (int i = 0; i < recapturePrerequisites.size(); i++) {
                String prerequisiteId = recapturePrerequisites.getString(i);

                SiegeFlag prerequisite = flags.get(prerequisiteId);
                if (prerequisite == null) {
                    Siege.LOGGER.error("Unknown flag \"{}\"", prerequisiteId);
                    throw new GameOpenException(new LiteralText("unknown flag"));
                }

                flag.recapturePrerequisites.add(prerequisite);
            }

            flag.flagIndicatorBlocks = metadata.getRegions("flag_indicator")
                    .filter(r -> flagId.equalsIgnoreCase(r.getData().getString("id")))
                    .map(TemplateRegion::getBounds)
                    .collect(Collectors.toList());
        });

        metadata.getRegions("respawn").forEach(region -> {
            NbtCompound data = region.getData();
            String flagId = data.getString("id");
            SiegeFlag flag = flags.get(flagId);
            if (flag != null) {
                float yaw = data.getFloat("yaw");
                SiegeSpawn respawn = new SiegeSpawn(region.getBounds(), yaw);

                GameTeam team = this.parseOptionalTeam(data);
                if (team == SiegeTeams.DEFENDERS) {
                    flag.defenderRespawn = respawn;
                } else if (team == SiegeTeams.ATTACKERS) {
                    flag.attackerRespawn = respawn;
                } else {
                    flag.defenderRespawn = flag.attackerRespawn = respawn;
                }

                if (data.contains("starting_spawn") && data.getBoolean("starting_spawn")) {
                    if (flag.team == SiegeTeams.DEFENDERS) {
                        map.defenderFirstSpawn = respawn;
                    } else {
                        map.attackerFirstSpawn = respawn;
                    }
                }
            } else {
                Siege.LOGGER.warn("Respawn attached to missing flag: {}", flagId);
            }
        });

        map.noBuildRegions = metadata.getRegionBounds("no_build").collect(Collectors.toList());

        map.gates = metadata.getRegions("gate_open")
                .map(region -> {
                    NbtCompound data = region.getData();

                    String id = data.getString("id");
                    SiegeFlag flag = flags.get(id);
                    if (flag == null) {
                        throw new GameOpenException(new LiteralText("Gate missing flag with id '" + id + "'!"));
                    }

                    TemplateRegion portcullisRegion = metadata.getRegions("portcullis")
                            .filter(r -> id.equalsIgnoreCase(r.getData().getString("id")))
                            .findFirst()
                            .orElseThrow(() -> {
                                Siege.LOGGER.error("Gate \"{}\" missing portcullis!", id);
                                return new GameOpenException(new LiteralText("Gate missing portcullis!"));
                            });

                    NbtCompound portcullisData = portcullisRegion.getData();
                    int retractHeight = portcullisData.getInt("retract_height");

                    int repairHealthThreshold = 50;

                    if (portcullisData.contains("repair_health_threshold")) {
                        repairHealthThreshold = portcullisData.getInt("repair_health_threshold");
                    }

                    int maxHealth = 100;

                    if (portcullisData.contains("max_health")) {
                        repairHealthThreshold = portcullisData.getInt("max_health");
                    }

                    BlockBounds brace = metadata.getRegions("gate_brace")
                            .filter(r -> id.equalsIgnoreCase(r.getData().getString("id")))
                            .map(TemplateRegion::getBounds)
                            .findFirst()
                            .orElse(null);

                    SiegeGate gate = new SiegeGate(flag, region.getBounds(), portcullisRegion.getBounds(), brace, retractHeight, repairHealthThreshold, maxHealth);
                    flag.gate = gate;
                    return gate;
                })
                .collect(Collectors.toList());

        for (SiegeFlag flag : flags.values()) {
            // TODO: remove this restriction (it's for warp enderpearl)
            if (flag.team == SiegeTeams.DEFENDERS && flag.defenderRespawn == null) {
                Siege.LOGGER.error("Flag \"{}\" missing respawn!", flag.name);
                throw new GameOpenException(new LiteralText("Flag missing respawn!"));
            }
        }
    }

    private GameTeam parseTeam(NbtCompound data) {
        String teamName = data.getString("team");
        GameTeam team = SiegeTeams.byKey(teamName);
        if (team == null) {
            Siege.LOGGER.error("Unknown team \"{}\"", teamName);
            return SiegeTeams.DEFENDERS;
        }
        return team;
    }

    @Nullable
    private GameTeam parseOptionalTeam(NbtCompound data) {
        String teamName = data.getString("team");
        return SiegeTeams.byKey(teamName);
    }

    private SiegeKit parseKitStandType(NbtCompound data) {
        String kitName = data.getString("type");
        return switch (kitName) {
            case "bow" -> SiegeKit.ARCHER;
            case "sword" -> SiegeKit.SOLDIER;
            case "shield" -> SiegeKit.SHIELD_BEARER;
            case "builder" -> SiegeKit.CONSTRUCTOR;
            case "demolitioner" -> SiegeKit.DEMOLITIONER;
            default -> {
                Siege.LOGGER.error("Unknown kit \"" + kitName + "\"");
                throw new GameOpenException(new LiteralText("unknown kit"));
            }
        };
    }
}
