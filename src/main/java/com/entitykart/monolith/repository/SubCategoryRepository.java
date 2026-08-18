package com.entitykart.monolith.repository;

import com.entitykart.monolith.entity.SubCategoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategoryEntity, Long> {

    List<SubCategoryEntity> findByCategoryId(Long categoryId);
}
