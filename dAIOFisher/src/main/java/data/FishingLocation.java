package data;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;

import java.util.List;
import java.util.Set;

public enum FishingLocation {
    Barb_Village(
            new RectangleArea(3099, 3424, 10, 12, 0), // Main fishing area
            List.of(
                    new RectangleArea(3107, 3432, 2, 2, 0),
                    new RectangleArea(3101, 3424, 2, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(3091, 3488, 7, 11, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.BANK, HandlingMode.DROP, HandlingMode.COOK, HandlingMode.COOKnBANK),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(3104, 3425, 0),
                                    new WorldPosition(3104, 3424, 0),
                                    new WorldPosition(3110, 3432, 0),
                                    new WorldPosition(3110, 3433, 0),
                                    new WorldPosition(3110, 3434, 0)
                            ),
                            "Fire",
                            "Cook",
                            "Bank booth",
                            "Bank",
                            FishingMethod.BankObjectType.BANK,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.BANK, HandlingMode.DROP, HandlingMode.COOK, HandlingMode.COOKnBANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(3104, 3425, 0),
                                    new WorldPosition(3104, 3424, 0),
                                    new WorldPosition(3110, 3432, 0),
                                    new WorldPosition(3110, 3433, 0),
                                    new WorldPosition(3110, 3434, 0)
                            ),
                            "Fire",
                            "Cook",
                            "Bank booth",
                            "Bank",
                            FishingMethod.BankObjectType.BANK,
                            30000
                    )
            )
    ),
    Ottos_Grotto(
            new RectangleArea(2495, 3490, 15, 38, 0), // Main fishing area
            List.of(
                    new RectangleArea(2502, 3492, 3, 3, 0),
                    new RectangleArea(2497, 3503, 3, 10, 0),
                    new RectangleArea(2499, 3515, 7, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(2530, 3568, 7, 9, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Barbarian Rod (Feathers)",
                            List.of(ItemID.LEAPING_TROUT, ItemID.LEAPING_SALMON, ItemID.LEAPING_STURGEON),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BARBARIAN_ROD, ItemID.FEATHER),
                            List.of(),
                            List.of(),
                            "Use-rod",
                            Set.of(
                                    // North
                                    new WorldPosition(2504, 3516, 0),

                                    // West
                                    new WorldPosition(2500, 3512, 0),
                                    new WorldPosition(2500, 3510, 0),
                                    new WorldPosition(2500, 3509, 0),
                                    new WorldPosition(2500, 3507, 0),
                                    new WorldPosition(2500, 3506, 0),

                                    // South
                                    new WorldPosition(2504, 3499, 0),
                                    new WorldPosition(2504, 3498, 0),
                                    new WorldPosition(2504, 3497, 0),
                                    new WorldPosition(2506, 3494, 0),
                                    new WorldPosition(2506, 3493, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            20000
                    )
            )
    ),
    Mount_Quidamortem_CoX(
            new RectangleArea(1251, 3540, 24, 11, 0), // Main fishing area
            List.of(
                    new RectangleArea(1251, 3541, 5, 4, 0),
                    new RectangleArea(1260, 3541, 8, 4, 0),
                    new RectangleArea(1269, 3543, 5, 7, 0)
            ), // Fishing spot areas
            new RectangleArea(1249, 3565, 11, 10, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Barbarian Rod (Feathers)",
                            List.of(ItemID.LEAPING_TROUT, ItemID.LEAPING_SALMON, ItemID.LEAPING_STURGEON),
                            List.of( HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BARBARIAN_ROD, ItemID.FEATHER),
                            List.of(),
                            List.of(),
                            "Use-rod",
                            Set.of(
                                    // West
                                    new WorldPosition(1253, 3542, 0),

                                    // Middle
                                    new WorldPosition(1266, 3541, 0),
                                    new WorldPosition(1261, 3542, 0),

                                    // East
                                    new WorldPosition(1272, 3546, 0),
                                    new WorldPosition(1272, 3547, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            20000
                    )
            )
    ),
    Karamja_West(
            new RectangleArea(2799, 3007, 14, 16, 0), // Main fishing area
            List.of(
                    new RectangleArea(2806, 3015, 7, 8, 0),
                    new RectangleArea(2800, 3008, 4, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_KARAMBWANJI, ItemID.RAW_SHRIMPS),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(ItemID.RAW_SHRIMPS),
                            List.of(),
                            "Net",
                            Set.of(
                                    // East
                                    new WorldPosition(2807, 3021, 0),
                                    new WorldPosition(2810, 3017, 0),

                                    // South
                                    new WorldPosition(2801, 3010, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Small Fishing Net (SafeMode/10HP)",
                            List.of(ItemID.RAW_KARAMBWANJI, ItemID.RAW_SHRIMPS),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(ItemID.RAW_SHRIMPS),
                            List.of(),
                            "Net",
                            Set.of(
                                    // East
                                    new WorldPosition(2807, 3021, 0),
                                    new WorldPosition(2810, 3017, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    )
            )
    ),
    Shilo_Village(
            new RectangleArea(2833, 2970, 32, 2, 0), // Main fishing area
            List.of(
                    new RectangleArea(2859, 2969, 6, 3, 0),
                    new RectangleArea(2834, 2968, 11, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(2850, 2952, 4, 4, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.BANK, HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    // South east
                                    new WorldPosition(2860, 2972, 0),
                                    new WorldPosition(2865, 2972, 0),

                                    // South west
                                    new WorldPosition(2836, 2971, 0),
                                    new WorldPosition(2841, 2971, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    // South east
                                    new WorldPosition(2860, 2972, 0),
                                    new WorldPosition(2865, 2972, 0),

                                    // South west
                                    new WorldPosition(2836, 2971, 0),
                                    new WorldPosition(2841, 2971, 0)

                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            35000
                    )
            )
    ),
    Zul_Andra(
            new RectangleArea(2179, 3060, 26, 10, 0), // Main fishing area
            List.of(
                    new RectangleArea(2197, 3064, 2, 2, 0),
                    new RectangleArea(2188, 3067, 4, 2, 0),
                    new RectangleArea(2182, 3065, 3, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.SACRED_EEL),
                            List.of(HandlingMode.COOK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT, ItemID.KNIFE),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // West
                                    new WorldPosition(2181, 3067, 0),

                                    // Middle
                                    new WorldPosition(2186, 3070, 0),
                                    new WorldPosition(2192, 3070, 0),

                                    // East
                                    new WorldPosition(2200, 3066, 0),
                                    new WorldPosition(2199, 3066, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            50000
                    )
            )
    ),
    Port_Piscarilius_East(
            new RectangleArea(1823, 3769, 16, 8, 0), // Main fishing area
            List.of(
                    new RectangleArea(1824, 3771, 3, 2, 0),
                    new RectangleArea(1831, 3770, 2, 2, 0),
                    new RectangleArea(1837, 3776, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(1803, 3785, 7, 5, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Sandworms)",
                            List.of(ItemID.RAW_ANGLERFISH),
                            List.of(HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.SANDWORMS),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // South
                                    new WorldPosition(1827, 3770, 0),
                                    new WorldPosition(1826, 3770, 0),
                                    new WorldPosition(1831, 3767, 0),
                                    new WorldPosition(1836, 3771, 0),
                                    new WorldPosition(1833, 3769, 0),

                                    // East
                                    new WorldPosition(1840, 3776, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            50000
                    )
            )
    ),
    Port_Piscarilius_West(
            new RectangleArea(1744, 3793, 23, 7, 0), // Main fishing area
            List.of(
                    new RectangleArea(1760, 3794, 5, 1, 0),
                    new RectangleArea(1746, 3799, 5, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(1803, 3785, 7, 5, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    ),
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    ),
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.BIG_SWORDFISH),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    // West
                                    new WorldPosition(1747, 3802, 0),
                                    new WorldPosition(1748, 3802, 0),
                                    new WorldPosition(1749, 3802, 0),
                                    new WorldPosition(1750, 3802, 0),

                                    // East
                                    new WorldPosition(1760, 3796, 0),
                                    new WorldPosition(1761, 3796, 0),
                                    new WorldPosition(1762, 3796, 0),
                                    new WorldPosition(1763, 3796, 0),
                                    new WorldPosition(1764, 3796, 0),
                                    new WorldPosition(1765, 3796, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            50000
                    )
            )
    ),
    Kingstown(
            new RectangleArea(1719, 3684, 2, 1, 0), // Main fishing area
            List.of(
                    new RectangleArea(1720, 3684, 1, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(1720, 3683, 0),
                                    new WorldPosition(1722, 3685, 0),
                                    new WorldPosition(1721, 3683, 0),
                                    new WorldPosition(1721, 3686, 0),
                                    new WorldPosition(1720, 3686, 0),
                                    new WorldPosition(1722, 3684, 0)

                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(1720, 3683, 0),
                                    new WorldPosition(1722, 3685, 0),
                                    new WorldPosition(1721, 3683, 0),
                                    new WorldPosition(1721, 3686, 0),
                                    new WorldPosition(1720, 3686, 0),
                                    new WorldPosition(1722, 3684, 0)

                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            35000
                    )
            )
    ),
    Farming_Guild(
            new RectangleArea(1262, 3700, 17, 11, 0), // Main fishing area
            List.of(
                    new RectangleArea(1270, 3708, 4, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(1271, 3707, 0),
                                    new WorldPosition(1272, 3707, 0),
                                    new WorldPosition(1273, 3707, 0),
                                    new WorldPosition(1267, 3703, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(1271, 3707, 0),
                                    new WorldPosition(1272, 3707, 0),
                                    new WorldPosition(1273, 3707, 0),
                                    new WorldPosition(1267, 3703, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Chaos_Druid_Tower(
            new RectangleArea(2557, 3362, 12, 13, 0), // Main fishing area
            List.of(
                    new RectangleArea(2561, 3369, 3, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(2566, 3369, 0),
                                    new WorldPosition(2566, 3370, 0),
                                    new WorldPosition(2561, 3374, 0),
                                    new WorldPosition(2562, 3374, 0),
                                    new WorldPosition(2568, 3365, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2566, 3369, 0),
                                    new WorldPosition(2566, 3370, 0),
                                    new WorldPosition(2561, 3374, 0),
                                    new WorldPosition(2562, 3374, 0),
                                    new WorldPosition(2568, 3365, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Lumbridge_Goblins(
            new RectangleArea(3238, 3239, 5, 20, 0), // Main fishing area
            List.of(
                    new RectangleArea(3239, 3252, 1, 6, 0),
                    new RectangleArea(3240, 3241, 2, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    // North
                                    new WorldPosition(3238, 3255, 0),
                                    new WorldPosition(3238, 3254, 0),
                                    new WorldPosition(3238, 3253, 0),
                                    new WorldPosition(3238, 3252, 0),
                                    new WorldPosition(3238, 3251, 0),

                                    // South
                                    new WorldPosition(3239, 3243, 0),
                                    new WorldPosition(3239, 3242, 0),
                                    new WorldPosition(3239, 3241, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    // North
                                    new WorldPosition(3238, 3255, 0),
                                    new WorldPosition(3238, 3254, 0),
                                    new WorldPosition(3238, 3253, 0),
                                    new WorldPosition(3238, 3252, 0),
                                    new WorldPosition(3238, 3251, 0),

                                    // South
                                    new WorldPosition(3239, 3243, 0),
                                    new WorldPosition(3239, 3242, 0),
                                    new WorldPosition(3239, 3241, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Lumbridge_Swamp(
            new RectangleArea(3236, 3142, 9, 12, 0), // Main fishing area
            List.of(
                    new RectangleArea(3240, 3150, 3, 3, 0),
                    new RectangleArea(3239, 3142, 1, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(3246, 3155, 0),
                                    new WorldPosition(3245, 3152, 0),
                                    new WorldPosition(3244, 3150, 0),
                                    new WorldPosition(3241, 3148, 0),
                                    new WorldPosition(3242, 3148, 0),
                                    new WorldPosition(3240, 3146, 0),
                                    new WorldPosition(3240, 3147, 0),
                                    new WorldPosition(3242, 3143, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    ),
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Net",
                            Set.of(
                                    new WorldPosition(3246, 3155, 0),
                                    new WorldPosition(3245, 3152, 0),
                                    new WorldPosition(3244, 3150, 0),
                                    new WorldPosition(3241, 3148, 0),
                                    new WorldPosition(3242, 3148, 0),
                                    new WorldPosition(3240, 3146, 0),
                                    new WorldPosition(3240, 3147, 0),
                                    new WorldPosition(3242, 3143, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    )
            )
    ),
    Seers_SinclairMansion(
            new RectangleArea(2711, 3518, 24, 18, 0), // Main fishing area
            List.of(
                    new RectangleArea(2725, 3525, 4, 2, 0),
                    new RectangleArea(2715, 3531, 2, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(2723, 3490, 6, 3, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    // West
                                    new WorldPosition(2714, 3533, 0),
                                    new WorldPosition(2714, 3532, 0),
                                    new WorldPosition(2716, 3530, 0),
                                    new WorldPosition(2718, 3529, 0),

                                    // South
                                    new WorldPosition(2726, 2524, 0),
                                    new WorldPosition(2727, 2524, 0),
                                    new WorldPosition(2728, 3524, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    // West
                                    new WorldPosition(2714, 3533, 0),
                                    new WorldPosition(2714, 3532, 0),
                                    new WorldPosition(2716, 3530, 0),
                                    new WorldPosition(2718, 3529, 0),

                                    // South
                                    new WorldPosition(2726, 2524, 0),
                                    new WorldPosition(2727, 2524, 0),
                                    new WorldPosition(2728, 3524, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            30000
                    )
            )
    ),
    Rellekka_WestPier(
            new RectangleArea(2630, 3687, 2, 8, 0), // Main fishing area
            List.of(
                    new RectangleArea(2631, 3690, 1, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2633, 3693, 0),
                                    new WorldPosition(2633, 3692, 0),
                                    new WorldPosition(2633, 3691, 0),
                                    new WorldPosition(2633, 3690, 0),
                                    new WorldPosition(2633, 3689, 0),
                                    new WorldPosition(2633, 3688, 0),
                                    new WorldPosition(2633, 3687, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    ),
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    new WorldPosition(2633, 3693, 0),
                                    new WorldPosition(2633, 3692, 0),
                                    new WorldPosition(2633, 3691, 0),
                                    new WorldPosition(2633, 3690, 0),
                                    new WorldPosition(2633, 3689, 0),
                                    new WorldPosition(2633, 3688, 0),
                                    new WorldPosition(2633, 3687, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    )
            )
    ),
    Rellekka_MiddlePier(
            new RectangleArea(2640, 3691, 1, 8, 0), // Main fishing area
            List.of(
                    new RectangleArea(2640, 3694, 1, 4, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    new WorldPosition(2640, 3700, 0),
                                    new WorldPosition(2639, 3695, 0),
                                    new WorldPosition(2639, 3694, 0),
                                    new WorldPosition(2639, 3693, 0),
                                    new WorldPosition(2642, 3699, 0),
                                    new WorldPosition(2642, 3698, 0),
                                    new WorldPosition(2642, 3697, 0),
                                    new WorldPosition(2642, 3696, 0),
                                    new WorldPosition(2642, 3695, 0),
                                    new WorldPosition(2642, 3694, 0),
                                    new WorldPosition(2642, 3693, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.BIG_SWORDFISH),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2640, 3700, 0),
                                    new WorldPosition(2639, 3695, 0),
                                    new WorldPosition(2639, 3694, 0),
                                    new WorldPosition(2639, 3693, 0),
                                    new WorldPosition(2642, 3699, 0),
                                    new WorldPosition(2642, 3698, 0),
                                    new WorldPosition(2642, 3697, 0),
                                    new WorldPosition(2642, 3696, 0),
                                    new WorldPosition(2642, 3695, 0),
                                    new WorldPosition(2642, 3694, 0),
                                    new WorldPosition(2642, 3693, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            50000
                    )
            )
    ),
    Rellekka_NorthPier(
            new RectangleArea(2641, 3709, 8, 1, 0), // Main fishing area
            List.of(
                    new RectangleArea(2643, 3709, 3, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_SHARK, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2648, 3708, 0),
                                    new WorldPosition(2647, 3708, 0),
                                    new WorldPosition(2646, 3708, 0),
                                    new WorldPosition(2645, 3708, 0),
                                    new WorldPosition(2644, 3708, 0),
                                    new WorldPosition(2643, 3708, 0),
                                    new WorldPosition(2642, 3708, 0),
                                    new WorldPosition(2641, 3708, 0),
                                    new WorldPosition(2647, 3711, 0),
                                    new WorldPosition(2648, 3711, 0),
                                    new WorldPosition(2640, 3710, 0),
                                    new WorldPosition(2640, 3709, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            90000
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    new WorldPosition(2648, 3708, 0),
                                    new WorldPosition(2647, 3708, 0),
                                    new WorldPosition(2646, 3708, 0),
                                    new WorldPosition(2645, 3708, 0),
                                    new WorldPosition(2644, 3708, 0),
                                    new WorldPosition(2643, 3708, 0),
                                    new WorldPosition(2642, 3708, 0),
                                    new WorldPosition(2641, 3708, 0),
                                    new WorldPosition(2647, 3711, 0),
                                    new WorldPosition(2648, 3711, 0),
                                    new WorldPosition(2640, 3710, 0),
                                    new WorldPosition(2640, 3709, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            40000
                    )
            )
    ),
    Jatizso(
            new RectangleArea(2405, 3781, 18, 1, 0), // Main fishing area
            List.of(
                    new RectangleArea(2411, 3781, 4, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.RAW_SHARK, ItemID.BIG_SWORDFISH, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2407, 3783, 0),
                                    new WorldPosition(2408, 3783, 0),
                                    new WorldPosition(2409, 3783, 0),
                                    new WorldPosition(2410, 3783, 0),
                                    new WorldPosition(2411, 3783, 0),
                                    new WorldPosition(2412, 3783, 0),
                                    new WorldPosition(2413, 3783, 0),
                                    new WorldPosition(2414, 3783, 0),
                                    new WorldPosition(2415, 3783, 0),
                                    new WorldPosition(2416, 3783, 0),
                                    new WorldPosition(2417, 3783, 0),
                                    new WorldPosition(2418, 3783, 0),
                                    new WorldPosition(2419, 3783, 0),
                                    new WorldPosition(2420, 3783, 0),
                                    new WorldPosition(2421, 3783, 0),
                                    new WorldPosition(2422, 3783, 0),
                                    new WorldPosition(2423, 3783, 0),
                                    new WorldPosition(2424, 3781, 0),
                                    new WorldPosition(2424, 3782, 0),
                                    new WorldPosition(2406, 3780, 0),
                                    new WorldPosition(2407, 3780, 0),
                                    new WorldPosition(2408, 3780, 0),
                                    new WorldPosition(2409, 3780, 0),
                                    new WorldPosition(2410, 3780, 0),
                                    new WorldPosition(2411, 3780, 0),
                                    new WorldPosition(2412, 3780, 0),
                                    new WorldPosition(2413, 3780, 0),
                                    new WorldPosition(2414, 3780, 0),
                                    new WorldPosition(2415, 3780, 0),
                                    new WorldPosition(2416, 3780, 0),
                                    new WorldPosition(2417, 3780, 0),
                                    new WorldPosition(2418, 3780, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            90000
                    ),
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    new WorldPosition(2407, 3783, 0),
                                    new WorldPosition(2408, 3783, 0),
                                    new WorldPosition(2409, 3783, 0),
                                    new WorldPosition(2410, 3783, 0),
                                    new WorldPosition(2411, 3783, 0),
                                    new WorldPosition(2412, 3783, 0),
                                    new WorldPosition(2413, 3783, 0),
                                    new WorldPosition(2414, 3783, 0),
                                    new WorldPosition(2415, 3783, 0),
                                    new WorldPosition(2416, 3783, 0),
                                    new WorldPosition(2417, 3783, 0),
                                    new WorldPosition(2418, 3783, 0),
                                    new WorldPosition(2419, 3783, 0),
                                    new WorldPosition(2420, 3783, 0),
                                    new WorldPosition(2421, 3783, 0),
                                    new WorldPosition(2422, 3783, 0),
                                    new WorldPosition(2423, 3783, 0),
                                    new WorldPosition(2424, 3781, 0),
                                    new WorldPosition(2424, 3782, 0),
                                    new WorldPosition(2406, 3780, 0),
                                    new WorldPosition(2407, 3780, 0),
                                    new WorldPosition(2408, 3780, 0),
                                    new WorldPosition(2409, 3780, 0),
                                    new WorldPosition(2410, 3780, 0),
                                    new WorldPosition(2411, 3780, 0),
                                    new WorldPosition(2412, 3780, 0),
                                    new WorldPosition(2413, 3780, 0),
                                    new WorldPosition(2414, 3780, 0),
                                    new WorldPosition(2415, 3780, 0),
                                    new WorldPosition(2416, 3780, 0),
                                    new WorldPosition(2417, 3780, 0),
                                    new WorldPosition(2418, 3780, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            42500
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Net",
                            Set.of(
                                    new WorldPosition(2407, 3783, 0),
                                    new WorldPosition(2408, 3783, 0),
                                    new WorldPosition(2409, 3783, 0),
                                    new WorldPosition(2410, 3783, 0),
                                    new WorldPosition(2411, 3783, 0),
                                    new WorldPosition(2412, 3783, 0),
                                    new WorldPosition(2413, 3783, 0),
                                    new WorldPosition(2414, 3783, 0),
                                    new WorldPosition(2415, 3783, 0),
                                    new WorldPosition(2416, 3783, 0),
                                    new WorldPosition(2417, 3783, 0),
                                    new WorldPosition(2418, 3783, 0),
                                    new WorldPosition(2419, 3783, 0),
                                    new WorldPosition(2420, 3783, 0),
                                    new WorldPosition(2421, 3783, 0),
                                    new WorldPosition(2422, 3783, 0),
                                    new WorldPosition(2423, 3783, 0),
                                    new WorldPosition(2424, 3781, 0),
                                    new WorldPosition(2424, 3782, 0),
                                    new WorldPosition(2406, 3780, 0),
                                    new WorldPosition(2407, 3780, 0),
                                    new WorldPosition(2408, 3780, 0),
                                    new WorldPosition(2409, 3780, 0),
                                    new WorldPosition(2410, 3780, 0),
                                    new WorldPosition(2411, 3780, 0),
                                    new WorldPosition(2412, 3780, 0),
                                    new WorldPosition(2413, 3780, 0),
                                    new WorldPosition(2414, 3780, 0),
                                    new WorldPosition(2415, 3780, 0),
                                    new WorldPosition(2416, 3780, 0),
                                    new WorldPosition(2417, 3780, 0),
                                    new WorldPosition(2418, 3780, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            40000
                    )
            )
    ),
    Hosidius_West(
            new RectangleArea(1709, 3606, 6, 9, 0), // Main fishing area
            List.of(
                    new RectangleArea(1713, 3610, 1, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(1751, 3594, 5, 5, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(1715, 3613, 0),
                                    new WorldPosition(1715, 3612, 0),
                                    new WorldPosition(1715, 3611, 0),
                                    new WorldPosition(1715, 3610, 0),
                                    new WorldPosition(1714, 3607, 0),
                                    new WorldPosition(1714, 3606, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(1715, 3613, 0),
                                    new WorldPosition(1715, 3612, 0),
                                    new WorldPosition(1715, 3611, 0),
                                    new WorldPosition(1715, 3610, 0),
                                    new WorldPosition(1714, 3607, 0),
                                    new WorldPosition(1714, 3606, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            30000
                    )
            )
    ),
    Lands_End_East(
            new RectangleArea(1524, 3414, 27, 6, 0), // Main fishing area
            List.of(
                    new RectangleArea(1540, 3418, 2, 1, 0),
                    new RectangleArea(1533, 3416, 4, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(1508, 3420, 5, 3, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    // West
                                    new WorldPosition(1534, 3414, 0),
                                    new WorldPosition(1535, 3414, 0),
                                    new WorldPosition(1536, 3414, 0),

                                    // East
                                    new WorldPosition(1541, 3417, 0),
                                    new WorldPosition(1542, 3417, 0),
                                    new WorldPosition(1543, 3417, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            25000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // West
                                    new WorldPosition(1534, 3414, 0),
                                    new WorldPosition(1535, 3414, 0),
                                    new WorldPosition(1536, 3414, 0),

                                    // East
                                    new WorldPosition(1541, 3417, 0),
                                    new WorldPosition(1542, 3417, 0),
                                    new WorldPosition(1543, 3417, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            25000
                    )
            )
    ),
    Lands_End_West(
            new RectangleArea(1479, 3426, 19, 21, 0), // Main fishing area
            List.of(
                    new RectangleArea(1486, 3431, 2, 2, 0),
                    new RectangleArea(1495, 3440, 2, 4, 0)
            ), // Fishing spot areas
            new RectangleArea(1508, 3420, 5, 3, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    // North
                                    new WorldPosition(1494, 3443, 0),
                                    new WorldPosition(1493, 3442, 0),

                                    // South
                                    new WorldPosition(1485, 3433, 0),
                                    new WorldPosition(1485, 3432, 0),
                                    new WorldPosition(1485, 3431, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            25000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    // North
                                    new WorldPosition(1494, 3443, 0),
                                    new WorldPosition(1493, 3442, 0),

                                    // South
                                    new WorldPosition(1485, 3433, 0),
                                    new WorldPosition(1485, 3432, 0),
                                    new WorldPosition(1485, 3431, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            25000
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    // North
                                    new WorldPosition(1494, 3443, 0),
                                    new WorldPosition(1493, 3442, 0),

                                    // South
                                    new WorldPosition(1485, 3433, 0),
                                    new WorldPosition(1485, 3432, 0),
                                    new WorldPosition(1485, 3431, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            40000
                    ),
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    // North
                                    new WorldPosition(1494, 3443, 0),
                                    new WorldPosition(1493, 3442, 0),

                                    // South
                                    new WorldPosition(1485, 3433, 0),
                                    new WorldPosition(1485, 3432, 0),
                                    new WorldPosition(1485, 3431, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.RAW_SHARK, ItemID.BIG_SWORDFISH, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    // North
                                    new WorldPosition(1494, 3443, 0),
                                    new WorldPosition(1493, 3442, 0),

                                    // South
                                    new WorldPosition(1485, 3433, 0),
                                    new WorldPosition(1485, 3432, 0),
                                    new WorldPosition(1485, 3431, 0)
                            ),
                            "None",
                            "None",
                            "Bank chest",
                            "Use",
                            FishingMethod.BankObjectType.BANK,
                            90000
                    )
            )
    ),
    Isle_Of_Souls_South(
            new RectangleArea(2154, 2778, 19, 13, 0), // Main fishing area
            List.of(
                    new RectangleArea(2161, 2783, 5, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    new WorldPosition(2161, 2782, 0),
                                    new WorldPosition(2162, 2782, 0),
                                    new WorldPosition(2163, 2782, 0),
                                    new WorldPosition(2164, 2782, 0),
                                    new WorldPosition(2166, 2781, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2161, 2782, 0),
                                    new WorldPosition(2162, 2782, 0),
                                    new WorldPosition(2163, 2782, 0),
                                    new WorldPosition(2164, 2782, 0),
                                    new WorldPosition(2166, 2781, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    )
            )
    ),
    Isle_Of_Souls_East(
            new RectangleArea(2272, 2831, 17, 21, 0), // Main fishing area
            List.of(
                    new RectangleArea(2277, 2836, 2, 3, 0),
                    new RectangleArea(2286, 2846, 1, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_SHARK, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    // North
                                    new WorldPosition(2288, 2848, 0),
                                    new WorldPosition(2288, 2847, 0),

                                    // South
                                    new WorldPosition(2280, 2839, 0),
                                    new WorldPosition(2280, 2838, 0),
                                    new WorldPosition(2280, 2837, 0),
                                    new WorldPosition(2280, 2836, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            90000
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    // North
                                    new WorldPosition(2288, 2848, 0),
                                    new WorldPosition(2288, 2847, 0),

                                    // South
                                    new WorldPosition(2280, 2839, 0),
                                    new WorldPosition(2280, 2838, 0),
                                    new WorldPosition(2280, 2837, 0),
                                    new WorldPosition(2280, 2836, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            40000
                    )
            )
    ),
    Isle_Of_Souls_North(
            new RectangleArea(2274, 2968, 20, 8, 0), // Main fishing area
            List.of(
                    new RectangleArea(2285, 2971, 3, 2, 0),
                    new RectangleArea(2276, 2973, 3, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    // West
                                    new WorldPosition(2276, 2976, 0),
                                    new WorldPosition(2278, 2976, 0),

                                    // East
                                    new WorldPosition(2286, 2974, 0),
                                    new WorldPosition(2287, 2974, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.BIG_SWORDFISH),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    // West
                                    new WorldPosition(2276, 2976, 0),
                                    new WorldPosition(2278, 2976, 0),

                                    // East
                                    new WorldPosition(2286, 2974, 0),
                                    new WorldPosition(2287, 2974, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            50000
                    )
            )
    ),
    Tree_Gnome_Village(
            new RectangleArea(2472, 3146, 6, 12, 0), // Main fishing area
            List.of(
                    new RectangleArea(2475, 3152, 1, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(2472, 3156, 0),
                                    new WorldPosition(2474, 3153, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2472, 3156, 0),
                                    new WorldPosition(2474, 3153, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Observatory(
            new RectangleArea(2457, 3148, 14, 12, 0), // Main fishing area
            List.of(
                    new RectangleArea(2466, 3158, 2, 1, 0),
                    new RectangleArea(2459, 3150, 1, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(2469, 3157, 0),
                                    new WorldPosition(2468, 3157, 0),
                                    new WorldPosition(2465, 3156, 0),
                                    new WorldPosition(2461, 3150, 0),
                                    new WorldPosition(2461, 3151, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2469, 3157, 0),
                                    new WorldPosition(2468, 3157, 0),
                                    new WorldPosition(2465, 3156, 0),
                                    new WorldPosition(2461, 3150, 0),
                                    new WorldPosition(2461, 3151, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Burgh_de_Rott(
            new RectangleArea(3469, 3174, 35, 27, 0), // Main fishing area
            List.of(
                    new RectangleArea(3497, 3176, 1, 5, 0),
                    new RectangleArea(3487, 3185, 3, 1, 0),
                    new RectangleArea(3477, 3191, 4, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(3495, 3210, 4, 3, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_SHARK, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(3471, 3199, 0),
                                    new WorldPosition(3472, 3196, 0),
                                    new WorldPosition(3476, 3191, 0),
                                    new WorldPosition(3479, 3189, 0),
                                    new WorldPosition(3482, 3186, 0),
                                    new WorldPosition(3484, 3185, 0),
                                    new WorldPosition(3486, 3184, 0),
                                    new WorldPosition(3489, 3184, 0),
                                    new WorldPosition(3492, 3181, 0),
                                    new WorldPosition(3490, 3183, 0),

                                    // Pier
                                    new WorldPosition(3496, 3176, 0),
                                    new WorldPosition(3496, 3177, 0),
                                    new WorldPosition(3496, 3178, 0),
                                    new WorldPosition(3496, 3179, 0),
                                    new WorldPosition(3496, 3180, 0),
                                    new WorldPosition(3499, 3180, 0),
                                    new WorldPosition(3499, 3179, 0),
                                    new WorldPosition(3499, 3178, 0),
                                    new WorldPosition(3499, 3177, 0),
                                    new WorldPosition(3499, 3176, 0),
                                    new WorldPosition(3497, 3175, 0),
                                    new WorldPosition(3498, 3175, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            90000
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    new WorldPosition(3471, 3199, 0),
                                    new WorldPosition(3472, 3196, 0),
                                    new WorldPosition(3476, 3191, 0),
                                    new WorldPosition(3479, 3189, 0),
                                    new WorldPosition(3482, 3186, 0),
                                    new WorldPosition(3484, 3185, 0),
                                    new WorldPosition(3486, 3184, 0),
                                    new WorldPosition(3489, 3184, 0),
                                    new WorldPosition(3492, 3181, 0),
                                    new WorldPosition(3490, 3183, 0),

                                    // Pier
                                    new WorldPosition(3496, 3176, 0),
                                    new WorldPosition(3496, 3177, 0),
                                    new WorldPosition(3496, 3178, 0),
                                    new WorldPosition(3496, 3179, 0),
                                    new WorldPosition(3496, 3180, 0),
                                    new WorldPosition(3499, 3180, 0),
                                    new WorldPosition(3499, 3179, 0),
                                    new WorldPosition(3499, 3178, 0),
                                    new WorldPosition(3499, 3177, 0),
                                    new WorldPosition(3499, 3176, 0),
                                    new WorldPosition(3497, 3175, 0),
                                    new WorldPosition(3498, 3175, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            40000
                    )
            )
    ),
    Piscatoris(
            new RectangleArea(2329, 3699, 25, 4, 0), // Main fishing area
            List.of(
                    new RectangleArea(2332, 3701, 3, 1, 0),
                    new RectangleArea(2346, 3700, 4, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(2327, 3686, 5, 7, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.BIG_SWORDFISH),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2353, 3703, 0),
                                    new WorldPosition(2352, 3703, 0),
                                    new WorldPosition(2337, 3703, 0),
                                    new WorldPosition(2336, 3703, 0),
                                    new WorldPosition(2332, 3703, 0),
                                    new WorldPosition(2349, 3702, 0),
                                    new WorldPosition(2348, 3702, 0),
                                    new WorldPosition(2347, 3702, 0),
                                    new WorldPosition(2346, 3702, 0),
                                    new WorldPosition(2345, 3702, 0),
                                    new WorldPosition(2344, 3702, 0),
                                    new WorldPosition(2343, 3702, 0),
                                    new WorldPosition(2342, 3702, 0),
                                    new WorldPosition(2341, 3702, 0),
                                    new WorldPosition(2340, 3702, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            50000
                    ),
                    new FishingMethod(
                            "Net",
                            List.of(ItemID.RAW_MONKFISH),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(ItemID.MONKFISH),
                            List.of(ItemID.BURNT_MONKFISH),
                            "Net",
                            Set.of(
                                    new WorldPosition(2353, 3703, 0),
                                    new WorldPosition(2352, 3703, 0),
                                    new WorldPosition(2337, 3703, 0),
                                    new WorldPosition(2336, 3703, 0),
                                    new WorldPosition(2332, 3703, 0),
                                    new WorldPosition(2349, 3702, 0),
                                    new WorldPosition(2348, 3702, 0),
                                    new WorldPosition(2347, 3702, 0),
                                    new WorldPosition(2346, 3702, 0),
                                    new WorldPosition(2345, 3702, 0),
                                    new WorldPosition(2344, 3702, 0),
                                    new WorldPosition(2343, 3702, 0),
                                    new WorldPosition(2342, 3702, 0),
                                    new WorldPosition(2341, 3702, 0),
                                    new WorldPosition(2340, 3702, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            40000
                    )
            )
    ),
    Prifddinas_North(
            new RectangleArea(2226, 3422, 1, 7, 0), // Main fishing area
            List.of(
                    new RectangleArea(2226, 3428, 1, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0 ,0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    new WorldPosition(2225, 3428, 0),
                                    new WorldPosition(2225, 3429, 0),
                                    new WorldPosition(2226, 3430, 0),
                                    new WorldPosition(2227, 3430, 0),
                                    new WorldPosition(2228, 3429, 0),
                                    new WorldPosition(2228, 3428, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            40000
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.RAW_SHARK, ItemID.BIG_SWORDFISH, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2225, 3428, 0),
                                    new WorldPosition(2225, 3429, 0),
                                    new WorldPosition(2226, 3430, 0),
                                    new WorldPosition(2227, 3430, 0),
                                    new WorldPosition(2228, 3429, 0),
                                    new WorldPosition(2228, 3428, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            90000
                    ),
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    new WorldPosition(2225, 3428, 0),
                                    new WorldPosition(2225, 3429, 0),
                                    new WorldPosition(2226, 3430, 0),
                                    new WorldPosition(2227, 3430, 0),
                                    new WorldPosition(2228, 3429, 0),
                                    new WorldPosition(2228, 3428, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            42500
                    )
            )
    ),
    Prifddinas_South_NorthSide(
            new RectangleArea(2260, 3258, 8, 4, 0), // Main fishing area
            List.of(
                    new RectangleArea(2264, 3259, 1, 0, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0 ,0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(2264, 3258, 0),
                                    new WorldPosition(2265, 3258, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2264, 3258, 0),
                                    new WorldPosition(2265, 3258, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Prifddinas_South_SouthSide(
            new RectangleArea(2263, 3250, 7, 3, 0), // Main fishing area
            List.of(
                    new RectangleArea(2266, 3251, 1, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0 ,0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(2266, 3253, 0),
                                    new WorldPosition(2267, 3253, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2266, 3253, 0),
                                    new WorldPosition(2267, 3253, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Corsair_Cove(
            new RectangleArea(2500, 2834, 22, 9, 0), // Main fishing area
            List.of(
                    new RectangleArea(2505, 2838, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(2566, 2859, 9, 7, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2510, 2838, 0),
                                    new WorldPosition(2509, 2838, 0),
                                    new WorldPosition(2505, 2835, 0),
                                    new WorldPosition(2516, 2838, 0),
                                    new WorldPosition(2515, 2838, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    ),
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    new WorldPosition(2510, 2838, 0),
                                    new WorldPosition(2509, 2838, 0),
                                    new WorldPosition(2505, 2835, 0),
                                    new WorldPosition(2516, 2838, 0),
                                    new WorldPosition(2515, 2838, 0)
                            ),
                            "None",
                            "None",
                            "Bank deposit box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    )
            )
    ),
    Myths_Guild(
            new RectangleArea(2454, 2891, 5, 1, 0), // Main fishing area
            List.of(
                    new RectangleArea(2455, 2891, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    new WorldPosition(2459, 2890, 0),
                                    new WorldPosition(2458, 2890, 0),
                                    new WorldPosition(2457, 2890, 0),
                                    new WorldPosition(2456, 2890, 0),
                                    new WorldPosition(2455, 2890, 0),
                                    new WorldPosition(2454, 2890, 0),
                                    new WorldPosition(2453, 2891, 0),
                                    new WorldPosition(2453, 2892, 0),
                                    new WorldPosition(2459, 2893, 0),
                                    new WorldPosition(2458, 2893, 0),
                                    new WorldPosition(2457, 2893, 0),
                                    new WorldPosition(2456, 2893, 0),
                                    new WorldPosition(2455, 2893, 0),
                                    new WorldPosition(2454, 2893, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.BIG_SWORDFISH),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2459, 2890, 0),
                                    new WorldPosition(2458, 2890, 0),
                                    new WorldPosition(2457, 2890, 0),
                                    new WorldPosition(2456, 2890, 0),
                                    new WorldPosition(2455, 2890, 0),
                                    new WorldPosition(2454, 2890, 0),
                                    new WorldPosition(2453, 2891, 0),
                                    new WorldPosition(2453, 2892, 0),
                                    new WorldPosition(2459, 2893, 0),
                                    new WorldPosition(2458, 2893, 0),
                                    new WorldPosition(2457, 2893, 0),
                                    new WorldPosition(2456, 2893, 0),
                                    new WorldPosition(2455, 2893, 0),
                                    new WorldPosition(2454, 2893, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            50000
                    )
            )
    ),
    Varlamore(
            new RectangleArea(1547, 3182, 11, 7, 0), // Main fishing area
            List.of(
                    new RectangleArea(1552, 3185, 1, 2, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0 ,0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(1550, 3186, 0),
                                    new WorldPosition(1552, 3188, 0),
                                    new WorldPosition(1553, 3188, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(1550, 3186, 0),
                                    new WorldPosition(1552, 3188, 0),
                                    new WorldPosition(1553, 3188, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Entrana_Middle(
            new RectangleArea(2839, 3359, 12, 5, 0), // Main fishing area
            List.of(
                    new RectangleArea(2846, 3362, 3, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0 ,0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fly Fishing Rod (Feathers)",
                            List.of(ItemID.RAW_TROUT, ItemID.RAW_SALMON),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FLY_FISHING_ROD, ItemID.FEATHER),
                            List.of(ItemID.TROUT, ItemID.SALMON),
                            List.of(ItemID.BURNT_FISH_343),
                            "Lure",
                            Set.of(
                                    new WorldPosition(2849, 3361, 0),
                                    new WorldPosition(2848, 3361, 0),
                                    new WorldPosition(2847, 3361, 0),
                                    new WorldPosition(2843, 3359, 0),
                                    new WorldPosition(2842, 3359, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            20000
                    ),
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_PIKE),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(ItemID.PIKE),
                            List.of(ItemID.BURNT_FISH_343),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2849, 3361, 0),
                                    new WorldPosition(2848, 3361, 0),
                                    new WorldPosition(2847, 3361, 0),
                                    new WorldPosition(2843, 3359, 0),
                                    new WorldPosition(2842, 3359, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            30000
                    )
            )
    ),
    Entrana_East(
            new RectangleArea(2874, 3332, 4, 9, 0), // Main fishing area
            List.of(
                    new RectangleArea(2875, 3335, 2, 3, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0 ,0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2875, 3331, 0),
                                    new WorldPosition(2876, 3331, 0),
                                    new WorldPosition(2877, 3331, 0),
                                    new WorldPosition(2879, 3334, 0),
                                    new WorldPosition(2879, 3335, 0),
                                    new WorldPosition(2879, 3338, 0),
                                    new WorldPosition(2879, 3339, 0),
                                    new WorldPosition(2877, 3342, 0),
                                    new WorldPosition(2876, 3342, 0),
                                    new WorldPosition(2875, 3342, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    ),
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Net",
                            Set.of(
                                    new WorldPosition(2875, 3331, 0),
                                    new WorldPosition(2876, 3331, 0),
                                    new WorldPosition(2877, 3331, 0),
                                    new WorldPosition(2879, 3334, 0),
                                    new WorldPosition(2879, 3335, 0),
                                    new WorldPosition(2879, 3338, 0),
                                    new WorldPosition(2879, 3339, 0),
                                    new WorldPosition(2877, 3342, 0),
                                    new WorldPosition(2876, 3342, 0),
                                    new WorldPosition(2875, 3342, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    )
            )
    ),
    Catherby(
            new RectangleArea(2836, 3423, 26, 12, 0), // Main fishing area
            List.of(
                    new RectangleArea(2852, 3425, 4, 2, 0),
                    new RectangleArea(2844, 3431, 2, 1, 0),
                    new RectangleArea(2836, 3432, 5, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(2804, 3435, 10, 9, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Fishing Rod (Bait)",
                            List.of(ItemID.RAW_SARDINE, ItemID.RAW_HERRING),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.FISHING_ROD, ItemID.FISHING_BAIT),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2836, 3431, 0),
                                    new WorldPosition(2837, 3431, 0),
                                    new WorldPosition(2838, 3431, 0),
                                    new WorldPosition(2839, 3431, 0),
                                    new WorldPosition(2840, 3431, 0),
                                    new WorldPosition(2841, 3431, 0),
                                    new WorldPosition(2844, 3429, 0),
                                    new WorldPosition(2845, 3429, 0),
                                    new WorldPosition(2846, 3429, 0),
                                    new WorldPosition(2853, 3423, 0),
                                    new WorldPosition(2854, 3423, 0),
                                    new WorldPosition(2855, 3423, 0),
                                    new WorldPosition(2856, 3431, 0),
                                    new WorldPosition(2859, 3426, 0),
                                    new WorldPosition(2860, 3426, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    ),
                    new FishingMethod(
                            "Small Fishing Net",
                            List.of(ItemID.RAW_SHRIMPS, ItemID.RAW_ANCHOVIES),
                            List.of(HandlingMode.DROP),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    new WorldPosition(2836, 3431, 0),
                                    new WorldPosition(2837, 3431, 0),
                                    new WorldPosition(2838, 3431, 0),
                                    new WorldPosition(2839, 3431, 0),
                                    new WorldPosition(2840, 3431, 0),
                                    new WorldPosition(2841, 3431, 0),
                                    new WorldPosition(2844, 3429, 0),
                                    new WorldPosition(2845, 3429, 0),
                                    new WorldPosition(2846, 3429, 0),
                                    new WorldPosition(2853, 3423, 0),
                                    new WorldPosition(2854, 3423, 0),
                                    new WorldPosition(2855, 3423, 0),
                                    new WorldPosition(2856, 3431, 0),
                                    new WorldPosition(2859, 3426, 0),
                                    new WorldPosition(2860, 3426, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    ),
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    new WorldPosition(2836, 3431, 0),
                                    new WorldPosition(2837, 3431, 0),
                                    new WorldPosition(2838, 3431, 0),
                                    new WorldPosition(2839, 3431, 0),
                                    new WorldPosition(2840, 3431, 0),
                                    new WorldPosition(2841, 3431, 0),
                                    new WorldPosition(2844, 3429, 0),
                                    new WorldPosition(2845, 3429, 0),
                                    new WorldPosition(2846, 3429, 0),
                                    new WorldPosition(2853, 3423, 0),
                                    new WorldPosition(2854, 3423, 0),
                                    new WorldPosition(2855, 3423, 0),
                                    new WorldPosition(2856, 3431, 0),
                                    new WorldPosition(2859, 3426, 0),
                                    new WorldPosition(2860, 3426, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.RAW_SHARK, ItemID.BIG_SWORDFISH, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2836, 3431, 0),
                                    new WorldPosition(2837, 3431, 0),
                                    new WorldPosition(2838, 3431, 0),
                                    new WorldPosition(2839, 3431, 0),
                                    new WorldPosition(2840, 3431, 0),
                                    new WorldPosition(2841, 3431, 0),
                                    new WorldPosition(2844, 3429, 0),
                                    new WorldPosition(2845, 3429, 0),
                                    new WorldPosition(2846, 3429, 0),
                                    new WorldPosition(2853, 3423, 0),
                                    new WorldPosition(2854, 3423, 0),
                                    new WorldPosition(2855, 3423, 0),
                                    new WorldPosition(2856, 3431, 0),
                                    new WorldPosition(2859, 3426, 0),
                                    new WorldPosition(2860, 3426, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            90000
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    new WorldPosition(2836, 3431, 0),
                                    new WorldPosition(2837, 3431, 0),
                                    new WorldPosition(2838, 3431, 0),
                                    new WorldPosition(2839, 3431, 0),
                                    new WorldPosition(2840, 3431, 0),
                                    new WorldPosition(2841, 3431, 0),
                                    new WorldPosition(2844, 3429, 0),
                                    new WorldPosition(2845, 3429, 0),
                                    new WorldPosition(2846, 3429, 0),
                                    new WorldPosition(2853, 3423, 0),
                                    new WorldPosition(2854, 3423, 0),
                                    new WorldPosition(2855, 3423, 0),
                                    new WorldPosition(2856, 3431, 0),
                                    new WorldPosition(2859, 3426, 0),
                                    new WorldPosition(2860, 3426, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            40000
                    )
            )
    ),
    Mor_Ul_Rek_East(
            new RectangleArea(2532, 5085, 10, 7, 0), // Main fishing area
            List.of(
                    new RectangleArea(2535, 5089, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(2536, 5135, 11, 10, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Oily Fishing Rod (Bait)",
                            List.of(ItemID.INFERNAL_EEL),
                            List.of(HandlingMode.COOK, HandlingMode.BANK),
                            List.of(ItemID.OILY_FISHING_ROD, ItemID.FISHING_BAIT, ItemID.HAMMER),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2541, 5088, 0),
                                    new WorldPosition(2540, 5088, 0),
                                    new WorldPosition(2539, 5088, 0),
                                    new WorldPosition(2536, 5086, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            35000
                    )
            )
    ),
    Mor_Ul_Rek_West(
            new RectangleArea(2473, 5074, 12, 9, 0), // Main fishing area
            List.of(
                    new RectangleArea(2478, 5080, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(2536, 5135, 11, 10, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Oily Fishing Rod (Bait)",
                            List.of(ItemID.INFERNAL_EEL),
                            List.of(HandlingMode.COOK, HandlingMode.BANK),
                            List.of(ItemID.OILY_FISHING_ROD, ItemID.FISHING_BAIT, ItemID.HAMMER),
                            List.of(),
                            List.of(),
                            "Bait",
                            Set.of(
                                    new WorldPosition(2479, 5078, 0),
                                    new WorldPosition(2478, 5078, 0),
                                    new WorldPosition(2477, 5078, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            35000
                    )
            )
    ),
    Karambwans(
            new RectangleArea(2891, 3103, 22, 18, 0), // Main fishing area
            List.of(
                    new RectangleArea(2898, 3117, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Karambwan Vessel (Bait)",
                            List.of(ItemID.RAW_KARAMBWAN),
                            List.of(HandlingMode.BANK),
                            List.of(ItemID.KARAMBWAN_VESSEL_3159, ItemID.RAW_KARAMBWANJI),
                            List.of(),
                            List.of(),
                            "Fish",
                            Set.of(
                                    new WorldPosition(2899, 3119, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            25000
                    )
            )
    ),
    Fishing_Guild_South(
            new RectangleArea(2602, 3406, 9, 11, 0), // Main fishing area
            List.of(
                    new RectangleArea(2603, 3414, 2, 1, 0),
                    new RectangleArea(2609, 3411, 2, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(2586, 3416, 8, 6, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    new WorldPosition(2602, 3411, 0),
                                    new WorldPosition(2602, 3412, 0),
                                    new WorldPosition(2602, 3413, 0),
                                    new WorldPosition(2602, 3414, 0),
                                    new WorldPosition(2602, 3415, 0),
                                    new WorldPosition(2602, 3416, 0),
                                    new WorldPosition(2603, 3417, 0),
                                    new WorldPosition(2604, 3417, 0),
                                    new WorldPosition(2605, 3416, 0),
                                    new WorldPosition(2606, 3416, 0),
                                    new WorldPosition(2607, 3416, 0),
                                    new WorldPosition(2608, 3416, 0),
                                    new WorldPosition(2609, 3416, 0),
                                    new WorldPosition(2610, 3416, 0),
                                    new WorldPosition(2611, 3416, 0),
                                    new WorldPosition(2612, 3415, 0),
                                    new WorldPosition(2612, 3414, 0),
                                    new WorldPosition(2612, 3412, 0),
                                    new WorldPosition(2612, 3411, 0),
                                    new WorldPosition(2606, 3410, 0),
                                    new WorldPosition(2607, 3410, 0),
                                    new WorldPosition(2608, 3410, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.RAW_SHARK, ItemID.BIG_SWORDFISH, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2602, 3411, 0),
                                    new WorldPosition(2602, 3412, 0),
                                    new WorldPosition(2602, 3413, 0),
                                    new WorldPosition(2602, 3414, 0),
                                    new WorldPosition(2602, 3415, 0),
                                    new WorldPosition(2602, 3416, 0),
                                    new WorldPosition(2603, 3417, 0),
                                    new WorldPosition(2604, 3417, 0),
                                    new WorldPosition(2605, 3416, 0),
                                    new WorldPosition(2606, 3416, 0),
                                    new WorldPosition(2607, 3416, 0),
                                    new WorldPosition(2608, 3416, 0),
                                    new WorldPosition(2609, 3416, 0),
                                    new WorldPosition(2610, 3416, 0),
                                    new WorldPosition(2611, 3416, 0),
                                    new WorldPosition(2612, 3415, 0),
                                    new WorldPosition(2612, 3414, 0),
                                    new WorldPosition(2612, 3412, 0),
                                    new WorldPosition(2612, 3411, 0),
                                    new WorldPosition(2606, 3410, 0),
                                    new WorldPosition(2607, 3410, 0),
                                    new WorldPosition(2608, 3410, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            90000
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    new WorldPosition(2602, 3411, 0),
                                    new WorldPosition(2602, 3412, 0),
                                    new WorldPosition(2602, 3413, 0),
                                    new WorldPosition(2602, 3414, 0),
                                    new WorldPosition(2602, 3415, 0),
                                    new WorldPosition(2602, 3416, 0),
                                    new WorldPosition(2603, 3417, 0),
                                    new WorldPosition(2604, 3417, 0),
                                    new WorldPosition(2605, 3416, 0),
                                    new WorldPosition(2606, 3416, 0),
                                    new WorldPosition(2607, 3416, 0),
                                    new WorldPosition(2608, 3416, 0),
                                    new WorldPosition(2609, 3416, 0),
                                    new WorldPosition(2610, 3416, 0),
                                    new WorldPosition(2611, 3416, 0),
                                    new WorldPosition(2612, 3415, 0),
                                    new WorldPosition(2612, 3414, 0),
                                    new WorldPosition(2612, 3412, 0),
                                    new WorldPosition(2612, 3411, 0),
                                    new WorldPosition(2606, 3410, 0),
                                    new WorldPosition(2607, 3410, 0),
                                    new WorldPosition(2608, 3410, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            40000
                    )
            )
    ),
    Fishing_Guild_North(
            new RectangleArea(2595, 3420, 9, 5, 0), // Main fishing area
            List.of(
                    new RectangleArea(2601, 3420, 3, 1, 0),
                    new RectangleArea(2601, 3424, 3, 1, 0)
            ), // Fishing spot areas
            new RectangleArea(2586, 3416, 8, 6, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Pot",
                            List.of(ItemID.RAW_LOBSTER),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.LOBSTER_POT),
                            List.of(),
                            List.of(),
                            "Cage",
                            Set.of(
                                    new WorldPosition(2598, 3419, 0),
                                    new WorldPosition(2599, 3419, 0),
                                    new WorldPosition(2600, 3419, 0),
                                    new WorldPosition(2602, 3419, 0),
                                    new WorldPosition(2603, 3419, 0),
                                    new WorldPosition(2604, 3419, 0),
                                    new WorldPosition(2605, 3420, 0),
                                    new WorldPosition(2605, 3421, 0),
                                    new WorldPosition(2604, 3422, 0),
                                    new WorldPosition(2603, 3422, 0),
                                    new WorldPosition(2602, 3422, 0),
                                    new WorldPosition(2601, 3422, 0),
                                    new WorldPosition(2601, 3423, 0),
                                    new WorldPosition(2602, 3423, 0),
                                    new WorldPosition(2603, 3423, 0),
                                    new WorldPosition(2604, 3423, 0),
                                    new WorldPosition(2605, 3424, 0),
                                    new WorldPosition(2605, 3425, 0),
                                    new WorldPosition(2604, 3426, 0),
                                    new WorldPosition(2603, 3426, 0),
                                    new WorldPosition(2602, 3426, 0),
                                    new WorldPosition(2598, 3424, 0),
                                    new WorldPosition(2598, 3423, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            42500
                    ),
                    new FishingMethod(
                            "Harpoon",
                            List.of(ItemID.RAW_TUNA, ItemID.RAW_SWORDFISH, ItemID.RAW_SHARK, ItemID.BIG_SWORDFISH, ItemID.BIG_SHARK),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.HARPOON),
                            List.of(),
                            List.of(),
                            "Harpoon",
                            Set.of(
                                    new WorldPosition(2598, 3419, 0),
                                    new WorldPosition(2599, 3419, 0),
                                    new WorldPosition(2600, 3419, 0),
                                    new WorldPosition(2602, 3419, 0),
                                    new WorldPosition(2603, 3419, 0),
                                    new WorldPosition(2604, 3419, 0),
                                    new WorldPosition(2605, 3420, 0),
                                    new WorldPosition(2605, 3421, 0),
                                    new WorldPosition(2604, 3422, 0),
                                    new WorldPosition(2603, 3422, 0),
                                    new WorldPosition(2602, 3422, 0),
                                    new WorldPosition(2601, 3422, 0),
                                    new WorldPosition(2601, 3423, 0),
                                    new WorldPosition(2602, 3423, 0),
                                    new WorldPosition(2603, 3423, 0),
                                    new WorldPosition(2604, 3423, 0),
                                    new WorldPosition(2605, 3424, 0),
                                    new WorldPosition(2605, 3425, 0),
                                    new WorldPosition(2604, 3426, 0),
                                    new WorldPosition(2603, 3426, 0),
                                    new WorldPosition(2602, 3426, 0),
                                    new WorldPosition(2598, 3424, 0),
                                    new WorldPosition(2598, 3423, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            90000
                    ),
                    new FishingMethod(
                            "Big Net",
                            List.of(ItemID.RAW_MACKEREL, ItemID.RAW_COD, ItemID.RAW_BASS, ItemID.BIG_BASS, ItemID.CASKET, ItemID.LEATHER_BOOTS, ItemID.LEATHER_GLOVES, ItemID.OYSTER, ItemID.SEAWEED),
                            List.of(HandlingMode.DROP, HandlingMode.BANK),
                            List.of(ItemID.BIG_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Big Net",
                            Set.of(
                                    new WorldPosition(2598, 3419, 0),
                                    new WorldPosition(2599, 3419, 0),
                                    new WorldPosition(2600, 3419, 0),
                                    new WorldPosition(2602, 3419, 0),
                                    new WorldPosition(2603, 3419, 0),
                                    new WorldPosition(2604, 3419, 0),
                                    new WorldPosition(2605, 3420, 0),
                                    new WorldPosition(2605, 3421, 0),
                                    new WorldPosition(2604, 3422, 0),
                                    new WorldPosition(2603, 3422, 0),
                                    new WorldPosition(2602, 3422, 0),
                                    new WorldPosition(2601, 3422, 0),
                                    new WorldPosition(2601, 3423, 0),
                                    new WorldPosition(2602, 3423, 0),
                                    new WorldPosition(2603, 3423, 0),
                                    new WorldPosition(2604, 3423, 0),
                                    new WorldPosition(2605, 3424, 0),
                                    new WorldPosition(2605, 3425, 0),
                                    new WorldPosition(2604, 3426, 0),
                                    new WorldPosition(2603, 3426, 0),
                                    new WorldPosition(2602, 3426, 0),
                                    new WorldPosition(2598, 3424, 0),
                                    new WorldPosition(2598, 3423, 0)
                            ),
                            "None",
                            "None",
                            "Bank Deposit Box",
                            "Deposit",
                            FishingMethod.BankObjectType.DEPOSIT_BOX,
                            40000
                    )
            )
    ),
    Minnows(
            new RectangleArea(0, 0, 0, 0, 0), // Main fishing area
            List.of(
                    new RectangleArea(0, 0, 0, 0, 0)
            ), // Fishing spot areas
            new RectangleArea(0, 0, 0, 0, 0), // Bank area
            List.of(
                    new FishingMethod(
                            "Minnows (Net)",
                            List.of(ItemID.MINNOW),
                            List.of(HandlingMode.STACK),
                            List.of(ItemID.SMALL_FISHING_NET),
                            List.of(),
                            List.of(),
                            "Small Net",
                            Set.of(
                                    new WorldPosition(0, 0, 0)
                            ),
                            "None",
                            "None",
                            "None",
                            "None",
                            FishingMethod.BankObjectType.NONE,
                            25000
                    )
            )
    );
//    Wilderness_Resource_Area(
//            new RectangleArea(3181, 3925, 7, 2, 0), // Main fishing area
//            List.of(
//                    new RectangleArea(3188, 3927, 2, 2, 0)
//            ), // Fishing spot areas
//            new RectangleArea(3182, 3931, 7, 8, 0), // Bank area
//            List.of(
//                    new FishingMethod(
//                            "Lobster Pot (Cage)",
//                            List.of(ItemID.RAW_DARK_CRAB),
//                            List.of(HandlingMode.NOTE, HandlingMode.COOKnNOTE),
//                            List.of(ItemID.LOBSTER_POT, ItemID.DARK_FISHING_BAIT),
//                            List.of(ItemID.DARK_CRAB),
//                            List.of(ItemID.BURNT_DARK_CRAB),
//                            "Cage",
//                            Set.of(
//                                    new WorldPosition(3187, 3927, 0),
//                                    new WorldPosition(3185, 3926, 0),
//                                    new WorldPosition(3183, 3927, 0),
//                                    new WorldPosition(3181, 3927, 0)
//                            ),
//                            "Fire",
//                            "None",
//                            "Piles",
//                            "Talk-to",
//                            FishingMethod.BankObjectType.NPC,
//                            60000
//                    )
//            )
//    );

    private final Area fishingArea;
    private final List<Area> fishingSpotAreas;
    private final Area bankArea;
    private final List<FishingMethod> methods;

    FishingLocation(Area fishingArea, List<Area> fishingSpotAreas, Area bankArea, List<FishingMethod> methods) {
        this.fishingArea = fishingArea;
        this.fishingSpotAreas = fishingSpotAreas;
        this.bankArea = bankArea;
        this.methods = methods;
    }

    public Area getFishingArea() {
        return fishingArea;
    }

    public List<Area> getFishingSpotAreas() {
        return fishingSpotAreas;
    }

    public Area getBankArea() {
        return bankArea;
    }

    public List<FishingMethod> getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        return name().replace('_', ' ');
    }
}