package at.paik;

import at.paik.domain.Spot;
import at.paik.service.Dao;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.addons.maplibre.PointField;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DeleteButton;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.layouts.HorizontalFloatLayout;

@Route(layout = TopLayout.class)
@MenuItem(icon = VaadinIcon.LIST_UL, title = "Spots & dogs", parent = AdminViews.class)
@PermitAll
public class SpotListView extends VerticalLayout {
    private final Session session;
    H2 activeTitle = new H2("Active spots & dogs:");
    H2 inActiveTitle = new H2("Inactive spots:");
    Div active = new Div(){{setWidthFull();}};
    Div inactive = new Div(){{setWidthFull();}};

    public SpotListView(Session session, Dao dao) {
        this.session = session;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        init();
    }

    private void init() {
        removeAll();
        active.removeAll();
        inactive.removeAll();
        if (session.getCurrentTeam() != null) {
            add(new Button("New spot..."){{
                addClickListener(e -> {
                    new SpotEditor(session, new Spot()).addDetachListener(d -> init());
                });
            }});

            add(activeTitle);
            add(active);
            add(inActiveTitle);
            add(inactive);
            for (Spot spot : session.getCurrentTeam().getSpots()) {
                if (session.getCurrentTeam().isActive(spot)) {
                    active.add(new SpotCard(spot));
                } else {
                    inactive.add(new SpotCard(spot));
                }

            }
        }
    }

    class SpotCard extends HorizontalFloatLayout {
        private Spot spot;

        Button relocate = new Button(VaadinIcon.EDIT.create()) {{
            addClickListener(e -> {
                new SpotEditor(session, spot).addDetachListener(d -> init());
            });
        }};
        Button activeToggle = new Button(VaadinIcon.POWER_OFF.create()) {{
            addClickListener(e -> {
                toggle();
            });
        }};
        Button delete = new DeleteButton() {{
            setEnabled(false);
            onClick(() -> {
                VNotification.show("TODO");
            });
        }};

        public SpotCard(Spot spot) {
            this.spot = spot;
            setWidthFull();

            if(spot.getPoint() == null) {
                addToStart(new Emphasis(spot.getName()));
            } else {
                addToStart(new Div(spot.getName()));
            }
            addToEnd(relocate,activeToggle);
        }

        private void toggle() {
            boolean enabled = session.getCurrentTeam().togglSpot(spot);
            if (enabled) {
                active.add(this);
            } else {
                inactive.add(this);
            }
        }
    }

    public static class SpotPicker extends PointField {
        public SpotPicker(Spot spot) {
            setSizeFull();
            setValue(spot.getPoint());
            addValueChangeListener(e -> {
                spot.setPoint(e.getValue());
            });
        }
    }

}
