package at.paik;

import at.paik.domain.Hunt;
import at.paik.domain.Spot;
import at.paik.domain.User;
import at.paik.service.Dao;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.vaadin.addons.maplibre.MapLibre;
import org.vaadin.addons.maplibre.Marker;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DeleteButton;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.layouts.HorizontalFloatLayout;
import org.vaadin.firitin.util.BrowserPrompt;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(layout = TopLayout.class)
@MenuItem(title = "Team & Assignments", icon = VaadinIcon.USERS, order = MenuItem.BEGINNING + 1)
@PermitAll
public class TeamMembersView extends VerticalLayout {
    private final Session session;
    private final OneTimeTokenService oneTimeTokenService;
    private final Dao service;
    private final Paragraph status = new Paragraph();
    private final Hr unassignedHr = new Hr();
    H2 activeTitle = new H2("Active:");
    H2 inActiveTitle = new H2("Inactive:");
    Div assigned = new Slot();
    Div unassigned = new Slot();
    Div inactive = new Slot();

    public TeamMembersView(Session session, OneTimeTokenService oneTimeTokenService, Dao service) {
        this.session = session;
        this.oneTimeTokenService = oneTimeTokenService;
        this.service = service;
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
        add(new VButton("Start new hunt", this::startHunt));
        add(new HorizontalFloatLayout(
                new VButton("Randomize remaining", this::randomize),
                new DeleteButton("Reset", this::resetAssignments) {{
                    setConfirmationPrompt("Reset all assignments?");
                    setOkText("Reset");
                }},
                new Button("Quick add...") {{
                    setTooltipText("Directly adds a new hunter to team. If hunter wants to start using the app later, a sign-in link can be shared.");
                    addClickListener(e -> {
                        User user = new User();
                        BrowserPrompt.promptString("Username")
                                .thenAccept(n -> {
                                    user.name = n;
                                    session.getCurrentTeam().hunters.add(user);
                                    user.teams.add(session.getCurrentTeam());
                                    service.saveNewUser(user);
                                    init();
                                });
                    });
                }},
                new Button("Invite...", e-> {
                    VNotification.prominent("TODO group invite link");
                })
        ));
        add(activeTitle);
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

    private void startHunt() {
        session.getCurrentTeam().startHunt();
        VNotification.prominent("TODO notifications for hunters etc.");
        init();
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
        status.setText("Hunters: %s %s/%s | Active spots: %s (%s)".formatted(
                activeHunters,
                (unassignedHunters > 0 ? "(" + unassignedHunters + ")" : ""),
                (activeHunters + inactiveHunters),
                spots,
                (spots - activeHunters)
        ));
        unassignedHr.setVisible(unassignedHunters > 0);
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

            String url = "https://paik.at/my-ott-submit?token=" + token.getTokenValue();

            getElement().executeJs("""
                    const shareData = {
                      title: "Login to Paik.at as %s",
                      text: "Use this one-time-link to login to Paik.at. Link is valid only for a a period of time. For smooth experiene in the future, register a passkey right after login.",
                      url: "%s",
                    };
                    navigator.share(shareData);
                    """.formatted(user.getUsername(), url));
        }

        private void toggle() {
            if (user.getAssignment() != null) {
                user.setSpot(null);
            }
            session.getCurrentTeam().togglSpot(user);
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
                    TabSheet tabSheet = new TabSheet();
                    tabSheet.add("List", new HorizontalFloatLayout() {
                        {
                            add(new VButton(VaadinIcon.TRASH, e -> {
                                user.setSpot(null);
                                close();
                                placeInCorrectSlot();
                                updateCard();
                            }));
                            List<Spot> activeSpots = session.getCurrentTeam().getActiveSpots();
                            for (Spot spot : activeSpots) {
                                add(new Button(spot.getName(), e -> {
                                    assign(spot);
                                }));
                            }
                        }
                    });
                    tabSheet.add("Map", new MapLibre() {{
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
                    tabSheet.setSizeFull();
                    add(tabSheet);
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
