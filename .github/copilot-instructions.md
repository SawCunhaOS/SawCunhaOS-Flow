# CONSTITUIÇÃO TÉCNICA DO PROJETO

## FONTE DE CONTEXTO (Onde carregar as regras)
Para garantir que todas as decisões sigam os padrões do projeto, o Copilot DEVE carregar e indexar o conteúdo dos seguintes locais antes de propor alterações:
- **Diretriz Principal:** Todos os padrões de desenvolvimento estão em `.github/copilot-instructions.md`.
- **Regras de Negócio e Arquitetura:** Localizadas em `etc/architecture/`.
- **Padrões de Banco de Dados:** Localizados em `etc/database/`.
- **Histórico de Decisões:** Localizado em `etc/docs/decisions/` (ADRs).
- **Templates de Plano:** Sempre usar o modelo em `etc/templates/`.

---

## STACK TECNOLÓGICA (SSOT)
- **Linguagem:** Java 25 (Uso obrigatório de Virtual Threads, Pattern Matching e Records).
- **Frameworks:** Spring Boot 4.0+, Hibernate JPA, Spring Security.
- **Build & Dependências:** Maven (pom.xml).
- **Testes:** JUnit 5, Mockito, AssertJ, TestContainers.
- **Bancos de Dados:** PostgreSQL (Relacional), Redis (Cache).

## PADRÕES DE QUALIDADE (DoD)
- **Clean Code:** Métodos < 20 linhas; nomes de variáveis autoexplicativos.
- **SOLID:** Injeção de dependência apenas via construtor; interfaces para desacoplamento.
- **Testes:** Cobertura mínima de 80%; padrão Given/When/Then.
- **Arquitetura:** Seguir Clean Architecture; lógica de negócio nunca no Controller ou Repository.

## ESTRUTURA DE CONTEXTO OPERACIONAL
- **Planos de Trabalho:** Salvar em `etc/plans/YYYYMMDD_HHmmss_descricao.md`.
- **Documentação Técnica:** `etc/docs/`.

## PROTOCOLO DE INTERAÇÃO DOS AGENTES
1. Todas as alterações EXIGEM um plano prévio criado pelo **@dev-senior**.
2. O Copilot deve usar o comando `@workspace` para ler os arquivos citados na seção "FONTE DE CONTEXTO".
3. O agentes devem ter contexto total do plano e das diretrizes antes de sugerir qualquer código.
4. O plano deve ser validado pelos agentes pertinentes (@arquiteto, @especialista, @dba) conforme o impacto.
5. A ordem de validação deve ser SEMPRE a seguinte: **@arquiteto** (validação arquitetural), **@especialista** (validação técnica), **@dba** (validação de banco de dados, se aplicável).
6. **NUNCA** codificar sem aprovação explícita do usuário no [PONTO DE PARADA].

## DIRETRIZES DE EDIÇÃO (Git e Fluxo)
- **Mensagens de Commit:** Usar Conventional Commits (`[FEAT]`, `[FIX]`, etc).
- **Branching:** Novas funcionalidades em `feature/`, correções em `bugfix/`.
- **Fase de Planejamento:** Para arquivos >300 linhas, o plano é obrigatório e deve ser revisado pelo **@especialista**.

## DIRETRIZES DE BUSCA E LEITURA
- **NUNCA** use `run_in_terminal` para comandos de busca (ex: grep, find, ls, cat) se houver uma ferramenta nativa disponível.
- **USE SEMPRE** as ferramentas nativas de busca do Copilot:
  - `file_search`: Para localizar arquivos pelo nome.
  - `grep_search`: Para buscar conteúdo (texto/regex) dentro dos arquivos.
  - `read_file`: Para ler o conteúdo de um arquivo específico.
  - `list_dir`: Para listar diretórios.
- Somente peça para rodar comando no terminal se for para execução de testes (`mvn test`) ou build, onde não há alternativa.