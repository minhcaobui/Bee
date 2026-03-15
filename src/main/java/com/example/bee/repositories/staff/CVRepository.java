package com.example.bee.repositories.staff;

import com.example.bee.entities.staff.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CVRepository extends JpaRepository<CV, Integer> {
}
