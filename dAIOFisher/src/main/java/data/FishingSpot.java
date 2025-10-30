package data;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;

public class FishingSpot {
    private final WorldPosition position;
    private final Polygon fishingSpotPoly;

    public FishingSpot(WorldPosition position, Polygon fishingSpotPoly) {
        this.position = position;
        this.fishingSpotPoly = fishingSpotPoly;
    }

    public Polygon getFishingSpotPoly() {
        return fishingSpotPoly;
    }

    public WorldPosition getPosition() {
        return position;
    }
}