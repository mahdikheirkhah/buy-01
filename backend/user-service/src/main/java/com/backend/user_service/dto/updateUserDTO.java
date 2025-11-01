package com.backend.user_service.dto;

import com.backend.common.dto.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class updateUserDTO {
    @Email(message = "Email should be valid")
    private String email;
    private String firstName;
    private String lastName;
    @Size(min = 5, message = "Password should be at least 5 characters")
    private String currentPassword;
    @Size(min = 5, message = "Password should be at least 5 characters")
    private String newPassword;
}
