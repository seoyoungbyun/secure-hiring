package com.example.securehiring.repository;

import com.example.securehiring.domain.Envelope;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvelopeRepository extends JpaRepository<Envelope, Long> {
}
