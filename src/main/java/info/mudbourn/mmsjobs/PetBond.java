package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Creeper;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Beastmaster Pack Bond: each kind of nearby pet the player owns grants a buff, up to a rank-based cap.
public final class PetBond {
    private static final int REFRESH_TICKS = 40;
    private static final double PET_RANGE = 48.0;
    private static final double CAT_CREEPER_VISIBILITY = 0.4;

    private enum Kind {
        WOLF(Attributes.ATTACK_DAMAGE, 1.5, AttributeModifier.Operation.ADD_VALUE),
        CAT(null, 0.0, null),
        PARROT(Attributes.FALL_DAMAGE_MULTIPLIER, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
        STEED(Attributes.MOVEMENT_SPEED, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
        DRAGON(Attributes.MAX_HEALTH, 6.0, AttributeModifier.Operation.ADD_VALUE);

        final Holder<Attribute> attribute;
        final double amount;
        final AttributeModifier.Operation operation;

        Kind(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
        }

        Identifier modifierId() {
            return Identifier.fromNamespaceAndPath("mms_jobs", "pack_bond_" + name().toLowerCase());
        }
    }

    private static final Map<UUID, Set<Kind>> ACTIVE = new ConcurrentHashMap<>();

    private PetBond() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % REFRESH_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                refresh(player);
            }
        });
    }

    // Lowers how visible a Beastmaster bonded with a cat is to creepers.
    public static double visibility(LivingEntity entity, Entity looker, double visibility) {
        if (!(looker instanceof Creeper)) return visibility;
        Set<Kind> kinds = ACTIVE.get(entity.getUUID());
        return kinds != null && kinds.contains(Kind.CAT) ? visibility * CAT_CREEPER_VISIBILITY : visibility;
    }

    public static void forget(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    private static void refresh(ServerPlayer player) {
        int cap = switch (PowerupRanks.rank(player, "beastmaster", "pack_bond")) {
            case 4, 3 -> Kind.values().length;
            case 2 -> 3;
            case 1 -> 1;
            default -> 0;
        };

        Set<Kind> active = EnumSet.noneOf(Kind.class);
        if (cap > 0) {
            Map<Kind, Integer> counts = new EnumMap<>(Kind.class);
            for (Entity pet : player.level().getEntities(
                player,
                player.getBoundingBox().inflate(PET_RANGE),
                entity -> entity instanceof OwnableEntity ownable && ownable.getOwner() == player
            )) {
                Kind kind = kindOf(pet);
                if (kind != null) counts.merge(kind, 1, Integer::sum);
            }
            counts.entrySet().stream()
                .sorted(Map.Entry.<Kind, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(cap)
                .forEach(entry -> active.add(entry.getKey()));
        }

        if (active.isEmpty()) {
            ACTIVE.remove(player.getUUID());
        } else {
            ACTIVE.put(player.getUUID(), active);
        }
        for (Kind kind : Kind.values()) {
            if (kind.attribute != null) apply(player, kind, active.contains(kind));
        }
    }

    private static Kind kindOf(Entity pet) {
        if (pet instanceof Wolf) return Kind.WOLF;
        if (pet instanceof Cat) return Kind.CAT;
        if (pet instanceof Parrot) return Kind.PARROT;
        if (pet instanceof AbstractHorse) return Kind.STEED;
        if (DragonBond.isDragon(pet)) return Kind.DRAGON;
        return null;
    }

    private static void apply(ServerPlayer player, Kind kind, boolean on) {
        AttributeInstance instance = player.getAttribute(kind.attribute);
        if (instance == null) return;

        if (!on) {
            instance.removeModifier(kind.modifierId());
            return;
        }
        if (instance.getModifier(kind.modifierId()) == null) {
            instance.addTransientModifier(new AttributeModifier(kind.modifierId(), kind.amount, kind.operation));
        }
    }
}
