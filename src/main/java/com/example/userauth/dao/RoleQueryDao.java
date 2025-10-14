package com.example.userauth.dao;

import com.example.userauth.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class RoleQueryDao {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String BASE_SELECT = """
        SELECT r.id, r.name, r.description, r.created_at, r.updated_at
        FROM roles r
        """;
    
    public List<Role> findAll() {
        String sql = BASE_SELECT + " ORDER BY r.name";
        return jdbcTemplate.query(sql, new RoleRowMapper());
    }
    
    public Optional<Role> findById(Long id) {
        String sql = BASE_SELECT + " WHERE r.id = ?";
        List<Role> results = jdbcTemplate.query(sql, new RoleRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public Optional<Role> findByName(String name) {
        String sql = BASE_SELECT + " WHERE r.name = ?";
        List<Role> results = jdbcTemplate.query(sql, new RoleRowMapper(), name);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<Role> findByUsername(String username) {
        String sql = """
            SELECT r.id, r.name, r.description, r.created_at, r.updated_at
            FROM roles r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            INNER JOIN users u ON ur.user_id = u.id
            WHERE u.username = ?
            ORDER BY r.name
            """;
        return jdbcTemplate.query(sql, new RoleRowMapper(), username);
    }
    
    public List<Role> findByUserId(Long userId) {
        String sql = """
            SELECT r.id, r.name, r.description, r.created_at, r.updated_at
            FROM roles r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = ?
            ORDER BY r.name
            """;
        return jdbcTemplate.query(sql, new RoleRowMapper(), userId);
    }
    
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM roles WHERE name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name);
        return count != null && count > 0;
    }
    
    public int countUsersWithRole(Long roleId) {
        String sql = "SELECT COUNT(*) FROM user_roles WHERE role_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roleId);
        return count != null ? count : 0;
    }
    
    public int countPermissionsInRole(Long roleId) {
        String sql = "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roleId);
        return count != null ? count : 0;
    }
    
    // Role with permissions details
    public List<RoleWithPermissionCount> findAllWithPermissionCounts() {
        String sql = """
            SELECT r.id, r.name, r.description, r.created_at, r.updated_at,
                   COUNT(DISTINCT rp.permission_id) as permission_count,
                   COUNT(DISTINCT ur.user_id) as user_count
            FROM roles r
            LEFT JOIN role_permissions rp ON r.id = rp.role_id
            LEFT JOIN user_roles ur ON r.id = ur.role_id
            GROUP BY r.id, r.name, r.description, r.created_at, r.updated_at
            ORDER BY r.name
            """;
        return jdbcTemplate.query(sql, new RoleWithPermissionCountRowMapper());
    }
    
    private static class RoleRowMapper implements RowMapper<Role> {
        @Override
        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
            Role role = new Role();
            role.setId(rs.getLong("id"));
            role.setName(rs.getString("name"));
            role.setDescription(rs.getString("description"));
            
            // Handle timestamps
            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                role.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                role.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            return role;
        }
    }
    
    public static class RoleWithPermissionCount {
        private Long id;
        private String name;
        private String description;
        private int permissionCount;
        private int userCount;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getPermissionCount() { return permissionCount; }
        public void setPermissionCount(int permissionCount) { this.permissionCount = permissionCount; }
        
        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }
    }
    
    private static class RoleWithPermissionCountRowMapper implements RowMapper<RoleWithPermissionCount> {
        @Override
        public RoleWithPermissionCount mapRow(ResultSet rs, int rowNum) throws SQLException {
            RoleWithPermissionCount role = new RoleWithPermissionCount();
            role.setId(rs.getLong("id"));
            role.setName(rs.getString("name"));
            role.setDescription(rs.getString("description"));
            role.setPermissionCount(rs.getInt("permission_count"));
            role.setUserCount(rs.getInt("user_count"));
            return role;
        }
    }
}
