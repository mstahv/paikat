package at.paik;

import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.customfield.CustomField;
import org.vaadin.firitin.components.button.VButton;

import java.util.Arrays;
import java.util.List;

public class SymbolSelector extends CustomField<MapSymbol> {

    private final List<? extends VButton> buttons;
    private MapSymbol value;

    public SymbolSelector() {
        super(MapSymbol.MARKER);
        buttons = Arrays.stream(MapSymbol.values()).map(s -> new VButton() {{
            Svg svg = new Svg();
            svg.setSvg(s.getSvg());
            // SVG componet still has the wrapper element...
            svg.getElement().executeJs("""
                    this.firstChild.style.maxWidth = '1em';
                    """);
            setIcon(svg);
            addClickListener(e -> {
                setModelValue(s, true);
                setPresentationValue(s);
            });
        }}).toList();
        buttons.forEach(this::add);
        setPresentationValue(MapSymbol.MARKER);
    }

    @Override
    protected MapSymbol generateModelValue() {
        return value;
    }

    @Override
    protected void setPresentationValue(MapSymbol mapSymbol) {
        if(value != null) {
            buttons.get(value.ordinal()).getStyle().remove("background-color");
        }
        buttons.get(mapSymbol.ordinal()).getStyle().setBackgroundColor("lightblue");
        this.value = mapSymbol;
   }
}
