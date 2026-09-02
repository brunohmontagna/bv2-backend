# BV2 — Sistema de Gerenciamento de Ordens de Serviço

## O projeto

**BV2** é o projeto acadêmico da disciplina de **Projeto de Software**, 3º ano de
**Engenharia de Software da UEPG** (Universidade Estadual de Ponta Grossa).

**Equipe:**
- Bruno Holowchak Montagna
- Vicente Johansen Capri
- Vitor Giovani Clock

## O cliente

**M2 Equipamentos** — MEI real, sediada em Ponta Grossa (PR). Prestadora de serviços de
**manutenção de máquinas elétricas e mecânicas**, atendendo desde furadeiras até jatos de
areia.

**Problema que o sistema resolve:** hoje a empresa não consegue conciliar as diferentes
áreas do negócio (administrativo, financeiro, operacional). Os processos vivem espalhados
e não há uma fonte única de verdade sobre o que entrou na oficina, o que está pronto e o
que já foi entregue.

**Objetivo do BV2:** centralizar os processos da empresa em torno da **ordem de serviço
(OS)**, organizando o fluxo de trabalho e dando visibilidade a todas as áreas.

## Glossário — leia antes de mexer no código

A palavra "cliente" é ambígua neste projeto e já custou uma refatoração inteira. Fixe
estes termos:

| Termo | Significa |
|---|---|
| **Usuário** | Quem faz **login** no sistema. Só existem dois papéis: `MASTER` e `ADMIN`. |
| **MASTER** | A **equipe desenvolvedora**. Faz tudo que o ADMIN faz e, além disso, enxerga o cadastro de usuários do sistema. |
| **ADMIN** | A **M2 Equipamentos**. Opera o sistema inteiro. Não enxerga o cadastro de usuários. |
| **Cliente** | O **cliente da M2** — a pessoa ou empresa que leva a furadeira para consertar. **Não faz login. Não é usuário do sistema.** É apenas um registro cadastral, como marca ou serviço. |

Ou seja: **a M2 é o único usuário do sistema**. O que o `/clientes` lista é a carteira de
clientes *dela*.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.0.7 (Web MVC, Data JPA, Security, Validation) |
| Banco | PostgreSQL 18, migrations com Flyway |
| Auth | JWT (HMAC256, biblioteca `com.auth0:java-jwt`) |
| Docs | springdoc-openapi / Swagger UI |
| Automações | n8n 2.x (notificações — ainda não implementado) |
| Frontend | React 19, TypeScript 6, Vite 8 (ainda não iniciado neste repositório) |

Este repositório contém **apenas o backend** por enquanto. O frontend virá depois (ou em
repositório próprio).

## Estado atual

As APIs REST estão finalizadas e já refatoradas para o modelo de papéis correto:

| Recurso | Endpoint base | Situação |
|---|---|---|
| Autenticação | `/auth/login` | pronto |
| Clientes | `/clientes` | pronto |
| Marcas | `/marcas` | pronto |
| Serviços | `/servicos` | pronto |
| Equipamentos | `/equipamentos` | pronto |
| Ordens de serviço + itens | `/ordens-servico` | pronto |
| Usuários | `/usuarios` | pronto (MASTER; `/usuarios/eu` para qualquer autenticado) |
| Notificações | — | tabela e entidade criadas (V8), sem endpoints e sem integração n8n |

Não há testes automatizados além do `contextLoads` gerado pelo Spring Initializr. A
validação hoje é manual, via Swagger UI e a coleção Postman em `postman/`.

## Credenciais de teste

Banco local. Rode `POST /auth/login` com `{"email", "senha"}` — o token volta em `token`,
com validade em `expiraEm`.

| Papel | E-mail | Senha |
|---|---|---|
| MASTER | `bv2uepg2026@gmail.com` | `admin123` |
| ADMIN | `brunoh.montagna@gmail.com` | `Bruno1010!` |

O MASTER é semeado pela migration `V9`, então reaparece sozinho sempre que o banco é
recriado do zero. O ADMIN não é semeado: crie-o com `POST /usuarios` autenticado como
MASTER.

## Comandos

```bash
# subir a aplicação (carregue o .env antes: DB_*, JWT_*)
./mvnw spring-boot:run

# compilar / rodar testes
./mvnw clean verify

# Swagger UI
http://localhost:8080/swagger-ui.html
```

Variáveis de ambiente em `.env` (modelo em `.env.example`): `DB_USERNAME`, `DB_PASSWORD`,
`DB_HOST`, `DB_PORT`, `DB_NAME`, `JWT_SECRET`, `JWT_EXPIRACAO_MINUTOS`.

## Arquitetura

Camadas clássicas, pacote raiz `dev.brunohm.bv2_projeto_software_uepg`:

```
controller/   REST, validação de entrada (@Valid), documentação OpenAPI
service/      regras de negócio — onde as decisões moram
repository/   Spring Data JPA + Specifications para filtros dinâmicos
domain/       entity/ (JPA) e enums/
dto/          records de request/response, um subpacote por recurso
security/     JWT, UserDetails, SecurityConfig, tradução de 401/403
exception/    exceções de negócio + GlobalExceptionHandler (RFC 7807)
config/       OpenApiConfig
```

Migrations em `src/main/resources/db/migration` (`V1` … `V10`).

## Modelo de dados

```
usuarios                        (MASTER / ADMIN — quem loga; ilha isolada)

clientes ──┬── (N) equipamentos ── (N:1) marcas
           ├── (N) ordens_servico ── (N) itens_os ──┬── equipamento
           └── (N) notificacoes                     └── servicos
```

`usuarios` **não se relaciona com nada**. Autenticação e domínio são grafos separados.

- **usuarios** — credenciais, `role` (`MASTER` | `ADMIN`) e flag `ativo`. Senha em BCrypt.
- **clientes** — cadastro dos clientes da M2. Nome, telefone, flag `ativo`. **Sem login.**
- **marcas** — catálogo, nome único.
- **servicos** — catálogo com `valor`, `contador_uso` e flag `ativo`.
- **equipamentos** — pertencem a um cliente e a uma marca.
- **ordens_servico** — pertencem a um cliente; `status`, três datas, `valor_total`.
- **itens_os** — composição da OS: `(ordem_servico, equipamento, servico)`, chave única.
- **notificacoes** — mensagens a enviar ao cliente da M2; `tipo`, `status`, `tentativas`.

Todas as FKs são `ON DELETE RESTRICT`: nada com histórico vinculado é apagado por engano.
Os enums são tipos nativos do Postgres (`status_os`, `role_usuario`, `tipo_notificacao`,
`status_notificacao`), mapeados com `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.

O vínculo `clientes.id_usuario` foi removido na `V12`; a flag `ativo` de `usuarios` veio na
`V13`.

## Regras de negócio

### Autorização

Dois papéis, e a diferença entre eles é **uma só**:

| | MASTER | ADMIN |
|---|---|---|
| Quem é | equipe desenvolvedora | M2 Equipamentos |
| Clientes, equipamentos, marcas, serviços, OSs, itens | acesso total | acesso total |
| Cadastro de **usuários do sistema** (`/usuarios`) | **enxerga e gerencia** | **não enxerga** |

Consequências, todas importantes:

- **Não existe checagem de posse.** Qualquer usuário autenticado enxerga e altera todos os
  clientes da M2 e tudo que pende deles. Não há dado "de outro cliente" a proteger, porque
  cliente não é usuário. Nenhum service tem `garantirAcesso`.
- **Não existe filtro implícito por cliente nas listagens.** O `?clienteId=` é um filtro de
  conveniência e vale igual para os dois papéis, sem sobrescrita.
- **`clienteId` é obrigatório** ao criar equipamento e ordem de serviço. Nenhum usuário é
  um cliente, então não há de quem herdá-lo.
- O único `403` da API é um ADMIN tentando acessar o cadastro de usuários. As únicas seis
  `@PreAuthorize` do projeto estão no `UsuarioController`, todas `hasRole('MASTER')`.

### Usuários do sistema

- **Só o MASTER cria usuários.** Não há auto-cadastro nem ADMIN criando ADMIN. Todo usuário
  criado pela API nasce `ADMIN`: o papel não é atribuível por requisição.
- **Existe um único MASTER**, o semeado na migration, e ele **não pode ser desativado** —
  seria trancar o cadastro de usuários para sempre (422).
- **O ADMIN edita o próprio cadastro** por `GET`/`PUT /usuarios/eu`, onde o id vem do token
  e nunca do path. É o único ponto de `/usuarios` aberto a ele.
- **Usuário não se apaga: desativa-se** (`PATCH /usuarios/{id}/desativar`), como cliente e
  serviço. Não há `DELETE`.
- **Desativar vale na hora.** `isEnabled()` barra o login, e o `JwtAuthFilter` recusa o
  token já emitido a cada requisição — sem isso o desativado continuaria entrando pelos
  120 minutos de validade do JWT.
- Senha ausente no `PUT` significa "manter a atual", não "apagar".

### Ordem de serviço

Máquina de estados (`StatusOs`):

```
EM_ANDAMENTO ──concluir──> CONCLUIDA ──entregar──> ENTREGUE  (terminal)
      │                        │
      └────cancelar────────────┴────> CANCELADA              (terminal)
```

- Nasce sempre `EM_ANDAMENTO`, com `dataEntrada` = hoje (ou a data informada, que não pode
  ser futura).
- **A OS não abre vazia: `itens` é obrigatório e precisa ter ao menos um elemento**
  (`@NotEmpty`; lista ausente, `null` ou `[]` responde 400). Uma ordem sem nenhum serviço
  lançado não representa trabalho nenhum e distorceria qualquer leitura de faturamento.
  Cada item da lista passa pelas mesmas validações do `POST` de item, e a OS já nasce com o
  `valorTotal` somado. Itens adicionais continuam podendo ser lançados depois, conforme o
  diagnóstico avança.
- **Não pode ir direto de `EM_ANDAMENTO` para `ENTREGUE`**: precisa passar por `CONCLUIDA`.
- `ENTREGUE` e `CANCELADA` são terminais — não há reabertura.
- Repetir o status atual é **no-op idempotente**, não erro.
- `dataConcluida` e `dataEntregue` são preenchidas automaticamente na transição. A regra
  está duplicada de propósito: em Java (para responder 422 com mensagem legível e devolver
  as datas na resposta HTTP) e no trigger `trg_datas_os` da migration V6 (rede de segurança
  no banco).
- Só é **mutável enquanto `EM_ANDAMENTO`**: depois disso vira histórico. Isso vale para
  editar a observação e para adicionar, editar ou remover itens.
- Cliente, status e datas **não** são editáveis pelo `PUT` — só a observação.
- Não se abre OS para cliente inativo.
- Só pode ser excluída se não tiver itens — como toda OS nasce com pelo menos um, excluir
  exige antes esvaziá-la item a item (só possível enquanto `EM_ANDAMENTO`). Na prática o
  caminho normal para desfazer uma OS é **cancelar**, não excluir.

### Itens da OS

- Um item é o par **equipamento + serviço** dentro de uma OS. O trio
  `(os, equipamento, servico)` é único: o mesmo serviço não é lançado duas vezes para o
  mesmo equipamento na mesma OS.
- **Os dois vínculos são obrigatórios**: não existe item sem equipamento e sem serviço.
  `equipamentoId` e `servicoId` são `@NotNull` no request e `NOT NULL` na tabela — não há
  item "solto", de mão de obra avulsa ou de peça sem serviço associado.
- O **serviço sempre tem preço**: `servicos.valor` é `NOT NULL` (`CHECK valor >= 0`) e
  obrigatório no request. É daí que sai o dinheiro do item — **o item não tem valor próprio
  nem quantidade**, ele vale o `valor` do serviço no momento da leitura. Consequência a ter
  em mente: alterar o preço de um serviço no catálogo muda o total das OSs automáticas que
  já o usam (as manuais ficam congeladas).
- O equipamento precisa **pertencer ao cliente da OS**.
- O serviço precisa estar **ativo** no catálogo.
- Equipamento e serviço são **imutáveis** no item (formam sua chave) — trocar um deles é
  remover o item e adicionar outro. Só a observação é editável.
- Adicionar item incrementa `contadorUso` do serviço; remover decrementa (com piso em zero).
- Item de outra OS responde **404**: pela rota informada ele não existe.
- Os itens moram no `OrdemServicoController` porque não existem fora de uma OS.

### valorTotal: automático ou fixado à mão

O `valorTotal` tem dois modos, controlados pela flag `valorTotalManual` (coluna
`valor_total_manual`, migration V16):

- **Automático** (padrão, `valorTotalManual = false`): é a **soma dos valores dos serviços
  dos itens**. Adicionar/remover item recalcula. `recalcularValorTotal` é o único ponto que
  o escreve nesse modo.
- **Manual** (`valorTotalManual = true`): o valor foi **fixado à mão** (desconto, preço
  fechado) e **congela** — adicionar/remover item deixa de recalculá-lo (`recalcularValorTotal`
  vira no-op). Os itens continuam sendo lançados normalmente, só não mexem no total.

Como se define:

- Na **criação**, enviando `valorTotal` no corpo → nasce manual. Se omitido, nasce
  automático — a soma dos serviços dos `itens`, que nunca é vazia (a OS exige ao menos um).
- Depois, por **`PATCH /ordens-servico/{id}/valor-total`**: um valor **congela** (vira
  manual); `valorTotal` **null** (ou corpo `{}`) **reseta** para automático e recalcula na
  hora. Só enquanto `EM_ANDAMENTO`.

### Cadastros

- **Cliente** é um cadastro simples: nome, telefone, situação. Não cria usuário nem senha.
- Cliente e serviço têm **ativar/desativar** (`PATCH`), idempotentes — o caminho normal
  para "remover" sem perder histórico. `DELETE` é remoção definitiva e falha com 409 se
  houver vínculos.
- **Equipamento**: **não existe sem cliente e marca**. Ambos (`clienteId`, `marcaId`) são
  obrigatórios na criação e precisam já existir — id inexistente responde 404. O cliente
  dono é **imutável**: transferir um equipamento com histórico de OS não é edição de
  cadastro (só nome e marca mudam no `PUT`). Não se cadastra equipamento para cliente inativo
  (422).
- **Um equipamento é uma unidade física, não um modelo de catálogo.** O vínculo é com **um
  único cliente** (`id_cliente`, `NOT NULL`, `ManyToOne`) e não há como compartilhá-lo: dois
  clientes com "furadeira Makita" são **duas linhas** em `equipamentos`, duas unidades reais,
  cada uma com seu histórico de OS. Por isso **não existe unicidade** em `equipamentos` —
  nem por nome, nem por `(nome, marca)`, nem dentro do mesmo cliente (a M2 pode ter duas
  furadeiras iguais do mesmo dono). Quem dá identidade ao equipamento é o `id`, não o nome.
  Esse é também o motivo de o dono ser imutável: transferir a linha reescreveria o histórico
  da unidade errada.
- **Serviço**: `ativo` e `contadorUso` não vêm do request — o primeiro muda pelos PATCH,
  o segundo é mantido pelas ordens de serviço. O par **(nome, valor) é único** no catálogo
  (case-insensitive): pode haver "Troca de tela" por 450 e outra por 320, mas não duas por
  320 — colisão responde 409. Garantido pelo índice `uq_servicos_nome_valor` (V15) e por uma
  pré-checagem no service.
- **Marca**: nome único (case-insensitive).

### Notificações

Tabela e entidade existem (`PRAZO_INICIADO`, `PRAZO_ENCERRADO`, `PERSONALIZADO`; status
`PENDENTE` / `ENVIADO` / `FALHOU`, com contador de `tentativas`), mas **não há regra de
negócio implementada** — nem endpoints, nem disparo, nem integração com o n8n. Vão para o
cliente da M2, não para um usuário do sistema. A ser definido.

## Convenções do código

- **Idioma:** todo o domínio é em **português** — classes, campos, tabelas, mensagens de
  erro e comentários. Siga isso.
- **Comentários explicam o "porquê", não o "o quê".** O código existente comenta decisões
  não óbvias (por que uma FK é RESTRICT, por que existe um `flush()` explícito, por que um
  DTO resumido em vez de reaproveitar o completo). Mantenha esse padrão e não adicione
  comentários narrando o óbvio.
- **DTOs são `record`s**, com `fromEntity` estático para respostas. Entidades JPA nunca são
  expostas na API.
- **DTOs resumidos** (`ClienteResumoResponse`, `EquipamentoResumoResponse`,
  `ServicoResumoResponse`) existem para não vazar dados sensíveis nem forçar joins extras
  ao aninhar recursos.
- **Erros** seguem RFC 7807 (`ProblemDetail`), centralizados no `GlobalExceptionHandler`:
  - `400` — validação (`@Valid`), JSON malformado, parâmetro de tipo errado
  - `401` — token ausente/inválido, credenciais erradas
  - `403` — `AccessDeniedException`
  - `404` — `RecursoNaoEncontradoException`
  - `409` — `RecursoDuplicadoException` e violações de integridade do banco
  - `422` — `RegraDeNegocioException` (requisição válida que fere uma regra)
- **Paginação** usa o envelope próprio `PaginaResponse`, nunca o `Page` do Spring Data
  serializado direto (a estrutura JSON dele não é estável entre versões).
- **`open-in-view=false`.** Associações são `LAZY`; toda leitura consumida pela API declara
  `@EntityGraph` no repositório. Ao criar consulta nova, verifique o que a resposta toca.
- **`flush()` explícito** antes de traduzir violação de FK: faz o erro aparecer na operação
  e não no commit.
- Filtros de listagem usam **Specifications**, sempre opcionais.

## Fluxo de trabalho

- Branch por endpoint/feature (`endpoint/ordens-servico`, `endpoints/equipamentos-marcas-servicos`),
  PR para `main`.
- Toda mudança de schema é uma **migration Flyway nova** (`V<n>__descricao.sql`). Nunca
  edite uma migration já aplicada.
- A coleção Postman em `postman/API-REST-BV2.postman_collection.json` acompanha os
  endpoints — atualize-a ao mexer na API.
