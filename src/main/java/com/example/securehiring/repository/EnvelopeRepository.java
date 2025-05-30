package com.example.securehiring.repository;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.enums.EnvelopeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvelopeRepository extends JpaRepository<Envelope, Long> {
    List<Envelope> findByEnvelopeType(EnvelopeType type);
    List<Envelope> findByEnvelopeTypeAndSenderName(EnvelopeType type, String name);
}
