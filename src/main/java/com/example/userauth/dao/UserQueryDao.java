package com.example.userauth.dao;

import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UserQueryDao {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String BASE_SELECT = """
        SELECT u.id, u.username, u.email, u.full_name, u.role, u.is_enabled, 
               u.created_at, u.updated_at, u.last_login
        FROM users u
        """;
    
    public List<User> findAll() {
        String sql = BASE_SELECT + " ORDER BY u.username";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    
    public Optional<User> findById(Long id) {
        String sql = BASE_SELECT + " WHERE u.id = ?";
        List<User> results = jdbcTemplate.query(sql, new UserRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public Optional<User> findByUsername(String username) {
        String sql = BASE_SELECT + " WHERE u.username = ?";
        List<User> results = jdbcTemplate.query(sql, new UserRowMapper(), username);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public Optional<User> findByEmail(String email) {
        String sql = BASE_SELECT + " WHERE u.email = ?";
        List<User> results = jdbcTemplate.query(sql, new UserRowMapper(), email);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<User> findByRole(UserRole role) {
        String sql = BASE_SELECT + " WHERE u.role = ? ORDER BY u.username";
        return jdbcTemplate.query(sql, new UserRowMapper(), role.name());
    }
    
    public List<User> findActiveUsers() {
        String sql = BASE_SELECT + " WHERE u.is_enabled = true ORDER BY u.username";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    
    public List<User> findInactiveUsers() {
        String sql = BASE_SELECT + " WHERE u.is_enabled = false ORDER BY u.username";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }
    
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }
    
    public List<User> findByRoleId(Long roleId) {
        String sql = """
            SELECT u.id, u.username, u.email, u.full_name, u.role, u.is_enabled, 
                   u.created_at, u.updated_at, u.last_login
            FROM users u
            INNER JOIN user_roles ur ON u.id = ur.user_id
            WHERE ur.role_id = ?
            ORDER BY u.username
            """;
        return jdbcTemplate.query(sql, new UserRowMapper(), roleId);
    }

    public List<User> findByRoleName(String roleName) {
        String sql = """
            SELECT u.id, u.username, u.email, u.full_name, u.role, u.is_enabled,
                   u.created_at, u.updated_at, u.last_login
            FROM users u
            INNER JOIN user_roles ur ON u.id = ur.user_id
            INNER JOIN roles r ON ur.role_id = r.id
            WHERE LOWER(r.name) = LOWER(?)
            ORDER BY u.username
            """;
        return jdbcTemplate.query(sql, new UserRowMapper(), roleName);
    }
    
    public List<User> findUsersWithMultipleRoles() {
        String sql = """
            SELECT u.id, u.username, u.email, u.full_name, u.role, u.is_enabled, 
                   u.created_at, u.updated_at, u.last_login
            FROM users u
            INNER JOIN (
                SELECT user_id
                FROM user_roles
                GROUP BY user_id
                HAVING COUNT(*) > 1
            ) multi_role ON u.id = multi_role.user_id
            ORDER BY u.username
            """;
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    
    public int countUsersByRole(UserRole role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, role.name());
        return count != null ? count : 0;
    }
    
    public long countActiveUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_enabled = true";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
    
    // User search functionality
    public List<User> searchUsers(String searchTerm) {
        String sql = BASE_SELECT + """
            WHERE (LOWER(u.username) LIKE LOWER(?) 
                OR LOWER(u.email) LIKE LOWER(?) 
                OR LOWER(u.full_name) LIKE LOWER(?))
            ORDER BY u.username
            """;
        String pattern = "%" + searchTerm + "%";
        return jdbcTemplate.query(sql, new UserRowMapper(), pattern, pattern, pattern);
    }
    
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setFullName(rs.getString("full_name"));
            user.setRole(UserRole.valueOf(rs.getString("role")));
            user.setEnabled(rs.getBoolean("is_enabled"));
            
            // Handle timestamps
            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                user.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            var lastLogin = rs.getTimestamp("last_login");
            if (lastLogin != null) {
                user.setLastLogin(lastLogin.toLocalDateTime());
            }
            return user;
        }
    }
}
