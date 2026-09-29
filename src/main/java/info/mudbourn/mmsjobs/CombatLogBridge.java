package info.mudbourn.mmsjobs;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Reads mms-combat's PvP toggle without a compile dependency on it.
public final class CombatLogBridge {

    private static final Logger LOG = LoggerFactory.getLogger("mms_compat");
    private static final String MANAGER = "info.mudbourn.mmscombat.combatlog.CombatManager";

    private static boolean resolved;
    private static Object manager;
    private static MethodHandle isPvpEnabled;

    private CombatLogBridge() {}

    // Whether a player has PvP on, or true when mms-combat is absent so nothing is gated.
    public static boolean isPvpEnabled(ServerPlayer player) {
        if (!resolved) resolve();
        if (isPvpEnabled == null) return true;
        try {
            return (boolean) isPvpEnabled.invoke(manager, player);
        } catch (Throwable t) {
            return true;
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded("mms_combat")) return;
        try {
            Class<?> type = Class.forName(MANAGER);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            manager = lookup.findStatic(type, "get", MethodType.methodType(type)).invoke();
            isPvpEnabled = lookup.findVirtual(
                type,
                "isPvpEnabled",
                MethodType.methodType(boolean.class, ServerPlayer.class));
        } catch (Throwable t) {
            LOG.warn("[mms_compat] mms-combat is loaded but its PvP toggle could not be resolved", t);
        }
    }
}
