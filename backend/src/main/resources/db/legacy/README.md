# Legacy SQL migrations (pre-Flyway)

These `migration-*.sql` files in the parent `db/` directory were the historical incremental migration chain executed via `spring.sql.init.mode=always`.

As of P8, **Flyway** in `db/migration/` is the sole runtime schema management path. New structure changes must be added as `V002__description.sql`, `V003__...`, etc.

The legacy scripts are retained for reference and audit only; they are no longer executed at application startup.
