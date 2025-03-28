package at.paik;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route
public class LoginView extends VerticalLayout {

    public LoginView(OttButton ottButton) {
        setAlignItems(Alignment.CENTER);
        add(new H1("}> Paik.at Login"));

        add(new PasskeyLogin());

        add(ottButton);

    }

}
