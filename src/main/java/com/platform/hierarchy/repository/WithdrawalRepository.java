package com.platform.hierarchy.repository;

import com.platform.hierarchy.model.Withdrawal;
import com.platform.hierarchy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByUser(User user);
    List<Withdrawal> findByUserOrderByCreatedAtDesc(User user);
    List<Withdrawal> findByUserInOrderByCreatedAtDesc(List<User> users);
    List<Withdrawal> findAllByOrderByCreatedAtDesc();
}
