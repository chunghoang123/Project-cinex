package com.cinex.cinema.config;

import com.cinex.cinema.user.ERole;
import com.cinex.cinema.user.Role;
import com.cinex.cinema.user.RoleRepository;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(2)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role adminRole = role(ERole.ROLE_ADMIN);
        Role staffRole = role(ERole.ROLE_STAFF);
        Role customerRole = role(ERole.ROLE_CUSTOMER);

        createUser("admin", "admin@cinex.vn", "Admin@123", "Quản trị viên", "0900000001",
            Set.of(adminRole));
        createUser("staff", "staff@cinex.vn", "Staff@123", "Nhân viên bán vé", "0900000002",
            Set.of(staffRole));
        createUser("customer", "customer@cinex.vn", "Customer@123", "Minh Dương", "0901234567",
            Set.of(customerRole));
        createUser("walkin", "walkin@cinex.vn", "walkin123", "Khách vãng lai", null,
            Set.of(customerRole));
    }

    private Role role(ERole name) {
        return roleRepository.findByName(name)
            .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }

    private void createUser(String username, String email, String rawPassword,
                            String fullName, String phone, Set<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .fullName(fullName)
            .phone(phone)
            .roles(roles)
            .active(true)
            .build());
    }
}
