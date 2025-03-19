package at.paik;

import at.paik.domain.Team;
import at.paik.domain.User;
import at.paik.service.Dao;
import com.vaadin.flow.component.UI;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.vaadin.firitin.components.notification.VNotification;

@Component
public class AuthenticationEvents {

    private final Session session;
    private final Dao dao;

    public AuthenticationEvents(Dao dao, Session session) {
        this.dao = dao;
        this.session = session;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        Authentication authentication = success.getAuthentication();
        User principal = (User) authentication.getPrincipal();
        if(principal.teams.isEmpty()) {
            System.out.println("No teams found for the user, creating as stub...");
            Team team = new Team();
            team.name = "Your first team";
            team.hunters.add(principal);
            principal.teams.add(team);
            dao.storeTeams(principal);
        }
    }
}