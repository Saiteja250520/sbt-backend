package com.platform.hierarchy.repository;

import com.platform.hierarchy.model.Investment;
import com.platform.hierarchy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUser(User user);
    List<Investment> findByUserOrderByCreatedAtDesc(User user);
    List<Investment> findByUserInOrderByCreatedAtDesc(List<User> users);
    List<Investment> findAllByOrderByCreatedAtDesc();
}
