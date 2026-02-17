-- ============================================================================
-- INSTALAÇÃO DO BANCO DE DADOS INSIDE FLOW
-- ============================================================================
-- IMPORTANTE: Execute como superuser (postgres) ou com privilégios suficientes
-- ============================================================================

-- Criando extensões do sistema
-- NOTA: Se receber erro "could not open extension control file",
-- instale PostGIS no servidor antes de executar este script
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis SCHEMA public;

-- Criar schemas
CREATE SCHEMA IF NOT EXISTS scos;

-- Garantir que o schema public está no search_path para postgis
ALTER DATABASE "ScosDev" SET search_path TO scos;

-- ============================================================================
-- CRIAR USUÁRIO E CONCEDER PERMISSÕES
-- ============================================================================

-- Criar usuário (será criado apenas se não existir)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'scos_user') THEN
        CREATE ROLE scos_user WITH LOGIN PASSWORD 'scos_user';
    END IF;
END
$$;

-- Conceder acesso ao banco
GRANT CONNECT ON DATABASE "ScosDev" TO scos_user;

-- Conceder USAGE nos schemas
GRANT USAGE ON SCHEMA scos TO scos_user;

-- Conceder USAGE no schema public (necessário para PostGIS)
GRANT USAGE ON SCHEMA public TO scos_user;

-- Conceder permissões nas tabelas (schemas específicos)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA scos TO scos_user;

-- Conceder permissões nas tabelas (public - necessário para PostGIS)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO scos_user;

-- Conceder permissões nas sequências
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA scos TO scos_user;

-- Conceder permissões nas sequências (public)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO scos_user;

-- Conceder permissão para criar objetos
GRANT CREATE ON SCHEMA scos TO scos_user;
