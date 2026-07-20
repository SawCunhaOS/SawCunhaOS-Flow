---
title: 'Reconciliação — PRD vs. Product Brief (Input Reconciliation)'
status: done
created: '2026-07-18'
input_original: 'etc/doc/Briefing.md (Product Brief "SawCunhaOS Motor Fundacional", Fase 1, v1.0)'
prd_final: '_bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/prd.md'
addendum: '_bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Organization-2026-07-18/addendum.md'
---

# Reconciliação — PRD final vs. Product Brief original

## 0. Método

Leitura integral dos três documentos (`Briefing.md`, `prd.md`, `addendum.md`) e verificação elemento a elemento: Executive Summary/Vision, Problem, os 4 pilares da Solution, What Makes This Different (Zero Trust), Who This Serves, Success Criteria (5 métricas), Scope in/out, Vision de longo prazo (Fase 2), e as 4 decisões do Addendum do próprio Brief. Atenção adicional a perdas de **tom/ênfase qualitativa** (não só cobertura funcional), e busca ativa por contradições factuais.

**Veredito geral: o PRD reflete o Brief com fidelidade alta — inclusive citando literalmente seções do Brief ("What Makes This Different", "Success Criteria original do Brief", "decisão de escopo já registrada no Brief") como ancoragem. Não foram encontradas contradições factuais.** Foram encontradas 4 atenuações qualitativas/estruturais (nenhuma bloqueante) e 1 expansão de escopo transparente, documentadas abaixo.

---

## 1. Verificação elemento a elemento

| Elemento do Brief | Onde está no PRD/addendum | Veredito |
|---|---|---|
| Executive Summary — núcleo de governança/comunicação para ISPs P&M porte | PRD §1 Visão, parágrafo 1 (quase verbatim) | Coberto |
| Executive Summary — "blindagem interna e estrutural", não captação de cliente | PRD §1 Visão, parágrafo 3: "entrega de blindagem interna — não de captação de cliente" | Coberto (mesma expressão) |
| Executive Summary — "neutraliza passivos trabalhistas e riscos patrimoniais" | PRD §1: "neutraliza três passivos concretos" (trabalhista, vazamento, SLA) | Coberto em substância; ver Gap #1 |
| Executive Summary — fundação para acoplagem de CRM/faturamento na Fase 2 | PRD §1 último parágrafo; §8 Integrações e Dependências | Coberto |
| Problem — vulnerabilidade trabalhista/segurança (acesso fora de turno) | PRD §4.2 inteiro (FR-8 a FR-12), UJ-1 | Coberto, com mais detalhe que o Brief |
| Problem — offboarding frágil / contas órfãs | PRD §4.3 (FR-13/14), UJ-2, UJ-15 | Coberto |
| Problem — inconsistência geotemporal / erro de SLA | PRD §4.4 (FR-15 a FR-18), UJ-3 | Coberto |
| Solution — Governança Ativa via API | PRD §4.2 | Coberto |
| Solution — Ciclo de Vida Integrado (Kill Switch) | PRD §4.3 | Coberto |
| Solution — Motor Geotemporal de Alta Performance | PRD §4.4 | Coberto |
| Solution — Motor de Notificações Híbrido (síncrono/assíncrono) | PRD §4.5 (FR-19 a FR-22) | Coberto |
| What Makes This Different — postura Zero Trust, sem burla client-side | PRD §1 ("de forma inescapável, na camada de API, não no front-end"), Glossário ("Zero Trust"), §4.2 NFR (cita literalmente "é a premissa central do Brief, seção 'What Makes This Different'") | Coberto — é o ponto mais bem preservado do documento, citação explícita da seção do Brief |
| Who This Serves — Gestores ISP/RH (primário) | PRD §2.1 JTBD, bullet 1 (tradução quase 1:1 das necessidades) | Coberto |
| Who This Serves — Técnicos/Operadores (secundário) | PRD §2.1 JTBD, bullet 2 (inclui "alertas precisos de escala e rota de turno", mesma frase do Brief) | Coberto |
| Success Criteria SM-1 Governança de APIs | PRD §14 SM-1 | Coberto (refinado: "por perfis sujeitos a bloqueio", coerente com FR-8) |
| Success Criteria SM-2 Rastreabilidade Trabalhista | PRD §14 SM-2 / FR-11 | Coberto — na verdade ampliado (ver observação em Gap #2) |
| Success Criteria SM-3 Offboarding em Tempo Real | PRD §14 SM-3 / FR-13 | Coberto, mesmo limiar ("< 1 segundo") |
| Success Criteria SM-4 Precisão Geotemporal | PRD §14 SM-4 / FR-17 | Coberto |
| Success Criteria SM-5 Segurança de Notificação | PRD §14 SM-5 / FR-19,20,22 | Coberto |
| Success Criteria — coluna "Responsável" (Engenharia/QA) | — | **Não carregado** — ver Gap #2 |
| Scope IN — IAM & API Gateway | PRD §4.2 | Coberto |
| Scope IN — Ciclo de Vida RH (admissão, departamentos, escalas, Kill Switch) | PRD §4.1, §4.3 | Coberto |
| Scope IN — Motor Geotemporal (CEP, calendário, tags) | PRD §4.4 | Coberto |
| Scope IN — Mensageria Híbrida (Email/SMS/App/Web) | PRD §4.5, FR-21 | Coberto |
| Scope OUT — CRM, Financeiro, Provisionamento de Rede, Portal do Assinante | PRD §12 Não-Objetivos (quase item a item) | Coberto |
| Vision de longo prazo — Fase 2 traz CRM/faturamento/provisionamento | PRD §1, §2.1, §8 (menção genérica) | Coberto em substância; ver Gap #3 |
| Vision de longo prazo — ambição de liderança de mercado em 2-3 anos | — | **Não carregado** — ver Gap #3 |
| Addendum do Brief — Desacoplamento do CRM | `addendum.md` §1, bullet 1 (quase verbatim); PRD §12 | Coberto |
| Addendum do Brief — Controle Invisível no Back-end | `addendum.md` §1, bullet 2; PRD §4.2 NFR, FR-9 | Coberto |
| Addendum do Brief — Bloqueio Seletivo por Cargo | `addendum.md` §1, bullet 3; PRD FR-8 | Coberto |
| Addendum do Brief — Tags Operacionais no Calendário | `addendum.md` §1, bullet 4; PRD FR-18 "Out of Scope", §12 | Coberto |
| Addendum Técnico do Brief — Mecanismo Kill Switch (Redis, evento síncrono) | `addendum.md` §2, bullet 1 (verbatim); reaberto como decisão técnica em `addendum.md` §4 | Coberto — sem contradição (Brief já usa "ex:" para Redis, indicando exemplo, não mandato) |
| Addendum Técnico do Brief — Cache Geotemporal (ViaCEP + fallback + cache local) | `addendum.md` §2, bullet 2; PRD FR-15/FR-16 | Coberto |
| Addendum Técnico do Brief — Dualidade de Notificação | `addendum.md` §2, bullet 3; PRD FR-19/FR-20 | Coberto |

---

## 2. Gaps qualitativos encontrados (o tipo que mais importa nesta etapa)

### Gap #1 — "Riscos patrimoniais" perde nome próprio
O Executive Summary do Brief afirma que a SawCunhaOS "neutraliza **passivos trabalhistas e riscos patrimoniais**" — dois eixos de dano explicitamente distintos. O PRD (§1 Visão) reformula isso em "três passivos concretos": trabalhista, vazamento de dados (contas órfãs) e erro de SLA. Esses três cobrem em substância o eixo trabalhista e parte do eixo patrimonial (vazamento de dados é, em certo sentido, um risco patrimonial), mas o **termo "patrimonial"** e a ideia mais ampla de proteção de ativos do negócio (rede, infraestrutura, continuidade operacional) nunca aparece, nomeada, em nenhum lugar do PRD ou do addendum. Não é uma omissão funcional (a arquitetura Zero Trust cobre o caso), é uma atenuação de enquadramento/vocabulário.

### Gap #2 — Coluna "Responsável" da tabela de Success Criteria não migrou
A tabela de Success Criteria do Brief (§6) atribui um responsável por métrica (Engenharia/Dev para SM-1, SM-2, SM-3, SM-5; QA para SM-4). O PRD (§14 Métricas de Sucesso) reproduz as metas e os limiares com fidelidade, mas **não carrega a atribuição de responsabilidade** — nenhuma métrica em §14 é associada a um dono/time. É uma perda estrutural pequena, mas é informação de governança do próprio Brief que desapareceu sem menção. Observação lateral: em compensação, o PRD amplia SM-2 além do que o Brief pedia literalmente — o Brief fala em logar "acessos **permitidos** fora da jornada"; o PRD (FR-11, UJ-1) audita **toda** avaliação de turno, permitida ou negada. É uma expansão razoável (não contradição), mas vale registrar que o critério ficou mais rígido que o texto literal do Brief.

### Gap #3 — Ambição de posicionamento de mercado da Vision de longo prazo não é carregada
O Brief (§8) projeta a SawCunhaOS, em 2-3 anos, como "a plataforma **líder** em governança, provisionamento e inteligência operacional para ISPs de médio porte no Brasil" e "sinônimo de **conformidade legal e eficiência em larga escala**" — uma afirmação de ambição estratégica/branding, não só de escopo funcional. O Brief também cita dois detalhes específicos de Fase 2: "faturamento inteligente **por WhatsApp**" e "provisionamento automatizado de rede diretamente no hardware da planta de fibra (**OLTs**)". O PRD menciona Fase 2 apenas de forma genérica e funcional ("CRM, faturamento inteligente, provisionamento de rede", em §1/§2.1/§8), como consumidor downstream da fundação — sem os detalhes de canal (WhatsApp) ou de hardware (OLT), e sem qualquer menção à ambição de liderança de mercado. Esperado e correto para um PRD de Fase 1 (esses detalhes são decisão de Fase 2, fora de escopo), mas é uma perda de tom de ambição de negócio que valia registrar nesta reconciliação.

### Gap #4 — Enquadramento de "desperdício de receita" como motor do Problem é diluído
O Brief abre a seção Problem com um enquadramento de impacto no negócio: "Provedores... sofrem com o **desperdício de receita** devido à falta de governança interna...". O PRD não tem uma seção "Problem" isolada (funde o problema na Visão e nas descrições de feature), e ao fazer essa fusão, reformula tudo em linguagem técnico-operacional-jurídica (passivo trabalhista, risco de segurança, erro de SLA) sem nunca reconectar explicitamente esses passivos ao enquadramento original de "desperdício de receita" como o dano de negócio agregado. Substância preservada, mas o fio condutor financeiro/de negócio do Brief perde nome explícito no PRD.

---

## 3. Itens do Brief não refletidos em lugar nenhum

Nenhum item de **capacidade/requisito** do Brief ficou de fora do PRD ou do addendum — os 4 pilares, os 5 critérios de sucesso, o scope in/out e as 4+3 decisões do Addendum do Brief estão todos rastreáveis em algum lugar do PRD final. As únicas ausências encontradas são de **tom/ênfase/atribuição** (Gaps #1-#4 acima), não de conteúdo funcional ausente.

## 4. Itens do PRD que parecem contradizer o Brief

**Nenhuma contradição factual foi encontrada.** Um ponto foi investigado com cuidado por parecer tensão e não é:

- O Addendum do Brief descreve o mecanismo do Kill Switch de forma aparentemente definitiva ("disparará um evento síncrono... limpando o cache de sessões (ex: Redis)... forçando a invalidação imediata de JWTs"), enquanto o addendum do PRD (`addendum.md` §4) trata o mesmo mecanismo como **decisão técnica ainda aberta** ("cache compartilhado... vs. revogação via Keycloak Admin API... não decidido"). Não é contradição: o próprio Brief já qualifica Redis como exemplo ("ex:"), não como mandato, e o PRD preserva essa mesma citação literal em `addendum.md` §2 antes de reabrir a decisão de mecanismo em §4. É tratamento coerente de uma sugestão ilustrativa do Brief, não uma reversão dela.

## 5. Expansão de escopo transparente (nem gap, nem contradição — registrar por completude)

O PRD introduz em §4.1 um mecanismo extenso de **aprovação obrigatória (four-eyes) para criação/reativação de Login, troca de Perfil e criação/edição de Perfil** (FR-23 a FR-28), com cadeia Supervisor → Gerente → grupo `APPROVE_SYSTEM_ACCESS`, SLA de 1 dia útil e escalonamento — e a métrica correspondente SM-6. **Nada disso está no Product Brief.** O próprio PRD é transparente sobre a origem: declara explicitamente em §0 que a Fundação de Identidade (§4.1) segue a especificação funcional já existente em `etc/doc/usecase/00` a `06` (162 endpoints), não o Brief — ou seja, é escopo herdado de uma fonte de verdade funcional paralela e pré-existente, corretamente citada, não uma invenção não rastreável. Não é um gap de reconciliação (o Brief não precisa mencionar o que já está especificado em outro documento de mesma hierarquia), mas é uma diferença de escopo real entre os dois documentos que vale deixar registrada aqui.

## 6. Conclusão

O PRD é uma reconciliação de alta fidelidade do Brief — chega a superar o padrão usual ao citar literalmente trechos e títulos de seção do Brief como justificativa de requisitos (§4.2 NFR, §14, §12), e ao dedicar uma seção inteira (§7) explicitamente para não deixar o motivador trabalhista/CLT do Brief se perder em meio aos FRs, com a nota própria "seção adicionada por ser o motivador central do Product Brief — não é um cluster padrão de template". As lacunas encontradas são de tom e de um campo de tabela (Responsável), não de substância ou de contradição. Nenhuma delas é bloqueante para prosseguir com o PRD; recomenda-se, se o tempo permitir antes do handoff para Arquitetura/Épicos, apenas: (a) decidir se a atribuição Engenharia/QA por métrica deve voltar a §14, e (b) confirmar que a omissão da ambição de mercado da Vision de Fase 2 é aceitável (era esperado, mas vale um "de acordo" explícito do stakeholder de produto).
