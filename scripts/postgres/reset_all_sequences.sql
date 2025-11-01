-- Resets every owned sequence to the current MAX(id) value of its table.
DO $$
DECLARE
    seq record;
    max_id bigint;
    is_called boolean;
BEGIN
    FOR seq IN
        SELECT
            n.nspname AS schema_name,
            c.relname AS table_name,
            a.attname AS column_name,
            format('%I.%I', n.nspname, s.relname) AS qualified_sequence,
            format('%I.%I', n.nspname, c.relname) AS qualified_table
        FROM pg_class s
        JOIN pg_depend d ON d.objid = s.oid AND d.deptype = 'a'
        JOIN pg_class c ON d.refobjid = c.oid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = d.refobjsubid
        WHERE s.relkind = 'S'
          AND n.nspname NOT IN ('pg_catalog', 'information_schema')
    LOOP
        EXECUTE format('SELECT MAX(%s) FROM %s', quote_ident(seq.column_name), seq.qualified_table)
        INTO max_id;

        is_called := max_id IS NOT NULL;
        IF NOT is_called THEN
            max_id := 0;
        END IF;

        EXECUTE format('SELECT setval(%L, %s, %s)',
                       seq.qualified_sequence,
                       max_id,
                       CASE WHEN is_called THEN 'true' ELSE 'false' END);
    END LOOP;
END
$$;
