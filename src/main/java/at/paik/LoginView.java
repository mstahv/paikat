package at.paik;

import at.paik.domain.User;
import at.paik.service.Dao;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.vaadin.firitin.components.RichText;
import org.vaadin.firitin.components.button.DefaultButton;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.textfield.VTextField;

import java.util.Collections;

@Route
public class LoginView extends VVerticalLayout {

    public LoginView(Dao dao) {
        setAlignItems(Alignment.CENTER);
        getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
        add(new Image("public/paikat.svg", "Logo"));
        add(new H3("Paik.at }> Login!"));

        add(new RichText().withMarkDown("""
        This is a demo app for [Vaadin](https://vaadin.com/), passwordless authentication (passkeys/webauthn), GIS features with razor sharp vector tile maps (MapLibre GL), EclipseStore, PWA, Websockets, Web Push Notifications and and other nerdy things. 
        Full source code available [via GitHub](https://github.com/mstahv/paikat).
        There are no service guarantees (DB currently not backed up and data not necessarily migrated on new version), but it should work as fully featured hunt planner and spot randomizer 😎"""));

        add(new Emphasis("""
        TIP: Add this app as "home screen web app" for best experience."""));

        add(new PasskeyLogin());

        add(new com.vaadin.flow.component.Component[]{new Details("Register as a new user...") {
            TextField username = new VTextField("Username") {{
                addValueChangeListener(e -> {
                    String name = e.getValue();
                    try {
                        dao.validateUsername(name);
                        setHelperText("");
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
                    VNotification.prominent("You are now registered, add passkey to your account to login later and to enable all features!")
                            .withThemeVariants(NotificationVariant.LUMO_WARNING);
                    navigate(Profile.class);
                });
            }};

            {
                add(new VerticalLayout() {{
                    add("You can register as a new user, if you don't have an account yet and want to establish your own team.");
                    add(username, registerButton);
                }});

                addOpenedChangeListener(event -> {
                    System.out.println("OPened changed");
                    if(event.isOpened()) {
                        username.focus();
                        username.scrollIntoView();
                    }
                });

            }
        }});
        add(new Paragraph(dao.dataSummary()));
    }

    public static class RegistrationAuthenticationToken extends UsernamePasswordAuthenticationToken {
        public RegistrationAuthenticationToken(User user) {
            super(user, "", Collections.emptyList());
        }
    }
}