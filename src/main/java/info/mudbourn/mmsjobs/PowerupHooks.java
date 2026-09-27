package info.mudbourn.mmsjobs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Rogue Mob Awareness and Rancher Shearing Yield, called from their mixins.
 */
public final class PowerupHooks {

    private PowerupHooks() {}

    /** Scales how visible a sneaking Rogue is to mobs looking for a target. */
    public static double visibility(LivingEntity entity, double visibility) {
        if (!(entity instanceof ServerPlayer player) || !player.isDiscrete()) return visibility;

        return switch (PowerupRanks.rank(player, "rogue", "mob_awareness")) {
            case 2 -> visibility * 0.5;
            case 1 -> visibility * 0.75;
            default -> visibility;
        };
    }

    /** Rolls a bonus wool of the sheep's own colour after a Rancher shears it. */
    public static void sheared(Sheep sheep, Player player) {
        if (!(sheep.level() instanceof ServerLevel level)) return;

        float chance = switch (PowerupRanks.rank(player, "rancher", "shearing_yield")) {
            case 2 -> 0.25f;
            case 1 -> 0.15f;
            default -> 0.0f;
        };
        if (chance == 0.0f || sheep.getRandom().nextFloat() >= chance) return;

        Identifier wool = Identifier.withDefaultNamespace(sheep.getColor().getSerializedName() + "_wool");
        sheep.spawnAtLocation(level, new ItemStack(BuiltInRegistries.ITEM.getValue(wool)), 1.0f);
    }
}
