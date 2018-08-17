package controllers.domainmodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserRepository
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(readOnly=true)
    public List<User> findAll() {
        return jdbcTemplate.query("SELECT username, password, email, authority, created_on FROM users", new UserRowMapper());
    }

    @Transactional(readOnly=true)
    public User findByUsername(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT username, password, email, authority, created_on FROM users WHERE username = ?",
                new Object[]{username}, new UserRowMapper());
    }

    @Transactional(readOnly=true)
    public User findByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT username, password, email, authority, created_on FROM users WHERE email = ?",
                new Object[]{email}, new UserRowMapper());
    }

    @Transactional
    public void create(String username, String password, String email, String authority, String createdOn) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password, email, authority, created_on) VALUES (?, ?, ?, ?, ?)",
                new Object[]{username, password, email, authority, createdOn});
    }

    @Transactional
    public void delete(String username) {
        jdbcTemplate.update(
                "DELETE FROM users WHERE username = ?",
                new Object[]{username});
    }

    @Transactional
    public void updatePasswordByUsername(String username, String password) {
        jdbcTemplate.update(
                "UPDATE users SET password = ? WHERE username = ?",
                new Object[]{password, username});
    }
}

class UserRowMapper implements RowMapper<User>
{
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        User user = new User(rs.getString("username"), rs.getString("password"), rs.getString("email"), rs.getString("authority"), rs.getString("created_on"));

        return user;
    }
}
