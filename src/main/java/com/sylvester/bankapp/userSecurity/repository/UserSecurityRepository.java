package com.sylvester.bankapp.userSecurity.repository;

import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.userSecurity.entity.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, String> {

    Optional<UserSecurity> findById(String id);

    boolean existsByUser(User user);

    Optional<UserSecurity> findByUser_Email(String userEmail);

    Optional<UserSecurity> findByUser_Id(String userId);
}
