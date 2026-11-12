package info.mudbourn.mmsjobs;

import com.daqem.arc.api.action.AbstractAction;
import com.daqem.arc.api.action.IActionSerializer;
import com.daqem.arc.api.action.holder.IActionHolderType;
import com.daqem.arc.api.action.IActionType;
import com.daqem.arc.api.condition.ICondition;
import com.daqem.arc.api.reward.IReward;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.List;

// A job XP source paid from Java, held as an Arc action so the Jobs+ screen lists it with its XP range.
public final class JobXpSource extends AbstractAction {
    public static final IActionType<JobXpSource> FIND_LOOT = register("on_find_loot");
    public static final IActionType<JobXpSource> BLOCK_HIT = register("on_block_hit");
    public static final IActionType<JobXpSource> BACKSTAB = register("on_backstab");
    public static final IActionType<JobXpSource> COOK_MEAL = register("on_cook_meal");
    public static final IActionType<JobXpSource> CUT_INGREDIENT = register("on_cut_ingredient");
    public static final IActionType<JobXpSource> BARTER = register("on_barter");
    public static final IActionType<JobXpSource> CURE_VILLAGER = register("on_cure_villager");
    public static final IActionType<JobXpSource> TRAVEL = register("on_travel");
    public static final IActionType<JobXpSource> LONG_SHOT = register("on_long_shot");
    public static final IActionType<JobXpSource> PET_KILL = register("on_pet_kill");
    public static final IActionType<JobXpSource> SLAY_BOSS = register("on_slay_boss");

    private final IActionType<JobXpSource> type;

    private JobXpSource(
        IActionType<JobXpSource> type,
        Identifier location,
        Identifier holderLocation,
        IActionHolderType<?> holderType,
        List<IReward> rewards,
        List<ICondition> conditions
    ) {
        super(location, holderLocation, holderType, rewards, conditions);
        this.type = type;
    }

    public static void register() {}

    @Override
    public IActionType<?> getType() {
        return type;
    }

    private static IActionType<JobXpSource> register(String path) {
        Serializer serializer = new Serializer();
        serializer.type = IActionType.register(Identifier.fromNamespaceAndPath("mms_jobs", path), serializer);
        return serializer.type;
    }

    private static final class Serializer implements IActionSerializer<JobXpSource> {
        private IActionType<JobXpSource> type;

        @Override
        public JobXpSource fromJson(
            Identifier location,
            JsonObject json,
            Identifier holderLocation,
            IActionHolderType<?> holderType,
            List<IReward> rewards,
            List<ICondition> conditions
        ) {
            return new JobXpSource(type, location, holderLocation, holderType, rewards, conditions);
        }

        @Override
        public JobXpSource fromNetwork(
            Identifier location,
            RegistryFriendlyByteBuf buffer,
            Identifier holderLocation,
            IActionHolderType<?> holderType,
            List<IReward> rewards,
            List<ICondition> conditions
        ) {
            return new JobXpSource(type, location, holderLocation, holderType, rewards, conditions);
        }
    }
}
