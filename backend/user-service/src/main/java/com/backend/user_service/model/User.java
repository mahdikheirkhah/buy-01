package   com.backend.user_service.model;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Builder
@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    @NotBlank
    @Size(min = 5, message = "please provide a password with at least 5 characters")
    private String password;
    private Role role;
    private String avatarUrl;
}
