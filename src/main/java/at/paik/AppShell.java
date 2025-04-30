package at.paik;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(name = "Paikat", shortName = "Paikat",
        themeColor = "hsl(31, 100%, 42%)"
)
@Theme(value = "paikatgreen", variant = "dark")
public class AppShell implements AppShellConfigurator {

}
