package com.taskflow.repository;

import com.taskflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);
    
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId")
    List<User> findAllByTenantId(Long tenantId);
    
    @Query("SELECT u FROM User u WHERE u.id = :userId AND u.tenant.id = :tenantId")
    Optional<User> findByIdAndTenantId(Long userId, Long tenantId);
    
    boolean existsByEmailAndTenantId(String email, Long tenantId);
}
