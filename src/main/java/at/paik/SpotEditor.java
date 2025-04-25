package at.paik;

import at.paik.domain.Spot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import org.vaadin.addons.maplibre.Marker;
import org.vaadin.addons.maplibre.PointField;
import org.vaadin.firitin.components.dialog.VDialog;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.form.BeanValidationForm;
import org.vaadin.firitin.layouts.HorizontalFloatLayout;

import java.util.List;

public class SpotEditor extends BeanValidationForm<Spot> {
    private final Session session;
    private VTextField name = new VTextField();
    private SymbolSelector symbol = new SymbolSelector();
    private CompassField symbolRotation = new CompassField();
    private PointField point = new PointField(){{
        setHeight("70vh");
        getMap().setCursor("crosshair");
        symbol.addValueChangeListener(e -> {
            point.setMarkerFormatter(m -> formatMarker(m));
            if(e.getValue().rotatable()) {
                symbolRotation.setEnabled(true);
            } else {
                symbolRotation.setValue(0);
                symbolRotation.setEnabled(false);
            }
        });
        symbolRotation.addValueChangeListener(e -> {
            point.setMarkerFormatter(m -> formatMarker(m));
        });
    }};

    public SpotEditor(Session session, Spot spot, Double zoomLevel) {
        super(Spot.class);
        this.session = session;
        point.setMarkerFormatter(this::formatMarker);
        if(zoomLevel != null) {
            point.getMap().setZoomLevel(zoomLevel);
        }
        setEntity(spot);
        symbolRotation.setEnabled(symbol.getValue().rotatable());
        setSaveCaption("OK");
        setSavedHandler(s -> {
            session.saveSpot(spot);
            getPopup().close();
        });
        openInModalPopup();
    }

    @Override
    protected HasComponents getFormLayout() {
        return new VVerticalLayout().withPadding(false);
    }

    @Override
    public VDialog openInModalPopup() {
        VDialog dialog = super.openInModalPopup();
        dialog.setHeaderTitle("Spot editor");
        dialog.setSizeFull();
        return dialog;
    }

    @Override
    protected List<Component> getFormComponents() {
        return List.of(new HorizontalFloatLayout(name, symbol, symbolRotation), point);
    }

    void formatMarker(Marker marker) {
        symbol.getValue().formatMarker(marker);
        Integer value = symbolRotation.getValue();
        if(value != null) {
            marker.setRotation(value);
        }
    }
}
