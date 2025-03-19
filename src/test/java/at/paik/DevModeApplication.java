package at.paik;

import at.paik.domain.User;
import at.paik.service.Dao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

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

    @Autowired
    private Dao dao;

    @EventListener
    public void onAppReady(ContextRefreshedEvent event) {
        System.out.println("Application ready!");

        if(dao.getData().users.isEmpty()) {
            User user = new User();
            user.name = "Masa";
            dao.getData().users.add(user);
            dao.storeData();
        }

    }

}
