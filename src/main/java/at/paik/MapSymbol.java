package at.paik;

import org.vaadin.addons.maplibre.Marker;

import java.util.Map;

public enum MapSymbol {

    CROSSHAIR("""
            <svg
               width="100"
               height="100"
               viewBox="0 0 100 100"
               version="1.1"
               id="svg1"
               xmlns="http://www.w3.org/2000/svg"
               xmlns:svg="http://www.w3.org/2000/svg">
              <defs
                 id="defs1" />
              <g
                 id="layer1">
                <circle
                   style="fill:none;stroke:#000000;stroke-width:1.99937;stroke-linejoin:round;stroke-miterlimit:0.1;stroke-dasharray:none;stroke-opacity:1"
                   id="path2"
                   cx="50.075768"
                   cy="50.287952"
                   r="38.200062" />
                <path
                   style="fill:none;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1"
                   d="M 50,0 V 24.675677"
                   id="path3" />
                <path
                   style="fill:none;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1"
                   d="M 50,75.362469 V 100.03814"
                   id="path3-0" />
                <path
                   style="fill:none;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1"
                   d="M 100.61367,50 H 75.937989"
                   id="path3-5" />
                <path
                   style="fill:none;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1"
                   d="M 24.67568,50 H 0"
                   id="path3-5-8" />
                <circle
                   style="fill:#ff0000;stroke:none;stroke-width:1.99937;stroke-linejoin:round;stroke-miterlimit:0.1;stroke-dasharray:none;stroke-opacity:1"
                   id="path4"
                   cx="50"
                   cy="50"
                   r="5" />
              </g>
            </svg>
            """, 100, 100, 0, 0),
    ARROW("""
            <svg
               width="30"
               height="30"
               viewBox="0 0 0.5625 0.5625"
               version="1.1"
               xmlns="http://www.w3.org/2000/svg"
               xmlns:svg="http://www.w3.org/2000/svg">
                <path style="_SVG_STYLE_"
                   d="M 0.28125,0 0.09375,0.421875 0.1640625,0.3730591 c 0,0 0.0410234,-0.028497 0.094043,-0.0355591 l 0.001648,0.225 h 0.046875 L 0.3049805,0.3375 C 0.3584728,0.344261 0.4003784,0.3710815 0.4003784,0.3710815 L 0.46875,0.41601562 Z m 0,0.11524658 0.0918091,0.20115967 C 0.347609,0.30373425 0.3327629,0.28905025 0.28125,0.28905025 c -0.0496471,0 -0.0619519,0.0140353 -0.0878906,0.027356 z" />
            </svg>
            """, 30, 60, 0, 0),
    MARKER("""
            <svg
               width="30"
               height="30"
               viewBox="0 0 0.5625 0.5625"
               version="1.1"
               xmlns="http://www.w3.org/2000/svg"
               xmlns:svg="http://www.w3.org/2000/svg">
              <path style="_SVG_STYLE_"
                 d="m 0.28363037,0 c -0.091215,0 -0.19764404,0.05575154 -0.19764404,0.19764404 0,0.0962812 0.15203653,0.30911222 0.19764404,0.36485596 C 0.32417161,0.50675626 0.48123779,0.29899529 0.48123779,0.19764404 0.48123779,0.05575154 0.37484535,0 0.28363037,0 Z m 0.002673,0.06730957 A 0.12594795,0.12594795 0 0 1 0.41224365,0.19328613 0.12594795,0.12594795 0 0 1 0.28630371,0.31922607 0.12594795,0.12594795 0 0 1 0.16032715,0.19328613 0.12594795,0.12594795 0 0 1 0.28630337,0.06730957 Z" />
            </svg>
            """);

    private final String svg;
    private final int width;
    private final int height;
    private final int offsetx;
    private final int offsety;

    MapSymbol(String svg) {
        this(svg, 30, 30, 0, -15);
    }

    MapSymbol(String svg, int width, int height) {
        this(svg, width, height, 0, 0);
    }

    MapSymbol(String svg, int width, int height, int offsetx, int offsety) {
        this.svg = svg;
        this.width = width;
        this.height = height;
        this.offsetx = offsetx;
        this.offsety = offsety;
    }

    public void formatMarker(Marker marker) {
        this.formatMarker(marker, Map.of("_SVG_STYLE_", "fill:orange;", "_LABEL_", ""));
    }

    public void formatMarker(Marker marker, Map<String,String> replacements) {
        String html = """
                <div style="width:__W__; height:__H__;">
                """ +
                this.svg + """
                <div style="width:100%; max-width:__W__;font-size:10px; text-align:center; position:absolute;line-height:1;">_LABEL_<div>
                </div>
                """;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }
        html = html.replaceAll("__W__", width + "px");
        html = html.replace("__H__", height + "px");
        marker.setHtml(html);
        marker.setOffset(offsetx, offsety);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getSvg() {
        return svg;
    }

    public boolean rotatable() {
        return offsetx == 0 && offsety == 0;
    }
}
