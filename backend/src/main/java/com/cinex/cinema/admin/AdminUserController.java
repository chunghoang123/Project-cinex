package com.cinex.cinema.admin;

import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.user.CreateUserRequest;
import com.cinex.cinema.user.UpdateUserRolesRequest;
import com.cinex.cinema.user.UpdateUserStatusRequest;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(userService.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo tài khoản thành công", userService.create(request)));
    }

    @RequestMapping(path = "/{id}/roles", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<UserResponse>> updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request,
            Authentication authentication) {
        Long currentUserId = authentication != null && authentication.getPrincipal() instanceof User u
            ? u.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật phân quyền thành công",
            userService.updateRoles(id, request, currentUserId)));
    }

    @RequestMapping(path = "/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        Long currentUserId = authentication != null && authentication.getPrincipal() instanceof User u
            ? u.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công",
            userService.updateStatus(id, request, currentUserId)));
    }
}
