package at.paik.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.vaadin.firitin.geolocation.GeolocationEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class User implements PublicKeyCredentialUserEntity, UserDetails {
    public String name;
    // This will generate circular reference with Jackson (use with WebAuthn)
    @JsonIgnore
    public Set<Team> teams = new HashSet<>();
    public Map<Bytes, CredentialRecord> passkeys = new HashMap<>();
    @JsonIgnore
    public Team lastTeam;
    public Bytes webAuthnId = Bytes.random();
    @JsonIgnore
    private Spot assignment;

    @JsonIgnore
    private transient LinkedList<GeolocationEvent> lastPositions;

    public Map<Bytes, CredentialRecord> getPasskeys() {
        return passkeys;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Bytes getId() {
        return webAuthnId;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return name;
    }

    public Spot getAssignment() {
        return assignment;
    }

    public void setSpot(Spot assignment) {
        this.assignment = assignment;
    }

    public void savePosition(GeolocationEvent locationUpdate) {
        // TODO ignore positions with weak accuracy
        if(lastPositions == null) {
            lastPositions = new LinkedList<>();
        }
        lastPositions.add(locationUpdate);
        // TODO evict based on age
    }

    public List<GeolocationEvent> getLastPositions() {
        if(lastPositions == null) {
            lastPositions = new LinkedList<>();
        }
        return Collections.unmodifiableList(lastPositions);
    }
}
