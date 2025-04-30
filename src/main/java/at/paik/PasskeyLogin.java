package at.paik;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class PasskeyLogin extends Button {
    public PasskeyLogin() {
        super("Login with passkey", e -> {
            {
                // TODO refactor this to happen in SPA way via SecurityService, like saving passkey already works
                // This would also make no need for the filters coming from Spring Security WebAuthn  supports
                UI.getCurrent().getPage().executeJs("window.authenticateOrError();");
            }
        });
        loadWebauthJs();
        addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

    public static void loadWebauthJs() {
        try {
            String string = IOUtils.toString(LoginView.class.getResourceAsStream("/webauthn_vf.js"));
            UI.getCurrent().getPage().executeJs(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
