package info.mudbourn.mmsjobs.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.util.List;
import java.util.Set;

/**
 * Applies mixins only when Jobs+ is loaded, and Farmer's Delight, Brewin' and Chewin', Useless Reptile and Origins hooks only when those are too.
 */
public class JobsMixinGate implements IMixinConfigPlugin {

    private final boolean present = FabricLoader.getInstance().isModLoaded("jobsplus");
    private final boolean farmersDelight = FabricLoader.getInstance().isModLoaded("farmersdelight");
    private final boolean brewinAndChewin = FabricLoader.getInstance().isModLoaded("brewinandchewin");
    private final boolean uselessReptile = FabricLoader.getInstance().isModLoaded("uselessreptile");
    private final boolean origins = FabricLoader.getInstance().isModLoaded("origins-legacy");

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!present) return false;
        if (mixinClassName.contains("FarmersDelight")) return farmersDelight;
        if (mixinClassName.contains("BrewinChewin")) return brewinAndChewin;
        if (mixinClassName.contains("Dragon")) return uselessReptile;
        if (mixinClassName.contains("Orb")) return origins;
        return true;
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return List.of(); }
    @Override public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {}
}
