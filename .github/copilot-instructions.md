# DIRETRIZES OPERACIONAIS DE EDIÇÕES DO COPILOT
                
## DIRETRIZ PRINCIPAL
	Evite trabalhar em mais de um arquivo por vez.
	Múltiplas edições simultâneas em um arquivo causarão corrupção.
	Fique conversando e ensinando sobre o que você está fazendo enquanto codifica.

## PROTOCOLO DE ARQUIVO GRANDE E ALTERAÇÃO COMPLEXA

### FASE DE PLANEJAMENTO OBRIGATÓRIA
Ao trabalhar com arquivos grandes (>300 linhas) ou alterações complexas:
1. SEMPRE comece criando um plano detalhado ANTES de fazer qualquer edição
2. Seu plano DEVE incluir:
   - Todas as funções/seções que precisam de modificação
   - A ordem em que as alterações devem ser aplicadas
   - Dependências entre as alterações
   - Número estimado de edições separadas necessárias
3. Formate seu plano como:
   - Lista numerada ou com marcadores para cada alteração planejada
   - Explicações claras para cada alteração
   - Solicite aprovação explícita do usuário antes de prosseguir com as edições
   - Indique claramente que você está aguardando a aprovação do usuário para cada etapa antes de fazer qualquer alteração
	Total de edições planejadas: [número]

### FAZENDO EDIÇÕES
	- Concentre-se em uma mudança conceitual por vez
	- Mostre trechos claros de "antes" e "depois" ao propor alterações
	- Inclua explicações concisas sobre o que mudou e porquê
	- Sempre verifique se a edição mantém o estilo de codificação do projeto

### Sequência de edição:
	1. [Primeira alteração específica] - Propósito: [porquê]
	2. [Segunda alteração específica] - Propósito: [porquê]
	3. Você aprova este plano? Vou prosseguir com a Edição [número] após sua confirmação.
	4. ESPERE a confirmação explícita do usuário antes de fazer QUALQUER edição quando o usuário ok editar [número]
            
### FASE DE EXECUÇÃO
- Após cada edição individual, indique claramente o progresso: "✅ Edição [#] de [total] concluída. Pronto para a próxima edição?"
- Se você descobrir alterações adicionais necessárias durante a edição:
  - PARE e atualize o plano
  - Obtenha aprovação antes de continuar
- Valide após cada edição:
  - Código compila sem erros
  - Testes passam (unitários e integração)
  - Nenhum warning novo foi introduzido

### ORIENTAÇÃO DE REFATORAÇÃO
Ao refatorar arquivos grandes:
- Divida o trabalho em partes lógicas e funcionalmente independentes
- Certifique-se de que cada estado intermediário mantenha a funcionalidade
- Considere a duplicação temporária como uma etapa intermediária válida
- Sempre indique o padrão de refatoração que está sendo aplicado

### EVITAÇÃO DE LIMITE DE TAXA
- Para arquivos muito grandes, sugira dividir as alterações em várias sessões
- Priorize as alterações que são unidades logicamente completas
- Sempre forneça pontos de parada claros
            
## REQUISITOS GERAIS

### Stack Tecnológico do Projeto
- **Java 25** - Utilize as features modernas (records, sealed classes, pattern matching)
- **Spring Boot 4.x** - Siga os padrões Spring Boot atualizados
- **PostgreSQL 18+** - Use SQL moderno e otimizações disponíveis
- **Maven** - Respeite a estrutura de módulos (api, application, domain)
- **Arquitetura**: Clean Architecture + Hexagonal (Ports & Adapters) + DDD
- **Padrões**:
  - Use **Repositories** (camada Domain) para persistência do próprio domínio
  - Use **Ports & Adapters** (camada Application) para integrações externas
  - Implemente **Domain Events** para comunicação assíncrona
  - Use **Kafka** para eventos de domínio
  - Integre com **Keycloak** via Ports para autenticação
	- Revise os pom.xml para entender as dependências e plugins usados no projeto

### Código Limpo e Mantível
- Priorize legibilidade e clareza sobre concisão
- Adicione comentários em lógica complexa ou decisões de design
- Siga convenções de nomenclatura Java (camelCase, nomes descritivos)
- Mantenha consistência com o código existente do projeto
- Utilize tipos modernos: records para DTOs, sealed classes para hierarquias controladas
            
## UTILIZE O README.MD DO PROJETO
	O readme.md do projeto é a fonte definitiva de informações sobre o projeto. Sempre consulte-o para entender o propósito, as dependências e as diretrizes de codificação do projeto antes de fazer qualquer edição.

## LEIA A DOCUMENTAÇÃO DE REGRAS DE NEGÓCIO DO SISTEMA

### Localização da Documentação
- Arquivo principal: `/README.md` (seções: Visão Geral, Conceitos de Domínio, Fluxo de Dados)
- Arquivos YAML de especificação: `/etc/api/organization/`. Consulte:
  - `ScosOrganization_Company.yml` - Regras de empresa
  - `ScosOrganization_Department-Position.yml` - Hierarquia organizacional
  - `ScosOrganization_Employee.yml` - Gestão de colaboradores
  - `ScosOrganization_Login.yml` - Gestão de logins e perfil de acesso
- Documentos e diretrizes de arquitetura: `/etc/architecture/`. Consulte:
  - `definition_error_messages.md` - Diretrizes e mensagens de arquitetura do sistema

### Regras de Negócio Críticas a Compreender
Antes de implementar qualquer feature, **SEMPRE**:
1. Consulte o README na seção "Conceitos de Domínio" para entender as entidades principais
2. Revise os arquivos YAML em `/etc/api/organization/` para especificações detalhadas
3. Revise os documentos MD em `/etc/docs/` e os documentos de arquitetura em `/etc/architecture/` para atender requisitos específicos de casos de uso, regras de negócio e diretrizes arquiteturais
4. Revise os arquivos puml em `/etc/database/` para atender requisitos específicos de casos de uso e regras de negócio  
5. Identifique as invariantes de negócio que devem ser mantidas
6. Entenda o fluxo de dados e como as entidades se relacionam
7. Verifique restrições de estado e transições permitidas

### Exemplos de Contexto Crítico
- **Estrutura multinível**: Empresas podem ter filiais com até 5 níveis de profundidade
- **Configuração por organização**: Cada empresa/grupo tem sua própria configuração (timezone, idioma)
- **Sincronização Keycloak**: Usuários devem ser criados/atualizados no IdP
- **Eventos de Domínio**: Mudanças críticas geram eventos via Kafka para outros serviços
- **Regras de exclusão**: Entidades em uso não podem ser deletadas sem validação prévia

### Como Usar Essa Informação
- Ao criar um novo Use Case, leia primeiro a entidade envolvida no README
- Ao implementar validações, consulte as regras mencionadas nos YAML
- Ao fazer alterações de schema, revise como afeta o contrato descrito na documentação
- Mantenha sincronização entre código e documentação

## Utilize o código existente como guia
	Analise o código existente para entender o estilo de codificação, padrões e práticas recomendadas do projeto. Use isso como referência para garantir que suas edições estejam alinhadas com o estilo do projeto.

## TESTES E VALIDAÇÃO

### Estratégia de Testes
- **Testes Unitários**: Todos os serviços de domínio devem ter testes unitários
- **Testes de Integração**: Use @SpringBootTest para testar camadas de Application
- **Cobertura de Código**: Mantenha cobertura >80% em código crítico
- **Naming Convention**:
  - Testes unitários: `*Test.java` (ex: `CreateDepartmentUseCaseTest.java`)
  - Testes de integração: `*IntegrationTest.java` (ex: `CreateDepartmentUseCaseIntegrationTest.java`)

### Validação de Mudanças
Antes de finalizar qualquer alteração:
1. Execute `mvn clean install` para compilação e testes
2. Verifique relatórios de teste em `target/surefire-reports/`
3. Confirme que não há warnings novos
4. Valide que testes de integração continuam passando

## GIT E VERSIONAMENTO

### Padrão de Commits
- Use commits descritivos em português ou inglês
- Formate: `[TIPO] Descrição breve` (ex: `[FEAT] Adicionar validação de email`)
- Tipos recomendados: `[FEAT]`, `[FIX]`, `[REFACTOR]`, `[TEST]`, `[DOCS]`, `[CHORE]`
- Uma ideia/feature por commit

### Padrão de Branch
- `develop` - Nova versão de desenvolvimento para grandes mudanças
- `release/x.y.z` - Preparação para lançamento
- `fix/x.y.z` - Correção urgente em produção
- `feature/nome-descritivo` - novas funcionalidades
- `bugfix/nome-descritivo` - correções de bugs

## PARALELIZAÇÃO E EFICIÊNCIA

### Quando Usar Operações em Paralelo
✅ **Faça em paralelo:**
- Múltiplas leituras de arquivo (`read_file`)
- Buscas independentes (`grep_search`, `file_search`)
- Consultas a documentação sem dependências

❌ **NÃO faça em paralelo:**
- Múltiplas edições no mesmo arquivo (causa corrupção)
- Operações com dependências sequenciais
- Compilação/testes (Maven em sequência)

### Otimização de Use Cases
- Agrupe edições independentes usando `multi_replace_string_in_file`
- Para arquivos com >300 linhas, sempre crie um plano detalhado
- Prefira um grande `read_file` a múltiplas leituras pequenas
- Evite `run_in_terminal` em paralelo; execute comandos sequencialmente

## COMUNICAÇÃO CLARA
Mantenha uma comunicação clara e aberta com o usuário durante todo o processo de edição. Explique suas decisões de edição, peça feedback e esteja aberto a sugestões para garantir que as edições atendam às expectativas do usuário e estejam alinhadas com os objetivos do projeto.

Todas as respostas devem ser respondidas em português, a menos que o usuário solicite explicitamente o contrário.