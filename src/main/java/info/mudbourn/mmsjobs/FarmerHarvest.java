package info.mudbourn.mmsjobs;

import com.daqem.arc.api.event.ArcBlockEvent;
import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

// Farmer job right-click harvest: mature crops drop and replant, a held hoe widens the area by job level.
public final class FarmerHarvest {
    private static final TagKey<Block> BLACKLIST =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("mms_jobs", "harvest_blacklist"));
    private static final Identifier FARMER = Identifier.fromNamespaceAndPath("jobsplus", "farmer");
    private static final float EXHAUSTION = 0.005F;
    private static final int PLUS_LEVEL = 25;
    private static final int SQUARE_LEVEL = 50;
    private static final int DIAMOND_LEVEL = 75;

    private FarmerHarvest() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            Job farmer = player instanceof JobsPlayer jobsPlayer ? jobsPlayer.jobsplus$getJob(FARMER) : null;
            if (farmer == null) return InteractionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            BlockState state = serverLevel.getBlockState(pos);
            if (state.is(BLACKLIST)) return InteractionResult.PASS;

            ItemStack tool = serverPlayer.getMainHandItem();
            if (isTall(state)) return harvestTall(serverLevel, serverPlayer, pos, state, tool, hit.getDirection());
            if (!isMature(state)) return InteractionResult.PASS;

            boolean hoe = tool.is(ItemTags.HOES);
            int harvested = harvestCrop(serverLevel, serverPlayer, pos, state, tool, hoe) ? 1 : 0;
            if (hoe && !(state.getBlock() instanceof CocoaBlock)) {
                for (BlockPos near : area(pos, farmer.getLevel())) {
                    BlockState nearState = serverLevel.getBlockState(near);
                    if (nearState.is(BLACKLIST) || !isMature(nearState) || nearState.getBlock() instanceof CocoaBlock) continue;
                    if (harvestCrop(serverLevel, serverPlayer, near, nearState, tool, true)) harvested++;
                    if (tool.isEmpty()) break;
                }
            }
            if (harvested == 0) return InteractionResult.FAIL;

            serverLevel.playSound(
                null,
                pos,
                state.getBlock() instanceof NetherWartBlock ? SoundEvents.NETHER_WART_PLANTED : SoundEvents.CROP_PLANTED,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
            );
            return InteractionResult.SUCCESS_SERVER;
        });
    }

    // Extra crop positions a hoe reaches at a Farmer level: none, a plus, a 3x3 square or a radius-2 diamond.
    private static List<BlockPos> area(BlockPos center, int jobLevel) {
        int radius = jobLevel >= DIAMOND_LEVEL ? 2 : jobLevel >= PLUS_LEVEL ? 1 : 0;
        boolean square = jobLevel >= SQUARE_LEVEL && jobLevel < DIAMOND_LEVEL;
        List<BlockPos> found = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) continue;
                if (!square && Math.abs(x) + Math.abs(z) > radius) continue;
                found.add(center.offset(x, 0, z));
            }
        }
        return found;
    }

    private static boolean isTall(BlockState state) {
        return state.getBlock() instanceof SugarCaneBlock || state.getBlock() instanceof CactusBlock;
    }

    private static boolean isMature(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        return false;
    }

    private static BlockState replanted(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.getStateForAge(0);
        if (block instanceof NetherWartBlock) return state.setValue(NetherWartBlock.AGE, 0);
        return state.setValue(CocoaBlock.AGE, 0);
    }

    // Harvests one mature crop: drops minus one replant item, resets its age, and reports it to Arc.
    private static boolean harvestCrop(
        ServerLevel level,
        ServerPlayer player,
        BlockPos pos,
        BlockState state,
        ItemStack tool,
        boolean hoe
    ) {
        if (!level.mayInteract(player, pos)) return false;
        if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos))) {
            return false;
        }

        Item seed = state.getBlock().asItem();
        boolean kept = false;
        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool)) {
            if (!kept && drop.is(seed)) {
                drop.shrink(1);
                kept = true;
            }
            Block.popResource(level, pos, drop);
        }
        state.spawnAfterBreak(level, pos, tool, true);
        level.setBlockAndUpdate(pos, replanted(state));

        player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
        player.causeFoodExhaustion(EXHAUSTION);
        if (hoe) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        if (state.getBlock() instanceof CropBlock) {
            ArcBlockEvent.HARVEST_CROP.invoker().onHarvestCrop(level, pos, state, player, null);
        }
        return true;
    }

    // Breaks a sugar cane or cactus column above its bottom block, unless the player is placing more on top.
    private static InteractionResult harvestTall(
        ServerLevel level,
        ServerPlayer player,
        BlockPos clicked,
        BlockState state,
        ItemStack held,
        Direction face
    ) {
        Block block = state.getBlock();
        if (face == Direction.UP && held.is(block.asItem())) return InteractionResult.PASS;

        BlockPos bottom = clicked;
        while (level.getBlockState(bottom.below()).is(block)) bottom = bottom.below();
        BlockPos cut = bottom.above();
        if (!level.getBlockState(cut).is(block) || !level.mayInteract(player, cut)) return InteractionResult.PASS;
        if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, cut, state, null)) {
            return InteractionResult.FAIL;
        }

        level.destroyBlock(cut, true, player);
        player.awardStat(Stats.BLOCK_MINED.get(block));
        player.causeFoodExhaustion(EXHAUSTION);
        return InteractionResult.SUCCESS_SERVER;
    }
}
