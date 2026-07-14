package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.user.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h1>UserRepositoryPort</h1>
 * <p>
 * Output Port interface representing persistence capabilities for the {@link User} domain entity.
 * Implemented by persistence adapters in the infrastructure layer.
 * </p>
 */
public interface UserRepositoryPort {

    /**
     * Persists or updates a user in the repository.
     *
     * @param user the domain user profile to save
     * @return the saved user profile
     */
    User save(User user);

    /**
     * Resolves a user by their unique database identifier.
     *
     * @param id the user database UUID
     * @return an {@link Optional} containing the user, or empty
     */
    Optional<User> findById(UUID id);

    /**
     * Resolves a user by their Keycloak subject identifier.
     *
     * @param sub the Keycloak subject UUID string
     * @return an {@link Optional} containing the user, or empty
     */
    Optional<User> findByKeycloakSub(String sub);

    /**
     * Resolves a user by their email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user, or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Lists all users.
     *
     * @return list of all registered users
     */
    List<User> findAll();

    /**
     * Deletes a user by ID.
     *
     * @param id the user identifier
     */
    void delete(UUID id);
}
