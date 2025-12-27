package com.example.social.security.service;

import com.example.social.security.model.AuthUser;
import com.example.social.security.repository.AuthUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    public DbUserDetailsService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser u = authUserRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : u.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role));
        }

        return User.withUsername(u.getLogin())
                .password(u.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
