package at.paik;

import at.paik.domain.User;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.firitin.appframework.MainLayout;
import org.vaadin.firitin.geolocation.Geolocation;
import org.vaadin.firitin.geolocation.GeolocationEvent;
import org.vaadin.firitin.geolocation.GeolocationOptions;

public class TopLayout extends MainLayout {

    private final Session session;
    private final TeamSelector teamSelector;
    private GeolocationEvent geolocation;

    public TopLayout(Session session, TeamSelector teamSelector) {
        this.session = session;
        this.teamSelector = teamSelector;
    }

    @Override
    protected String getDrawerHeader() {
        return "}> Paik.at";
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User u) {
                addToDrawer(new Paragraph("User:" + u.getUsername()) {{
                    add(new Button(VaadinIcon.EXIT.create(), e -> {
                        session.logout();
                    }));
                }});
                addToDrawer(teamSelector);

                GeolocationOptions geolocationOptions = new GeolocationOptions();
                geolocationOptions.setEnableHighAccuracy(true);
                geolocationOptions.setMaximumAge(10*1000);
                Geolocation.watchPosition(update -> {
                            this.geolocation = update;
                            session.saveLocation(update);
                            System.out.println("Geo update for " + u.getName() + " " + update.toString());
                        },
                        error -> {
                            System.out.println("Geolocation error : " + error.getErrorMessage() + " " + error.getError());
                        }, geolocationOptions);

            } else {
                addToDrawer(new Paragraph("User:" + principal));
            }
        }

    }


    // TODO fire as some kind of events or piggypack listeners for other views.
    public GeolocationEvent getGeolocation() {
        return geolocation;
    }
}
