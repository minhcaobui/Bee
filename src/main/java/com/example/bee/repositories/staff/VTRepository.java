package com.example.bee.repositories.staff;

import com.example.bee.entities.staff.VT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VTRepository extends JpaRepository<VT, Integer> {
}
