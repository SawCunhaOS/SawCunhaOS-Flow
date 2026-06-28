#!/bin/bash
set -e

# -----------------------------------------------------------------
# Schema e usuario da aplicacao
# Roda apos 10_postgis.sh da imagem (ordem alfabetica garante isso).
# Extensoes (postgis, uuid-ossp) sao instaladas pelo 10_postgis.sh;
# este script so cria schema, role e concede privilegios.
# -----------------------------------------------------------------
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    -- uuid-ossp pode nao ter sido instalado pelo 10_postgis.sh
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

    -- Role da aplicacao (pula se for o mesmo superusuario ou ja existir)
    DO \$\$
    BEGIN
        IF '$APP_DB_USER' <> '$POSTGRES_USER'
           AND NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$APP_DB_USER') THEN
            CREATE ROLE "$APP_DB_USER" WITH LOGIN PASSWORD '$APP_DB_PASSWORD';
        END IF;
    END
    \$\$;

    -- Schema principal
    CREATE SCHEMA IF NOT EXISTS scos;
    ALTER SCHEMA scos OWNER TO "$APP_DB_USER";

    -- search_path padrao para o banco e para o role
    ALTER DATABASE "$POSTGRES_DB" SET search_path TO scos, public;
    ALTER ROLE "$APP_DB_USER" SET search_path TO scos, public;

    -- Privilegios sobre o banco
    GRANT CONNECT ON DATABASE "$POSTGRES_DB" TO "$APP_DB_USER";

    -- Acesso ao schema public (extensoes postgis, uuid-ossp, etc.)
    GRANT USAGE ON SCHEMA public TO "$APP_DB_USER";
    GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO "$APP_DB_USER";
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "$APP_DB_USER";
    GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO "$APP_DB_USER";

    -- Privilegios sobre o schema scos (objetos futuros criados pelo superusuario)
    ALTER DEFAULT PRIVILEGES IN SCHEMA scos
        GRANT ALL ON TABLES    TO "$APP_DB_USER";
    ALTER DEFAULT PRIVILEGES IN SCHEMA scos
        GRANT ALL ON SEQUENCES TO "$APP_DB_USER";
    ALTER DEFAULT PRIVILEGES IN SCHEMA scos
        GRANT ALL ON FUNCTIONS TO "$APP_DB_USER";
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT ALL ON TABLES    TO "$APP_DB_USER";
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT ALL ON SEQUENCES TO "$APP_DB_USER";

EOSQL

# -----------------------------------------------------------------
# Banco e usuario do Keycloak
# -----------------------------------------------------------------
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL

    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$KC_DB_USER') THEN
            CREATE ROLE "$KC_DB_USER" WITH LOGIN PASSWORD '$KC_DB_PASSWORD';
        END IF;
    END
    \$\$;

    SELECT 'CREATE DATABASE "$KC_DB_NAME" OWNER "$KC_DB_USER"'
    WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '$KC_DB_NAME')\gexec

EOSQL
