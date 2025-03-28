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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.vaadin.addons.maplibre.MapLibre;
import org.vaadin.addons.maplibre.Marker;
import org.vaadin.addons.maplibre.components.TrackerMarker;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Route(layout = TopLayout.class)
@MenuItem(icon = VaadinIcon.MAP_MARKER, parent = AdminViews.class)
@PermitAll
public class SpotMapView extends VerticalLayout {

    private final Session session;
    private MapLibre map;

    private Map<User, TrackerMarker> userToMarker = new HashMap<>();

    private Marker crosshair;

    private Button newSpotBtn = new VButton(VaadinIcon.MAP_MARKER, this::newSpot)
            .withThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE)
            .withEnabled(false);

    public SpotMapView(Session session) {
        this.session = session;
        setPadding(false);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        init();
    }

    private void init() {
        if(map != null) {
            map.removeFromParent();
            crosshair = null;
        }
        map = new MapLibre();
        addAndExpand(map);
        map.addMapClickListener(e -> {
            if(crosshair == null) {
                crosshair = map.addMarker(e.getPoint());
                MapSymbol.CROSSHAIR.formatMarker(crosshair);
                crosshair.addDragEndListener(coordinate -> {
                });
                crosshair.addClickListener(() -> {
                    crosshair.remove();
                    crosshair = null;
                    newSpotBtn.setEnabled(false);
                });
                newSpotBtn.setEnabled(true);
            } else {
                crosshair.setPoint(e.getPoint());
            }
        });
        userToMarker.clear();
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

        Set<User> hunters = currentTeam.getHunters();
        for (User h : hunters) {
            if (!h.getLastPositions().isEmpty()) {
                TrackerMarker trackerMarker = userToMarker.computeIfAbsent(h, h2 -> {
                    var marker = new TrackerMarker(map);
                    marker.setMaxAge(Duration.ofMinutes(5));
                    return marker;
                });
                trackerMarker.reset();
                for (var p : h.getLastPositions()) {
                    Coordinate coordinate = new Coordinate(p.getCoords().getLongitude(), p.getCoords().getLatitude());
                    Double heading = p.getCoords().getHeading();
                    Integer b = heading == null ? null : heading.intValue();
                    trackerMarker.addPoint(coordinate, Instant.ofEpochMilli(p.getTimestamp()), 0);
                }
                trackerMarker.getMarker().setPopover(() -> new Paragraph(h.getName() + " TODO show status, movement etcs"));
            }
        }

        findAncestor(TopLayout.class).addToNavbar(newSpotBtn);
        addDetachListener(detachEvent -> newSpotBtn.removeFromParent());

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
        if (!hunters.isEmpty()) {
            spotTitle += " (" + hunters.stream().map(User::getName).collect(Collectors.joining(",")) + ")";
        }
        var symbol = spot.getSymbol();
        symbol.formatMarker(marker, Map.of(
                "_SVG_STYLE_", active ? status.getColor() : status.getColor() + "fill-opacity:0.3;",
                "_LABEL_", spotTitle
        ));
        Integer symbolRotation = spot.getSymbolRotation();
        if(symbolRotation != null) {
            marker.setRotation(symbolRotation);
        }
        marker.setPoint(spot.getPoint());
    }

    enum SpotStatus {
        OCCUPIED("green"), ACTIVE("orange"), DISABLED("gray");

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
                        .forEach(h -> add(new H5(h.getName()) {{
                            add(new VButton(VaadinIcon.UNLINK, e -> {
                                h.setSpot(null);
                                updateMarker(s, marker);
                                init();
                            }));
                        }}));
                add(new VButton(VaadinIcon.USER_STAR, () -> {
                    new Dialog() {{
                        setHeaderTitle("Pick a user:");
                        add(new VVerticalLayout() {{
                            addAndExpand(new VGrid<User>() {{
                                addColumn(User::getName);
                                setItems(session.getCurrentTeam().getActiveHunters());
                                asSingleSelect().addValueChangeListener(e -> {
                                    session.getCurrentTeam().assignTo(e.getValue(), s);
                                    close();
                                    updateMarker(s, marker);
                                    init();
                                });
                            }});
                            add(new Button("Cancel", e -> {
                                close();
                            }));
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
            }) {{
                addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            }});
            add(new DefaultButton("Close", e -> {
                findAncestor(Popover.class).close();
            }));
        }

    }

    private void newSpot() {
        Spot spot = new Spot();
        spot.setPoint((Point) crosshair.getGeometry());
        new SpotEditor(session, spot).addDetachListener(
                detachEvent -> init());
    }


}
