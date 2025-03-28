package at.paik.domain;

import org.vaadin.firitin.geolocation.GeolocationEvent;

public record LocationUpdate(Team team, User user, GeolocationEvent event) {
}
