package at.paik;

import at.paik.finnishterrainmap.LocalMaastokartta;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vaadin.addons.maplibre.BaseMapConfigurer;
import org.vaadin.addons.maplibre.MapLibre;

@Configuration
public class BaseMapConfig {

    @Bean
    BaseMapConfigurer mapLibreBaseMapProvider() {
        return map -> {
            LocalMaastokartta.CONFIG.configure(map);
        };
    }

}
