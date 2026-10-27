package com.cinex.cinema.auth;

import com.cinex.cinema.user.ERole;
import com.cinex.cinema.user.Role;
import com.cinex.cinema.user.RoleRepository;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import com.cinex.cinema.user.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /** Google OAuth Client ID, cấu hình qua biến môi trường GOOGLE_CLIENT_ID */
    @Value("${app.oauth2.google.client-id:}")
    private String googleClientId;

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
            .roles(new HashSet<>(Set.of(customerRole)))
            .active(true)
            .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        Set<String> roleNames = user.getRoles().stream()
            .map(r -> r.getName().name()).collect(Collectors.toSet());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getFullName(),
            user.getAvatarUrl(), roleNames);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .or(() -> userRepository.findByEmail(request.getUsername()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc email không tồn tại"));

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        String token = jwtService.generateToken(user);
        Set<String> roleNames = user.getRoles().stream()
            .map(r -> r.getName().name()).collect(Collectors.toSet());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getFullName(),
            user.getAvatarUrl(), roleNames);
    }

    public UserResponse me(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .avatarUrl(user.getAvatarUrl())
            .roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()))
            .active(user.isActive())
            .createdAt(user.getCreatedAt())
            .build();
    }

    /**
     * Cập nhật hồ sơ của chính người dùng đang đăng nhập.
     * Email được chuẩn hoá chữ thường và không được trùng với tài khoản khác.
     */
    @Transactional
    public UserResponse updateProfile(User principal, UpdateProfileRequest request) {
        User user = findCurrentUser(principal);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(email)
            .filter(other -> !other.getId().equals(user.getId()))
            .ifPresent(other -> {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email đã được sử dụng bởi tài khoản khác");
            });

        user.setFullName(normalizeOptional(request.getFullName()));
        user.setEmail(email);
        user.setPhone(normalizeOptional(request.getPhone()));
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeOptional(request.getAvatarUrl()));
        }

        return me(userRepository.save(user));
    }

    /**
     * Đổi mật khẩu: phải xác thực đúng mật khẩu hiện tại trước khi đặt mật khẩu mới.
     */
    @Transactional
    public void changePassword(User principal, ChangePasswordRequest request) {
        User user = findCurrentUser(principal);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Tài khoản {} đã đổi mật khẩu", user.getUsername());
    }

    private User findCurrentUser(User principal) {
        return userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Đăng nhập bằng Google ID Token từ Google Identity Services (frontend gửi credential).
     * Xác minh chữ ký + audience + hạn token bằng thư viện Google, sau đó:
     * - email đã có trong hệ thống → liên kết tài khoản (lưu googleId, avatar) và trả token cũ
     * - email chưa có → tạo tài khoản mới vai trò CUSTOMER
     */
    public AuthResponse googleLogin(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Server chưa cấu hình Google Client ID (GOOGLE_CLIENT_ID)");
        }

        GoogleIdToken.Payload payload;
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(googleClientId))
                .build();
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token không hợp lệ");
            }
            payload = idToken.getPayload();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xác minh Google token", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác minh được Google token");
        }

        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();
        String googleId = payload.getSubject();
        String fullName = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");

        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email Google chưa được xác minh");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> createUserFromGoogle(email, googleId, fullName));

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
        }
        if (user.getAvatarUrl() == null && avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        Set<String> roleNames = user.getRoles().stream()
            .map(r -> r.getName().name()).collect(Collectors.toSet());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getFullName(),
            user.getAvatarUrl(), roleNames);
    }

    /** Tạo tài khoản cho lần đầu đăng nhập Google (mật khẩu ngẫu nhiên, biết qua email). */
    private User createUserFromGoogle(String email, String googleId, String fullName) {
        String username = userRepository.existsByUsername(email) ? "gg-" + googleId : email;
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
            .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));

        return userRepository.save(User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
            .fullName(fullName)
            .googleId(googleId)
            .roles(new HashSet<>(Set.of(customerRole)))
            .active(true)
            .build());
    }
}