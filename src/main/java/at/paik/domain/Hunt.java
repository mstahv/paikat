package at.paik.domain;

import java.time.Instant;
import java.util.HashSet;

public class Hunt {
    public Hunt(Team team) {
        this.team = team;
    }

    public enum Status {
        WAITING_FOR_HUNTERS, HUNT_IN_PROGRESS, CLOSED;
    }

    private final Team team;
    private Instant created = Instant.now();
    private Instant start;
    private Instant stop;
    private HashSet<User> readyHunters = new HashSet<>();

    @Override
    public String toString() {
        return "Hunt " + created;
    }

    /**
     *
     * @param user
     * @return true if hunt is now ready to start
     */
    public boolean markReady(User user) {
        readyHunters.add(user);
        int activeHunters = team.getActiveHunters().size();
        int ready = readyHunters.size();
        if(ready == activeHunters) {
            start = Instant.now();
            return true;
        }
        return false;
    }

    public boolean onSpot(User user) {
        return readyHunters.contains(user);
    }

    public Status getStatus() {
        if(start == null) {
            return Status.WAITING_FOR_HUNTERS;
        }
        if(stop != null) {
            return Status.CLOSED;
        }
        return Status.HUNT_IN_PROGRESS;
    }

    public Team getTeam() {
        return team;
    }
}
