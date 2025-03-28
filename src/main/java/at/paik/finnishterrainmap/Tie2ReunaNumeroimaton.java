package at.paik.finnishterrainmap;

import org.vaadin.addons.maplibre.dto.expressions.In;

import static at.paik.finnishterrainmap.LocalMaastokartta.ROAD_OVERALL_BREAKPOINT;


public class Tie2ReunaNumeroimaton extends BasicLine {

    public Tie2ReunaNumeroimaton() {
        super("tieviiva-muut", "#000000");
        setId(getClass().getName());
        setFilter(new In("kohdeluokka", Kohdeluokka.AUTOTIE_IIA, Kohdeluokka.AUTOTIE_IIB));
        getPaint().setLineWidth(RoadWidthConstants.ROAD_EDGE_M);
        setMinZoom(ROAD_OVERALL_BREAKPOINT);
    }
}
