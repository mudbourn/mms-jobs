package info.mudbourn.mmsjobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.content.OrbOfOriginItem;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// Keeps the Orb of Origin from resetting the class layer, so it only rerolls race layers.
@Mixin(OrbOfOriginItem.class)
public class OrbClassLockMixin {

    @Unique
    private static final Identifier mmsJobs$CLASS_LAYER = Identifier.fromNamespaceAndPath("origins-classes", "class");

    @WrapOperation(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lio/github/apace100/origins/component/OriginComponent;setOrigin(Lio/github/apace100/origins/origin/OriginLayer;Lio/github/apace100/origins/origin/Origin;)V"
        )
    )
    private void mmsJobs$keepClass(OriginComponent component, OriginLayer layer, Origin origin, Operation<Void> original) {
        if (mmsJobs$CLASS_LAYER.equals(layer.getIdentifier())) return;
        original.call(component, layer, origin);
    }
}
