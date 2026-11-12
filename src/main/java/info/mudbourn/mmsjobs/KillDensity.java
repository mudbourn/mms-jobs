package info.mudbourn.mmsjobs;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Scales combat XP down when a player keeps killing in the same spot, as a mob farm does.
public final class KillDensity {
    private static final long WINDOW_TICKS = 6000;
    private static final double RADIUS_SQUARED = 16.0 * 16.0;
    private static final int FULL_PAY_KILLS = 12;
    private static final int NO_PAY_KILLS = 36;
    private static final int MAX_TRACKED = 256;

    private record Kill(Vec3 pos, long time) {}

    private static final Map<UUID, Deque<Kill>> KILLS = new ConcurrentHashMap<>();

    private KillDensity() {}

    // Logs a kill at a position and returns the XP scale for it, from 1 down to 0.
    public static double record(UUID player, Vec3 pos, long gameTime) {
        Deque<Kill> kills = KILLS.computeIfAbsent(player, id -> new ArrayDeque<>());
        synchronized (kills) {
            double scale = scaleFor(kills, pos, gameTime);
            kills.addLast(new Kill(pos, gameTime));
            if (kills.size() > MAX_TRACKED) kills.removeFirst();
            return scale;
        }
    }

    // Returns the XP scale at a position without logging a kill, for hits.
    public static double peek(UUID player, Vec3 pos, long gameTime) {
        Deque<Kill> kills = KILLS.get(player);
        if (kills == null) return 1.0;
        synchronized (kills) {
            return scaleFor(kills, pos, gameTime);
        }
    }

    private static double scaleFor(Deque<Kill> kills, Vec3 pos, long gameTime) {
        while (!kills.isEmpty() && gameTime - kills.peekFirst().time() > WINDOW_TICKS) kills.removeFirst();

        int nearby = 0;
        for (Kill kill : kills) {
            if (kill.pos().distanceToSqr(pos) <= RADIUS_SQUARED) nearby++;
        }
        if (nearby < FULL_PAY_KILLS) return 1.0;
        return Math.max(0.0, 1.0 - (double) (nearby - FULL_PAY_KILLS) / (NO_PAY_KILLS - FULL_PAY_KILLS));
    }

    public static void forget(UUID player) {
        KILLS.remove(player);
    }
}
