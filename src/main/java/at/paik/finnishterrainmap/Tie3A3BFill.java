package at.paik.finnishterrainmap;

import org.vaadin.addons.maplibre.dto.expressions.In;

import static at.paik.finnishterrainmap.LocalMaastokartta.ROAD_OVERALL_BREAKPOINT;

public class Tie3A3BFill extends BasicLine {

    public Tie3A3BFill() {
        super("tieviiva-numeroidut", "#BB271A");
        setId(getClass().getName());
        setFilter(new In("kohdeluokka", Kohdeluokka.AUTOTIE_IIIA, Kohdeluokka.AUTOTIE_IIIB));
        getPaint().setLineWidth(RoadWidthConstants.ROAD_FILL_S);
        setMinZoom(ROAD_OVERALL_BREAKPOINT);
    }
}
