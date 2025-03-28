package at.paik.finnishterrainmap;

import org.vaadin.addons.maplibre.dto.expressions.In;

import static at.paik.finnishterrainmap.LocalMaastokartta.ROAD_OVERALL_BREAKPOINT;


public class Tie1A1BReuna extends BasicLine {

    public Tie1A1BReuna() {
        super("tieviiva-numeroidut", "#000000");
        setId(getClass().getName());
        setFilter(new In("kohdeluokka",  Kohdeluokka.AUTOTIE_IA, Kohdeluokka.AUTOTIE_IB));
        getPaint().setLineWidth(RoadWidthConstants.ROAD_EDGE_L);
        setMinZoom(ROAD_OVERALL_BREAKPOINT);
    }
}
