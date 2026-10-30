package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    void save(User user);
}
