package com.cinex.cinema.admin;

import com.cinex.cinema.user.CreateUserRequest;
import com.cinex.cinema.user.UpdateUserRolesRequest;
import com.cinex.cinema.user.UpdateUserStatusRequest;
import com.cinex.cinema.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin: quản lý tài khoản & phân quyền (task 14.1).
 * Chỉ ROLE_ADMIN mới truy cập được (consistent với AdminCatalogController).
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id,
                                    @Valid @RequestBody UpdateUserRolesRequest request) {
        return userService.updateRoles(id, request);
    }

    @PutMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateStatus(id, request);
    }
}
