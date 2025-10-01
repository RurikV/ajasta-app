package top.ajasta.AjastaApp.auth_users.entity;

import top.ajasta.AjastaApp.order.entity.Order;
import top.ajasta.AjastaApp.payment.entity.Payment;
import top.ajasta.AjastaApp.review.entity.Review;
import top.ajasta.AjastaApp.role.entity.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    @NotBlank(message = "password is required")
    private String password;

    private String phoneNumber;

    private String profileUrl;

    private String address;

    @ElementCollection
    @CollectionTable(name = "user_saved_emails", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "email")
    private List<String> savedEmails;

    private boolean isActive;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL)
    private List<Order> orders;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL)
    private List<Payment> payments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}











