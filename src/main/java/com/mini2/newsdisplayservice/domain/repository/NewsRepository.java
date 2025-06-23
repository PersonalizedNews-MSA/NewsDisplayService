package com.mini2.newsdisplayservice.domain.repository;

import com.mini2.newsdisplayservice.domain.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NewsRepository extends JpaRepository<News, Long> {

    @Modifying
    @Query("delete from News n where n.createdTime < :batchTime")
    void deleteByCreatedTimeBefore(@Param("batchTime") LocalDateTime batchTime);
}
