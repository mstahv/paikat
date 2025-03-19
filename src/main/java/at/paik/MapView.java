package at.paik;

import at.paik.domain.Spot;
import at.paik.domain.Team;
import at.paik.domain.User;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.addons.maplibre.MapLibre;
import org.vaadin.addons.maplibre.Marker;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;

import java.util.List;
import java.util.stream.Collectors;

@Route(layout = TopLayout.class)
@MenuItem(icon = VaadinIcon.MAP_MARKER)
@PermitAll
public class MapView extends VerticalLayout {

    private final Session session;
    MapLibre map = new MapLibre();

    public MapView(Session session) {
        this.session = session;
        setPadding(false);
        addAndExpand(map);
        // TODO figure out if it is better to have a separate dialog to create new spot
        map.addMapClickListener(e -> {
            Spot spot = new Spot();
            spot.setPoint(e.getPoint());
            new SpotEditor(session, spot)
                    .addDetachListener(d -> init());
        });

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        init();
    }

    private void init() {
        map.removeAll();
        Team currentTeam = session.getCurrentTeam();

        List<Spot> spots = currentTeam.getSpots();
        for (Spot s : spots) {
            if (s.getPoint() != null) {
                Marker marker = spotMarker(s);
                marker.setPopover(() -> {
                    return new SpotPopover(s, marker);

                });
            }
        }

    }

    public Marker spotMarker(Spot spot) {
        Marker marker = map.addMarker(spot.getPoint());
        updateMarker(spot, marker);
        return marker;
    }

    private void updateMarker(Spot spot, Marker marker) {
        boolean active = session.getCurrentTeam().isActive(spot);
        List<User> hunters = session.getCurrentTeam().huntersIn(spot);
        SpotStatus status;
        if (!active) {
            status = SpotStatus.DISABLED;
        } else {
            if (hunters.isEmpty()) {
                status = SpotStatus.ACTIVE;
            } else {
                status = SpotStatus.OCCUPIED;
            }
        }
        String spotTitle = spot.getName();
        if(!hunters.isEmpty()) {
            spotTitle += " ("+hunters.stream().map(User::getName).collect(Collectors.joining(",")) +")";
        }
        var svgMarker = """
                <svg width="30px" height="30px" viewBox="0 0 15 15" version="1.1" id="marker" xmlns="http://www.w3.org/2000/svg">
                  <path style="_S_" id="p" d="M7.5,0C5.0676,0,2.2297,1.4865,2.2297,5.2703&#xA;&#x9;C2.2297,7.8378,6.2838,13.5135,7.5,15c1.0811-1.4865,5.2703-7.027,5.2703-9.7297C12.7703,1.4865,9.9324,0,7.5,0z"/>
                </svg>
                <div style="width:100%; max-width:30px;font-size:10px; text-align:center; position:absolute;line-height:1;">_N_<div>
                """.replace("_N_", spotTitle)
                .replace("_S_", active ? status.getColor() : status.getColor() + "fill-opacity:0.3;");
        marker.setHtml(svgMarker);
        marker.setPoint(spot.getPoint());
    }

    enum SpotStatus {
        OCCUPIED("green"), ACTIVE("blue"), DISABLED("gray");

        private final String color;

        SpotStatus(String color) {
            this.color = color;
        }

        public String getColor() {
            return "fill:" + color + ";";
        }
    }

    private class SpotPopover extends VerticalLayout {
        final Spot s;
        private final Marker marker;

        public SpotPopover(Spot s, Marker marker) {
            this.s = s;
            this.marker = marker;
            init();
        }

        void init() {
            removeAll();
            add(new VHorizontalLayout(new H5(s.getName()), new VButton(VaadinIcon.POWER_OFF, () -> {
                session.getCurrentTeam().togglSpot(s);
                init();
                updateMarker(s, marker);
            })).withAlignItems(Alignment.BASELINE));
            if (session.getCurrentTeam().isActive(s)) {
                session.getCurrentTeam().huntersIn(s)
                        .forEach(h -> add(new H5(h.getName()){{
                            add(new VButton(VaadinIcon.UNLINK, e-> {
                                h.setSpot(null);
                                updateMarker(s, marker);
                                init();
                            }));
                        }}));
                add(new VButton(VaadinIcon.USER_STAR, () -> {
                    new Dialog() {{
                        setHeaderTitle("Pick a user:");
                        add(new VVerticalLayout(){{
                            addAndExpand(new VGrid<User>(){{
                                addColumn(User::getName);
                                setItems(session.getCurrentTeam().getActiveHunters());
                                asSingleSelect().addValueChangeListener(e -> {
                                    session.getCurrentTeam().assignTo(e.getValue(), s);
                                    close();
                                    updateMarker(s, marker);
                                    init();
                                });
                            }});
                            add(new Button("Cancel", e->{close();}));
                        }});
                    }}.open();
                }));

            } else {
                add(new Emphasis("Inactive"));
            }
            add(new Button("Edit spot", e -> {
                new SpotEditor(session, s)
                        .addDetachListener(e1 -> {
                            init();
                            updateMarker(s, marker);
                        });
            }){{
                addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            }});
            add(new DefaultButton("Close", e-> {
                findAncestor(Popover.class).close();
            }));
        }

        public class HunterCard extends HorizontalLayout {
            public HunterCard(User hunter) {
                add(hunter.getName());
                add(new VButton(VaadinIcon.CLOSE, () -> {
                    VNotification.prominent("TODO inassosiate");
                }));
            }
        }

    }
}
