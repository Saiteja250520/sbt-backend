package com.platform.hierarchy.security;

import com.platform.hierarchy.model.User;
import com.platform.hierarchy.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with Login ID: " + loginId));

        // Validate account status and throw security exceptions
        String status = user.getStatus();
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) {
            throw new DisabledException("Account is pending approval. You cannot log in yet.");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            throw new DisabledException("Account registration has been rejected.");
        } else if ("BLOCKED".equalsIgnoreCase(status)) {
            throw new LockedException("Account has been blocked. Access denied.");
        } else if ("SUSPENDED".equalsIgnoreCase(status)) {
            throw new LockedException("Account is suspended.");
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            throw new DisabledException("Account is inactive.");
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase());

        return new org.springframework.security.core.userdetails.User(
                user.getLoginId(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
