package at.paik;

import at.paik.domain.User;
import at.paik.service.Dao;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ott.InMemoryOneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Delegating the responsibility of general configurations
        // of http security to the super class. It's configuring
        // the followings: Vaadin's CSRF protection by ignoring
        // framework's internal requests, default request cache,
        // ignoring public views annotated with @AnonymousAllowed,
        // restricting access to other views/endpoints, and enabling
        // NavigationAccessControl authorization.
        // You can add any possible extra configurations of your own
        // here (the following is just an example):

        // http.rememberMe().alwaysRemember(false);

        // Configure your static resources with public access before calling
        // super.configure(HttpSecurity) as it adds final anyRequest matcher

        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                        new AntPathRequestMatcher("/public/**"),
                        new AntPathRequestMatcher(("/my-ott-submit")))
                .permitAll());

        http.oneTimeTokenLogin(httpSecurityOneTimeTokenLoginConfigurer -> {
            //httpSecurityOneTimeTokenLoginConfigurer.showDefaultSubmitPage(false);
        });
        http.webAuthn(withDefaults());


        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/webauthn/**")).permitAll());
        http.csrf(cfg -> cfg.ignoringRequestMatchers(
                new AntPathRequestMatcher("/webauthn/**"), new AntPathRequestMatcher("/login/webauthn")));

        super.configure(http);


        // This is important to register your login view to the
        // navigation access control mechanism:
        setLoginView(http, LoginView.class);
    }

    @Value("${webauthn.id}")
    String webauthnId;
    @Value("${webauthn.origin}")
    String webauthnOrigin;

    @Bean
    public WebAuthnRelyingPartyOperations relyingPartyOperations(PublicKeyCredentialUserEntityRepository userEntities, UserCredentialRepository userCredentials) {
        return new Webauthn4JRelyingPartyOperations(userEntities, userCredentials,
                PublicKeyCredentialRpEntity.builder().id(
                                webauthnId)
                        .name("Paik.at").build(), Set.of(webauthnOrigin));
    }


    @Bean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(Session session, Dao dao) {
        return new PublicKeyCredentialUserEntityRepository() {

            @Override
            public PublicKeyCredentialUserEntity findById(Bytes id) {
                System.out.println("Find by id " + id);
                return dao.getData().users.stream().filter(u -> u.webAuthnId.equals(id)).findAny().get();
            }

            @Override
            public PublicKeyCredentialUserEntity findByUsername(String username) {
                Optional<User> any = dao.getData().users.stream().filter(u -> u.getName().equals(username)).findAny();
                if (any.isEmpty()) {
                    return null;
                } else {
                    User user = any.get();
                    if (user.webAuthnId == null) {
                        return null;
                    } else {
                        return user;
                    }

                }
            }

            @Override
            public void save(PublicKeyCredentialUserEntity userEntity) {
                Optional<User> user = session.user();
                if (!user.isEmpty()) {
                    user.get().webAuthnId = userEntity.getId();
                } else {
                    // Login
                }
                System.out.println(userEntity);
            }

            @Override
            public void delete(Bytes id) {
                System.out.println(id);
            }
        };
    }

    @Bean
    public UserCredentialRepository userCredentialRepository(Session session, Dao dao) {
        return new UserCredentialRepository() {
            @Override
            public void delete(Bytes credentialId) {
                // TODO
            }

            @Override
            public void save(CredentialRecord credentialRecord) {
                System.out.println("saving CredentialRecord");
                Optional<User> user = session.user();

                if (user.isEmpty()) {
                    user = dao.getData().users.stream().filter(u -> u.webAuthnId.equals(credentialRecord.getUserEntityUserId())).findAny();
                }

                User user1 = user.get();
                user1.getPasskeys().put(credentialRecord.getCredentialId(), credentialRecord);
                dao.storeCredential(user1, credentialRecord);
            }

            @Override
            public CredentialRecord findByCredentialId(Bytes credentialId) {
                // TODO add map for efficiency (if this ever becomes a problems...)
                for (User u : dao.getData().users) {
                    CredentialRecord credentialRecord = u.getPasskeys().get(credentialId);
                    if (credentialRecord != null) {
                        return credentialRecord;
                    }
                }
                return null;
            }

            @Override
            public List<CredentialRecord> findByUserId(Bytes userId) {
                Optional<User> user = dao.getData().users.stream().filter(u -> u.webAuthnId.equals(userId)).findAny();
                if (user.isEmpty()) {
                    return Collections.emptyList();
                }
                return Collections.unmodifiableList(new ArrayList<>(user.get().getPasskeys().values()));
            }
        };
    }

    @Bean
    OneTimeTokenGenerationSuccessHandler tokenGenerationSuccessHandler(Dao dao) {
        return new OTTHandler(dao);
    }

    @Bean
    OneTimeTokenService oneTimeTokenService() {
        // This is fine for single node apps, but for a cluster you would need share the
        // tokens between the nodes somehow
        return new InMemoryOneTimeTokenService();
    }

    /*
     * Spring Security by default shows an intermediate form (with prefilled value).
     * This overrides that with a form that autosubmits. Don't know exactly why they do it, but
     * I prefer immediate login with the magic link. As the code in Spring Security OTT is so
     * tightly bolted to form POST & CSRF, replacing the default form with very similar one that
     * autoposts with JS -> immediate login.
     */
    @Controller
    public class OttTokenAutoSubmitForm {

        @GetMapping("/my-ott-submit")
        @ResponseBody
        public String ottSubmitPage(@RequestParam(name = "token", required = true) String token, CsrfToken csrfToken, HttpServletResponse response) {
            response.setContentType("text/html");
            return """
                    <html>
                    <body>
                    <form class="login-form" action="/login/ott" method="post">
                        <input type="hidden" id="token" name="token" value="%s"/>
                        <input type="hidden" name="%s" value="%s" />
                    </form>
                    <script>
                        document.forms[0].submit();
                    </script>
                    </body>
                    </html>
                    """.formatted(token, csrfToken.getParameterName(), csrfToken.getToken());
        }

    }

}
