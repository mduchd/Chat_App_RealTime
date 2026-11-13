
package com.teamchat.realtime_chat.repository;

import com.teamchat.realtime_chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByGoogleSub(String googleSub);
}
