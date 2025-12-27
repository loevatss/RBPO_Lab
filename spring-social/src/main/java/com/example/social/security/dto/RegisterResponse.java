package com.example.social.security.dto;

import java.util.Set;

public class RegisterResponse {
    private Long id;
    private String login;
    private Set<String> roles;

    public RegisterResponse(Long id, String login, Set<String> roles) {
        this.id = id;
        this.login = login;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
