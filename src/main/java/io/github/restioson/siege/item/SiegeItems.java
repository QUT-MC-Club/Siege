package io.github.restioson.siege.item;

import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import io.github.restioson.siege.Siege;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SiegeItems {
    public static final Item HORN = register("captains_horn", new SiegeHorn(new Item.Settings()));

    public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
            .displayName(Text.translatable("gameType.siege.siege"))
            .icon(HORN::getDefaultStack)
            .entries((context, entries) -> entries.add(HORN))
            .build();

    @SuppressWarnings("SameParameterValue") // Keep this general, just in case
    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Siege.ID, path), item);
    }

    public static void register() {
        PolymerItemGroupUtils.registerPolymerItemGroup(new Identifier(Siege.ID, "general"), ITEM_GROUP);
    }
}
