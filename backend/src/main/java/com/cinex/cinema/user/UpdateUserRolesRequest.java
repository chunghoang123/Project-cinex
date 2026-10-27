package com.cinex.cinema.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRolesRequest {

    @NotEmpty(message = "Vui lòng chọn ít nhất một vai trò")
    private Set<String> roles;
}
