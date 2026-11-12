package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Explorer XP for distance covered by elytra or on a mount.
public final class ExplorerTravel {
    private static final double BLOCKS_PER_GRANT = 200;
    private static final double MAX_STEP = 200;

    // Last checked position and distance travelled since the last grant
    private record Trip(Vec3 last, double distance) {}

    private static final Map<UUID, Trip> TRIPS = new ConcurrentHashMap<>();

    private ExplorerTravel() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                track(player);
            }
        });
    }

    public static void forget(UUID playerId) {
        TRIPS.remove(playerId);
    }

    private static void track(ServerPlayer player) {
        Vec3 now = player.position();
        if (!player.isFallFlying() && !player.isPassenger()) {
            TRIPS.remove(player.getUUID());
            return;
        }

        Trip trip = TRIPS.get(player.getUUID());
        double step = trip == null ? 0 : now.subtract(trip.last()).horizontalDistance();
        if (step > MAX_STEP) step = 0;
        double distance = (trip == null ? 0 : trip.distance()) + step;
        if (distance >= BLOCKS_PER_GRANT) {
            distance -= BLOCKS_PER_GRANT;
            JobXpGrants.grant(player, "explorer", JobXpGrants.EASY, JobXpSource.TRAVEL);
        }
        TRIPS.put(player.getUUID(), new Trip(now, distance));
    }
}
