package at.paik;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;
import org.vaadin.firitin.appframework.MainLayout;
import org.vaadin.firitin.geolocation.Geolocation;
import org.vaadin.firitin.geolocation.GeolocationEvent;
import org.vaadin.firitin.geolocation.GeolocationOptions;

public class TopLayout extends MainLayout {

    private final Session session;
    private GeolocationEvent geolocation;

    private HorizontalLayout actions = new HorizontalLayout() {{
        getStyle().setPosition(Style.Position.ABSOLUTE);
        getStyle().setRight("1em");
    }};

    public TopLayout(Session session) {
        this.session = session;
    }

    @Override
    protected String getDrawerHeader() {
        addToDrawer(new Image("/public/paikat.svg", "Logo"){{
            setHeight("4em");
            getStyle().setMargin("1em");
        }});
        return "Paik.at }>";
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        session.user().ifPresent(u -> {
                    addToDrawer(new HorizontalLayout() {{
                        setPadding(true);
                        add(new Paragraph(u.getUsername()));
                        add(new Button(VaadinIcon.EXIT.create(), e -> {
                            session.logout();
                        }));
                    }});

                    GeolocationOptions geolocationOptions = new GeolocationOptions();
                    geolocationOptions.setEnableHighAccuracy(true);
                    Geolocation.watchPosition(update -> {
                                this.geolocation = update;
                                session.saveLocation(update);
                            },
                            error -> {
                                System.out.println("Geolocation error : " + error.getErrorMessage() + " " + error.getError());
                            }, geolocationOptions);
                }
        );

    }


    // TODO fire as some kind of events or piggypack listeners for other views.
    public GeolocationEvent getGeolocation() {
        return geolocation;
    }


    @Override
    public void showRouterLayoutContent(HasElement content) {
        super.showRouterLayoutContent(content);
        actions.removeAll();
        if (content instanceof ActionButtonOwner abo) {
            actions.add(abo.getButtons());
            addToNavbar(true, actions);
        }
    }

    @Override
    public void removeRouterLayoutContent(HasElement oldContent) {
        super.removeRouterLayoutContent(oldContent);
    }
}
