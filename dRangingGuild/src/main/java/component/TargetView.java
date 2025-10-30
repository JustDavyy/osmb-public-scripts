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
import com.osmb.api.definition.SpriteDefinition;

import java.awt.*;

public class TargetView extends ComponentCentered {

    private static final int ORANGE_TEXT_COLOR = -21948;
    private static final Rectangle TITLE_BOUNDS = new Rectangle(0, 0, 488, 35);
    private static final String TITLE_TEXT = "Target View";
    private static final int RESULT_TEXT_COLOR = -256;

    public TargetView(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(488, 300, ColorUtils.TRANSPARENT_PIXEL);

        // Fill main area transparent
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);

        // Clear bottom half of title border
        canvas.fillRect(0, 23, canvas.canvasWidth, 17, ColorUtils.TRANSPARENT_PIXEL);

        // Draw corners only (not full border)
        drawBorderCorners(canvas, core, BorderPalette.STEEL_BORDER);

        return new ComponentImage<>(canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB), -1, 1);
    }

    private void drawBorderCorners(Canvas canvas, ScriptCore core, BorderPalette palette) {
        // Set full draw area (no trimming via setDrawArea anymore)
        canvas.setDrawArea(0, 0, canvas.canvasWidth, canvas.canvasHeight);

        SpriteDefinition topLeft = core.getSpriteManager().getSprite(palette.getTopLeftBorderID());
        SpriteDefinition topRight = core.getSpriteManager().getSprite(palette.getTopRightBorderID());
        SpriteDefinition bottomLeft = core.getSpriteManager().getSprite(palette.getBottomLeftBorderID());
        SpriteDefinition bottomRight = core.getSpriteManager().getSprite(palette.getBottomRightBorderID());

        // === Manually draw top-left clipped (skip right + bottom 6px) ===
        drawClippedSprite(canvas, topLeft, 0, 0, 0, 0, topLeft.width - 6, topLeft.height - 6);

        // === Manually draw top-right clipped (skip left + bottom 6px) ===
        int trDrawX = canvas.canvasWidth - topRight.width;
        drawClippedSprite(canvas, topRight, trDrawX + 6, 0, 6, 0, topRight.width - 6, topRight.height - 6);

        // === Bottom corners (draw full) ===
        canvas.drawSpritePixels(bottomLeft, 0, canvas.canvasHeight - bottomLeft.height);
        canvas.drawSpritePixels(bottomRight, canvas.canvasWidth - bottomRight.width, canvas.canvasHeight - bottomRight.height);
    }

    private void drawClippedSprite(Canvas canvas, SpriteDefinition sprite, int destX, int destY,
                                   int srcX, int srcY, int clipWidth, int clipHeight) {
        int[] srcPixels = sprite.pixels;
        int canvasWidth = canvas.canvasWidth;
        int[] canvasPixels = canvas.pixels;

        for (int y = 0; y < clipHeight; y++) {
            for (int x = 0; x < clipWidth; x++) {
                int srcIndex = (srcY + y) * sprite.width + (srcX + x);
                int destIndex = (destY + y) * canvasWidth + (destX + x);

                int rgb = srcPixels[srcIndex];
                if (rgb != 0 && rgb != 0xFF00FF) { // skip transparent/magenta
                    canvasPixels[destIndex] = rgb;
                }
            }
        }
    }

    @Override
    public boolean isVisible() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }

        Rectangle titleArea = bounds.getSubRectangle(TITLE_BOUNDS);
        core.getScreen().getDrawableCanvas().drawRect(titleArea, Color.RED.getRGB());

        String text = core.getOCR().getText(Font.STANDARD_FONT_BOLD, titleArea, ORANGE_TEXT_COLOR);

        return text.equalsIgnoreCase(TITLE_TEXT);
    }

    public boolean close() {
        Rectangle bounds = getBounds();
        if (bounds == null) return false;

        // Use hardcoded position based on UI design
        int relativeX = 488 - 26;
        int relativeY = 5;
        Rectangle closeRect = new Rectangle(bounds.x + relativeX, bounds.y + relativeY, 21, 21);

        core.log(getClass(), "Clicking close button at " + closeRect);
        boolean tapped = core.getFinger().tap(closeRect);
        if (!tapped) {
            core.log(getClass(), "Failed to tap close button.");
            return false;
        }

        boolean closed = core.pollFramesHuman(() -> !isVisible(), core.random(4000, 8000));
        core.log(getClass(), closed ? "TargetView closed." : "Timeout waiting for TargetView to close.");
        return closed;
    }

    public String getResultText() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return "";
        }

        Rectangle resultArea = bounds.getSubRectangle(new Rectangle(215, 85, 80, 35));
        String resultText = core.getOCR().getText(Font.STANDARD_FONT_BOLD, resultArea, RESULT_TEXT_COLOR);

        return resultText.trim();
    }
}