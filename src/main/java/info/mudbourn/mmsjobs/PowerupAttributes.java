package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.function.Predicate;

/**
 * Job powerups that work as transient attribute modifiers, refreshed every tick.
 */
public final class PowerupAttributes {

    private record Bonus(
        String job,
        String powerup,
        Holder<Attribute> attribute,
        AttributeModifier.Operation operation,
        double rankOne,
        double rankTwo,
        double rankThree,
        Predicate<ServerPlayer> applies
    ) {
        Identifier modifierId() {
            return Identifier.fromNamespaceAndPath("mms_jobs", powerup);
        }
    }

    private static final List<Bonus> BONUSES = List.of(
        new Bonus(
            "explorer",
            "sprint_efficiency",
            Attributes.MOVEMENT_SPEED,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
            0.20,
            0.40,
            0.60,
            ServerPlayer::isSprinting
        ),
        new Bonus(
            "explorer",
            "swim_speed",
            Attributes.WATER_MOVEMENT_EFFICIENCY,
            AttributeModifier.Operation.ADD_VALUE,
            0.33,
            0.66,
            1.0,
            player -> true
        ),
        new Bonus(
            "rogue",
            "sneak_speed",
            Attributes.SNEAKING_SPEED,
            AttributeModifier.Operation.ADD_VALUE,
            0.15,
            0.30,
            0.45,
            player -> true
        ),
        new Bonus(
            "explorer",
            "lucky_finds",
            Attributes.LUCK,
            AttributeModifier.Operation.ADD_VALUE,
            2.0,
            4.0,
            6.0,
            player -> true
        )
    );

    private PowerupAttributes() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                for (Bonus bonus : BONUSES) {
                    apply(player, bonus);
                }
            }
        });
    }

    private static void apply(ServerPlayer player, Bonus bonus) {
        AttributeInstance instance = player.getAttribute(bonus.attribute());
        if (instance == null) return;

        double amount = bonus.applies().test(player) ? amountFor(player, bonus) : 0.0;
        AttributeModifier current = instance.getModifier(bonus.modifierId());

        if (amount <= 0.0) {
            if (current != null) instance.removeModifier(bonus.modifierId());
            return;
        }

        if (current == null || current.amount() != amount) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(bonus.modifierId(), amount, bonus.operation()));
        }
    }

    private static double amountFor(ServerPlayer player, Bonus bonus) {
        return switch (PowerupRanks.rank(player, bonus.job(), bonus.powerup())) {
            case 4 -> bonus.rankThree() + (bonus.rankThree() - bonus.rankTwo());
            case 3 -> bonus.rankThree();
            case 2 -> bonus.rankTwo();
            case 1 -> bonus.rankOne();
            default -> 0.0;
        };
    }
}
