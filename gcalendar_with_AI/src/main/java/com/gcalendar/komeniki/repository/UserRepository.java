package com.gcalendar.komeniki.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gcalendar.komeniki.model.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByAuthId(String authId);
    Optional<User> findByApiKey(String apiKey);
}
