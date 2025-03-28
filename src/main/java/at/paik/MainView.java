package at.paik;

import at.paik.domain.Hunt;
import at.paik.domain.Spot;
import at.paik.domain.Team;
import at.paik.domain.User;
import at.paik.service.TeamEventDistributor;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.geolocation.GeolocationEvent;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.PI;

@Route(layout = TopLayout.class)
@MenuItem(order = 1, icon = VaadinIcon.BULLSEYE, title ="Home")
@AnonymousAllowed
public class MainView extends VVerticalLayout implements Consumer<HuntStatusEvent> {

    private final Session session;
    private Hunt hunt;
    UI ui;

    public MainView(Session session, TeamEventDistributor ted) {
        this.session = session;
        init();
        ui = UI.getCurrent();
        if(session.user().isPresent() && session.getCurrentTeam() != null) {
            ted.register( session.getCurrentTeam(), this);
        }
    }

    private void init() {
        removeAll();
        if (session.user().isPresent()) {

            Team currentTeam = session.getCurrentTeam();
            currentTeam.getActiveHunt().ifPresentOrElse(h -> {
                hunt = h;

                add(new H3(h.toString()));

                Spot assignment = session.user().get().getAssignment();
                if (assignment == null) {
                    add("You are not yet assigned to a spot.");
                } else {
                    add(new H5("Assignment: " + assignment.getName()));
                    session.getLatestLocation().ifPresent(geolocationEvent -> {
                        var coordinate = new Coordinate(geolocationEvent.getCoords().getLongitude(), geolocationEvent.getCoords().getLatitude());
                        GeometryFactory gf = new GeometryFactory();

                        List<GeolocationEvent> lastPositions = session.user().get().getLastPositions();
                        int distance = (int) (assignment.getPoint().distance(gf.createPoint(coordinate))* (PI/180) * 6378137/2);
                        var timestamp = Instant.ofEpochMilli(lastPositions.getLast().getTimestamp());
                        Instant now = Instant.now();
                        add(new H5("Distance to assignment: %d meters (%s)".formatted(distance, timestamp)));
                    });

                    if (!h.onSpot(session.user().get())) {
                        add(new DefaultButton("Ready to hunt!", this::readyToHunt));
                    } else {
                        h.getStatus();
                        if(h.getStatus() == Hunt.Status.HUNT_IN_PROGRESS) {
                            add(new H5("Hunt in progress!"){{getStyle().setColor("green");}});
                        } else {
                            add(new H5("Waiting for others"));
                        }
                    }

                    add(new HuntSummary(h));
                    add("It works?!, tähän jahdin tila, onko passit valmiit, statistiikat ja AI arviot kuinka kaukana ollaan, lopetus/uuden aloitus");
                }
            }, () -> {
                add(new Emphasis("No ongoing hunt, wait for your assignment..."));
            });
        } else {
            add(new H3("Welcome to Paik.at hunting spot management system!"));

            add("You are not logged in. Login, register to create your own team or ask for an invite link from the hunt leader!");

            add(new PasskeyLogin());

            add(new VButton("Register as new user...", e -> Notification.show("TODO")).withThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE));
        }
    }

    private void readyToHunt() {
        session.readyToHunt();
        init();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        GeolocationEvent geolocation = findAncestor(TopLayout.class).getGeolocation();
        if (geolocation != null) {
            add(new Emphasis("Your location: " + geolocation.getCoords() + " TODO X meters from the spot."));
        }

        session.user().ifPresent(user -> {
            if(user.getPasskeys().isEmpty()) {
                attachEvent.getUI().navigate(Profile.class);

                VNotification.prominent("No passkeys attached to your account, add one now!");
            }
        });

    }

    @Override
    public void accept(HuntStatusEvent event) {
        init();
    }

    public class HuntSummary extends VVerticalLayout {

        public HuntSummary(Hunt h) {
            withPadding(false);
            init(h);
        }

        private void init(Hunt h) {
            removeAll();
            addAndExpand(new VGrid<User>() {
                {
                    setItems(session.getCurrentTeam().getActiveHunters());
                    addColumn(u -> u.getName()).setHeader("Hunter");
                    addColumn(u -> {
                        return h.onSpot(u) ? "On spot" : "-";
                    }).setHeader("Status");
                    addColumn(u -> {
                        return "1m ago, 100m from ⦁";
                    }).setHeader("Last seen");
                    addComponentColumn(u -> {
                        // TODO an actual checkbox instead so can remove status
                        return new VButton(VaadinIcon.CHECK_SQUARE, () -> {
                            session.markReady(u);
                            init(h);
                            VNotification.prominent("TODO event/notifications/redraw others");
                        });
                    }).setHeader("");
                    withRowStyler((user, style) -> {
                        if (h.onSpot(user)) {
                            // set with variables??
                            style.setBackgroundColor("green");
                            style.setColor("white");
                        }
                    });
                }
            });
        }
    }

}
