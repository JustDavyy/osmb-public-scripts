package data;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;

import java.util.List;

public class MLMAreaProvider {
    public static final RectangleArea SOUTH_AREA = new RectangleArea(3733, 5645, 27, 10, 0);
    public static final RectangleArea WEST_AREA = new RectangleArea(3728, 5655, 10, 17, 0);
    public static final PolyArea TOP_FLOOR_AREA = new PolyArea(List.of(
            new WorldPosition(3751, 5678, 0),
            new WorldPosition(3751, 5677, 0),
            new WorldPosition(3754, 5675, 0),
            new WorldPosition(3756, 5675, 0),
            new WorldPosition(3756, 5674, 0),
            new WorldPosition(3757, 5673, 0),
            new WorldPosition(3759, 5672, 0),
            new WorldPosition(3760, 5671, 0),
            new WorldPosition(3760, 5669, 0),
            new WorldPosition(3761, 5668, 0),
            new WorldPosition(3762, 5667, 0),
            new WorldPosition(3763, 5667, 0),
            new WorldPosition(3763, 5666, 0),
            new WorldPosition(3762, 5665, 0),
            new WorldPosition(3761, 5664, 0),
            new WorldPosition(3761, 5663, 0),
            new WorldPosition(3761, 5660, 0),
            new WorldPosition(3761, 5655, 0),
            new WorldPosition(3765, 5655, 0),
            new WorldPosition(3766, 5655, 0),
            new WorldPosition(3766, 5657, 0),
            new WorldPosition(3763, 5660, 0),
            new WorldPosition(3763, 5662, 0),
            new WorldPosition(3763, 5664, 0),
            new WorldPosition(3765, 5666, 0),
            new WorldPosition(3764, 5669, 0),
            new WorldPosition(3764, 5670, 0),
            new WorldPosition(3763, 5671, 0),
            new WorldPosition(3763, 5674, 0),
            new WorldPosition(3765, 5676, 0),
            new WorldPosition(3766, 5677, 0),
            new WorldPosition(3766, 5678, 0),
            new WorldPosition(3765, 5679, 0),
            new WorldPosition(3764, 5680, 0),
            new WorldPosition(3764, 5683, 0),
            new WorldPosition(3763, 5684, 0),
            new WorldPosition(3761, 5685, 0),
            new WorldPosition(3757, 5685, 0),
            new WorldPosition(3751, 5685, 0),
            new WorldPosition(3748, 5685, 0),
            new WorldPosition(3747, 5686, 0),
            new WorldPosition(3744, 5686, 0),
            new WorldPosition(3744, 5687, 0),
            new WorldPosition(3743, 5687, 0),
            new WorldPosition(3742, 5686, 0),
            new WorldPosition(3740, 5686, 0),
            new WorldPosition(3739, 5685, 0),
            new WorldPosition(3737, 5685, 0),
            new WorldPosition(3736, 5686, 0),
            new WorldPosition(3735, 5686, 0),
            new WorldPosition(3733, 5686, 0),
            new WorldPosition(3733, 5683, 0),
            new WorldPosition(3734, 5682, 0),
            new WorldPosition(3735, 5681, 0),
            new WorldPosition(3739, 5681, 0),
            new WorldPosition(3740, 5682, 0),
            new WorldPosition(3744, 5682, 0),
            new WorldPosition(3745, 5681, 0),
            new WorldPosition(3746, 5681, 0),
            new WorldPosition(3747, 5680, 0),
            new WorldPosition(3750, 5680, 0),
            new WorldPosition(3750, 5679, 0)
    ));
    public static final Area[] MINING_MINE_AREAS = new Area[]{SOUTH_AREA, WEST_AREA, TOP_FLOOR_AREA};
    public static final RectangleArea LADDER_AREA = new RectangleArea(3753, 5670, 2, 3, 0);
    public static final RectangleArea WATER_INNER_AREA = new RectangleArea(3744, 5661, 3, 10, 0);
    public static final PolyArea TOP_FLOOR_ACCESSIBLE_AREA = new PolyArea(List.of(
            new WorldPosition(3758, 5675, 0),
            new WorldPosition(3758, 5674, 0),
            new WorldPosition(3762, 5674, 0),
            new WorldPosition(3762, 5669, 0),
            new WorldPosition(3761, 5668, 0),
            new WorldPosition(3761, 5670, 0),
            new WorldPosition(3760, 5671, 0),
            new WorldPosition(3759, 5672, 0),
            new WorldPosition(3758, 5673, 0),
            new WorldPosition(3757, 5673, 0),
            new WorldPosition(3756, 5674, 0),
            new WorldPosition(3755, 5674, 0),
            new WorldPosition(3754, 5675, 0),
            new WorldPosition(3753, 5676, 0),
            new WorldPosition(3752, 5676, 0),
            new WorldPosition(3752, 5677, 0),
            new WorldPosition(3751, 5678, 0),
            new WorldPosition(3750, 5679, 0),
            new WorldPosition(3750, 5681, 0),
            new WorldPosition(3747, 5681, 0),
            new WorldPosition(3746, 5683, 0),
            new WorldPosition(3747, 5683, 0),
            new WorldPosition(3748, 5683, 0),
            new WorldPosition(3749, 5683, 0),
            new WorldPosition(3749, 5684, 0),
            new WorldPosition(3750, 5685, 0),
            new WorldPosition(3751, 5684, 0),
            new WorldPosition(3753, 5684, 0),
            new WorldPosition(3754, 5684, 0),
            new WorldPosition(3754, 5681, 0),
            new WorldPosition(3753, 5680, 0),
            new WorldPosition(3754, 5679, 0),
            new WorldPosition(3755, 5679, 0),
            new WorldPosition(3756, 5679, 0),
            new WorldPosition(3756, 5676, 0),
            new WorldPosition(3757, 5676, 0),
            new WorldPosition(3758, 5676, 0)));
    public static final PolyArea BOTTOM_FLOOR_ACCESSIBLE_AREA = new PolyArea(List.of(new WorldPosition(3732, 5678, 0), new WorldPosition(3734, 5680, 0), new WorldPosition(3735, 5680, 0), new WorldPosition(3738, 5680, 0), new WorldPosition(3740, 5681, 0), new WorldPosition(3744, 5681, 0), new WorldPosition(3746, 5680, 0), new WorldPosition(3747, 5679, 0), new WorldPosition(3749, 5679, 0), new WorldPosition(3750, 5678, 0), new WorldPosition(3751, 5677, 0), new WorldPosition(3751, 5676, 0), new WorldPosition(3752, 5675, 0), new WorldPosition(3753, 5675, 0), new WorldPosition(3754, 5674, 0), new WorldPosition(3755, 5673, 0), new WorldPosition(3756, 5673, 0), new WorldPosition(3757, 5672, 0), new WorldPosition(3758, 5672, 0), new WorldPosition(3759, 5671, 0), new WorldPosition(3760, 5670, 0), new WorldPosition(3760, 5668, 0), new WorldPosition(3761, 5667, 0), new WorldPosition(3761, 5665, 0), new WorldPosition(3760, 5664, 0), new WorldPosition(3760, 5661, 0), new WorldPosition(3760, 5658, 0), new WorldPosition(3759, 5657, 0), new WorldPosition(3759, 5656, 0), new WorldPosition(3759, 5655, 0), new WorldPosition(3760, 5654, 0), new WorldPosition(3762, 5654, 0), new WorldPosition(3762, 5653, 0), new WorldPosition(3761, 5652, 0), new WorldPosition(3761, 5651, 0), new WorldPosition(3758, 5651, 0), new WorldPosition(3755, 5648, 0), new WorldPosition(3748, 5645, 0), new WorldPosition(3740, 5645, 0), new WorldPosition(3731, 5651, 0), new WorldPosition(3729, 5650, 0), new WorldPosition(3729, 5651, 0), new WorldPosition(3728, 5652, 0), new WorldPosition(3728, 5655, 0), new WorldPosition(3728, 5667, 0), new WorldPosition(3728, 5674, 0)));
    public static final PolyArea TOP_FLOOR_NORTHEAST_AREA = new PolyArea(List.of(new WorldPosition(3758, 5676, 0),new WorldPosition(3766, 5676, 0),new WorldPosition(3766, 5678, 0),new WorldPosition(3764, 5682, 0),new WorldPosition(3763, 5687, 0),new WorldPosition(3760, 5685, 0),new WorldPosition(3759, 5685, 0),new WorldPosition(3755, 5684, 0),new WorldPosition(3755, 5679, 0),new WorldPosition(3757, 5677, 0)));
    public static final WorldPosition TOP_FLOOR_NORTHEAST_ROCK = new WorldPosition(3757, 5677, 0);
    public static final Area TOP_FLOOR_NORTHEAST_WALKAREA = new RectangleArea(3755, 5675, 2, 1, 0);
    public static final PolyArea TOP_FLOOR_NORTHWEST_AREA = new PolyArea(List.of(new WorldPosition(3748, 5684, 0),new WorldPosition(3739, 5682, 0),new WorldPosition(3732, 5683, 0),new WorldPosition(3731, 5687, 0),new WorldPosition(3736, 5686, 0),new WorldPosition(3737, 5685, 0),new WorldPosition(3740, 5686, 0),new WorldPosition(3743, 5687, 0),new WorldPosition(3746, 5686, 0)));
    public static final WorldPosition TOP_FLOOR_NORTHWEST_ROCK = new WorldPosition(3748, 5684, 0);
    public static final Area TOP_FLOOR_NORTHWEST_WALKAREA = new RectangleArea(3751, 5681, 2, 2, 0);
    public static final PolyArea TOP_FLOOR_SOUTHEAST_AREA = new PolyArea(List.of(new WorldPosition(3762, 5668, 0),new WorldPosition(3764, 5670, 0),new WorldPosition(3765, 5664, 0),new WorldPosition(3764, 5661, 0),new WorldPosition(3764, 5659, 0),new WorldPosition(3766, 5657, 0),new WorldPosition(3768, 5657, 0),new WorldPosition(3765, 5654, 0),new WorldPosition(3760, 5655, 0),new WorldPosition(3761, 5663, 0)));
    public static final WorldPosition TOP_FLOOR_SOUTHEAST_ROCK = new WorldPosition(3762, 5668, 0);
    public static final Area TOP_FLOOR_SOUTHEAST_WALKAREA = new RectangleArea(3758, 5673, 3, 0, 0);

    public static final PolyArea BOTTOM_FLOOR_WEST_SOUTH_AREA = new PolyArea(List.of(new WorldPosition(3716, 5664, 0),new WorldPosition(3713, 5640, 0),new WorldPosition(3724, 5640, 0),new WorldPosition(3725, 5645, 0),new WorldPosition(3727, 5650, 0),new WorldPosition(3721, 5664, 0),new WorldPosition(3724, 5655, 0),new WorldPosition(3726, 5653, 0)));
    public static final WorldPosition BOTTOM_FLOOR_WEST_SOUTH_ROCK = new WorldPosition(3728, 5651, 0);
    public static final Area BOTTOM_FLOOR_WEST_SOUTH_WALKAREA = new RectangleArea(3730, 5651, 4, 2, 0);
    public static final PolyArea BOTTOM_FLOOR_WEST_SOUTH_AREA2 = new PolyArea(List.of(new WorldPosition(3727, 5665, 0),new WorldPosition(3723, 5665, 0),new WorldPosition(3724, 5660, 0),new WorldPosition(3725, 5655, 0),new WorldPosition(3726, 5654, 0),new WorldPosition(3728, 5655, 0),new WorldPosition(3728, 5661, 0)));
    public static final WorldPosition BOTTOM_FLOOR_WEST_SOUTH_ROCK2 = new WorldPosition(3726, 5654, 0);
    public static final Area BOTTOM_FLOOR_WEST_SOUTH_WALKAREA2 = new RectangleArea(3723, 5651, 2, 2, 0);
    public static final PolyArea BOTTOM_FLOOR_WEST_NORTH_AREA = new PolyArea(List.of(new WorldPosition(3714, 5666, 0),new WorldPosition(3719, 5665, 0),new WorldPosition(3721, 5666, 0),new WorldPosition(3722, 5670, 0),new WorldPosition(3723, 5674, 0),new WorldPosition(3723, 5678, 0),new WorldPosition(3723, 5682, 0),new WorldPosition(3725, 5683, 0),new WorldPosition(3726, 5683, 0),new WorldPosition(3726, 5684, 0),new WorldPosition(3731, 5684, 0),new WorldPosition(3731, 5692, 0),new WorldPosition(3716, 5696, 0),new WorldPosition(3715, 5694, 0)));
    public static final WorldPosition BOTTOM_FLOOR_WEST_NORTH_ROCK1 = new WorldPosition(3731, 5683, 0);
    public static final WorldPosition BOTTOM_FLOOR_WEST_NORTH_ROCK2 = new WorldPosition(3733, 5680, 0);
    public static final Area BOTTOM_FLOOR_WEST_NORTH_WALKAREA1 = new RectangleArea(3731, 5681, 2, 1, 0);
    public static final Area BOTTOM_FLOOR_WEST_NORTH_WALKAREA2 = new RectangleArea(3734, 5677, 3, 2, 0);
    public static final PolyArea BOTTOM_FLOOR_WEST_NORTH_AREA3 = new PolyArea(List.of(new WorldPosition(3728, 5664, 0),new WorldPosition(3723, 5664, 0),new WorldPosition(3723, 5672, 0),new WorldPosition(3724, 5674, 0),new WorldPosition(3724, 5680, 0),new WorldPosition(3725, 5682, 0),new WorldPosition(3729, 5682, 0),new WorldPosition(3729, 5676, 0)));
    public static final WorldPosition BOTTOM_FLOOR_WEST_NORTH_ROCK3 = new WorldPosition(3727, 5683, 0);
    public static final Area BOTTOM_FLOOR_WEST_NORTH_WALKAREA3 = new RectangleArea(3726, 5685, 4, 1, 0);

    public static final String rockName = "Rockfall";
    public static final String rockAction = "Mine";
}
