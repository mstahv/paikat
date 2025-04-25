package at.paik;

import at.paik.domain.MapStyle;
import at.paik.finnishterrainmap.LocalMaastokartta;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vaadin.addons.maplibre.BaseMapConfigurer;

@Configuration
public class BaseMapConfig {

    @Bean
    BaseMapConfigurer mapLibreBaseMapProvider(ApplicationContext applicationContext) {
        return map -> {
            Session session = applicationContext.getBean(Session.class);
            if(session.getMapStyle() == MapStyle.Maptiler_OSM ) {
                map.initStyle("https://api.maptiler.com/maps/streets/style.json?key=6c5QDyG7DyFdqb74rjI5");
            } else {
                LocalMaastokartta.CONFIG.configure(map);
            }
        };
    }

}
