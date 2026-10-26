package com.cinex.cinema.auth;

import com.cinex.cinema.user.ERole;
import com.cinex.cinema.user.Role;
import com.cinex.cinema.user.RoleRepository;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import com.cinex.cinema.user.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Username đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Email đã tồn tại");
        }

        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
            .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .phone(request.getPhone())
            .roles(Set.of(customerRole))
            .active(true)
            .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        Set<String> roleNames = user.getRoles().stream()
            .map(r -> r.getName().name()).collect(Collectors.toSet());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getFullName(), roleNames);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);
        Set<String> roleNames = user.getRoles().stream()
            .map(r -> r.getName().name()).collect(Collectors.toSet());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getFullName(), roleNames);
    }

    public UserResponse me(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()))
            .active(user.isActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}