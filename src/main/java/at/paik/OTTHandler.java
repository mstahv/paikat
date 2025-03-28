package at.paik;

import at.paik.domain.User;
import at.paik.service.Dao;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.vaadin.firitin.components.notification.VNotification;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class OTTHandler implements OneTimeTokenGenerationSuccessHandler {

    private final List<User> users;

    public OTTHandler(Dao dao) {
        this.users = dao.getData().users;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken) throws IOException, ServletException {
        // In a real word app this would be sent to you via email/sms
        String username = oneTimeToken.getUsername();

        Optional<User> user = users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        if(user.isPresent() && user.get().getPasskeys().isEmpty()) {
            var notification = VNotification.prominent("Use this link to login or share it to actual user");
            notification.add(new VerticalLayout(){{
                String url = "http://localhost:8080/my-ott-submit?token=" + oneTimeToken.getTokenValue();
                System.out.println(url);
                add(new Anchor(url, "Click here to login"){{setRouterIgnore(true); /* This god damn wrong defualt 🤬*/ focus();}});
            }});
            notification.setDuration(20*1000);
        } else {
            VNotification.show("User not found or already has passkeys set, login with username not allowed.");
        }

    }
}
