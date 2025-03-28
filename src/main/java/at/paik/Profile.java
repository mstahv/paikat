package at.paik;

import at.paik.domain.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.DeleteButton;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.layouts.HorizontalFloatLayout;

import java.util.ArrayList;
import java.util.List;

@Route(layout = TopLayout.class)
@MenuItem(order = MenuItem.END, icon = VaadinIcon.USER, title = "Your profile")
@PermitAll
public class Profile extends VerticalLayout {

    private final Session session;

    public Profile(Session session) {
        this.session = session;
        initView();
    }

    private void initView() {
        removeAll();
        User user = session.user().get(); // Who came up with this API 🤣
        add(new H1("Profile: " + user.name));

        add(new H3("Passkeys:"));

        List<CredentialRecord> passkeys = new ArrayList<>(user.getPasskeys().values());
        if (passkeys.isEmpty()) {
            add(new Paragraph("No passkeys registered"));
        } else {
            passkeys.forEach(passkey -> {
                add(new HorizontalLayout() {{
                    add(new Paragraph(passkey.getLabel()));
                    add(new DeleteButton(() -> {
                        session.deletePasskey(passkey);
                        initView();
                    }));
                }});
            });
        }

        add(new Button("Register new passkey", e -> {
            session.startWebAuthnRegistration()
                    .thenRun(() -> {
                        VNotification.prominent("Passkey registered!");
                        initView();
                    });
        }));

        add(new H3("Your teams:"));

        var teams = session.user().get().teams;
        teams.forEach(t -> {
            add(new HorizontalFloatLayout() {{
                add(new VTextField() {{
                    setValue(t.name);
                    addValueChangeListener(v -> {
                        add(new VButton(VaadinIcon.CHECK, e -> {
                            t.name = getValue();
                            // Name is spread to a couple of places and action is rare, just make a full reload to update...
                            // Note, name for others don't update immediately, for perfect solution a new event would be needed.
                            UI.getCurrent().getPage().reload();
                        }));
                        v.unregisterListener();
                    });
                }});
            }});
        });


    }
}
