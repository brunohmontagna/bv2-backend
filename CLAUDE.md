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
| **Usuário** | Quem faz **login** no sistema. Só existem dois papéis: `MASTER` e `ADMIN`. Cada usuário é dono de uma **conta**. |
| **Conta** | A base de dados própria de um usuário: clientes, serviços, templates de notificação e tudo que pende deles. Uma conta não enxerga a outra. |
| **MASTER** | A **equipe desenvolvedora**. Tem a própria conta, **escolhe em qual conta opera** (header `X-Conta-Id`) e enxerga o cadastro de usuários do sistema. |
| **ADMIN** | Uma empresa usuária — hoje, a **M2 Equipamentos**. Opera **só a própria conta**. Não enxerga o cadastro de usuários. |
| **Cliente** | O **cliente de uma conta** — a pessoa ou empresa que leva a furadeira para consertar. **Não faz login. Não é usuário do sistema.** É apenas um registro cadastral da conta. |

Ou seja: o que o `/clientes` lista é a carteira de clientes **da conta em que se está
operando** — para a M2, a carteira *dela*.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.0.7 (Web MVC, Data JPA, Security, Validation) |
| Banco | PostgreSQL 18, migrations com Flyway |
| Auth | JWT (HMAC256, biblioteca `com.auth0:java-jwt`) |
| Docs | springdoc-openapi / Swagger UI |
| Automações | n8n 2.x (webhook de saída que repassa a notificação ao WhatsApp) |
| E-mail | Spring Mail (`suporte@bv2.tech`, SMTP da Hostinger) |
| Frontend | React 19, TypeScript 6, Vite 8 (ainda não iniciado neste repositório) |

Este repositório contém **apenas o backend** por enquanto. O frontend virá depois (ou em
repositório próprio).

## Estado atual

As APIs REST estão finalizadas e já refatoradas para o modelo de papéis correto:

| Recurso | Endpoint base | Situação |
|---|---|---|
| Autenticação | `/auth/login` | pronto |
| Senha | `/auth/senha/*`, `/usuarios/eu/senha` | pronto (troca autenticada + recuperação por e-mail) |
| E-mail do usuário | `/usuarios/eu/email`, `/auth/email/confirmar` | pronto (confirmação no endereço novo) |
| Clientes | `/clientes` | pronto |
| Marcas | `/marcas` | pronto |
| Serviços | `/servicos` | pronto |
| Equipamentos | `/equipamentos` | pronto |
| Ordens de serviço + itens | `/ordens-servico` | pronto |
| Usuários | `/usuarios` | pronto (MASTER; `/usuarios/eu` para qualquer autenticado) |
| Painel | `/painel` | pronto (dashboard agregado, somente leitura) |
| Notificações | `/notificacoes` | pronto (disparo automático na troca de status, via n8n) |

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

Essas duas senhas ficam aqui em texto claro de propósito: são de banco local semeado e
descartável. **Credencial de serviço externo não entra neste arquivo** — ele é versionado.

### Credenciais de serviço (fora do repositório)

Valores reais vivem só no `.env`, que está no `.gitignore`. O `.env.example` versiona as
chaves, nunca os valores. Abaixo, onde encontrar cada uma:

| Variável | O que é | Onde obter |
|---|---|---|
| `MAIL_USERNAME` | `suporte@bv2.tech` | fixo |
| `MAIL_PASSWORD` | senha da **caixa postal**, não a da conta hPanel — a Hostinger não usa App Password como o Gmail | hPanel → E-mails → Contas de e-mail → `suporte@bv2.tech` → Alterar senha |
| `N8N_WEBHOOK_URL` / `_TOKEN` | webhook que repassa a notificação ao WhatsApp | painel do n8n, no node Webhook |
| `DB_PASSWORD` | Postgres local | definida na criação do volume Docker |
| `JWT_SECRET` | assinatura dos tokens | qualquer segredo com 32+ caracteres |

Ambiente local: o container do Postgres usa a **5432**. Suba com:

```bash
docker compose -p migrations-projeto-bv2 -f infra/docker-compose.dev.yml up -d
```

O `-p migrations-projeto-bv2` não é opcional: é o nome de projeto com que o volume de dados
foi criado. Sem ele o Compose deriva o nome do diretório (`infra`), cria um volume **novo e
vazio**, e a aplicação sobe num banco zerado — os dados continuam intactos no volume antigo,
só não estão montados.

Se a 5432 estiver ocupada, é o PostgreSQL nativo do host (instalado via apt, habilitado no
boot). O container sobe "healthy" mas **sem porta publicada**, e a aplicação passa a bater
no banco errado — o sintoma é falha de autenticação por senha. `sudo systemctl disable --now
postgresql` resolve.

## Comandos

```bash
# subir a aplicação (o .env é lido sozinho via spring.config.import; rode de dentro de backend/)
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
integration/  saída para fora da aplicação (webhook do n8n, envio de e-mail)
config/       OpenApiConfig
```

Migrations em `src/main/resources/db/migration` (`V1` … `V22`).

## Modelo de dados

```
usuarios (MASTER / ADMIN — quem loga; cada um é dono de uma conta)
   │
   ├──< clientes ──┬── (N) equipamentos ── (N:1) marcas   (marcas: catálogo global)
   │               ├── (N) ordens_servico ── (N) itens_os ──┬── equipamento
   │               └── (N) notificacoes                     └── servicos
   ├──< servicos
   └──< templates_notificacao   (configuração da notificação; único por conta + status_os)
```

**O dono (`id_usuario`) mora só nas tabelas raiz**: `clientes`, `servicos` e
`templates_notificacao` (V22). `equipamentos`, `ordens_servico`, `itens_os` e
`notificacoes` **herdam o dono pelo cliente** — gravar `id_usuario` nelas abriria espaço
para um equipamento pertencer a uma conta e o cliente dele a outra. Nas entidades o dono é
um `Long usuarioId` imutável, não um `@ManyToOne`: só serve para filtrar e comparar.

- **usuarios** — credenciais, `role` (`MASTER` | `ADMIN`) e flag `ativo`. Senha em BCrypt.
- **clientes** — cadastro dos clientes de uma conta. Nome, telefone, flag `ativo`. **Sem login.**
- **marcas** — catálogo **global** (compartilhado entre contas), nome único.
- **servicos** — catálogo da conta com `valor`, `contador_uso` e flag `ativo`.
- **equipamentos** — pertencem a um cliente e a uma marca.
- **ordens_servico** — pertencem a um cliente; `status`, três datas, `valor_total`.
- **itens_os** — composição da OS: `(ordem_servico, equipamento, servico)`, chave única.
- **notificacoes** — log de envios ao cliente da M2: o texto que saiu, `status_os` que
  disparou, `status` do envio e `tentativas`. Escrita só pelo sistema.
- **templates_notificacao** — configuração: um texto e uma flag `ativo` por (conta, status de OS).

Todas as FKs são `ON DELETE RESTRICT`: nada com histórico vinculado é apagado por engano.
Os enums são tipos nativos do Postgres (`status_os`, `role_usuario`,
`status_notificacao`), mapeados com `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`. O
`tipo_notificacao` existiu entre a `V8` e a `V18`: as notificações passaram a ser chaveadas
pelo próprio `status_os`, e um segundo enum paralelo só criaria duas fontes de verdade.

O vínculo `clientes.id_usuario` da V4 era 1:1 (cliente como usuário) e foi removido na
`V12`; a `V22` recriou a coluna com outro sentido — o **dono da conta**, N:1. A flag `ativo`
de `usuarios` veio na `V13`.

## Regras de negócio

### Autorização

Cada usuário tem a **própria conta** (V22), e os dois papéis diferem em duas coisas:

| | MASTER | ADMIN |
|---|---|---|
| Quem é | equipe desenvolvedora | empresa usuária (hoje, a M2) |
| Clientes, equipamentos, serviços, OSs, itens, notificações, painel | **de qualquer conta**, escolhida pelo header `X-Conta-Id` | **só da própria conta** |
| Marcas | catálogo global | catálogo global |
| Cadastro de **usuários do sistema** (`/usuarios`) | **enxerga e gerencia** | **não enxerga** |

**A conta da requisição vem de `security/ContaAtual`**, e só dele:

- **ADMIN** opera sempre a própria conta. Se mandar `X-Conta-Id` de **outra** conta, **403**
  explícito — o front nunca manda o header para ele, então é tentativa de acesso, e ignorar
  em silêncio esconderia isso. O próprio id no header é aceito.
- **MASTER sem header** opera a própria conta; com header, a conta pedida. Id inexistente é
  **404** (`Usuário`), valor não numérico é **422**. Conta **inativa** é aceita: o MASTER
  pode consultar o histórico de um usuário desativado (o front só lista as ativas).
- Header, e não query param, para nenhuma assinatura de endpoint mudar. O resultado fica
  guardado como atributo da requisição: a checagem roda uma vez, por mais que os services
  chamem `id()`.

Consequências, todas importantes:

- **Registro de outra conta responde 404, não 403.** Pela conta em que se está operando ele
  não existe. Todo `buscarEntidade` filtra pelo dono (`usuarioId` ou `cliente.usuarioId`), e
  toda `Specification` de listagem ganha o predicado fixo de conta.
- **As buscas de apoio também são por conta**: cliente da OS e do equipamento, serviço e
  equipamento do item. Não há como montar uma OS misturando cadastros de contas diferentes
  — a referência de outra conta simplesmente não é encontrada (404).
- **O `?clienteId=` continua sendo filtro de conveniência**, aplicado *dentro* da conta.
- **`clienteId` é obrigatório** ao criar equipamento e ordem de serviço. Nenhum usuário é
  um cliente, então não há de quem herdá-lo.
- **O disparo de notificação não usa `ContaAtual`.** Ele roda em `AFTER_COMMIT`, fora do
  fluxo da requisição, e busca o template pela conta **dona do cliente da OS**.
- **Excluir marca** pode dar 409 por equipamento de outra conta: o catálogo é global.
- Os `403` da API são: ADMIN no cadastro de usuários (seis `@PreAuthorize("hasRole('MASTER')")`
  no `UsuarioController`) e ADMIN com `X-Conta-Id` de outra conta.

### Usuários do sistema

- **Só o MASTER cria usuários.** Não há auto-cadastro nem ADMIN criando ADMIN. Todo usuário
  criado pela API nasce `ADMIN`: o papel não é atribuível por requisição.
- **Usuário novo nasce com conta vazia e com os 3 templates de notificação desligados**
  (`TemplatesNotificacaoPadrao`, mesmo texto da V18). O modal do front e o disparo contam
  com as três linhas existindo.
- **Existe um único MASTER**, o semeado na migration, e ele **não pode ser desativado** —
  seria trancar o cadastro de usuários para sempre (422).
- **O ADMIN edita o próprio cadastro** por `GET`/`PUT /usuarios/eu`, onde o id vem do token
  e nunca do path. É o único ponto de `/usuarios` aberto a ele.
- **Desativar é o caminho normal** (`PATCH /usuarios/{id}/desativar`), como cliente e
  serviço: tira o acesso e preserva a conta e o histórico.
- **Excluir (`DELETE /usuarios/{id}`) apaga o usuário e a conta inteira**: notificações,
  itens, OS, equipamentos, clientes, serviços, templates e tokens, nessa ordem (folhas
  primeiro), em deletes em massa na mesma transação. As FKs continuam `ON DELETE RESTRICT`
  de propósito — a limpeza é explícita no `UsuarioService.excluir`, e nada some por cascata
  em outro ponto do sistema. O MASTER não pode ser excluído (422). O front exige digitar
  `EXCLUIR` para liberar o botão. Os JWT do excluído morrem sozinhos: o `JwtAuthFilter` não
  acha mais o e-mail.
- **Desativar vale na hora.** `isEnabled()` barra o login, e o `JwtAuthFilter` recusa o
  token já emitido a cada requisição — sem isso o desativado continuaria entrando pelos
  120 minutos de validade do JWT.
- **O `PUT` só troca o nome.** Senha e e-mail já estiveram ali e saíram pelo mesmo motivo:
  mudavam sem prova de posse, e os dois são credenciais — o e-mail é o login. Um token
  roubado bastava para tomar a conta, e um typo no e-mail a trancava para sempre. Cada um tem
  seu fluxo verificado (seções Senha e E-mail do usuário), e isso vale **inclusive para o
  MASTER**, que não troca senha nem e-mail de outro usuário. Campos extras no corpo são
  ignorados, não recusados.

### Senha

Dois caminhos, os dois com prova de posse. **Não existe terceiro** — em particular, nem o
MASTER redefine a senha de outro usuário: quem esqueceu usa a recuperação como todo mundo.

| Situação | Rota | Prova |
|---|---|---|
| Sabe a senha e quer trocar | `PUT /usuarios/eu/senha` | informa a senha atual |
| Esqueceu a senha | `POST /auth/senha/esqueci` → e-mail → `POST /auth/senha/redefinir` | acessa a caixa postal |

As duas rotas de `/auth/senha` são **públicas** — quem esqueceu a senha não tem como se
autenticar para pedir a troca.

**A nova senha precisa ser diferente da atual**, nos dois caminhos (422). Na recuperação a
checagem vem **depois** de validar o token — só quem provou a posse da conta pode descobrir se
a senha escolhida é a atual — e a exceção desfaz a transação, então o link continua valendo
para uma nova tentativa. O 422 sai com `erros: { senhaNova }` (`RegraDeNegocioException` com
campo), para o front mostrar o aviso no campo e não no alerta de link inválido.

**Senha atual errada responde 422, não 401.** O 401 seria errado (o token é válido, o
usuário *está* autenticado) e perigoso na prática: interceptador de front costuma deslogar
em qualquer 401, então um erro de digitação expulsaria o usuário da sessão.

**Trocar a senha derruba as sessões abertas.** `usuarios.senha_alterada_em` (V19) guarda a
marca e o `JwtAuthFilter` recusa token cujo `iat` seja anterior a ela. Não foi preciso claim
novo: o `iat` já era emitido, só não era lido. Dois detalhes que quebram em silêncio se
mexidos:

- **`senha_alterada_em` é gravado truncado a segundos.** O `iat` do JWT tem precisão de
  segundos e a coluna guarda microssegundos — sem truncar, a comparação erraria por
  arredondamento e derrubaria sessões legítimas de forma intermitente.
- **A comparação é estritamente "antes".** Token emitido no mesmo segundo da troca
  sobrevive. Janela de 1 segundo aceita de propósito: a alternativa (`<=`) rejeitaria o
  login imediatamente seguinte à troca, que é um problema real de uso contra um risco
  teórico. `NULL` significa "nunca trocou" e o token passa.

**O fluxo de recuperação não revela quem tem conta.** `POST /auth/senha/esqueci` responde
**202 com corpo vazio** nos três casos — e-mail cadastrado, não cadastrado ou de usuário
inativo. É uma inconsistência deliberada com `POST /usuarios`, que devolve 409 dizendo que o
e-mail já existe: lá quem pergunta é um MASTER autenticado, aqui é um anônimo. Pelo mesmo
motivo, **todos os motivos de recusa da redefinição usam a mesma mensagem** — distinguir
"não existe" de "expirou" contaria ao atacante que aquele token um dia existiu.

Sobre o token (`tokens_verificacao`, criada na V19 e generalizada na V20 — ver a seção
E-mail do usuário):

- **Só o SHA-256 vai para o banco**, nunca o token. Vazamento do banco não pode virar tomada
  de contas. SHA-256 e não BCrypt por dois motivos: o token já nasce com 256 bits de
  `SecureRandom` (key stretching não acrescenta nada) e precisa ser **buscável por
  igualdade**, o que o sal do BCrypt impediria sem varrer a tabela inteira.
- **Uso único e prazo curto** (30 min, configurável). Redimir marca `usado_em` e invalida os
  demais pendentes do usuário; pedir um link novo também invalida os anteriores. Com dois
  e-mails na caixa de entrada, só o último funciona.
- `usado_em` nulo = pendente. O histórico fica, então dá para auditar quantos links foram
  pedidos e quais viraram troca de senha.

**Limitações aceitas** — são escopo, não descuido:

- **Sem rate limiting.** Nada impede pedir mil links. A invalidação em cascata mitiga em
  parte, mas ainda dá para inundar a caixa de entrada de alguém e estourar a cota de envio
  da Hostinger, derrubando o e-mail para todos.
- **Timing attack residual.** A resposta é idêntica, mas o caminho "e-mail existe" faz mais
  trabalho (gera token, grava, envia) e demora mais.
- **Token na query string** entra no histórico do navegador e pode vazar por `Referer`. É a
  prática corrente; a vida curta e o uso único são a mitigação.

**Envio de e-mail** (`integration/EmailService`): síncrono, sem `@Async` — o projeto não tem
`@EnableAsync` e a notificação por WhatsApp deliberadamente não abriu esse precedente. Os
timeouts SMTP são explícitos (5s) porque sem eles um servidor que aceita a conexão e não
responde prende a thread da requisição indefinidamente. **`spring.mail.username` em branco
desliga o envio**: a aplicação sobe, o token continua sendo gravado e só o e-mail não sai —
mesmo contrato do `N8nWebhookClient` com a URL vazia. O `EmailService` nunca lança e **nunca
loga o token nem o link**.

Configuração da Hostinger: `MAIL_USERNAME` é o endereço completo e `MAIL_PASSWORD` é a senha
da caixa postal (não há App Password como no Gmail). A porta **465 é SSL implícito**
(`MAIL_SSL=true`, `MAIL_STARTTLS=false`); a 587 é o inverso. Ligar o modo errado para a porta
trava a conexão até o timeout com `EOFException` e nenhuma mensagem útil. O `MAIL_FROM`
precisa ser a mesma conta que autentica, senão o servidor recusa com "sender address
rejected"; só o nome de exibição é livre.

### E-mail do usuário

O e-mail é o login (`findByEmail`, e o `subject` do JWT), então trocá-lo é tão sensível
quanto trocar a senha. **Duas etapas, e nada muda na primeira:**

```
PUT /usuarios/eu/email      (autenticado; senhaAtual + novoEmail + confirmação)  → 202
        │  link enviado ao endereço NOVO
POST /auth/email/confirmar  (público; só o token)                                → 204
```

**A confirmação vai para o endereço novo, não para o atual.** Verificar o atual provaria
identidade, mas não que o novo existe — e como o e-mail é o login, um typo trancaria a conta
de vez: o único jeito de sair seria excluir o usuário junto com a conta inteira, e se fosse
o MASTER — que não se exclui —, o cadastro de usuários ficaria inacessível (o seed da V9 só
roda em banco novo). Verificando no destino, endereço errado é
só um link que nunca chega, e a conta continua como estava.

**Por isso a senha atual é exigida.** Com a confirmação no destino, quem clica é quem
controla o destino — no ataque, o próprio atacante. Sem a senha, uma sessão roubada viraria
tomada de conta: o atacante aponta a conta para a caixa dele, confirma sozinho e depois usa o
"esqueci minha senha". Senha errada é **422, não 401**, pela mesma razão do endpoint de senha.

- **Unicidade checada duas vezes**: no pedido e na confirmação. Entre uma e outra, alguém pode
  cadastrar aquele endereço; sem a segunda checagem, a corrida estouraria como violação de
  constraint no commit, virando 409 genérico em vez de mensagem legível.
- **O endereço novo viaja no token** (`tokens_verificacao.email_novo`), não na URL nem no
  corpo da confirmação. A tela do front não tem formulário: só posta o token.
- **A sessão cai sozinha, sem código.** Depois da troca, o `JwtAuthFilter` faz
  `loadUserByUsername(emailAntigo)`, não acha ninguém e não autentica. Parece bug; não é.
- `/auth/email/confirmar` é **pública** como `/auth/senha/redefinir`: o token é a credencial,
  e o link costuma ser aberto em outro navegador, sem sessão.

**A tabela de tokens é compartilhada** com a recuperação de senha. A V20 renomeou
`tokens_recuperacao_senha` para `tokens_verificacao` e acrescentou `finalidade`
(`RECUPERACAO_SENHA` | `ALTERACAO_EMAIL`). A mecânica — gerar, hashear, validar, consumir —
mora **só** no `TokenVerificacaoService`; os dois fluxos decidem o que fazer com o token, não
como ele funciona. Duas cópias de código de segurança divergiriam na primeira correção
aplicada só de um lado.

Uma armadilha que quebra em silêncio se mexida: **a invalidação em cascata filtra por
finalidade.** Sem o filtro, pedir uma troca de e-mail derrubaria um link de recuperação de
senha recém-pedido, e vice-versa. E um token de uma finalidade é recusado na outra com a
**mesma mensagem** de token inexistente.

Sobra cosmética da V20: o PostgreSQL 18 dá nome às constraints `NOT NULL`, e as cinco
herdadas da V19 ainda se chamam `tokens_recuperacao_senha_*_not_null`. Não afetam nada —
ninguém referencia esses nomes — e não justificaram uma migration só para renomeá-las.

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
- **Exclusão é definitiva e leva junto os itens e o log de notificações** da ordem. Só vale
  para `EM_ANDAMENTO` e `CANCELADA`: `CONCLUIDA` e `ENTREGUE` entram no faturamento, e
  apagá-las reescreveria em silêncio o painel de um período já fechado (422). O caminho
  normal para tirar uma OS de circulação continua sendo **cancelar**, que preserva tudo.
- A limpeza é explícita no service, na ordem das dependências (itens, notificações, OS),
  porque as FKs são `ON DELETE RESTRICT`. O `contadorUso` dos serviços é devolvido item a
  item: sem isso o ranking de mais executados contaria para sempre uma execução que deixou
  de existir.

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
  cada uma com seu histórico de OS. Esse é também o motivo de o dono ser imutável: transferir
  a linha reescreveria o histórico da unidade errada.
- **Unicidade do equipamento: `(cliente, marca, nome)`** (case-insensitive, nome sem espaços
  nas pontas). O mesmo nome pode se repetir em clientes diferentes, e no mesmo cliente desde
  que a marca seja outra — mas um cliente não tem duas "Furadeira" da Makita, porque na
  abertura da OS elas seriam indistinguíveis. Colisão responde 409, na criação e no `PUT`.
  Garantido pelo índice `uq_equipamentos_cliente_marca_nome` (V21) e por uma pré-checagem no
  service. Até a V20 não havia unicidade nenhuma em `equipamentos`.
- **Serviço**: `ativo` e `contadorUso` não vêm do request — o primeiro muda pelos PATCH,
  o segundo é mantido pelas ordens de serviço. O par **(nome, valor) é único** no catálogo
  (case-insensitive): pode haver "Troca de tela" por 450 e outra por 320, mas não duas por
  320 — colisão responde 409. Garantido pelo índice `uq_servicos_nome_valor` (V15) e por uma
  pré-checagem no service.
- **Marca**: nome único (case-insensitive).

### Painel

`GET /painel` é a única leitura agregada da API: consolida resumo, faturamento, série
mensal e rankings numa resposta só, para o dashboard. Qualquer autenticado acessa, sem
`@PreAuthorize`, e **tudo é recortado pela conta** (`ContaAtual`): o `usuarioId` é resolvido
uma vez e passado às doze consultas, que filtram por `o.cliente.usuarioId`. O MASTER vê o
painel da conta escolhida no `X-Conta-Id`, nunca a soma de todas.

- **Período** por `?dataInicio=&dataFim=`, ambos opcionais e **inclusivos nas duas pontas**.
  Omitidos, valem os últimos 30 dias. `dataInicio` sozinha ancora no `dataFim` informado, e
  não em hoje — senão `?dataFim=2024-01-01` geraria um intervalo invertido artificial. A
  janela resolvida volta em `periodo`: sem esse eco o front não sabe o que está exibindo.
- **Período invertido responde 422**, ao contrário de `GET /ordens-servico`, que aceita e
  devolve página vazia. A diferença é deliberada: lá o vazio é obviamente vazio; aqui um
  painel zerado se disfarça de "não houve movimento".
- **Faturamento**: OS `ENTREGUE` é `faturamentoRealizado`; `EM_ANDAMENTO` + `CONCLUIDA` é
  `faturamentoEmAberto`; `CANCELADA` **nunca** entra em nenhum dos dois. A soma vem sempre
  de `ordens_servico.valor_total`, **jamais dos itens** — `valorTotalManual` existe
  justamente para os dois números divergirem.
- **Cada métrica usa a data do próprio evento**, e não uma data única para tudo: entrada de
  trabalho e execução de serviço por `dataEntrada`; faturamento realizado e entrega por
  `dataEntregue`. `faturamentoEmAberto` e a contagem de valor fixado à mão ficam com
  `dataEntrada` por eliminação — uma OS não entregue não tem outro evento datado.
- **`resumo` é a exceção: não é recortado por período.** Contagem de OS por status e
  tamanho dos cadastros são estado atual, não movimento da janela.
- Período sem movimento responde **200 com tudo zerado** — nunca 404, nunca corpo parcial.
  Os quatro status aparecem mesmo em zero, a série traz todos os meses do intervalo
  (inclusive os vazios, para o gráfico não ter buracos) e os rankings vêm `[]`.
- **Rankings contam coisas diferentes de propósito**: serviço conta *execuções* (`count`
  de itens — o mesmo serviço entra duas vezes na OS se for para equipamentos diferentes);
  marca conta *ordens* (`count distinct` de OS). Os dois excluem OS `CANCELADA`. Top 5 fixo,
  sem query param.
- **É o primeiro precedente de query agregada do projeto** (antes não havia nenhum `@Query`).
  JPQL com projeção por construtor, não SQL nativo: os enums são tipos nomeados do Postgres,
  então no nativo toda comparação precisaria de `::status_os`. **Nunca escreva o literal do
  status dentro do JPQL** — passe `StatusOs` como parâmetro. As queries moram no repositório
  da entidade que agregam; as dos rankings de serviço e marca estão no `ItemOsRepository`
  porque `OrdemServico` não mapeia `@OneToMany` de itens.
- São **12 consultas por requisição**, número fixo que não cresce com o volume (não há N+1).
- A `V17` criou os primeiros índices não-implícitos do schema (datas, FKs). Com o volume
  atual o planner ainda prefere seq scan; eles documentam o padrão de acesso.

### Notificações

Quando a OS muda de status, o cliente da M2 recebe um WhatsApp. Vão para o **cliente da
M2**, nunca para um usuário do sistema. O fluxo é de mão única — o BV2 é quem troca o
status, então ele mesmo chama o n8n; não há polling nem rota de entrada:

```
transicionar() ──commit──> evento AFTER_COMMIT ──> POST no webhook do n8n ──> WhatsApp
```

**Duas tabelas, dois papéis.** `templates_notificacao` é configuração (o que cada conta liga e
desliga); `notificacoes` é log de execução (o que o sistema escreveu). A API **edita a
primeira e só lê a segunda** — não há `POST` nem `DELETE` de notificação.

**As duas são por conta.** Cada conta tem os próprios três templates (V22: uma linha por
`(id_usuario, status)`, id surrogate) e só vê o log dos próprios clientes. O disparo
automático usa os templates da conta **dona do cliente da OS**, não a de quem trocou o
status.

**O texto do template não é editável pela M2.** Pela API oficial do WhatsApp, mensagem
iniciada pela empresa sai de um modelo pré-aprovado, então quem define o texto é o modelo, não
a tela. O `PUT` aceita `conteudo` e o backend valida os placeholders, mas o front manda de
volta o conteudo que já está salvo e só mexe na flag `ativo`; na tela o texto aparece como
prévia, com os placeholders trocados pelos exemplos do `GET /notificacoes/placeholders`.

- **A chave natural é conta + `StatusOs`**, não um enum de "tipo de notificação". O que dispara o
  envio é a transição da OS; um segundo enum paralelo criaria duas fontes de verdade. Foi o
  que motivou dropar o `tipo_notificacao` na `V18`.
- **Notificam `CONCLUIDA`, `ENTREGUE` e `CANCELADA`**, as três linhas semeadas na `V18`,
  copiadas para cada conta na `V22` e criadas junto com todo usuário novo.
  `EM_ANDAMENTO` não notifica por não ter linha — não por um `if` no código. Habilitar
  abertura de OS um dia é um `INSERT`, não uma migration de enum.
- **Nascem todas desligadas** (`ativo = false`): ninguém manda WhatsApp para cliente real
  por acidente na primeira subida.
- **Quem decide se o aviso sai é o BV2, não o n8n.** Com `ativo = false` não há POST e não
  há linha em `notificacoes` — o n8n nem fica sabendo da transição. O n8n não tem `IF` de
  decisão nem campo de texto: recebe a mensagem pronta e encaminha.
- **O texto é um template com placeholders** (`{cliente}`, `{os}`, `{valor}`, `{status}`,
  `{dataEntrada}`, `{dataConcluida}`, `{dataEntregue}`), resolvidos no `RenderizadorMensagem`.
  O conjunto é fechado e vive só lá: é a mesma lista que valida o `PUT` e que o
  `GET /notificacoes/placeholders` publica para o front. **Placeholder desconhecido é 422 no
  `PUT`**, não erro no envio — sem isso o cliente receberia `{nome}` literal no WhatsApp.
- **`notificacoes.conteudo` guarda o texto renderizado, não o template.** Congelar no envio é
  o que impede que editar o template reescreva o histórico do que já saiu.
- **Falha do n8n nunca quebra a troca de status.** O disparo é `AFTER_COMMIT`, o client
  devolve `false` em vez de lançar, e a linha fica `FALHOU` com `tentativas` incrementado.
  `N8N_WEBHOOK_URL` em branco desliga a integração e a aplicação sobe normalmente — é o que
  permite rodar o projeto sem n8n configurado.
- **Repetir o status atual não remanda mensagem**: o `return` idempotente de `transicionar`
  sai antes do `publishEvent`.
- Não há reenvio nem retry agendado. A coluna `tentativas` existe e é incrementada, mas hoje
  sempre vale 1.

Dois detalhes de implementação que **quebram em silêncio** se mexidos:

- `processarTransicao` é `@Transactional(REQUIRES_NEW)`. Em `AFTER_COMMIT` não há mais
  transação ativa: sem isso o `save` é descartado sem erro, o n8n recebe a chamada e o
  `GET /notificacoes` volta vazio.
- O `NotificacaoOsListener` é **classe separada** do `NotificacaoService`. Chamada interna
  não passa pelo proxy do Spring, e o `REQUIRES_NEW` seria ignorado se o listener morasse
  dentro do próprio service.

Divergência deliberada de convenção: os templates usam `PUT /notificacoes/templates/{statusOs}`
com corpo, e não o par `PATCH /{id}/ativar` + `/desativar` de cliente e serviço. Lá é ciclo
de vida de cadastro; aqui é um formulário de configuração que o front lê e escreve inteiro.

Configuração em `N8N_WEBHOOK_URL`, `N8N_WEBHOOK_TOKEN` (vai no header `X-BV2-Token`, casa
com a credencial *Header Auth* do node Webhook) e `N8N_WEBHOOK_TIMEOUT_SEGUNDOS`.

**Do lado do n8n**, três nodes: Webhook → WhatsApp → Respond to Webhook. O BV2 marca
`ENVIADO` no `200` do webhook, então o *Respond to Webhook* precisa ficar no **fim** do
fluxo — em `Immediately` o `ENVIADO` significaria só "n8n recebeu". O payload leva a
`mensagem` pronta **e** os campos estruturados: fora da janela de 24h a Cloud API recusa
texto livre (erro `131047`) e exige template aprovado na Meta, e aí o n8n mapeia
`cliente.nome` / `ordemServico.id` nas variáveis sem tocar no backend.

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
