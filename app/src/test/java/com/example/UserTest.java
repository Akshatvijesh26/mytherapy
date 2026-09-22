package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.data.model.User;

import org.junit.Test;

/**
 * Unit tests for User entity model and credentials validation.
 */
public class UserTest {

    @Test
    public void testUserCreationAndProperties() {
        User user = new User("Jane Doe", 28, "Female", "janedoe", "securePass123");

        assertEquals(0, user.getId());
        assertEquals("Jane Doe", user.getName());
        assertEquals(28, user.getAge());
        assertEquals("Female", user.getGender());
        assertEquals("janedoe", user.getUsername());
        assertEquals("securePass123", user.getPassword());

        user.setId(42);
        assertEquals(42, user.getId());
    }

    @Test
    public void testUserEquality() {
        User user1 = new User("Jane Doe", 28, "Female", "janedoe", "pass");
        user1.setId(1);

        User user2 = new User("Jane Doe", 28, "Female", "janedoe", "pass");
        user2.setId(1);

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());

        User user3 = new User("Jane Smith", 30, "Female", "janesmith", "pass2");
        user3.setId(2);
        org.junit.Assert.assertNotEquals(user1, user3);
    }

    @Test
    public void testUserUpdateProperties() {
        User user = new User("John Doe", 25, "Male", "johndoe", "oldPass");
        user.setId(10);

        user.setName("Johnathan Doe");
        user.setAge(26);
        user.setGender("Other");
        user.setPassword("newPass456");

        assertEquals("Johnathan Doe", user.getName());
        assertEquals(26, user.getAge());
        assertEquals("Other", user.getGender());
        assertEquals("newPass456", user.getPassword());
        assertEquals("johndoe", user.getUsername());
    }
}
