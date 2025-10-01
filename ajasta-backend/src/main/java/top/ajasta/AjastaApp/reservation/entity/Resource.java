package top.ajasta.AjastaApp.reservation.entity;

import top.ajasta.AjastaApp.reservation.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import top.ajasta.AjastaApp.auth_users.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    private String location;

    @Column(length = 2000)
    private String description;

    private String imageUrl;

    // Price per 30-minute booking slot
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerSlot;

    // Number of simultaneous units (e.g., number of courts/chairs)
    @Builder.Default
    private Integer unitsCount = 1;

    // Opening and closing times for scheduling (optional)
    private LocalTime openTime;
    private LocalTime closeTime;

    // Unavailability configuration
    // Comma-separated weekday indices 0-6 (0=Sunday)
    @Column(length = 50)
    private String unavailableWeekdays;

    // Comma-separated dates yyyy-MM-dd
    @Column(length = 2000)
    private String unavailableDates;

    // Semicolon-separated time ranges per day e.g. "12:00-13:30;16:00-17:00"
    @Column(length = 2000)
    private String dailyUnavailableRanges;

    // Users who are managers/admins for this resource
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "resource_managers",
            joinColumns = @JoinColumn(name = "resource_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> managers;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.unitsCount == null) this.unitsCount = 1;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.unitsCount == null) this.unitsCount = 1;
    }
}
