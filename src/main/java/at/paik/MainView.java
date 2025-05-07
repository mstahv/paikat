package at.paik;

import at.paik.domain.Hunt;
import at.paik.domain.Spot;
import at.paik.domain.Team;
import at.paik.domain.User;
import at.paik.service.TeamEventDistributor;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.webpush.WebPush;
import elemental.json.JsonNumber;
import elemental.json.impl.JreJsonNumber;
import jakarta.annotation.security.PermitAll;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.geolocation.GeolocationEvent;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.PI;

@Route(layout = TopLayout.class)
@MenuItem(order = 1, icon = VaadinIcon.BULLSEYE, title ="Home")
@PermitAll
public class MainView extends VVerticalLayout implements Consumer<HuntStatusEvent> {

    private final Session session;
    private final WebPush webPush;
    private Hunt hunt;
    UI ui;

    public MainView(Session session, TeamEventDistributor ted, WebPush webPush) {
        this.session = session;
        this.webPush = webPush;
        init();
        ui = UI.getCurrent();
        if(session.user().isPresent() && session.getCurrentTeam() != null) {
            ted.register( session.getCurrentTeam(), this);
        }
    }

    private void init() {
        removeAll();
        Team currentTeam = session.getCurrentTeam();
        add(new H1(currentTeam.name));
        // TODO redesing this view to be more intuitive
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
            }
        }, () -> {
            add(new Emphasis("The session is being planned, wait for your assignment..."));

            if(webPush != null) {
                // TODO figure out if Profile view would be better place for push notification settings 🤔
                // Here more prominently visible, but logically in wrong place...
                UI current = UI.getCurrent();
                webPush.subscriptionExists(current, registered -> {
                    if(registered) {
                        add(new Paragraph("You have web push notifications enabled on this device, you'll be notified even if the application is closed."));
                        if(registered && session.user().get().webPushSubscriptions.isEmpty()) {
                            /* TODO this should never happen :-) and technically empty check is not enough if a developer loses the subscription as there can be multiple devices and subscriptions for same user...  */
                            webPush.fetchExistingSubscription(ui, subscription -> {
                                session.saveWebPushSubscription(subscription);
                            });
                        }
                    } else {
                        add(new VButton("Subscribe for notifications...", e-> {
                            webPush.subscribe(ui, subscription -> {
                                session.saveWebPushSubscription(subscription);
                                Notification.show("Subsribed, you'll now get a notification even if the app is closed!");
                                e.getSource().setVisible(false);
                            });
                        }));
                    }
                });
            }
        });
    }

    private void readyToHunt() {
        session.readyToHunt();
        init();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        session.user().ifPresent(user -> {
            if(user.getPasskeys().isEmpty()) {
                attachEvent.getUI().navigate(Profile.class);

                VNotification.prominent("No passkeys attached to your account, add one now to enable all features!")
                        .withThemeVariants(NotificationVariant.LUMO_WARNING);
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
                        // TODO an actual checkbox instead so one can remove ready status, if suddenly more time is needed
                        return new VButton(VaadinIcon.CHECK_SQUARE, () -> {
                            session.markReady(u);
                            init(h);
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
