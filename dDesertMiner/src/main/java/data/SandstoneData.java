package data;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;

import java.util.List;
import java.util.Set;

public final class SandstoneData {
  public static final String TARGET_ROCK_NAME = "Sandstone rocks";
  public static final WorldPosition GRINDER_POS = new WorldPosition(3152, 2909, 0);
  public static final WorldPosition NORTH_ANCHOR = new WorldPosition(3165, 2914, 0);
  public static final WorldPosition SOUTH_ANCHOR = new WorldPosition(3165, 2906, 0);
  public static final List<WorldPosition> NORTH_ROCKS = List.of(
    new WorldPosition(3167, 2913, 0),
    new WorldPosition(3166, 2913, 0),
    new WorldPosition(3164, 2914, 0)
  );
  public static final List<WorldPosition> SOUTH_ROCKS = List.of(
    new WorldPosition(3166, 2905, 0),
    new WorldPosition(3164, 2905, 0),
    new WorldPosition(3164, 2906, 0),
    new WorldPosition(3166, 2906, 0)
  );
  public static final int[] WATERSKIN_IDS = new int[]{
    ItemID.WATERSKIN4,
    ItemID.WATERSKIN3,
    ItemID.WATERSKIN2,
    ItemID.WATERSKIN1,
    ItemID.WATERSKIN0
  };

  public enum MiningLocation {
    NORTH("North"),
    SOUTH("South");

    private final String label;

    MiningLocation(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private SandstoneData() {
  }

  public static WorldPosition getAnchor(MiningLocation location) {
    return location == MiningLocation.SOUTH ? SOUTH_ANCHOR : NORTH_ANCHOR;
  }

  public static List<WorldPosition> getAllowedRocks(MiningLocation location) {
    return location == MiningLocation.SOUTH ? SOUTH_ROCKS : NORTH_ROCKS;
  }
}
