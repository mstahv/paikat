package at.paik;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(name = "Paikat", shortName = "Paikat",
        backgroundColor = "hsl(22, 96%, 47%)",
        themeColor = "hsl(22, 96%, 47%)"
)
@Theme(value = "paikatgreen", variant = "dark")
public class AppShell implements AppShellConfigurator {

}
