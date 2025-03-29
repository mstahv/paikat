package at.paik;

import at.paik.domain.LocationUpdate;
import at.paik.domain.Spot;
import at.paik.domain.Team;
import at.paik.domain.User;
import at.paik.service.TeamEventDistributor;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.locationtech.jts.geom.Coordinate;
import org.vaadin.addons.maplibre.MapLibre;
import org.vaadin.addons.maplibre.Marker;
import org.vaadin.addons.maplibre.components.TrackerMarker;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.geolocation.GeolocationEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Route(layout = TopLayout.class)
@MenuItem(icon = VaadinIcon.MAP_MARKER)
@PermitAll
public class HuntMapView extends VerticalLayout implements Consumer<LocationUpdate> {

    private final Session session;
    private MapLibre map;

    private Map<User, TrackerMarker> userToMarker = new HashMap<>();

    public HuntMapView(Session session, TeamEventDistributor ted) {
        this.session = session;
        setPadding(false);
        ted.registerLocationListener(session.getCurrentTeam(), this);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        init();
    }

    private void init() {
        if(map != null) {
            map.removeFromParent();
        }
        map = new MapLibre();
        addAndExpand(map);
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
                TrackerMarker trackerMarker = getTrackerMarker(h);
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
    }

    private TrackerMarker getTrackerMarker(User h) {
        TrackerMarker trackerMarker = userToMarker.computeIfAbsent(h, h2 -> {
            var marker = new TrackerMarker(map);
            marker.setMaxAge(Duration.ofMinutes(5));
            return marker;
        });
        return trackerMarker;
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

    @Override
    public void accept(LocationUpdate locationUpdate) {
        System.out.println("Location update: " + locationUpdate);
        TrackerMarker trackerMarker = getTrackerMarker(locationUpdate.user());
        GeolocationEvent event = locationUpdate.event();
        Double heading = event.getCoords().getHeading();
        if (heading == null) {
            heading = 0d;
        }
        trackerMarker.addPoint(new Coordinate(event.getCoords().getLongitude(), event.getCoords().getLatitude()),event.getInstant(), heading.intValue());
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
            add(new H5(s.getName()));
            if (session.getCurrentTeam().isActive(s)) {
                session.getCurrentTeam().huntersIn(s)
                        .forEach(h -> add(new H5(h.getName())));
            }
            add(new DefaultButton("Close", e -> {
                findAncestor(Popover.class).close();
            }));
        }

    }


}
