package at.paik;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route
public class LoginView extends VerticalLayout {

    public LoginView(OttButton ottButton) {
        add("Login");

        add(new PasskeyLogin());

        add(ottButton);

    }

}
