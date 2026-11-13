package info.mudbourn.mmsjobs;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// Cook XP for Brewin' and Chewin' keg results, called from the Brewin' and Chewin' mixins.
public final class BrewinChewinKeg {

    private static final String KEG_MENU = "umpaz.brewinandchewin.common.block.entity.container.KegMenu";
    private static final int RESULT_SLOT = 5;

    private BrewinChewinKeg() {}

    // Pays a cook when a keg hands out something to eat or drink.
    public static void extracted(Player player, List<ItemStack> items) {
        if (items.stream().anyMatch(BrewinChewinKeg::consumable)) {
            poured(player);
        }
    }

    // Pays a cook for a drink poured from a keg.
    public static void poured(Player player) {
        if (player instanceof ServerPlayer cook) {
            JobXpGrants.grant(cook, "cook", JobXpGrants.EASY, JobXpSource.COOK_MEAL);
        }
    }

    // Pays a cook for a result taken from the keg result slot.
    public static void resultTaken(Slot slot, Player player, ItemStack stack) {
        if (!consumable(stack) || !KEG_MENU.equals(player.containerMenu.getClass().getName())) return;
        if (player.containerMenu.slots.get(RESULT_SLOT) == slot) {
            poured(player);
        }
    }

    private static boolean consumable(ItemStack stack) {
        return stack.has(DataComponents.CONSUMABLE);
    }
}
