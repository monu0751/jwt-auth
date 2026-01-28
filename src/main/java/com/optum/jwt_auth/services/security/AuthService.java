package com.optum.jwt_auth.services.security;


import com.optum.jwt_auth.entities.RoleEntity;
import com.optum.jwt_auth.entities.UserEntity;
import com.optum.jwt_auth.repo.RoleRepository;
import com.optum.jwt_auth.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserEntity registerUser(String username, String rawPassword, Set<String> roleNames) {
        UserEntity user = userRepository.findByUsername(username).orElse(new UserEntity());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        // load roles from DB and assign
        List<RoleEntity> rolesList = roleRepository.findAllByNameIn(roleNames);
        Set<RoleEntity> roles = new HashSet<>(rolesList);
        user.setRoles(roles);
        return userRepository.save(user);
    }
}

