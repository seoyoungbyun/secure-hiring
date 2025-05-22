package com.example.securehiring.domain;

import com.example.securehiring.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String secretKey;

    private String publicKey;

    private String privateKey;

    @Enumerated(EnumType.STRING)
    private Role role; // APPLICANT, HR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company; // HR인 경우만 연결됨 (지원자는 null)
}
