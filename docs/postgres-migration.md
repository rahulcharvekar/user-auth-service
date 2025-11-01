# PostgreSQL Migration Notes for auth-service

auth-service now targets Postgres, but sibling repos still assume MySQL. Use the patterns below when porting other services.

## JSONB columns and JDBC

* Postgres stores policy expressions as `jsonb` (`auth.policies.expression`). When inserting/updating JSON payloads via `NamedParameterJdbcTemplate`, bind them with `Types.OTHER` so the driver treats them as JSON:

```java
parameters.addValue("metadata", jsonString, Types.OTHER);
```

* Hibernate `LIKE` on JSON needs a cast because Postgres does not support `jsonb ~~ text`. Cast the expression first:

```java
"WHERE CAST(p.expression AS string) LIKE CONCAT('%', :roleName, '%')"
```

Other services with policy/authorization lookups must apply this cast everywhere they search JSON text.

## Legacy MySQL reference

* Original MySQL properties remain commented in each `application-*.yml` for rollback. Update usernames/passwords per environment when enabling Postgres.

## Sequences after data copy

* After bulk-loading data, align every owned sequence with the table's max `id`. Run once per database:

```sql
\i scripts/postgres/reset_all_sequences.sql
```

The script iterates all schemas (except system schemas) and `setval`s each sequence to `max(id)` so inserts keep working.

## Validation checklist

1. `mvn test` passes
2. audit events persist (jsonb bindings)
3. `/api/me/authorizations` returns 200 for admins with `BASIC` role
