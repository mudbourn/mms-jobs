# mms-jobs design

## Jobs+ Tools fold-in

Jobs+ Tools (`jobsplustools`) is removed from the pack. Its tools are folded
into the jobs as perks on vanilla tools, earned through job levels, with no
new items. Its art is not used.

### Moved over

| Addon tool | Job | Perk | Unlock |
| --- | --- | --- | --- |
| Hammer (3x3 breaker) | Miner | `MinerAreaBreak`: sneak while mining with a pickaxe to break a 3x3 square flat to the face you look at | level 25, 5x5 at 75 |
| Excavator (3x3 shovel) | Miner | `MinerAreaBreak` with a shovel on shovel blocks | level 25, 5x5 at 75 |
| Wrench, arms toggle | Smith | `SmithWrench`: sneak and use an armor stand with an empty hand to toggle its arms | any level |
| Wrench, armor stand repair | Smith | `SmithWrench`: the same action repairs the stand's damaged gear for 1 experience point per 2 durability, per 3 from level 60 | level 25 |

Area blocks must be no harder than the mined block and suited to the tool;
each block costs durability through the normal break path. Sneaking also turns
off vein mining, so the two never stack.

### Skipped as duplicates

| Addon tool | Covered by |
| --- | --- |
| Hammer connected-block mode | `VeinBreaker` vein mining (Miner) |
| Hatchet tree breaker | `VeinBreaker` tree felling (Lumberjack) |
| Harvester crop replanting and area | `FarmerHarvest` (Farmer) |
| Longswords (7) | mms-weapons replacements |
| Compound bows (7) | mms-weapons replacements |

### Dropped, no matching job

| Addon tool | Addon job |
| --- | --- |
| Trowel (multi-block placer and replacer) | Builder |
| Experience jar | Enchanter |
| Fishing rod (multi-bobber) | Fisherman |
| Wand (stored and fired potions) | Alchemist; no replacement built |
| Cores and grips | crafting parts for the removed tools |
