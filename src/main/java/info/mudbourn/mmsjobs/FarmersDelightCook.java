package info.mudbourn.mmsjobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

// Cook XP for Farmer's Delight cooking, called from the Farmer's Delight mixins.
public final class FarmersDelightCook {

    // Player who last loaded each skillet or stove
    private static final Map<BlockEntity, UUID> OPERATORS = Collections.synchronizedMap(new WeakHashMap<>());

    private FarmersDelightCook() {}

    // Pays a cook for a meal taken from a cooking pot.
    public static void potMeal(Entity player) {
        if (player instanceof ServerPlayer cook) {
            JobXpGrants.grant(cook, "cook", JobXpGrants.EASY, JobXpSource.COOK_MEAL);
        }
    }

    // Pays a cook for a cut on a cutting board.
    public static void cut(Entity player) {
        if (player instanceof ServerPlayer cook) {
            JobXpGrants.grant(cook, "cook", JobXpGrants.EASY, JobXpSource.CUT_INGREDIENT);
        }
    }

    // Remembers who loaded a skillet or stove.
    public static void loaded(BlockEntity station, Entity player) {
        if (player instanceof ServerPlayer) {
            OPERATORS.put(station, player.getUUID());
        }
    }

    // Pays whoever loaded a skillet or stove when it finishes a food item.
    public static void finished(BlockEntity station) {
        UUID operator = OPERATORS.get(station);
        if (operator == null || !(station.getLevel() instanceof ServerLevel level)) return;

        ServerPlayer cook = level.getServer().getPlayerList().getPlayer(operator);
        if (cook != null) {
            JobXpGrants.grant(cook, "cook", JobXpGrants.EASY, JobXpSource.COOK_MEAL);
        }
    }
}
