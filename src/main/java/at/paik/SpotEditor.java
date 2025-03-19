package at.paik;

import at.paik.domain.Spot;
import at.paik.service.Dao;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import org.vaadin.addons.maplibre.PointField;
import org.vaadin.firitin.components.dialog.VDialog;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.form.BeanValidationForm;

import java.util.List;

public class SpotEditor extends BeanValidationForm<Spot> {
    private final Session session;
    private VTextField name = new VTextField();
    private PointField point = new PointField(){{
        setHeight("70vh");
    }};

    public SpotEditor(Session session, Spot spot) {
        super(Spot.class);
        this.session = session;
        setEntity(spot);
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
        return List.of(name, point);
    }
}
