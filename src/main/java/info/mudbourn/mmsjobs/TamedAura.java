package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

/**
 * Beastmaster Tamed Regeneration and Tamed Resistance: effects kept on the
 * player's own tamed animals while they stay nearby.
 */
public final class TamedAura {

    private static final int INTERVAL_TICKS = 40;
    private static final int EFFECT_TICKS = 60;
    private static final double RADIUS = 16.0;

    private TamedAura() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % INTERVAL_TICKS != 0) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                apply(player);
            }
        });
    }

    private static void apply(ServerPlayer player) {
        int regeneration = PowerupRanks.rank(player, "beastmaster", "tamed_regen");
        int resistance = PowerupRanks.rank(player, "beastmaster", "tamed_resistance");
        if (regeneration == 0 && resistance == 0) return;

        for (LivingEntity pet : player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(RADIUS),
            entity -> entity.isAlive() && entity instanceof OwnableEntity owned && owned.getOwner() == player
        )) {
            give(pet, MobEffects.REGENERATION, regeneration);
            give(pet, MobEffects.RESISTANCE, resistance);
        }
    }

    private static void give(LivingEntity pet, Holder<MobEffect> effect, int rank) {
        if (rank == 0) return;

        pet.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, rank - 1, true, true));
    }
}
