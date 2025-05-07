package at.paik.service;

import at.paik.domain.User;

import java.util.ArrayList;
import java.util.List;

/**
 * The object root for Eclipse Store based persistence.
 */
public class DataRoot {

    public List<User> users = new ArrayList<>();

    public DataRoot() {
    }
}
