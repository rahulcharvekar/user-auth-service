-- Bootstrap setup script
-- --------------------------------------------
-- Creates a PLATFORM_BOOTSTRAP role, a companion bootstrap user, and links
-- them together so the team can begin catalog seeding immediately after a
-- deployment. Run this against the auth-service database once the schema
-- migrations have executed.
--
-- Default credentials (change immediately after first login):
--   username: platform.bootstrap
--   email:    platform.bootstrap@lbe.local
--   password: Platform!Bootstrap1
--
-- The password hash below was generated with BCrypt (cost 12) to match the
-- Spring Security encoder in SecurityConfig.java.

SET @now := NOW();
SET @bootstrapPassword := '$2b$12$ABgKvrzZNrOVlOkKOvzBAuSChaCz/16C8lkWSxuOGf/BIKuZz7vFG';

-- 1. Ensure the bootstrap role exists
INSERT INTO roles (name, description, is_active, created_at, updated_at)
SELECT 'PLATFORM_BOOTSTRAP',
       'Bootstrap role with full administrative privileges for initial catalog setup',
       1,
       @now,
       @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'PLATFORM_BOOTSTRAP'
);

-- Keep description/active flag up to date if the role already existed
UPDATE roles
SET description = 'Bootstrap role with full administrative privileges for initial catalog setup',
    is_active   = 1,
    updated_at  = @now
WHERE name = 'PLATFORM_BOOTSTRAP';

-- 1a. Ensure a classic ADMIN role exists for PreAuthorize checks
INSERT INTO roles (name, description, is_active, created_at, updated_at)
SELECT 'ADMIN',
       'System administrator role with legacy ADMIN authority',
       1,
       @now,
       @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'ADMIN'
);

UPDATE roles
SET description = 'System administrator role with legacy ADMIN authority',
    is_active   = 1,
    updated_at  = @now
WHERE name = 'ADMIN';

SET @bootstrapRoleId := (
    SELECT id FROM roles WHERE name = 'PLATFORM_BOOTSTRAP' LIMIT 1
);
SET @adminRoleId := (
    SELECT id FROM roles WHERE name = 'ADMIN' LIMIT 1
);

-- 2. Ensure the bootstrap user exists
SET @existingBootstrapUserId := (
    SELECT id FROM users WHERE username = 'platform.bootstrap' LIMIT 1
);

INSERT INTO users (
    username,
    email,
    password,
    full_name,
    permission_version,
    role,
    is_enabled,
    is_account_non_expired,
    is_account_non_locked,
    is_credentials_non_expired,
    created_at,
    updated_at,
    last_login
)
SELECT 'platform.bootstrap',
       'platform.bootstrap@lbe.local',
       @bootstrapPassword,
       'Platform Bootstrap',
       1,
       'ADMIN',
       1,
       1,
       1,
       1,
       @now,
       @now,
       NULL
FROM DUAL
WHERE @existingBootstrapUserId IS NULL;

SET @bootstrapUserId := (
    SELECT id FROM users WHERE username = 'platform.bootstrap' LIMIT 1
);

-- Refresh user metadata and ensure permission version semantics
SET @shouldBumpPermission := IF(@existingBootstrapUserId IS NULL, 0, 1);

UPDATE users
SET email                      = 'platform.bootstrap@lbe.local',
    password                   = @bootstrapPassword,
    full_name                  = 'Platform Bootstrap',
    role                       = 'ADMIN',
    is_enabled                 = 1,
    is_account_non_expired     = 1,
    is_account_non_locked      = 1,
    is_credentials_non_expired = 1,
    permission_version = CASE
        WHEN @shouldBumpPermission = 1 THEN permission_version + 1
        WHEN permission_version IS NULL OR permission_version < 1 THEN 1
        ELSE permission_version
    END,
    updated_at = @now
WHERE id = @bootstrapUserId;

-- 3. Link user to role via user_roles junction table
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id,
       @bootstrapRoleId,
       @now
FROM users u
WHERE u.id = @bootstrapUserId
  AND @bootstrapRoleId IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = @bootstrapRoleId
  );

INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id,
       @adminRoleId,
       @now
FROM users u
WHERE u.id = @bootstrapUserId
  AND @adminRoleId IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = @adminRoleId
  );

-- 4. Seed platform bootstrap capability
INSERT INTO capabilities (
    name,
    description,
    module,
    action,
    resource,
    is_active,
    created_at,
    updated_at
)
SELECT 'platform.bootstrap.full-access',
       'Grants full access to service catalog and RBAC bootstrap workflows',
       'PLATFORM',
       'MANAGE',
       'BOOTSTRAP',
       1,
       @now,
       @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM capabilities WHERE name = 'platform.bootstrap.full-access'
);

UPDATE capabilities
SET description = 'Grants full access to service catalog and RBAC bootstrap workflows',
    module       = 'PLATFORM',
    action       = 'MANAGE',
    resource     = 'BOOTSTRAP',
    is_active    = 1,
    updated_at   = @now
WHERE name = 'platform.bootstrap.full-access';

SET @bootstrapCapabilityId := (
    SELECT id FROM capabilities WHERE name = 'platform.bootstrap.full-access' LIMIT 1
);

-- 5. Seed bootstrap policy and link capability
INSERT INTO policies (
    name,
    description,
    type,
    expression,
    is_active,
    created_at,
    updated_at
)
SELECT 'PLATFORM_BOOTSTRAP_POLICY',
       'Policy granting bootstrap users full administrative setup access',
       'RBAC',
       '{\"roles\": [\"PLATFORM_BOOTSTRAP\", \"ADMIN\"]}',
       1,
       @now,
       @now
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY'
);

UPDATE policies
SET description = 'Policy granting bootstrap users full administrative setup access',
    type        = 'RBAC',
    expression  = '{\"roles\": [\"PLATFORM_BOOTSTRAP\", \"ADMIN\"]}',
    is_active   = 1,
    updated_at  = @now
WHERE name = 'PLATFORM_BOOTSTRAP_POLICY';

SET @bootstrapPolicyId := (
    SELECT id FROM policies WHERE name = 'PLATFORM_BOOTSTRAP_POLICY' LIMIT 1
);

INSERT INTO policy_capabilities (policy_id, capability_id)
SELECT @bootstrapPolicyId,
       @bootstrapCapabilityId
FROM DUAL
WHERE @bootstrapPolicyId IS NOT NULL
  AND @bootstrapCapabilityId IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM policy_capabilities
      WHERE policy_id = @bootstrapPolicyId
        AND capability_id = @bootstrapCapabilityId
  );

-- 6. Link bootstrap policy to catalog-management endpoints only
INSERT INTO endpoint_policies (endpoint_id, policy_id)
SELECT e.id,
       @bootstrapPolicyId
FROM endpoints e
WHERE @bootstrapPolicyId IS NOT NULL
  AND (
      e.path LIKE '/api/admin/capabilities%'
      OR e.path LIKE '/api/admin/policies%'
      OR e.path LIKE '/api/admin/roles%'
      OR e.path LIKE '/api/admin/users%'
      OR e.path LIKE '/api/admin/endpoints%'
      OR e.path LIKE '/api/admin/ui-pages%'
      OR e.path LIKE '/api/admin/page-actions%'
  )
  AND NOT EXISTS (
      SELECT 1 FROM endpoint_policies ep
      WHERE ep.endpoint_id = e.id
        AND ep.policy_id = @bootstrapPolicyId
  );
