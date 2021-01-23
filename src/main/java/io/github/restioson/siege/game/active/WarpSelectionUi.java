package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.map.SiegeFlag;
import io.github.restioson.siege.game.map.SiegeMap;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.shop.ShopEntry;
import xyz.nucleoid.plasmid.shop.ShopUi;
import xyz.nucleoid.plasmid.util.ColoredBlocks;

import java.util.function.Consumer;

public final class WarpSelectionUi {
    public static ShopUi create(ServerWorld world, SiegeMap map, GameTeam team, Consumer<SiegeFlag> select) {
        long time = world.getTime();
        return ShopUi.create(new LiteralText("Warp to a Point"), shop -> {
            for (SiegeFlag flag : map.flags) {
                if (flag.team != team) {
                    continue;
                }

                MutableText name = new LiteralText(flag.name);
                name = name.formatted(flag.team.getFormatting());

                ItemStack icon = flag.icon;
                if (icon == null) {
                    icon = new ItemStack(ColoredBlocks.wool(flag.team.getDye()));
                }

                if (flag.isFrontLine(time)) {
                    icon.addEnchantment(null, 0);
                }

                shop.add(ShopEntry.ofIcon(icon).withName(name).noCost().onBuy(player -> {
                    select.accept(flag);
                    player.closeHandledScreen();
                }));
            }
        });
    }
}
