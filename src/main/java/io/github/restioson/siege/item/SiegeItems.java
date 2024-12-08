package io.github.restioson.siege.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import io.github.restioson.siege.Siege;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Function;

public final class SiegeItems {
    public static final Item HORN = register("captains_horn", SiegeHorn::new);
    public static final ComponentType<List<StatusEffectInstance>> HORN_DATA = Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Siege.ID, "horn_data"),
            ComponentType.<List<StatusEffectInstance>>builder().codec(StatusEffectInstance.CODEC.listOf()).build());

    public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
            .displayName(Text.translatable("gameType.siege.siege"))
            .icon(HORN::getDefaultStack)
            .entries((context, entries) -> entries.add(HORN))
            .build();

    @SuppressWarnings("SameParameterValue") // Keep this general, just in case
    private static <T extends Item> T register(String path, Function<Item.Settings, T> item) {
        return Registry.register(Registries.ITEM, Identifier.of(Siege.ID, path), item.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Siege.ID, path)))));
    }

    public static void register() {
        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(Siege.ID, "general"), ITEM_GROUP);
        PolymerComponent.registerDataComponent(HORN_DATA);
    }
}
