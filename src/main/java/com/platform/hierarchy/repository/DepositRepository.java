package com.platform.hierarchy.repository;

import com.platform.hierarchy.model.Deposit;
import com.platform.hierarchy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
    List<Deposit> findByUser(User user);
    List<Deposit> findByUserOrderByCreatedAtDesc(User user);
    List<Deposit> findByUserInOrderByCreatedAtDesc(List<User> users);
    List<Deposit> findAllByOrderByCreatedAtDesc();
}
