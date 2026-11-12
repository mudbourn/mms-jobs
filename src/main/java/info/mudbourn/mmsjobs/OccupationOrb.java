package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OccupationOrb extends Item {
    private static final long CONFIRM_WINDOW_TICKS = 200;
    private static final Map<UUID, Long> PENDING_CONFIRM = new ConcurrentHashMap<>();

    public static Item ITEM;

    public OccupationOrb(Properties properties) {
        super(properties);
    }

    // Registers the badge and lists it in the Tools and Utilities tab.
    public static void register() {
        ResourceKey<Item> key = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("mms_jobs", "orb_of_occupation")
        );
        ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            key,
            new OccupationOrb(new Item.Properties().setId(key).stacksTo(16).rarity(Rarity.EPIC))
        );
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries -> entries.accept(ITEM));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (!FabricLoader.getInstance().isModLoaded("jobsplus")) {
            serverPlayer.sendSystemMessage(Component.literal("Jobs are not installed on this server.")
                .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        long now = serverPlayer.level().getGameTime();
        Long armedAt = PENDING_CONFIRM.get(serverPlayer.getUUID());
        if (armedAt == null || now - armedAt > CONFIRM_WINDOW_TICKS) {
            PENDING_CONFIRM.put(serverPlayer.getUUID(), now);
            JobRespec.preview(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        PENDING_CONFIRM.remove(serverPlayer.getUUID());
        JobRespec.respec(serverPlayer);
        player.getItemInHand(hand).consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
