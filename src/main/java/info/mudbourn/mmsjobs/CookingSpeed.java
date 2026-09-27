package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Cook Cooking Speed: furnaces and smokers last opened by a Cook with the
 * powerup cook food 25% or 50% faster.
 */
public final class CookingSpeed {

    /**
     * Added to furnace block entities to hold the Cook rank of whoever last
     * opened them.
     */
    public interface Station {
        int mmsJobs$getCookRank();

        void mmsJobs$setCookRank(int rank);
    }

    private CookingSpeed() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() && level.getBlockEntity(hit.getBlockPos()) instanceof Station station) {
                opened(station, player);
            }
            return InteractionResult.PASS;
        });
    }

    private static void opened(Station station, Player player) {
        station.mmsJobs$setCookRank(PowerupRanks.rank(player, "cook", "cooking_speed"));
    }

    public static int cookTime(AbstractFurnaceBlockEntity furnace, int cookTime) {
        if (!(furnace instanceof Station station)) return cookTime;
        if (!furnace.getItem(0).has(DataComponents.FOOD)) return cookTime;

        return switch (station.mmsJobs$getCookRank()) {
            case 2 -> Math.max(1, Math.round(cookTime / 1.5f));
            case 1 -> Math.max(1, Math.round(cookTime / 1.25f));
            default -> cookTime;
        };
    }
}
