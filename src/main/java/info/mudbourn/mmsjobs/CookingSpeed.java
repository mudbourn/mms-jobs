package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Cook Cooking Speed and Smith Smelting Speed: furnaces last opened by a Cook
 * cook food faster, and those last opened by a Smith smelt everything else faster.
 */
public final class CookingSpeed {

    /**
     * Added to furnace block entities to hold the Cook and Smith ranks of
     * whoever last opened them.
     */
    public interface Station {
        int mmsJobs$getCookRank();

        void mmsJobs$setCookRank(int rank);

        int mmsJobs$getSmithRank();

        void mmsJobs$setSmithRank(int rank);
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
        station.mmsJobs$setSmithRank(PowerupRanks.rank(player, "smith", "smelting_speed"));
    }

    public static int cookTime(AbstractFurnaceBlockEntity furnace, int cookTime) {
        if (!(furnace instanceof Station station)) return cookTime;
        boolean food = furnace.getItem(0).has(DataComponents.FOOD);
        int rank = food ? station.mmsJobs$getCookRank() : station.mmsJobs$getSmithRank();

        return switch (rank) {
            case 4 -> Math.max(1, Math.round(cookTime / 2.0f));
            case 3 -> Math.max(1, Math.round(cookTime / 1.75f));
            case 2 -> Math.max(1, Math.round(cookTime / 1.5f));
            case 1 -> Math.max(1, Math.round(cookTime / 1.25f));
            default -> cookTime;
        };
    }
}
