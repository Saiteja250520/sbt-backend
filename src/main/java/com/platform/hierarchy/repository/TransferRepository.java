package com.platform.hierarchy.repository;

import com.platform.hierarchy.model.Transfer;
import com.platform.hierarchy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByAdmin(User admin);
    List<Transfer> findByUser(User user);
    List<Transfer> findByAdminOrderByCreatedAtDesc(User admin);
    List<Transfer> findByUserOrderByCreatedAtDesc(User user);
    List<Transfer> findByAdminInOrderByCreatedAtDesc(List<User> admins);
    List<Transfer> findAllByOrderByCreatedAtDesc();
}
