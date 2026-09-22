package com.example.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.data.model.User;

import java.util.List;

/**
 * Data Access Object for User entities.
 */
@Dao
public interface UserDao {

    /**
     * Inserts a user into the database.
     * @param user User entity to insert.
     * @return row ID of the newly inserted user.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(@NonNull User user);

    /**
     * Finds a user by their unique username.
     * @param username Username to search for.
     * @return User if found, null otherwise.
     */
    @Nullable
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(@NonNull String username);

    /**
     * Validates user credentials for login.
     * @param username Provided username.
     * @param password Provided password.
     * @return User if credentials match, null otherwise.
     */
    @Nullable
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    User validateUser(@NonNull String username, @NonNull String password);

    /**
     * Retrieves a user by their primary key ID.
     * @param id User ID.
     * @return User if found, null otherwise.
     */
    @Nullable
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(long id);

    /**
     * Retrieves all registered users.
     * @return List of all users.
     */
    @Query("SELECT * FROM users ORDER BY name ASC")
    List<User> getAllUsers();

    /**
     * Checks if a username already exists.
     * @param username Username to check.
     * @return Count of users with that username (0 or 1).
     */
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int countByUsername(@NonNull String username);

    /**
     * Updates an existing user record.
     * @param user User to update.
     */
    @Update
    void updateUser(@NonNull User user);

    /**
     * Deletes a user record.
     * @param user User to delete.
     */
    @Delete
    void deleteUser(@NonNull User user);

    /**
     * Deletes all users (useful for testing or resetting).
     */
    @Query("DELETE FROM users")
    void deleteAllUsers();
}
