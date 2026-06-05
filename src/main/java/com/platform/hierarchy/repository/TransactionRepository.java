package com.platform.hierarchy.repository;

import com.platform.hierarchy.model.Transaction;
import com.platform.hierarchy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
    List<Transaction> findByUserIn(List<User> users);
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    List<Transaction> findByUserInOrderByCreatedAtDesc(List<User> users);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
