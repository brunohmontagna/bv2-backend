# Orientação para Agente de IA — Povoamento do banco da API REST BV2 (M2 Equipamentos)

> **Leitor deste arquivo:** um agente de IA com acesso ao **MCP do Postman** e capacidade de fazer requisições HTTP à API.
> **Objetivo:** povoar o banco do sistema de Ordens de Serviço da **M2 Equipamentos** (assistência técnica de ferramentas elétricas em Ponta Grossa/PR) com dados **próximos da realidade**: marcas e modelos de ferramentas que existem de verdade no mercado brasileiro.

---

## 0. Regras de ouro (leia antes de tudo)

1. **Tudo deve ser criado pela API**, seguindo a documentação de rotas da collection do Postman. Nada de `INSERT` direto no banco. SQL só é aceitável se uma informação for **impossível** de gerar pela API — e, nesse caso, **pare e pergunte ao usuário antes**.
2. **Fonte da verdade dos contratos:** a collection **`API REST BV2 | Projeto UEPG`**
   - Workspace: `Bruno | Personal Projects` (`c348a994-66a5-4a52-80a2-5af86358e1a8`)
   - Collection UID: `46776779-292edbc2-f35e-49f0-9af1-6b34cc4fc7be`
   - Cada endpoint tem um request **"de referência"** com tabela de campos, regras e exemplos de resposta. **Confirme o nome exato dos campos do corpo ali antes de enviar.**
   - A collection completa (`model=full`) é grande e pode vir truncada. Alternativa confiável: `GET {{baseUrl}}/v3/api-docs` (OpenAPI, rota pública) traz os schemas exatos dos DTOs.
3. **Os `itens_os` NÃO são criados separadamente.** Eles são preenchidos **pelo próprio endpoint de criação de OS** (`POST /ordens-servico`), que já recebe a lista de itens (equipamento + serviço) no corpo, conforme a documentação do MCP do Postman. Use `POST /ordens-servico/{id}/itens` apenas se precisar complementar uma OS já criada.
4. **Os IDs são gerados pelo banco.** Este documento usa **chaves de referência** (`M01`, `S01`, `C01`, `E01`, `OS01`). Mantenha um mapa `chave → id real` a cada `201 Created` e use-o nos passos seguintes.
5. **Idempotência:** antes de criar, consulte com filtro (`GET /marcas?nome=...`, `GET /clientes?nome=...`) e pule o que já existir. `409` = já existe → registre o id existente e siga.
6. **Não altere** o usuário MASTER (semeado pela migration).

---

## 1. Ordem de execução

| # | Etapa | Endpoint(s) | Token |
|---|---|---|---|
| 1 | Login MASTER | `POST /auth/login` | — (credenciais MASTER: peça ao usuário ou leia `{{masterEmail}}`/`{{masterSenha}}` do environment) |
| 2 | Criar 2 usuários ADMIN | `POST /usuarios` | MASTER |
| 3 | Criar 8 marcas | `POST /marcas` | MASTER |
| 4 | Criar 62 serviços | `POST /servicos` | MASTER |
| 5 | Criar 21 clientes | `POST /clientes` | MASTER |
| 6 | Criar 44 equipamentos | `POST /equipamentos` | MASTER |
| 7 | Atualizar templates de notificação com `ativo=false` | `PUT /notificacoes/templates/{statusOs}` | MASTER |
| 8 | Criar 33 OS **já com itens** | `POST /ordens-servico` | MASTER |
| 9 | Ajustar valor total manual (3 OS) | `PATCH /ordens-servico/{id}/valor-total` (ou `valorTotal` na criação) | MASTER |
| 10 | Transições das OS de clientes **fictícios** (OS10–OS33) | `PATCH .../concluir`, `.../entregar`, `.../cancelar` | MASTER |
| 11 | Reativar templates (`ativo=true`) | `PUT /notificacoes/templates/{statusOs}` | MASTER |
| 12 | Transições das OS de **Bruno, Vicente e Vitinho** (OS01–OS09) | idem | MASTER |
| 13 | Desativar o que deve ficar inativo | `PATCH /clientes/{id}/desativar`, `PATCH /servicos/{id}/desativar` | MASTER |
| 14 | Conferência | `GET` das listas, `GET /notificacoes`, `GET /painel` | MASTER |

> **Por que desligar os templates nas etapas 7–10?** Toda transição para `CONCLUIDA`, `ENTREGUE` ou `CANCELADA` dispara um WhatsApp (via n8n) para o telefone do cliente. Os telefones dos clientes fictícios **podem pertencer a pessoas reais**. Só os 3 clientes de teste (Bruno, Vicente, Vitinho) devem receber mensagens.

---

## 2. Usuários (somente 2 ADMIN)

Criados com o token MASTER em `POST /usuarios` (o papel nasce sempre `ADMIN`). As senhas têm exatamente 8 caracteres, o mínimo aceito pela API.

| Nome | E-mail | Senha |
|---|---|---|
| Bruno Montagna | brunoh.montagna@gmail.com | bruno123 |
| Vitor Clock | vitorclock7@gmail.com | vitor123 |

> Se o DTO exigir outros campos (ex.: confirmação de senha), confira no request **"Criar usuario ADMIN"** da pasta *9. Usuarios (MASTER)*. Não crie nenhum outro usuário.

---

## 3. Marcas (8)

Corpo: `{ "nome": "..." }` — `nome` é único (sem diferenciar maiúsculas), máx. 50.

| Ref | Nome |
|---|---|
| M01 | Makita |
| M02 | Bosch |
| M03 | DeWalt |
| M04 | Stanley |
| M05 | Black+Decker |
| M06 | Vonder |
| M07 | Metabo |
| M08 | Skil |

---

## 4. Serviços (62)

Corpo esperado: `nome` (máx. 50), `descricao` (máx. 500), `valor` (BigDecimal ≥ 0, 2 casas). Confirme os nomes dos campos no request de referência da pasta *5. Servicos (CRUD)*.

> Valores são **estimativas de mercado** para mão de obra + peça comum em assistência técnica de ferramentas elétricas, não uma tabela oficial.

### 4.1 Gerais
| Ref | Nome | Descrição | Valor |
|---|---|---|---|
| S01 | Diagnóstico técnico e orçamento | Desmontagem, testes elétricos e mecânicos e emissão de orçamento detalhado. | 40.00 |
| S02 | Revisão preventiva completa | Desmontagem total, limpeza, troca de graxa, inspeção de escovas, rolamentos e cabo. | 120.00 |
| S03 | Limpeza interna e lubrificação | Remoção de pó e resíduos do motor e caixa de engrenagens, com nova lubrificação. | 60.00 |
| S04 | Troca de escovas de carvão | Substituição do par de escovas de carvão e verificação do coletor. | 45.00 |
| S05 | Troca de cabo de alimentação | Substituição do cabo elétrico danificado, com prensa-cabo e plugue padrão NBR 14136. | 35.00 |
| S06 | Troca de plugue | Substituição do plugue por modelo padrão NBR 14136. | 20.00 |
| S07 | Troca de interruptor/gatilho | Substituição do interruptor com ou sem controle de velocidade. | 65.00 |
| S08 | Troca de rolamentos do motor | Substituição dos rolamentos dianteiro e traseiro do induzido. | 80.00 |
| S09 | Troca de capacitor antirruído | Substituição do capacitor/filtro supressor de interferência. | 40.00 |
| S10 | Reparo de chave reversora | Reparo ou troca da chave de reversão de rotação. | 50.00 |
| S11 | Troca de carcaça ou empunhadura | Substituição de carcaça trincada ou empunhadura quebrada. | 70.00 |
| S12 | Teste de isolação e segurança elétrica | Medição de isolação com megômetro e teste de fuga de corrente. | 30.00 |

### 4.2 Furadeiras
| Ref | Nome | Descrição | Valor |
|---|---|---|---|
| S13 | Troca de induzido - furadeira até 710W | Substituição do induzido (rotor) de furadeiras de até 710 W. | 140.00 |
| S14 | Troca de induzido - furadeira acima de 710W | Substituição do induzido de furadeiras acima de 710 W. | 180.00 |
| S15 | Troca de estator - furadeira | Substituição do estator (campo) da furadeira. | 130.00 |
| S16 | Troca de mandril com chave 1/2" | Substituição do mandril de 13 mm com chave. | 75.00 |
| S17 | Troca de mandril de aperto rápido | Substituição do mandril por modelo de aperto rápido (sem chave). | 95.00 |
| S18 | Reparo do sistema de impacto | Troca das catracas de impacto e da mola do seletor. | 90.00 |
| S19 | Troca de engrenagens - furadeira | Substituição das engrenagens da caixa de redução. | 110.00 |
| S20 | Troca do módulo de velocidade variável | Substituição do controle eletrônico de rotação. | 85.00 |
| S21 | Troca do eixo árvore - furadeira | Substituição do eixo de saída empenado ou com folga. | 100.00 |
| S22 | Troca do seletor de impacto | Substituição do botão seletor furar/impacto. | 40.00 |

### 4.3 Parafusadeiras e ferramentas a bateria
| Ref | Nome | Descrição | Valor |
|---|---|---|---|
| S23 | Troca de motor brushless - parafusadeira | Substituição do motor sem escovas de parafusadeiras 18V/20V. | 280.00 |
| S24 | Troca de motor escovado - parafusadeira | Substituição do motor com escovas de parafusadeiras 12V/18V. | 160.00 |
| S25 | Troca da embreagem de torque | Substituição do anel e mecanismo de ajuste de torque. | 95.00 |
| S26 | Troca da caixa planetária | Substituição do conjunto de engrenagens planetárias. | 150.00 |
| S27 | Troca de placa eletrônica | Substituição do módulo eletrônico de controle. | 190.00 |
| S28 | Recuperação de bateria 18V/20V | Troca das células de lítio e balanceamento do pack. | 220.00 |
| S29 | Reparo de carregador de bateria | Diagnóstico e reparo da fonte do carregador. | 90.00 |
| S30 | Troca dos terminais de contato | Substituição dos terminais entre bateria e ferramenta. | 55.00 |

### 4.4 Esmerilhadeiras
| Ref | Nome | Descrição | Valor |
|---|---|---|---|
| S31 | Troca de induzido - esmerilhadeira 4.1/2" | Substituição do induzido de esmerilhadeiras de 115 mm. | 150.00 |
| S32 | Troca de induzido - esmerilhadeira 7"/9" | Substituição do induzido de esmerilhadeiras de 180/230 mm. | 230.00 |
| S33 | Troca de estator - esmerilhadeira | Substituição do estator da esmerilhadeira. | 140.00 |
| S34 | Troca de coroa e pinhão | Substituição do par de engrenagens cônicas do cabeçote. | 120.00 |
| S35 | Troca da trava do eixo | Substituição do botão e pino de trava do eixo. | 45.00 |
| S36 | Troca da proteção do disco | Substituição da capa de proteção (guarda) do disco. | 55.00 |
| S37 | Troca do eixo de saída e flange | Substituição do eixo M14 e da flange de apoio. | 80.00 |
| S38 | Rebobinamento de estator | Rebobinamento do campo com fio esmaltado e verniz. | 170.00 |

### 4.5 Marteletes
| Ref | Nome | Descrição | Valor |
|---|---|---|---|
| S39 | Troca de induzido - martelete | Substituição do induzido de marteletes SDS Plus. | 210.00 |
| S40 | Troca de pistão e anéis de vedação | Substituição do pistão e dos O-rings do sistema eletropneumático. | 160.00 |
| S41 | Troca do percussor | Substituição do percussor (batedor) do martelete. | 130.00 |
| S42 | Troca do porta-ferramenta SDS Plus | Substituição do cilindro e do encaixe SDS Plus. | 150.00 |
| S43 | Troca da embreagem de segurança | Substituição da embreagem de sobrecarga do martelete. | 120.00 |
| S44 | Troca do seletor de modo - martelete | Substituição do seletor furar/rompedor/impacto. | 75.00 |
| S45 | Troca de graxa da caixa de percussão | Limpeza e nova graxa específica para o mecanismo de percussão. | 70.00 |
| S46 | Troca do rolamento oscilante | Substituição do rolamento oscilante (wobble) do martelete. | 140.00 |

### 4.6 Serras
| Ref | Nome | Descrição | Valor |
|---|---|---|---|
| S47 | Troca de induzido - serra circular | Substituição do induzido de serras circulares 7.1/4". | 190.00 |
| S48 | Troca de induzido - serra tico-tico | Substituição do induzido de serras tico-tico. | 140.00 |
| S49 | Troca de engrenagem - serra circular | Substituição da engrenagem do eixo da lâmina. | 95.00 |
| S50 | Alinhamento da base - serra circular | Regulagem de esquadro, ângulo de 45° e profundidade de corte. | 60.00 |
| S51 | Troca da capa de proteção retrátil | Substituição da capa retrátil e da mola de retorno. | 85.00 |
| S52 | Troca do mecanismo pendular - tico-tico | Substituição da haste e do conjunto pendular. | 110.00 |
| S53 | Troca do rolete guia - tico-tico | Substituição do rolete de apoio da lâmina. | 45.00 |
| S54 | Troca de induzido - serra mármore | Substituição do induzido de serras mármore 110 mm. | 180.00 |
| S55 | Troca de rolamentos - serra mármore | Substituição dos rolamentos do eixo da serra mármore. | 90.00 |

### 4.7 Lixadeiras, plainas e tupias
| Ref | Nome | Descrição | Valor |
|---|---|---|---|
| S56 | Troca da base - lixadeira orbital | Substituição da base (patim) e das presilhas da lixa. | 70.00 |
| S57 | Troca de induzido - lixadeira | Substituição do induzido de lixadeiras orbitais e roto-orbitais. | 130.00 |
| S58 | Troca de correia - plaina elétrica | Substituição da correia dentada da plaina. | 45.00 |
| S59 | Afiação de facas - plaina elétrica | Afiação e balanceamento do par de facas. | 50.00 |
| S60 | Troca de pinça - tupia | Substituição da pinça (collet) de 6 ou 8 mm. | 60.00 |
| S61 | Troca de induzido - plaina/tupia | Substituição do induzido de plainas e tupias. | 160.00 |
| S62 | Troca do excêntrico - lixadeira orbital | Substituição do contrapeso/excêntrico da lixadeira. | 85.00 |

> **Etapa 13:** desative **S54** e **S59** (`PATCH /servicos/{id}/desativar`). Nenhuma OS usa esses dois, então o catálogo terá serviços inativos para testar filtros.

---

## 5. Clientes (21)

Corpo: `{ "nome": "...", "telefone": "..." }` — telefone **só dígitos, 10 a 13**.

> ⚠️ **Obrigatórios e com telefone exato:** C01, C02 e C03. São os únicos números reais e os únicos que devem receber WhatsApp. Todos os demais telefones são fictícios.

| Ref | Nome | Telefone |
|---|---|---|
| **C01** | **Bruno Montagna** | **5542988886690** |
| **C02** | **Vicente Silva** | **5542988085686** |
| **C03** | **Vitinho Clock** | **5542991092748** |
| C04 | Marcenaria Campos Gerais | 554232200404 |
| C05 | Serralheria Uvaranas | 554232210505 |
| C06 | Construtora Vila Velha | 554232220606 |
| C07 | João Carlos Pereira | 5542999110107 |
| C08 | Ana Paula Ferreira | 5542999120208 |
| C09 | Oficina Mecânica Nova Rússia | 554232250309 |
| C10 | Ricardo Almeida | 5542998130410 |
| C11 | Elétrica Oficinas | 554232260511 |
| C12 | Marcos Vinícius Ramos | 5542991140612 |
| C13 | Gesso e Drywall Contorno | 554232270713 |
| C14 | Fernanda Lopes | 5542998150814 |
| C15 | Carlos Eduardo Kaminski | 5542999160915 |
| C16 | Metalúrgica Boa Vista | 554232281016 |
| C17 | Luciana Andrade | 5542991171117 |
| C18 | Paulo Roberto Schmidt | 5542998181218 |
| C19 | Vidraçaria Estrela | 554232291319 |
| C20 | Tiago Wosniak | 5542999191420 |
| C21 | Renata Mendes | 5542991201521 |

> **Etapa 13:** desative **C21** (`PATCH /clientes/{id}/desativar`) **depois** de criar o equipamento dela. Ela não tem OS: cliente inativo não pode receber OS nem equipamentos novos (422).

---

## 6. Equipamentos (44)

Cada equipamento pertence a **um** cliente (imutável) e a **uma** marca. Corpo esperado: `nome` (máx. 50), id da marca e id do cliente — confirme os nomes exatos no request de referência da pasta *6. Equipamentos (CRUD)*.

Todos os modelos abaixo **existem no mercado brasileiro**. A tensão no nome diferencia unidades iguais de clientes diferentes; se mesmo assim a API devolver `409` por nome duplicado, acrescente ` - unid. 2` ao nome.

| Ref | Cliente | Marca | Nome do equipamento |
|---|---|---|---|
| E01 | C01 | M01 | Furadeira de Impacto Makita HP1640 127V |
| E02 | C01 | M02 | Esmerilhadeira Angular Bosch GWS 850 127V |
| E03 | C01 | M03 | Parafusadeira/Furadeira DeWalt DCD7781 20V |
| E04 | C02 | M03 | Martelete SDS Plus DeWalt D25133K 220V |
| E05 | C02 | M08 | Serra Circular Skil 5402 127V |
| E06 | C02 | M06 | Furadeira de Impacto Vonder FIV 852 220V |
| E07 | C03 | M04 | Esmerilhadeira Angular Stanley SG7115 127V |
| E08 | C03 | M05 | Serra Tico-Tico Black+Decker KS501 127V |
| E09 | C03 | M02 | Furadeira de Impacto Bosch GSB 13 RE 127V |
| E10 | C04 | M01 | Serra Circular Makita 5007N 220V |
| E11 | C04 | M01 | Serra Tico-Tico Makita 4329 220V |
| E12 | C04 | M05 | Lixadeira Orbital Black+Decker BS200 220V |
| E13 | C04 | M02 | Serra Tico-Tico Bosch GST 650 220V |
| E14 | C05 | M03 | Esmerilhadeira Angular DeWalt DWE4120 220V |
| E15 | C05 | M01 | Esmerilhadeira Angular Makita GA4530 220V |
| E16 | C05 | M07 | Esmerilhadeira Angular Metabo W 750-115 220V |
| E17 | C06 | M02 | Martelete Bosch GBH 2-24 D 220V |
| E18 | C06 | M01 | Martelete Makita HR2470 220V |
| E19 | C06 | M04 | Furadeira de Impacto Stanley SDH700 220V |
| E20 | C07 | M05 | Furadeira de Impacto Black+Decker TM500 127V |
| E21 | C08 | M02 | Parafusadeira Bosch GSR 120-LI 12V |
| E22 | C09 | M04 | Esmerilhadeira Angular Stanley SG6115 127V |
| E23 | C09 | M07 | Furadeira de Impacto Metabo SBE 650 127V |
| E24 | C10 | M06 | Furadeira de Impacto Vonder FIV 550N 127V |
| E25 | C10 | M01 | Esmerilhadeira Angular Makita M9510 127V |
| E26 | C11 | M03 | Parafusadeira/Furadeira DeWalt DCD996 20V |
| E27 | C11 | M02 | Furadeira de Impacto Bosch GSB 16 RE 127V |
| E28 | C12 | M02 | Serra Circular Bosch GKS 150 127V |
| E29 | C13 | M03 | Parafusadeira/Furadeira DeWalt DCD7781 18V |
| E30 | C13 | M04 | Furadeira de Impacto Stanley SDH600 220V |
| E31 | C14 | M05 | Furadeira de Impacto Black+Decker TM500 220V |
| E32 | C15 | M02 | Martelete Bosch GBH 2-20 D 127V |
| E33 | C15 | M02 | Esmerilhadeira Angular Bosch GWS 850 220V |
| E34 | C16 | M04 | Esmerilhadeira Angular Stanley STGS9115 220V |
| E35 | C16 | M03 | Esmerilhadeira Angular DeWalt DWE4120 127V |
| E36 | C16 | M06 | Furadeira de Impacto Vonder FIV 1050 220V |
| E37 | C17 | M05 | Serra Tico-Tico Black+Decker KS501 220V |
| E38 | C18 | M01 | Furadeira de Impacto Makita HP1630 127V |
| E39 | C18 | M08 | Serra Circular Skil 5402 220V |
| E40 | C19 | M03 | Furadeira de Impacto DeWalt DWD502 127V |
| E41 | C19 | M01 | Esmerilhadeira Angular Makita GA4530 127V |
| E42 | C20 | M06 | Furadeira de Impacto Vonder FIV 710N 220V |
| E43 | C20 | M01 | Martelete Makita HR2470 127V |
| E44 | C21 | M04 | Furadeira Stanley STDR5510 127V |

---

## 7. Templates de notificação (WhatsApp)

> ⚠️ **O campo `conteudo` dos 3 templates precisa ser alterado.** Hoje ele contém apenas textos de teste, que chegam no WhatsApp assim:
> - `Isto é um teste para OS CONCLUÍDA! ✅`
> - `Isso é um teste para OS ENTREGUE! ✅`
> - `Isso é um teste de OS CANCELADA! ❌`
>
> Substitua por mensagens reais, **condizentes com esses textos** (mesmo status e mesmos emojis), usando os placeholders da API.

Endpoint: `PUT /notificacoes/templates/{statusOs}` — corpo com o texto e o liga/desliga (confirme os nomes dos campos, provavelmente `conteudo` e `ativo`, no request de referência da pasta *11. Notificações*).

Placeholders aceitos: `{cliente}`, `{os}`, `{valor}`, `{status}`, `{dataEntrada}`, `{dataConcluida}`, `{dataEntregue}`. Antes de gravar, rode `GET /notificacoes/placeholders` e veja o **exemplo** de `{valor}`: se ele já vier com `R$`, remova o `R$` dos textos abaixo para não duplicar.

| statusOs | conteudo |
|---|---|
| `CONCLUIDA` | `Olá, {cliente}! ✅ Sua Ordem de Serviço nº {os} foi CONCLUÍDA em {dataConcluida}. O equipamento já está pronto para retirada na M2 Equipamentos. Valor total: R$ {valor}. Qualquer dúvida, é só responder esta mensagem!` |
| `ENTREGUE` | `Olá, {cliente}! ✅ Confirmamos a ENTREGA da sua Ordem de Serviço nº {os} em {dataEntregue}. Obrigado pela confiança na M2 Equipamentos!` |
| `CANCELADA` | `Olá, {cliente}. ❌ Sua Ordem de Serviço nº {os}, aberta em {dataEntrada}, foi CANCELADA. Se você não solicitou o cancelamento ou tiver alguma dúvida, entre em contato com a M2 Equipamentos.` |

**Sequência obrigatória:**
1. **Etapa 7:** grave os 3 textos com `ativo = false`.
2. **Etapa 10:** faça as transições das OS10–OS33 (clientes fictícios) → nenhuma mensagem sai.
3. **Etapa 11:** regrave os 3 templates com `ativo = true` (mesmo texto).
4. **Etapa 12:** faça as transições das OS01–OS09 → Bruno, Vicente e Vitinho recebem 4 mensagens cada (12 no total).

---

## 8. Ordens de Serviço (33) — com itens

> 📌 **Os itens (`itens_os`) entram no corpo do `POST /ordens-servico`**, conforme o endpoint de criação de OS documentado no MCP do Postman (pasta *7. OS*). Cada item = id do equipamento + id do serviço + observação opcional. Regras que a API valida:
> - o equipamento do item precisa ser **do mesmo cliente** da OS;
> - o serviço precisa estar **ativo**;
> - o par equipamento + serviço é **único** dentro da OS;
> - toda OS nasce `EM_ANDAMENTO` com **pelo menos um item**.

`valorTotal` é calculado automaticamente (soma dos serviços). Só as 3 OS marcadas com **valor manual** recebem valor fixo (na criação ou via `PATCH /ordens-servico/{id}/valor-total`).

### 8.1 Lista das OS

| OS | Cliente | Observação da OS | Itens (equipamento + serviço — obs. do item) | Status final | Valor manual |
|---|---|---|---|---|---|
| OS01 | C01 | Furadeira perdendo força e sem impacto em concreto. | E01+S04 — escovas no limite; E01+S18 — catracas gastas | ENTREGUE | — |
| OS02 | C01 | Ruído metálico alto no cabeçote ao ligar. | E02+S34 — dentes da coroa quebrados | CONCLUIDA | — |
| OS03 | C01 | Bateria 20V não segura carga. | E03+S28 — duas células em curto | CANCELADA | — |
| OS04 | C02 | Martelete só gira, não rompe nem impacta. | E04+S40 — O-rings ressecados; E04+S45 | ENTREGUE | — |
| OS05 | C02 | Faíscas excessivas e cheiro de queimado no motor. | E05+S47 — induzido com espiras em curto | CONCLUIDA | — |
| OS06 | C02 | Gatilho trava acionado. | E06+S07 | EM_ANDAMENTO | — |
| OS07 | C03 | Esmerilhadeira não liga. | E07+S31 — induzido queimado; E07+S04 | ENTREGUE | — |
| OS08 | C03 | Lâmina com folga lateral durante o corte. | E08+S52 | CANCELADA | — |
| OS09 | C03 | Mandril não prende a broca. | E09+S16 | CONCLUIDA | — |
| OS10 | C04 | Serra circular perdendo rotação sob carga. | E10+S47; E10+S50 — base fora de esquadro | ENTREGUE | — |
| OS11 | C04 | Tico-ticos com corte desalinhado e motor fraco. | E11+S53; E13+S48 | EM_ANDAMENTO | — |
| OS12 | C04 | Base da lixadeira soltando. | E12+S56 | ENTREGUE | — |
| OS13 | C05 | Três esmerilhadeiras da produção para revisão. | E14+S31; E15+S04; E16+S36 — guarda trincada | EM_ANDAMENTO | **230.00** (desconto: cliente recorrente) |
| OS14 | C05 | Barulho no cabeçote. | E15+S34 | CONCLUIDA | — |
| OS15 | C06 | Martelete não segura a broca SDS. | E17+S41; E17+S42 | EM_ANDAMENTO | — |
| OS16 | C06 | Martelete desarmando o disjuntor. | E18+S39 | ENTREGUE | — |
| OS17 | C06 | Revisão antes de obra nova. | E19+S02 | CANCELADA | — |
| OS18 | C07 | Furadeira parou após queda. | E20+S13 | ENTREGUE | — |
| OS19 | C08 | Bateria descarrega em poucos minutos. | E21+S28 | EM_ANDAMENTO | — |
| OS20 | C09 | Cabo derretido e gatilho falhando. | E22+S05; E23+S07 | CONCLUIDA | — |
| OS21 | C10 | Duas ferramentas com motor queimado. | E24+S13; E25+S33 | ENTREGUE | — |
| OS22 | C11 | Parafusadeira não liga, LED pisca. | E26+S23 | EM_ANDAMENTO | — |
| OS23 | C11 | Muito pó de gesso dentro do motor. | E27+S03 | ENTREGUE | — |
| OS24 | C12 | Disco não gira com o motor ligado. | E28+S49; E28+S51 — capa travando | CONCLUIDA | — |
| OS25 | C13 | Torque desregulado e furadeira fraca. | E29+S25; E30+S04 | EM_ANDAMENTO | — |
| OS26 | C14 | Orçamento para furadeira sem funcionar. | E31+S01 | CANCELADA | — |
| OS27 | C15 | Martelete sem impacto e trava do eixo quebrada. | E32+S40; E33+S35 | EM_ANDAMENTO | — |
| OS28 | C16 | Lote de manutenção da metalúrgica. | E34+S38; E35+S37; E36+S14 | ENTREGUE | **400.00** (preço fechado) |
| OS29 | C16 | Revisão preventiva trimestral. | E35+S02 | EM_ANDAMENTO | — |
| OS30 | C17 | Serra tico-tico não liga. | E37+S48 | CONCLUIDA | — |
| OS31 | C18 | Furadeira sem controle de velocidade e serra com ruído. | E38+S20; E39+S49 | EM_ANDAMENTO | — |
| OS32 | C19 | Mandril com folga e esmerilhadeira queimada. | E40+S17; E41+S31 | EM_ANDAMENTO | **220.00** (desconto à vista) |
| OS33 | C20 | Duas ferramentas da obra com defeito. | E42+S15; E43+S46 | CONCLUIDA | — |

Distribuição final: **11 EM_ANDAMENTO**, **8 CONCLUIDA**, **10 ENTREGUE**, **4 CANCELADA**.

### 8.2 Como chegar ao status final

| Status final | Chamadas, nesta ordem |
|---|---|
| EM_ANDAMENTO | nenhuma |
| CONCLUIDA | `PATCH /concluir` |
| ENTREGUE | `PATCH /concluir` → `PATCH /entregar` (entregar exige CONCLUIDA) |
| CANCELADA | `PATCH /cancelar` (direto de EM_ANDAMENTO) |

- Aplique o **valor manual antes** de qualquer transição: valor total só muda enquanto a OS está `EM_ANDAMENTO` (senão 422).
- `ENTREGUE` e `CANCELADA` são estados finais.
- Nunca use `DELETE` em OS.

### 8.3 Limitação de datas

`dataEntrada`, `dataConcluida` e `dataEntregue` são gravadas pelo servidor na data do dia. Todas as OS ficarão com a data da execução. Se o usuário quiser um histórico espalhado no tempo, isso **não é possível pela API** → não altere via SQL por conta própria; **pergunte ao usuário**.

---

## 9. Conferência final (etapa 14)

| Verificação | Esperado |
|---|---|
| `GET /usuarios` (MASTER) | MASTER + 2 ADMIN (Bruno e Vitor), todos ativos |
| `GET /marcas` | 8 |
| `GET /servicos` | 62 (60 ativos, S54 e S59 inativos) |
| `GET /clientes` | 21 (20 ativos, C21 inativa) |
| `GET /equipamentos` | 44 |
| `GET /ordens-servico` | 33, com a distribuição de status da seção 8.1 |
| `GET /ordens-servico/{id}/itens` (amostra) | itens batendo com a tabela |
| `GET /notificacoes` | 12 envios, todos para os telefones de C01, C02 e C03 |
| `GET /notificacoes/templates` | 3 templates com os textos novos e `ativo = true` |
| `GET /painel` | indicadores coerentes com os totais acima |

Ao terminar, devolva ao usuário: o mapa `ref → id` de cada entidade, a lista de chamadas que falharam (com o ProblemDetail retornado) e o status de cada notificação (`ENVIADO`/`FALHOU`).

---

## 10. Referência rápida de erros

| Status | O que fazer |
|---|---|
| 400 | Leia o mapa `erros` do ProblemDetail, corrija o campo e reenvie. |
| 401 | Token expirou (120 min): refaça o login MASTER e continue de onde parou. |
| 404 | Id errado no mapa `ref → id`: consulte com GET e corrija. |
| 409 | Registro já existe: busque o id existente e siga, sem duplicar. |
| 422 | Regra de negócio (cliente/serviço inativo, transição inválida, OS fora de EM_ANDAMENTO): reveja a ordem das etapas. |