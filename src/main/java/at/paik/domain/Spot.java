package at.paik.domain;

import at.paik.MapSymbol;
import at.paik.SymbolSelector;
import org.locationtech.jts.geom.Point;

public class Spot {

    private String name = "";
    private Point point;
    private boolean enabled = true;
    private MapSymbol symbol;
    private Integer symbolRotation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MapSymbol getSymbol() {
        if(symbol == null) {
            return MapSymbol.MARKER;
        }
        return symbol;
    }

    public void setSymbol(MapSymbol symbol) {
        this.symbol = symbol;
    }

    public Integer getSymbolRotation() {
        if(symbolRotation == null) {
            return 0;
        }
        return symbolRotation;
    }

    public void setSymbolRotation(Integer symbolRotation) {
        this.symbolRotation = symbolRotation;
    }
}
