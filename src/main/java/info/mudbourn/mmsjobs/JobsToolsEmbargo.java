package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Creative-menu embargo strip for the JobsPlusTools weapon duplicates.
 * Removes the longsword and compound-bow variants from creative tabs and
 * creative search. Uses a static ID list (not a datapack tag) because item
 * tags aren't guaranteed loaded when creative tabs first build. Keep in sync
 * with the #c:hidden_from_recipe_viewers tag (same ids).
 */
public class JobsToolsEmbargo {

    // Keep in sync with data/c/tags/item/hidden_from_recipe_viewers.json.
    private static final Set<Identifier> EMBARGO = Set.of(
        // jobsplustools longswords (7)
        Identifier.parse("jobsplustools:copper_longsword"),
        Identifier.parse("jobsplustools:diamond_longsword"),
        Identifier.parse("jobsplustools:golden_longsword"),
        Identifier.parse("jobsplustools:iron_longsword"),
        Identifier.parse("jobsplustools:netherite_longsword"),
        Identifier.parse("jobsplustools:stone_longsword"),
        Identifier.parse("jobsplustools:wooden_longsword"),
        // jobsplustools compound bows (7)
        Identifier.parse("jobsplustools:copper_compound_bow"),
        Identifier.parse("jobsplustools:diamond_compound_bow"),
        Identifier.parse("jobsplustools:golden_compound_bow"),
        Identifier.parse("jobsplustools:iron_compound_bow"),
        Identifier.parse("jobsplustools:netherite_compound_bow"),
        Identifier.parse("jobsplustools:stone_compound_bow"),
        Identifier.parse("jobsplustools:wooden_compound_bow")
    );

    public static void register() {
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((group, entries) -> {
            entries.getDisplayStacks().removeIf(JobsToolsEmbargo::isEmbargoed);
            entries.getSearchTabStacks().removeIf(JobsToolsEmbargo::isEmbargoed);
        });
    }

    // Package-external callers: JobsToolsMobEquipmentMixin reuses this so mob
    // equipment and the creative tabs cannot disagree about what is embargoed.
    public static boolean isEmbargoed(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return EMBARGO.contains(id);
    }
}
