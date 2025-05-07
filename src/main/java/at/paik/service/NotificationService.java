package at.paik.service;

import at.paik.domain.Team;
import com.vaadin.flow.server.webpush.WebPush;
import com.vaadin.flow.server.webpush.WebPushMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO figure out if this kind of separate service class is needed.
 */
@Service
public class NotificationService {

    private final WebPush webPush;
    private final Dao dao;

    public NotificationService(@Autowired(required = false) WebPush webPush, Dao dao) {
        this.webPush = webPush;
        this.dao = dao;
    }

    public void teamWideMessage(String title, String body, Team team) {
        if(webPush == null) {
            System.err.println("WebPush not configured!");
            return;
        }
        team.getActiveHunters().forEach(user -> {
            user.webPushSubscriptions.forEach(webPushSubscription -> {
                webPush.sendNotification(webPushSubscription, new WebPushMessage(title, body));
            });
        });
   }
}
