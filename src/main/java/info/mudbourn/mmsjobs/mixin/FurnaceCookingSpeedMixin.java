package info.mudbourn.mmsjobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import info.mudbourn.mmsjobs.CookingSpeed;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Shortens cook times in furnaces a Cook or Smith last opened.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceCookingSpeedMixin implements CookingSpeed.Station {

    @Unique
    private int mmsJobs$cookRank;

    @Unique
    private int mmsJobs$smithRank;

    @Override
    public int mmsJobs$getCookRank() {
        return mmsJobs$cookRank;
    }

    @Override
    public void mmsJobs$setCookRank(int rank) {
        mmsJobs$cookRank = rank;
    }

    @Override
    public int mmsJobs$getSmithRank() {
        return mmsJobs$smithRank;
    }

    @Override
    public void mmsJobs$setSmithRank(int rank) {
        mmsJobs$smithRank = rank;
    }

    @ModifyReturnValue(method = "getTotalCookTime", at = @At("RETURN"))
    private static int mmsJobs$cookFaster(int cookTime, @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        return CookingSpeed.cookTime(furnace, cookTime);
    }
}
