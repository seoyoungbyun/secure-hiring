package com.example.securehiring.repository;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.Member;
import com.example.securehiring.domain.ResultNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultNotificationRepository extends JpaRepository<ResultNotification, Long> {
    Optional<ResultNotification> findByEnvelope(Envelope envelope);
    List<ResultNotification> findAllByApplicant(Member applicant);
}

