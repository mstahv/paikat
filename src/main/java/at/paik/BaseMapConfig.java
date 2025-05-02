package at.paik;

import at.paik.domain.MapStyle;
import at.paik.domain.Team;
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
            MapStyle mapStyle = session.getMapStyle();
            if (mapStyle == null) {
                Team currentTeam = session.getCurrentTeam();
                if (currentTeam != null) {
                    mapStyle = currentTeam.getMapStyle();
                }
            }
            if (mapStyle == null) {
                mapStyle = MapStyle.Maptiler_OSM;
            }

            switch (mapStyle) {
                case Maptiler_OSM -> map.initStyle("https://api.maptiler.com/maps/streets/style.json?key=6c5QDyG7DyFdqb74rjI5");
                case FinlandNLS -> {
                    map.initStyle(getClass().getResourceAsStream("/maastokartta.json"));
                }
            }

            session.getMapViewport().ifPresent(viewPort -> {
                // TODO make MapLibre add-on support configurable animations
                map.fitBounds(viewPort.getBounds());
            });

        };
    }

}
