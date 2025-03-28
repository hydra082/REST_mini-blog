package repository;

import entity.Post;
import entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Testcontainers
class UserRepositoryImplTest extends TestDatabaseSetup {

    private UserRepositoryImpl userRepository;
    private PostRepositoryImpl postRepository;

    @BeforeEach
    void setup() {
        try (Connection conn = dataSourceUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE comments, posts, users RESTART IDENTITY CASCADE");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to truncate tables", e);
        }

        userRepository = new UserRepositoryImpl(dataSourceUtil);
        postRepository = new PostRepositoryImpl(dataSourceUtil);
        postRepository.setUserRepository(userRepository);
        userRepository.setPostRepository(postRepository);
    }

    @Test
    void shouldSaveAndFindUser() {
        User user = new User();
        user.setName("Test User");

        User saved = userRepository.save(user);
        User found = userRepository.findById(saved.getId());

        assertThat(saved.getId(), notNullValue());
        assertThat(found, notNullValue());
        assertThat(found.getName(), is("Test User"));
    }

    @Test
    void shouldUpdateUser() {
        User user = new User();
        user.setName("Initial");
        user = userRepository.save(user);

        user.setName("Updated");
        userRepository.update(user);

        User updated = userRepository.findById(user.getId());
        assertThat(updated.getName(), is("Updated"));
    }

    @Test
    void shouldDeleteUser() {
        User user = new User();
        user.setName("To Delete");
        user = userRepository.save(user);

        userRepository.delete(user.getId());
        assertThat(userRepository.existsById(user.getId()), is(false));
    }

    @Test
    void shouldReturnNullWhenUserNotFound() {
        User found = userRepository.findById(999L);
        assertThat(found, nullValue());
    }
    @Test
    void shouldLoadCommentsForUser() {
        User user = new User();
        user.setName("Test User");
        user = userRepository.save(user);

        Post post = new Post();
        post.setTitle("Test Post");
        post.setContent("Content");
        post.setUser(user);
        post = postRepository.save(post);

        try (Connection conn = dataSourceUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO comments (text, user_id, post_id) VALUES " +
                    "('Test Comment', " + user.getId() + ", " + post.getId() + ")");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert comment", e);
        }

        User loadedUser = userRepository.findById(user.getId());
        assertThat(loadedUser.getComments(), hasSize(1));
        assertThat(loadedUser.getComments().get(0).getText(), is("Test Comment"));
    }
}
