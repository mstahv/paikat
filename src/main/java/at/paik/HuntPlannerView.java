package at.paik;

import at.paik.domain.Hunt;
import at.paik.domain.Spot;
import at.paik.domain.Team;
import at.paik.domain.User;
import at.paik.service.Dao;
import at.paik.service.NotificationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.vaadin.addons.maplibre.MapLibre;
import org.vaadin.addons.maplibre.Marker;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.components.button.DeleteButton;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.layouts.HorizontalFloatLayout;
import org.vaadin.firitin.util.BrowserPrompt;
import org.vaadin.firitin.util.Share;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(layout = TopLayout.class)
@MenuItem(icon = VaadinIcon.USERS, order = MenuItem.BEGINNING + 1, parent = AdminViews.class)
@PermitAll
public class HuntPlannerView extends VVerticalLayout implements ActionButtonOwner {
    private final Session session;
    private final OneTimeTokenService oneTimeTokenService;
    private final Dao service;
    private final Paragraph status = new Paragraph();
    private final Hr unassignedHr = new Hr();
    private final NotificationService notificationService;
    H2 activeTitle = new H2("Active members:");
    H2 inActiveTitle = new H2("Inactive members:");
    Div assigned = new Slot();
    Div unassigned = new Slot();
    Div inactive = new Slot();
    Button newMember = new VButton(VaadinIcon.PLUS, this::createNewMember) {{
        setTooltipText("Directly adds a new hunter to team. If hunter wants to start using the app later, a sign-in link can be shared.");
    }};

    public HuntPlannerView(Session session, OneTimeTokenService oneTimeTokenService, Dao service, NotificationService notificationService) {
        this.session = session;
        this.oneTimeTokenService = oneTimeTokenService;
        this.service = service;
        this.notificationService = notificationService;
    }

    public void createNewMember() {
        User user = new User();
        BrowserPrompt.promptString("Username")
                .thenAccept(n -> {
                    user.name = n;
                    session.getCurrentTeam().hunters.add(user);
                    user.teams.add(session.getCurrentTeam());
                    service.saveNewUser(user);
                    init();
                });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        init();
    }

    private void init() {
        removeAll();
        assigned.removeAll();
        unassigned.removeAll();
        inactive.removeAll();
        add(status);

        Optional<Hunt> activeHunt = session.getCurrentTeam().getActiveHunt();
        if (activeHunt.isPresent()) {
            Hunt hunt = activeHunt.get();
            add(new Paragraph("Ongoing " + hunt.toString()));
            add(new VButton("Stop hunt", this::stopHunt));
        }
        add(new HorizontalLayout(
                new DefaultButton("Announce new hunt", this::announceHunt){{
                    setIcon(VaadinIcon.PLAY.create());
                }},
                new DeleteButton("Reset", this::resetAssignments) {{
                    setConfirmationPrompt("Reset all assignments?");
                    setOkText("Reset");
                    setIcon(VaadinIcon.TRAIN.create());
                }}
        ));
        add(activeTitle);
        add(new VButton("Randomize remaining", this::randomize).withIcon(VaadinIcon.RANDOM.create()));
        add(unassigned);
        add(unassignedHr);
        add(assigned);
        add(inActiveTitle);
        add(inactive);
        for (User u : session.getCurrentTeam().getHunters()) {
            new HunterCard(u).placeInCorrectSlot();
        }
        updateStatus();
    }

    private void stopHunt() {
        session.getCurrentTeam().closeHunt();
        VNotification.prominent("TODO notifications for hunters etc.");
        init();
    }

    private void announceHunt() {
        Team team = session.getCurrentTeam();
        team.announceHunt();
        notificationService.teamWideMessage(team.name, "Spots assigned, move to your spot!", team);
        navigate(MainView.class);
    }

    private void resetAssignments() {
        session.getCurrentTeam().resetAssignments();
        init();
    }

    private void randomize() {
        int missingspots = session.getCurrentTeam().randomSpots();
        if (missingspots > 0) {
            VNotification.prominent(missingspots + " hunters without free spot!")
                    .withThemeVariants(NotificationVariant.LUMO_WARNING);
        }
        init();
    }

    private void updateStatus() {
        int inactiveHunters = inactive.getComponentCount();
        int activeHunters = session.getCurrentTeam().getHunters().size() - inactiveHunters;
        int spots = session.getCurrentTeam().getActiveSpots().size();
        int unassignedHunters = unassigned.getComponentCount();
        status.setText("%s hunters %s | %s active spots".formatted(
                activeHunters,
                (unassignedHunters > 0 ? "(" + unassignedHunters + " unussigned)" : ""),
                spots
        ));
        unassignedHr.setVisible(unassignedHunters > 0);
    }

    @Override
    public List<Component> getButtons() {
        return List.of(newMember);
    }

    static class Slot extends Div {
        public Slot() {
            setWidthFull();
        }
    }

    class HunterCard extends HorizontalFloatLayout {
        private final User user;

        Span assignment = new Span() {{
            getStyle().setMarginRight("1em");
        }};
        Button shareAccount = new VButton(VaadinIcon.SHARE, this::ottLogin);
        Button delete = new DeleteButton() {{
            setEnabled(false);
            onClick(() -> {
                VNotification.show("TODO");
            });
        }};

        public HunterCard(User user) {
            this.user = user;
            withFullWidth();
            setSpacing(false);
            add(user.name);
            add(assignment);
            addToEnd(assignBtn);
            addToEnd(resetAssignment);
            addToEnd(shareAccount);
            addToEnd(activeToggle);
            updateCard();
        }

        private void ottLogin() {
            OneTimeToken token = oneTimeTokenService.generate(new GenerateOneTimeTokenRequest(user.getUsername()));

            var ottLoginUri = "https://paik.at/my-ott-submit?token=" + token.getTokenValue();
            /*
             * Using the browser's Share API, user can share the link via email, sms, etc.
             */
            Share.share("Join Paik.at as " +user.getUsername(),
                    """
                    Use this one-time-link to login to Paik.at. Link is valid only 
                    for a a period of time. For smooth experiene in the future, 
                    register a passkey right after login.
                    """,
                    ottLoginUri);
        }

        private void toggle() {
            if (user.getAssignment() != null) {
                user.setSpot(null);
            }
            session.getCurrentTeam().toggleUser(user);
            placeInCorrectSlot();
            updateCard();
        }

        private void placeInCorrectSlot() {
            removeFromParent();
            boolean enabled = session.getCurrentTeam().isActive(user);
            if (enabled) {
                if (user.getAssignment() == null) {
                    unassigned.add(this);
                } else {
                    assigned.add(this);
                }
            } else {
                inactive.add(this);
            }
        }

        private void deassign() {
            user.setSpot(null);
            placeInCorrectSlot();
            updateCard();
        }

        void assign() {
            new Dialog() {
                {
                    setHeaderTitle("Assing %s to spot".formatted(user.getName()));
                    add(new VButton(VaadinIcon.CLOSE){{
                        addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                        getStyle().setPosition(Style.Position.ABSOLUTE);
                        getStyle().setRight("0.5em");
                        getStyle().setTop("0.5em");
                        getStyle().setZIndex(1000);
                        addClickListener(e->close());
                    }});
                    add(new VVerticalLayout() {{
                        if(user.getAssignment() != null) {
                            add(new VButton(VaadinIcon.TRASH, e -> {
                                user.setSpot(null);
                                close();
                                placeInCorrectSlot();
                                updateCard();
                            }).withText("Remove assignment"));
                        }

                        HorizontalFloatLayout spots = new HorizontalFloatLayout();
                        HorizontalFloatLayout spotsTaken = new HorizontalFloatLayout();
                        List<Spot> activeSpots = session.getCurrentTeam().getActiveSpots();
                        for (Spot spot : activeSpots) {
                            List<User> hunters = session.getCurrentTeam().huntersIn(spot);
                            String huntersStr = hunters.isEmpty() ? "" : " (" + hunters.stream().map(User::getName).collect(Collectors.joining(",")) + ")";
                            Button assignBtn = new Button(spot.getName() + huntersStr, e -> {
                                assign(spot);
                            });
                            if (!hunters.isEmpty()) {
                                spotsTaken.add(assignBtn);
                            } else {
                                spots.add(assignBtn);
                            }
                        }
                        add(spots);
                        add(spotsTaken);
                        spots.setWrap(true);
                        spots.setSpacing("0.5em");
                        spotsTaken.setWrap(true);
                        spotsTaken.setSpacing("0.5em");

                        add(new MapLibre() {{
                            setHeight("40vh");
                            List<Spot> spots = session.getCurrentTeam().getSpots();
                            for (Spot s : spots) {
                                if (s.getPoint() != null) {
                                    Marker marker = addMarker(s.getPoint());
                                    marker.addClickListener(() -> {
                                        assign(s);
                                    });
                                }
                            }
                        }});

                    }});

                    setSizeFull();
                    open();
                }

                private void assign(Spot spot) {
                    List<User> other = session.getCurrentTeam().assignTo(user, spot);
                    if (!other.isEmpty()) {
                        VNotification.show(other.stream().map(User::getName).collect(Collectors.joining(",")) + " also on the spot!");
                    }
                    close();
                    placeInCorrectSlot();
                    updateCard();
                }

            };
        }

        private void updateCard() {
            Spot assignment1 = user.getAssignment();
            boolean assigned = assignment1 != null;
            String a = assigned ? assignment1.getName() : "";
            assignment.setText(" : " + a);
            if (session.getCurrentTeam().isActive(user)) {
                assignBtn.setVisible(true);
                resetAssignment.setVisible(assigned);
            } else {
                assignBtn.setVisible(false);
                resetAssignment.setVisible(false);
            }
            updateStatus();
        }        Button assignBtn = new VButton(VaadinIcon.LINK, this::assign);



        Button resetAssignment = new VButton(VaadinIcon.UNLINK, this::deassign);

        Button activeToggle = new Button(VaadinIcon.POWER_OFF.create()) {{
            addClickListener(e -> {
                toggle();
            });
        }};
    }

}
