package com.example.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing a user account in the My Therapy application.
 */
@Entity(
    tableName = "users",
    indices = {@Index(value = {"username"}, unique = true)}
)
public class User implements Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "age")
    private int age;

    @NonNull
    @ColumnInfo(name = "gender")
    private String gender;

    @NonNull
    @ColumnInfo(name = "username")
    private String username;

    @NonNull
    @ColumnInfo(name = "password")
    private String password;

    /**
     * Default constructor required by Room.
     */
    public User() {
        this.name = "";
        this.gender = "";
        this.username = "";
        this.password = "";
    }

    /**
     * Parameterized constructor for creating a new user instance before insertion.
     */
    @Ignore
    public User(@NonNull String name, int age, @NonNull String gender,
                @NonNull String username, @NonNull String password) {
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.age = age;
        this.gender = Objects.requireNonNull(gender, "Gender must not be null");
        this.username = Objects.requireNonNull(username, "Username must not be null");
        this.password = Objects.requireNonNull(password, "Password must not be null");
    }

    /**
     * Parameterized constructor including id.
     */
    @Ignore
    public User(long id, @NonNull String name, int age, @NonNull String gender,
                @NonNull String username, @NonNull String password) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.age = age;
        this.gender = Objects.requireNonNull(gender, "Gender must not be null");
        this.username = Objects.requireNonNull(username, "Username must not be null");
        this.password = Objects.requireNonNull(password, "Password must not be null");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = Objects.requireNonNull(name, "Name must not be null");
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @NonNull
    public String getGender() {
        return gender;
    }

    public void setGender(@NonNull String gender) {
        this.gender = Objects.requireNonNull(gender, "Gender must not be null");
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = Objects.requireNonNull(username, "Username must not be null");
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = Objects.requireNonNull(password, "Password must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
                age == user.age &&
                name.equals(user.name) &&
                gender.equals(user.gender) &&
                username.equals(user.username) &&
                password.equals(user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age, gender, username, password);
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
