package com.example.userauth.service;

import com.example.userauth.dao.RoleQueryDao;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.User;
import com.example.userauth.repository.CapabilityRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.RoleRepository;
import com.example.userauth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    
    @Autowired
    private RoleRepository roleRepository;
    
    // DEPRECATED: Old Permission system - replaced by Capability+Policy architecture
    // @Autowired
    // private PermissionRepository permissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleQueryDao roleQueryDao;

    @Autowired
    private CapabilityRepository capabilityRepository;

    @Autowired
    private PolicyRepository policyRepository;
        
    // READ OPERATIONS - Using Query DAO
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        logger.debug("Fetching all roles using query DAO");
        List<Role> roles = roleQueryDao.findAll();

        for (Role role : roles) {
            Set<String> capabilities = new LinkedHashSet<>(
                    capabilityRepository.findCapabilityNamesByRoleName(role.getName()));
            role.setCapabilityNames(capabilities);

            Set<String> policies = policyRepository.findRBACPoliciesByRole(role.getName()).stream()
                    .map(policy -> policy.getName())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            role.setPolicyNames(policies);
        }

        return roles;
    }
    
    @Transactional(readOnly = true)
    public List<RoleQueryDao.RoleWithPermissionCount> getAllRolesWithPermissionCounts() {
        logger.debug("Fetching all roles with permission counts using query DAO");
        return roleQueryDao.findAllWithPermissionCounts();
    }
    
    @Transactional(readOnly = true)
    public Optional<Role> getRoleById(Long id) {
        logger.debug("Fetching role by id: {} using query DAO", id);
        return roleQueryDao.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<Role> getRoleByName(String name) {
        logger.debug("Fetching role by name: {} using query DAO", name);
        return roleQueryDao.findByName(name);
    }
    
    @Transactional(readOnly = true)
    public List<Role> getRolesByUsername(String username) {
        logger.debug("Fetching roles for user: {} using query DAO", username);
        return roleQueryDao.findByUsername(username);
    }
    
    @Transactional(readOnly = true)
    public List<Role> getRolesByUserId(Long userId) {
        logger.debug("Fetching roles for user id: {} using query DAO", userId);
        return roleQueryDao.findByUserId(userId);
    }
    
    public Role createRole(String name, String description) {
        logger.info("Creating new role: {}", name);
        
        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("Role with name '" + name + "' already exists");
        }
        
        Role role = new Role(name, description);
        
        Long nextId = roleRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1L);
        role.setId(nextId);
        
        return roleRepository.save(role);
    }
    
    public Role updateRole(Long id, String name, String description) {
        logger.info("Updating role with id: {}", id);
        
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
        
        // Check if name is changing and if new name already exists
        if (!role.getName().equals(name) && roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("Role with name '" + name + "' already exists");
        }
        
        boolean changed = false;
        
        if (!Objects.equals(role.getName(), name)) {
            role.setName(name);
            changed = true;
        }
        if (!Objects.equals(role.getDescription(), description)) {
            role.setDescription(description);
            changed = true;
        }
        
        if (!changed) {
            logger.debug("No changes detected for role id: {}", id);
            return role;
        }
        
        return roleRepository.save(role);
    }
    
    public void deleteRole(Long id) {
        logger.info("Deleting role with id: {}", id);
        
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
        
        // Check if role is assigned to any users
        if (!role.getUsers().isEmpty()) {
            throw new IllegalStateException("Cannot delete role '" + role.getName() + 
                    "' as it is assigned to " + role.getUsers().size() + " user(s)");
        }
        
        roleRepository.delete(role);
    }
    
    /**
     * DEPRECATED: Old permission system methods - no longer used in new Capability+Policy system
     * These methods are commented out as the new system uses Capabilities and Policies instead.
     */
    
    // public Role addPermissionToRole(Long roleId, Long permissionId) {
    //     // OLD SYSTEM - use PolicyEngineService and AuthorizationService instead
    // }
    
    // public Role removePermissionFromRole(Long roleId, Long permissionId) {
    //     // OLD SYSTEM - use PolicyEngineService and AuthorizationService instead
    // }
    
    public User assignRoleToUser(Long userId, Long roleId) {
        logger.info("Assigning role {} to user {}", roleId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        
        user.addRole(role);
        return userRepository.save(user);
    }
    
    public User revokeRoleFromUser(Long userId, Long roleId) {
        logger.info("Revoking role {} from user {}", roleId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        
        user.removeRole(role);
        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return roleQueryDao.existsByName(name);
    }
    
    @Transactional(readOnly = true)
    public int countUsersWithRole(Long roleId) {
        return roleQueryDao.countUsersWithRole(roleId);
    }
    
    @Transactional(readOnly = true)
    public int countPermissionsInRole(Long roleId) {
        return roleQueryDao.countPermissionsInRole(roleId);
    }

    public Optional<Role> getRoleByNameWithPermissions(String name) {
        Optional<Role> roleOpt = roleRepository.findByName(name);
        if (roleOpt.isEmpty()) {
            return roleOpt;
        }

        Role role = roleOpt.get();
        Set<String> capabilities = new LinkedHashSet<>(capabilityRepository.findCapabilityNamesByRoleName(role.getName()));
        role.setCapabilityNames(capabilities);

        Set<String> policyNames = policyRepository.findRBACPoliciesByRole(role.getName()).stream()
                .map(policy -> policy.getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        role.setPolicyNames(policyNames);

        return Optional.of(role);
    }

}
