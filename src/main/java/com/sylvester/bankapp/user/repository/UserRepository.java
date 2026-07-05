package com.sylvester.bankapp.user.repository;

import com.sylvester.bankapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;



@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteUserById(String id);

    Optional<User> findById(String id);


    boolean existsByUsername(String username);


}
