package at.paik;

import at.paik.domain.LocationUpdate;
import at.paik.domain.Spot;
import at.paik.domain.Team;
import at.paik.domain.User;
import at.paik.service.Dao;
import at.paik.service.TeamEventDistributor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.apache.commons.io.IOUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.vaadin.firitin.geolocation.GeolocationEvent;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Component
@SessionScope
public class Session {

    private static ObjectMapper om = Jackson2ObjectMapperBuilder.json().modules(new WebauthnJackson2Module()).build();

    private final AuthenticationContext authenticationContext;
    private final UserCredentialRepository userCredentialRepository;
    private final WebAuthnRelyingPartyOperations rpOperations;
    private final Dao dao;
    private final TeamEventDistributor teamEventDistributor;
    private PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions;
    private Team currentTeam;

    public Session(Dao dao, AuthenticationContext authenticationContext, UserCredentialRepository userCredentialRepository, WebAuthnRelyingPartyOperations rpOperations, TeamEventDistributor teamEventDistributor) {
        this.dao = dao;
        this.authenticationContext = authenticationContext;
        this.userCredentialRepository = userCredentialRepository;
        this.rpOperations = rpOperations;
        this.teamEventDistributor = teamEventDistributor;
    }

    public static void loadWebauthJs() {
        try {
            String string = IOUtils.toString(Session.class.getResourceAsStream("/webauthn_vf.js"));
            UI.getCurrent().getPage().executeJs(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> user() {
        return authenticationContext.getAuthenticatedUser(User.class);
    }

    public void logout() {
        authenticationContext.logout();
    }

    /**
     * Start the WebAuthn registration process for the current user. This method does pretty much
     * the same as Spring Security's default filters can do, but in a more SPA friendly way.
     *
     * @return a {@link CompletableFuture} that completes when the registration is done.
     */
    public CompletableFuture<Void> startWebAuthnRegistration() {
        // These contain the user identity and the relying party information
        publicKeyCredentialCreationOptions = this.rpOperations.createPublicKeyCredentialCreationOptions(
                new ImmutablePublicKeyCredentialCreationOptionsRequest(SecurityContextHolder.getContext().getAuthentication()));
        try {
            loadWebauthJs();
            return UI.getCurrent().getPage().executeJs("""
                            const label = "%s";
                            const creds = %s;
                            const pk = await window.register(creds, label);
                            return JSON.stringify(pk);
                            """.formatted(
                            LocalDateTime.now().toString(),
                            om.writeValueAsString(publicKeyCredentialCreationOptions)))
                    .toCompletableFuture(String.class)
                    .thenAccept(publicKeyJson -> {
                        try {
                            RelyingPartyPublicKey publicKey = om.readValue(publicKeyJson, RelyingPartyPublicKey.class);
                            // this passkey negotiated with browser and the server now needs to be stored in the database
                            rpOperations.registerCredential(new ImmutableRelyingPartyRegistrationRequest(publicKeyCredentialCreationOptions, publicKey));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deletePasskey(CredentialRecord passkey) {
        // TODO
    }

    public Team getCurrentTeam() {
        if (currentTeam == null) {
            currentTeam = user().get().lastTeam;
            if (currentTeam == null && !user().get().teams.isEmpty()) {
                currentTeam = user().get().teams.iterator().next();
                user().get().lastTeam = currentTeam;
            }
        }
        return currentTeam;
    }

    public void setCurrentTeam(Team team) {
        currentTeam = team;
    }

    public void saveSpot(Spot spot) {
        dao.saveSpot(getCurrentTeam(), spot);
    }

    public void readyToHunt() {
        if (currentTeam.getActiveHunt().get().markReady(user().get())) {
            teamEventDistributor.fire(new HuntStatusEvent(currentTeam.getActiveHunt().get()));
        }
    }

    public void markReady(User user) {
        boolean huntReady = getCurrentTeam().getActiveHunt().get().markReady(user);
        if(huntReady) {
            teamEventDistributor.fire(new HuntStatusEvent(currentTeam.getActiveHunt().get()));
        }
    }

    public void saveLocation(GeolocationEvent update) {
        user().get().savePosition(update);
        teamEventDistributor.fire(new LocationUpdate(getCurrentTeam(), user().get(), update));
    }

    public Optional<GeolocationEvent> getLatestLocation() {
        try {
            GeolocationEvent last = user().get().getLastPositions().getLast();
            return Optional.of(last);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
