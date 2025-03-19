package at.paik;

import at.paik.domain.User;
import at.paik.service.DataRoot;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final DataRoot dataRoot;

    public UserDetailsServiceImpl(DataRoot dataRoot) {
        this.dataRoot = dataRoot;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = dataRoot.users.stream()
                .filter(u -> u.name.equals(username))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("No user present with username: " + username));
        return user;
    }
}
