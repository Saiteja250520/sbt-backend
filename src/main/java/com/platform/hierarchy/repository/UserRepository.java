package com.platform.hierarchy.repository;

import com.platform.hierarchy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    List<User> findByRole(String role);
    List<User> findByParent(User parent);
    List<User> findByParentAndRole(User parent, String role);
    List<User> findByRoleAndStatus(String role, String status);
}
