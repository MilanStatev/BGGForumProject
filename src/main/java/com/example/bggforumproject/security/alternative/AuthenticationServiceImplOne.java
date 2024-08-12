package com.example.bggforumproject.security.alternative;


import com.example.bggforumproject.models.Role;
import com.example.bggforumproject.models.User;
import com.example.bggforumproject.models.enums.RoleType;
import com.example.bggforumproject.repositories.contracts.RoleRepository;
import com.example.bggforumproject.repositories.contracts.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;


public class AuthenticationServiceImplOne {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper mapper;


    public AuthenticationServiceImplOne(UserRepository userRepository, RoleRepository roleRepository,
                                        PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                                        ModelMapper mapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.mapper = mapper;
    }

    public User registerUser(RegisterUserDTO input) {
        User user = mapper.map(input, User.class);

        Role role = roleRepository.getByAuthority(RoleType.USER.name());

        user.setPassword(passwordEncoder.encode(input.password()));
        user.setRoles(Set.of(role));
        userRepository.create(user);
        return userRepository.getByUsername(user.getUsername());
    }

    public User loginUser(LoginUserDTO input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.email(),
                        input.password()
                )
        );

        return userRepository.getByEmail(input.email());
    }


}
