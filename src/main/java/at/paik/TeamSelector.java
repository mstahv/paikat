package at.paik;

import at.paik.domain.Team;
import at.paik.domain.User;
import at.paik.service.Dao;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.vaadin.firitin.components.notification.VNotification;
import org.vaadin.firitin.layouts.HorizontalFloatLayout;
import org.vaadin.firitin.util.BrowserPrompt;

@SpringComponent
@RouteScope
public class TeamSelector extends HorizontalFloatLayout {
    private final Session session;
    private final Dao service;
    private User user;

    public TeamSelector(Session session, Dao dao) {
        this.session = session;
        this.service = dao;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (session.user().isPresent()) {
            this.user = session.user().get();
            if (user.teams.isEmpty()) {
                addTeam();
            } else {
                init();
            }
        }
    }

    private void init() {
        this.user = session.user().get();
        removeAll();
        add(new ComboBox<Team>() {{
            setLabel("Current team");
            setItems(user.teams);
            setItemLabelGenerator(t -> t.name);
            Team team = session.getCurrentTeam();
            setValue(team);
            addValueChangeListener(e -> {
                session.setCurrentTeam(e.getValue());
            });
        }});
        add(new Button(VaadinIcon.PLUS.create()) {{
            setText("Add new");
            addClickListener(e -> addTeam());
        }});
    }

    private void addTeam() {
        BrowserPrompt.promptString("Name for your team:", user.getName() +"'s team").thenAccept(name -> {
            Team team = new Team();
            team.name = name;
            team.hunters.add(user);
            user.teams.add(team);
            service.storeTeams(user);
            init();
        });
    }
}
