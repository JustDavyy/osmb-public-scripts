package data;

import com.osmb.api.location.area.Area;

public enum MineArea {
    TOP("Top floor", MLMAreaProvider.TOP_FLOOR_AREA),
    BOTTOM_SOUTH("Bottom floor - prefer south", MLMAreaProvider.SOUTH_AREA),
    BOTTOM_WEST("Bottom floor - prefer west", MLMAreaProvider.WEST_AREA);

    private final Area area;
    private final String name;

    MineArea(String name, Area area) {
        this.area = area;
        this.name = name;
    }

    public Area getArea() {
        return area;
    }

    @Override
    public String toString() {
        return name;
    }
}
