package info.mudbourn.mmsjobs;

import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

// Reads a player's Origins class through the Origins API; only touched when Origins is loaded.
final class OriginsClassReader {
    private static final Identifier CLASS_LAYER = Identifier.fromNamespaceAndPath("origins-classes", "class");

    private OriginsClassReader() {}

    // The path of the player's origins-classes origin, or null when they have none.
    static String classOf(ServerPlayer player) {
        OriginLayer layer;
        try {
            layer = OriginLayers.getLayer(CLASS_LAYER);
        } catch (RuntimeException e) {
            return null;
        }
        if (layer == null) return null;

        OriginComponent component = ModComponents.ORIGIN.get(player);
        if (!component.hasOrigin(layer)) return null;

        Origin origin = component.getOrigin(layer);
        Identifier id = origin == null ? null : origin.getIdentifier();
        return id != null && "origins-classes".equals(id.getNamespace()) ? id.getPath() : null;
    }
}
