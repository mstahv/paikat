package at.paik.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Team {

    public String name = "";
    public HashSet<User> hunters = new HashSet<>();
    public Set<Spot> spots = new LinkedHashSet<>();
    public HashSet<User> disabledHunters = new HashSet<>();
    public HashSet<Spot> disabledSpots = new HashSet<>();
    private Hunt activeHunt;
    private MapStyle mapStyle;

    public void announceHunt() {
        activeHunt = new Hunt(this);
    }

    public Optional<Hunt> getActiveHunt() {
        return Optional.ofNullable(activeHunt);
    }

    public void closeHunt() {
        activeHunt = null;
    }

    public boolean togglSpot(User user) {
        boolean enabled = disabledHunters.remove(user);
        if (enabled) {
            return true;
        } else {
            disabledHunters.add(user);
            return false;
        }
    }

    public Set<User> getHunters() {
        return hunters;
    }

    public List<User> getActiveHunters() {
        return hunters.stream().filter(this::isActive).toList();
    }

    public boolean isActive(User u) {
        return !disabledHunters.contains(u);
    }

    public boolean togglSpot(Spot spot) {
        boolean enabled = disabledSpots.remove(spot);
        if (enabled) {
            return true;
        } else {
            disabledSpots.add(spot);
            return false;
        }
    }

    public boolean isActive(Spot spot) {
        return !disabledSpots.contains(spot);
    }

    public List<Spot> getSpots() {
        ArrayList<Spot> spots1 = new ArrayList<>(spots);
        Collections.sort(spots1, Comparator.comparing(Spot::getName));
        return spots1;
    }

    public List<Spot> getActiveSpots() {
        HashSet<Spot> spots2 = new HashSet<>(spots);
        spots2.removeAll(disabledSpots);
        ArrayList<Spot> spots1 = new ArrayList<>(spots2);
        Collections.sort(spots1, Comparator.comparing(Spot::getName));
        return spots1;
    }

    public List<User> assignTo(User user, Spot spot) {
        List<User> otherHunters = hunters.stream().filter(h -> h.getAssignment() == spot).toList();
        user.setSpot(spot);
        return otherHunters;
    }

    public int randomSpots() {
        ArrayList<Spot> freespots = new ArrayList<>(getActiveSpots());
        ArrayList<User> users = new ArrayList<>(getHunters());
        var allUsers = users.iterator();
        while (allUsers.hasNext()) {
            User user = allUsers.next();
            if (!isActive(user)) {
                allUsers.remove();
            } else if (user.getAssignment() != null) {
                freespots.remove(user.getAssignment());
                allUsers.remove(); // no need for spot
            }
        }
        Collections.shuffle(users);
        var withoutSpot = users.iterator();
        while (withoutSpot.hasNext() && !freespots.isEmpty()) {
            var user = withoutSpot.next();
            var spot = freespots.removeFirst();
            assignTo(user, spot);
            withoutSpot.remove();
        }
        return users.size();
    }

    public void resetAssignments() {
        for(User u : hunters) {
            u.setSpot(null);
        }
    }

    public List<User> huntersIn(Spot spot) {
        return hunters.stream().filter(u -> u.getAssignment() == spot).toList();
    }

    public void setMapStyle(MapStyle mapStyle) {
        this.mapStyle = mapStyle;
    }

    public MapStyle getMapStyle() {
        return mapStyle;
    }
}
