package io.github.restioson.siege.game.active;

import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.map.SiegeFlag;
import io.github.restioson.siege.game.map.SiegeMap;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.shop.ShopEntry;
import xyz.nucleoid.plasmid.util.ColoredBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WarpSelectionUi extends SimpleGui {
    private WarpSelectionUi(ServerPlayerEntity player, List<GuiElementInterface> selectors, Text title) {
        super(ScreenHandlerType.GENERIC_9X3, player, false);
        this.setTitle(title);
        selectors.forEach(this::addSlot);
    }

    public static WarpSelectionUi createFlagWarp(ServerPlayerEntity player, SiegeMap map, GameTeam team,
                                                 Consumer<SiegeFlag> select) {
        var selectors = flagSelectors(player, map, team, select);
        return new WarpSelectionUi(player, selectors, Text.translatable("game.siege.warp.flag"));
    }

    public static WarpSelectionUi createKitSelect(ServerPlayerEntity player, @Nullable SiegeKit selectedKit,
                                                  Consumer<SiegeKit> select) {
        var selectors = kitSelectors(selectedKit, select);
        return new WarpSelectionUi(player, selectors, Text.translatable("game.siege.warp.kit"));
    }

    private static List<GuiElementInterface> kitSelectors(@Nullable SiegeKit selectedKit, Consumer<SiegeKit> select) {
        List<GuiElementInterface> selectors = new ArrayList<>();

        for (SiegeKit kit : SiegeKit.KITS) {
            ItemStack icon = kit.icon.getDefaultStack();

            if (selectedKit == kit) {
                icon.addEnchantment(null, 0);
            }

            var entry = ShopEntry.ofIcon(icon)
                    .withName(kit.getName())
                    .noCost()
                    .onBuy(p -> {
                        select.accept(kit);
                        p.closeHandledScreen();
                    });

            for (var desc : kit.getDescription()) {
                entry.addLore(desc);
            }

            selectors.add(entry);
        }

        return selectors;
    }

    private static List<GuiElementInterface> flagSelectors(ServerPlayerEntity player, SiegeMap map, GameTeam team,
                                                           Consumer<SiegeFlag> select) {
        List<GuiElementInterface> selectors = new ArrayList<>();

        long time = player.getWorld().getTime();

        for (SiegeFlag flag : map.flags) {
            if (flag.team != team) {
                continue;
            }

            MutableText name = Text.literal(flag.name);
            name = name.formatted(team.config().chatFormatting());

            ItemStack icon = flag.icon;
            if (icon == null) {
                icon = new ItemStack(ColoredBlocks.wool(team.config().blockDyeColor()));
            }

            if (flag.isFrontLine(time)) {
                icon.addEnchantment(null, 0);
            }

            selectors.add(ShopEntry.ofIcon(icon).withName(name).noCost().onBuy(p -> {
                select.accept(flag);
                p.closeHandledScreen();
            }));
        }

        return selectors;
    }
}
