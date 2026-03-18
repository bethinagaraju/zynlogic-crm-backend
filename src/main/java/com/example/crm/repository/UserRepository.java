package com.example.crm.repository;

import com.example.crm.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    java.util.Optional<Users> findByUsername(String username);
    void deleteByUsername(String username);
}
