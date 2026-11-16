package mod.crend.dynamiccrosshair.crosshair;

/** Result of one context evaluation. */
public class CrosshairState {

    public CrosshairVariant primary = CrosshairVariant.NONE;
    public CrosshairVariant secondary = CrosshairVariant.NONE;
    public boolean correctTool;
    public boolean incorrectTool;

    public boolean hasInteraction() {
        return primary != CrosshairVariant.NONE || secondary != CrosshairVariant.NONE || correctTool || incorrectTool;
    }

    public void clear() {
        primary = CrosshairVariant.NONE;
        secondary = CrosshairVariant.NONE;
        correctTool = false;
        incorrectTool = false;
    }

    @Override
    public String toString() {
        return "CrosshairState{" + primary + ", " + secondary + (correctTool ? ", correct" : "")
                + (incorrectTool ? ", incorrect" : "") + "}";
    }
}
