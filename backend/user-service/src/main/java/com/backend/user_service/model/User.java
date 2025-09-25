package   com.backend.user_service.model;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    @NotBlank
    @Email

    private String email;
    private String password; // Will be hashed later
    private Role role;
    private String avatarUrl; // For sellers
}

enum Role {
    CLIENT, SELLER
}