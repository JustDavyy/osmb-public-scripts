package utils;

import com.osmb.api.item.ZoomType;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.overlay.BuffOverlay;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.image.Image;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;

import java.util.ArrayList;
import java.util.List;

import static com.osmb.api.visual.ocr.fonts.Font.SMALL_FONT;

public class WaterskinTracker {

  private final Script script;
  private final List<BuffOverlay> waterskinOverlays = new ArrayList<>();
  private final List<WaterskinTemplate> waterskinTemplates = new ArrayList<>();

  private Integer lastWaterskinCharges = null;

  private boolean waterSkinDebugMode = false;

  public WaterskinTracker(Script script, int[] waterskinIds) {
    this.script = script;

    debug("Initializing WaterskinTracker");

    if (waterskinIds == null) {
      debug("waterskinIds is null, skipping initialization");
      return;
    }

    for (int itemId : waterskinIds) {
      debug("Registering waterskin overlay for itemId: " + itemId);

      waterskinOverlays.add(new BuffOverlay(script, itemId));

      WaterskinTemplate template = buildWaterskinTemplate(itemId);

      if (template != null) {
        waterskinTemplates.add(template);
        debug("Template created for itemId: " + itemId);
      } else {
        debug("Failed to create template for itemId: " + itemId);
      }
    }

    debug("Initialization complete. Overlays: " + waterskinOverlays.size() +
            " Templates: " + waterskinTemplates.size());
  }

  public void setDebugMode(boolean enabled) {
    this.waterSkinDebugMode = enabled;
  }

  private void debug(String msg) {
    if (waterSkinDebugMode) {
      script.log("WATERSKIN_DEBUG", msg);
    }
  }

  public Integer getCharges() {

    debug("getCharges() called");

    Integer overlay = getWaterskinChargesFromBuffOverlay();

    if (overlay != null) {
      debug("Charges detected via BuffOverlay: " + overlay);
      return overlay;
    }

    debug("BuffOverlay returned null, falling back to image search");

    Integer result = getWaterskinChargesFromImageSearch();

    debug("Image search result: " + result);

    return result;
  }

  private Integer getWaterskinChargesFromBuffOverlay() {

    debug("Checking buff overlays...");

    for (BuffOverlay overlay : waterskinOverlays) {

      String text = overlay.getBuffText();

      debug("Overlay text: " + text);

      Integer parsed = parseWaterskinBuffText(text);

      if (parsed != null) {
        lastWaterskinCharges = parsed;

        debug("Parsed overlay charges: " + parsed);

        return parsed;
      }
    }

    debug("No charges detected from buff overlays");

    return 0;
  }

  private WaterskinTemplate buildWaterskinTemplate(int itemId) {

    debug("Building template for itemId: " + itemId);

    try {

      Image itemImage = script.getItemManager().getItemImage(itemId, 999, ZoomType.SIZE_1, 0xFF00FF);

      if (itemImage == null) {
        debug("Item image returned null for itemId: " + itemId);
        return null;
      }

      debug("Item image loaded: " + itemImage.getWidth() + "x" + itemImage.getHeight());

      itemImage = itemImage.subImage(0, 0, itemImage.getWidth() - 5, itemImage.getHeight() - 11);

      debug("Image cropped for template creation");

      SearchableImage searchable = itemImage.toSearchableImage(
              new SingleThresholdComparator(25),
              ColorModel.RGB
      );

      Rectangle digitArea = new Rectangle(
              0,
              Math.max(0, itemImage.getHeight() - 12),
              itemImage.getWidth(),
              12
      );

      debug("Template digit area: " + digitArea);

      return new WaterskinTemplate(searchable, digitArea);

    } catch (Exception e) {

      debug("Exception while building template: " + e.getMessage());

      return null;
    }
  }

  private Integer getWaterskinChargesFromImageSearch() {

    debug("Starting image search for waterskins");

    try {

      for (WaterskinTemplate template : waterskinTemplates) {

        debug("Searching for waterskin icon on screen");

        ImageSearchResult result = script.getImageAnalyzer().findLocation(template.icon());

        if (result == null) {
          debug("Icon not found on screen");
          continue;
        }

        Rectangle bounds = result.getBounds();

        debug("Icon found at: " + bounds);

        Rectangle numberBounds = new Rectangle(
                bounds.x + template.digitArea.x,
                bounds.y + template.digitArea.y,
                template.digitArea.width,
                template.digitArea.height
        );

        debug("OCR area: " + numberBounds);

        String text = script.getOCR().getText(
                SMALL_FONT,
                numberBounds,
                new int[]{-1, -65536}
        );

        debug("OCR result: '" + text + "'");

        Integer parsed = parseWaterskinBuffText(text);

        if (parsed != null) {

          lastWaterskinCharges = parsed;

          debug("Parsed charges from OCR: " + parsed);

          return parsed;
        }
      }

    } catch (Exception e) {

      debug("Image search exception: " + e.getMessage());
    }

    debug("Image search failed to detect charges");

    return null;
  }

  private Integer parseWaterskinBuffText(String buffText) {

    debug("Parsing text: '" + buffText + "'");

    if (buffText == null || buffText.isEmpty()) {
      debug("Text empty or null");
      return null;
    }

    buffText = buffText.trim();

    try {
      int value = Integer.parseInt(buffText);

      debug("Parsed integer value: " + value);

      if (value >= 0) {
        return value;
      }

    } catch (NumberFormatException e) {
      debug("Failed to parse integer from text: " + buffText);
    }

    return null;
  }

  private static class WaterskinTemplate {

    private final SearchableImage icon;
    private final Rectangle digitArea;

    private WaterskinTemplate(SearchableImage icon, Rectangle digitArea) {
      this.icon = icon;
      this.digitArea = digitArea;
    }

    private SearchableImage icon() {
      return icon;
    }

    private Rectangle digitArea() {
      return digitArea;
    }
  }
}