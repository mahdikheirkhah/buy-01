package com.backend.user_service.dto;

import com.backend.common.dto.Role;
import com.backend.user_service.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor; // Import this
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // Import this

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class registerUserDTO {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 5, message = "Password should be at least 5 characters")
    private String password;
    private Role role;

    public User ToUser() {
        return User.builder()
                .firstName(this.getFirstName())
                .lastName(this.getLastName())
                .email(this.getEmail())
                .password(this.getPassword())
                .role(this.getRole())
                .build();
    }
}
