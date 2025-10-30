package component;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MushroomTransportInterface extends ComponentCentered {

    private static final int ORANGE_TEXT_COLOR = -26593;
    private static final int ORANGE_BUTTON_TEXT_COLOR = -27105;
    private static final Rectangle TITLE_BOUNDS = new Rectangle(0, 0, 472, 34);
    private static final String TITLE_TEXT = "Mycelium Transportation System";
    private Map<ButtonType, Rectangle> buttons;

    public MushroomTransportInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(472, 294, ColorUtils.TRANSPARENT_PIXEL);
        canvas.createBackground(core, BorderPalette.STONE_BORDER, null);
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        // bottom half of title border
        canvas.fillRect(0, 23, canvas.canvasWidth, 17, ColorUtils.TRANSPARENT_PIXEL);
        return new ComponentImage<>(canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB), -1, 1);
    }

    @Override
    public boolean isVisible() {
        Rectangle bounds = getBounds();
        if (bounds == null) return false;

        Rectangle titleArea = bounds.getSubRectangle(TITLE_BOUNDS);
        core.getScreen().getDrawableCanvas().drawRect(titleArea,Color.RED.getRGB());
        String text = core.getOCR().getText(Font.STANDARD_FONT_BOLD, titleArea, ORANGE_TEXT_COLOR);
        boolean visible = text.equalsIgnoreCase(TITLE_TEXT);
        if (visible && buttons == null) {
            this.buttons = registerButtons(bounds);
        }
        if(visible) {
            // highlight buttons
            for (Map.Entry<ButtonType, Rectangle> entry : buttons.entrySet()) {
                Rectangle button = entry.getValue();
                core.getScreen().getDrawableCanvas().drawRect(bounds.getSubRectangle(button), Color.RED.getRGB());
            }
        }
        return visible;
    }

    private Map<ButtonType, Rectangle> registerButtons(Rectangle bounds) {
        Map<ButtonType, Rectangle> buttons = new HashMap<>();
        List<Rectangle> containers = core.getImageAnalyzer().findContainers(bounds, 913, 914, 915, 916);
        core.log(getClass().getSimpleName(), "Buttons found: " + containers.size());

        for (Rectangle container : containers) {
            String rawText = core.getOCR().getText(Font.SMALL_FONT, container, ORANGE_BUTTON_TEXT_COLOR).trim();
            core.log(getClass().getSimpleName(), "OCR result: \"" + rawText + "\"");

            String normalizedText = rawText.replace('I', 'l').toLowerCase();

            for (ButtonType buttonType : ButtonType.values()) {
                String buttonLabel = buttonType.getText().toLowerCase();

                if (normalizedText.equals(buttonLabel)) {
                    Rectangle boundsRelativeToContainer = new Rectangle(container.x - bounds.x, container.y - bounds.y, container.width, container.height);
                    buttons.put(buttonType, boundsRelativeToContainer);
                    core.log(getClass().getSimpleName(), "✔ Matched: \"" + rawText + "\" → " + buttonType.name());
                    break;
                }
            }
        }

        core.log(getClass().getSimpleName(), "Buttons recognised: " + buttons.keySet());
        return buttons;
    }

    public boolean selectOption(ButtonType buttonType) {
        Rectangle buttonScreenBounds = getButtonScreenBounds(buttonType);
        if (buttonScreenBounds == null) return false;
        if (core.getFinger().tap(buttonScreenBounds)) {
            return core.pollFramesUntil(() -> !isVisible(), 5000);
        }
        return false;
    }

    private Rectangle getButtonScreenBounds(ButtonType buttonType) {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            // interface not visible
            return null;
        }
        if (!buttons.containsKey(buttonType)) {
            core.log(getClass().getSimpleName(), "⚠ Button not found: " + buttonType + ". Available: " + buttons.keySet());
            return null;
        }
        Rectangle buttonBounds = buttons.get(buttonType);
        return bounds.getSubRectangle(buttonBounds);
    }

    public enum ButtonType {
        HOUSE_ON_THE_HILL("House on the Hill"),
        VERDANT_VALLEY("Verdant Valley"),
        STICKY_SWAMP("Sticky Swamp"),
        MUSHROOM_MEADOW("Mushroom Meadow");

        private final String text;

        ButtonType(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}