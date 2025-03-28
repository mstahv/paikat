package at.paik.finnishterrainmap;

public class SuurjanniteLinja extends BasicLine {
    public SuurjanniteLinja() {
        super("sahkolinja", "#666");
        getPaint().setLineWidth(0.75);
        setMinZoom(13);
        // TODO
    }
}
