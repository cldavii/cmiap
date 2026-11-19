package ai.incorp.cmiap.agent.leads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 200)
    private String company;

    @Column(length = 120)
    private String jobTitle;

    @Column(length = 254)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(length = 150)
    private String industry;

    @Column(length = 100)
    private String companySize;

    @Column(length = 200)
    private String location;

    @Column(length = 2000)
    private String notes;

    @Setter
    private Integer score;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeadFit fit;

    @Setter
    @Column(columnDefinition = "text")
    private String reasoning;

    @Setter
    @Column(columnDefinition = "text")
    private String nextStep;

    @Setter
    private Instant qualifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Setter
    @Column(nullable = false)
    private Instant updatedAt;

    public Lead(UUID userId, LeadRequest request) {
        this.userId = userId;
        this.name = request.name().trim();
        this.company = clean(request.company());
        this.jobTitle = clean(request.jobTitle());
        this.email = clean(request.email());
        this.phone = clean(request.phone());
        this.website = clean(request.website());
        this.industry = clean(request.industry());
        this.companySize = clean(request.companySize());
        this.location = clean(request.location());
        this.notes = clean(request.notes());
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
