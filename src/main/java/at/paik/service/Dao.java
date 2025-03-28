package at.paik.service;

import at.paik.domain.Spot;
import at.paik.domain.Team;
import at.paik.domain.User;
import org.eclipse.serializer.persistence.types.Storer;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.stereotype.Service;

@Service
public class Dao {

    private final EmbeddedStorageManager storageManager;

    public Dao(EmbeddedStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void storeData() {
        storageManager.storeRoot();
    }

    public DataRoot getData() {
        DataRoot root = (DataRoot) storageManager.root();
        if(root == null) {
            root = new DataRoot();
            storageManager.setRoot(root);
        }
        return root;
    }

    public void storeCredential(User user1, CredentialRecord credentialRecord) {
        storageManager.storeAll(user1, user1.getPasskeys(), credentialRecord);
    }

    public void storeTeams(User user) {
        Storer eagerStorer = storageManager.createEagerStorer();
        eagerStorer.storeAll(user, user.teams);
        eagerStorer.commit();
    }

    public void saveNewUser(User user) {
        getData().users.add(user);
        storageManager.storeAll(getData().users, user.teams.iterator().next().hunters);
    }

    public void saveSpot(Team currentTeam, Spot spot) {
        currentTeam.spots.add(spot);
        storageManager.storeAll(currentTeam, currentTeam.spots, spot);
    }

    @EventListener
    public void onAppReady(ContextRefreshedEvent event) {
        if(getData().users.isEmpty()) {
            System.out.println("Application started without any users, adding Masa");
            User user = new User();
            user.name = "Matti";
            getData().users.add(user);
            storeData();
        }
    }

}
