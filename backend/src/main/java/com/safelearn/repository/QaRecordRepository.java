package com.safelearn.repository;

import com.safelearn.entity.QaRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaRecordRepository extends JpaRepository<QaRecord, String> {
    Page<QaRecord> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
