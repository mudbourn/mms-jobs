package info.mudbourn.mmsjobs.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Reads the grindstone's two input slots.
@Mixin(GrindstoneMenu.class)
public interface GrindstoneMenuAccessor {

    @Accessor("repairSlots")
    Container mmsJobs$getRepairSlots();
}
