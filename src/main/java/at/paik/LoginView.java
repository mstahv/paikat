package at.paik;

import at.paik.domain.User;
import at.paik.service.Dao;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.textfield.VTextField;

import java.util.Collections;
import java.util.List;

@Route
public class LoginView extends VVerticalLayout {

    public LoginView(OttButton ottButton, Dao dao) {
        setAlignItems(Alignment.CENTER);
        add(new H3("}> Paik.at Login!"));

        add("You are not logged in. Login, register to create your own team or ask for an invite link from the hunt leader!");

        add(new PasskeyLogin());

        add(ottButton);

        add(new com.vaadin.flow.component.Component[]{new Details("Register as a new user...") {
            TextField username = new VTextField("Username") {{
                addValueChangeListener(e -> {
                    String name = e.getValue();
                    try {
                        dao.validateUsername(name);
                    } catch (Exception ex) {
                        registerButton.setEnabled(false);
                        setHelperText(ex.getMessage());
                        return;
                    }
                    registerButton.setEnabled(true);
                });

            }};

            Button registerButton = new DefaultButton("Register") {{
                addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                addClickListener(e -> {
                    User user = dao.registerUser(username.getValue());
                    // now manually "login" the new user, TODO figure out how to do this properly 🤔
                    var auth = new RegistrationAuthenticationToken(user);
                    SecurityContext context = SecurityContextHolder.getContext();
                    // This works for this request...
                    context.setAuthentication(auth);
                    // ... but we also need to save it in the session
                    new HttpSessionSecurityContextRepository().saveContext(
                            context,
                            (HttpServletRequest) VaadinRequest.getCurrent(),
                            (HttpServletResponse) VaadinResponse.getCurrent());
                    VNotification.prominent("User registered, please register a passkey in the profile view!");
                    navigate(Profile.class);
                });
            }};

            {
                add(new VerticalLayout() {{
                    add("You can register as a new user, if you don't have an account yet and want to establish your own team.");
                    add(username, registerButton);
                }});

            }
        }});
    }

    public static class RegistrationAuthenticationToken extends UsernamePasswordAuthenticationToken {
        public RegistrationAuthenticationToken(User user) {
            super(user, "", Collections.emptyList());
        }
    }
}