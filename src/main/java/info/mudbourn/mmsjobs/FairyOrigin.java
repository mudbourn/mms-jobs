package info.mudbourn.mmsjobs;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

// Whether a player's Origins origin is the mms_origins Fairy.
public final class FairyOrigin {
    private static final boolean ORIGINS_LOADED = FabricLoader.getInstance().isModLoaded("origins-legacy");

    private FairyOrigin() {}

    // True when the player's origin layer holds mms_origins:fairy.
    public static boolean is(ServerPlayer player) {
        return ORIGINS_LOADED && OriginsClassReader.isFairy(player);
    }
}
