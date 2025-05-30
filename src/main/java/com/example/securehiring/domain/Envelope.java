package com.example.securehiring.domain;

import com.example.securehiring.domain.enums.EnvelopeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Envelope {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private byte[] envelopeData; // EnvelopeData 직렬화 + 암호화된 바이트만 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnvelopeType envelopeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_company_id", nullable = false)
    private Company targetCompany;
}
