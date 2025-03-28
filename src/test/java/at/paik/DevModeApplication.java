package at.paik;

import at.paik.domain.User;
import at.paik.service.Dao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.util.Set;

/**
 * The entry point of the Spring Boot application for development time.
 * 
 * This starts the project in development mode.
 */
@SpringBootApplication
public class DevModeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevModeApplication.class, args);
    }

}
