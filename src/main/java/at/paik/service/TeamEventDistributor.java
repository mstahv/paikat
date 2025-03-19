package at.paik.service;

import at.paik.HuntStatusEvent;
import at.paik.domain.Hunt;
import at.paik.domain.Team;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@Service
public class TeamEventDistributor {

    private HashMap<Team, List<Consumer<HuntStatusEvent>>> teamToListeners = new HashMap();
    private WeakHashMap<Component, UI> componentToUi = new WeakHashMap<>();

    public void register(Team team, Consumer<HuntStatusEvent> component) {
        Component c = (Component) component;
        List<Consumer<HuntStatusEvent>> consumers = teamToListeners.computeIfAbsent(team, t -> new ArrayList<>());
        consumers.add(component);
        c.addDetachListener(e -> consumers.remove(component));
        componentToUi.put(c, c.getUI().orElseGet(() -> UI.getCurrent()));
    }

    public void fire(HuntStatusEvent event) {
        Hunt hunt = event.getHunt();
        Team team = hunt.getTeam();
        List<Consumer<HuntStatusEvent>> list = teamToListeners.get(team);
        if(list != null) {
            List<Consumer<HuntStatusEvent>> consumers = Collections.unmodifiableList(list);
            consumers.forEach(c -> {
                componentToUi.get(c).access(() -> {
                    c.accept(event);
                });
            });
        }
    }
}
