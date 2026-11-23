# CMIAP — API de agentes de inteligência artificial

O **CMIAP** é o backend da plataforma Incorp AI: uma API REST que reúne agentes de IA para apoiar atividades de Marketing e Comercial. O sistema centraliza autenticação, histórico e dados dos usuários, enquanto mantém cada funcionalidade organizada em seu próprio módulo.

Esta documentação apresenta o que já está disponível, orienta novos usuários nos primeiros passos e explica como executar e integrar a API. A interface web é mantida separadamente em [C-26hub/Incorp_front](https://github.com/C-26hub/Incorp_front).

## Funcionalidades disponíveis

| Funcionalidade | O que permite fazer |
| --- | --- |
| **Produção de conteúdo** | Gerar rascunhos a partir de objetivo, público, canal, formato e informações fornecidas. As conversas e mensagens ficam salvas por usuário e podem ser continuadas. |
| **Prospecção e qualificação de leads** | Cadastrar leads, importar lotes de até 100 registros via API e avaliar sua aderência ao perfil ideal de cliente (ICP) informado. |
| **Auditoria de SEO** | Analisar sinais on-page de uma página publicada e obter recomendações editoriais, técnicas e de palavra-chave. |
| **Mídias Sociais** | Conferir uma publicação orgânica finalizada e simular o agendamento com confirmação humana. |
| **Relatórios e KPIs** | Consolidar indicadores informados por canal, comparar com metas e sinalizar lacunas ou divergências. |
| **Tráfego** | Estruturar um rascunho de campanha paga, calcular sua performance informada e apontar desvios das metas. |
| **Editais e Licitações** | Analisar editais de compras públicas, prazos, riscos contratuais, checklist de habilitação e recomendações para propostas. |
| **Apoio Comercial** | Apoiar estratégias de vendas, diagnóstico de objeções, diferenciais frente a concorrentes e direcionamento de fechamento. |
| **Contas e autenticação** | Criar conta, iniciar e encerrar sessão com cookies JWT HttpOnly, renovação de sessão e proteção CSRF. |

O sistema não publica conteúdo, não consulta bases externas para enriquecer leads e não entra em contato com eles. As recomendações de IA são auxiliares: revise textos, dados e sugestões antes de usá-los. A auditoria de SEO não garante posicionamento nos buscadores.

### Uso responsável dos dados

As solicitações de produção de conteúdo, qualificação de leads e auditoria SEO são processadas pelo Gemini. Não envie informações pessoais ou confidenciais sem autorização e sem verificar as políticas aplicáveis à sua organização. Na qualificação de leads, o backend envia ao modelo o ICP e dados como empresa, cargo, setor, porte, localização e observações; nome e dados de contato não são incluídos no contexto da IA. Na auditoria SEO, o HTML extraído da URL e o texto complementar são enviados ao modelo.

## Guia rápido para novos usuários

1. Crie uma conta com nome, sobrenome, CPF, e-mail e senha. O cadastro público cria uma conta `DEFAULT` que aguarda um departamento.
2. Um administrador atribui Marketing ou Comercial na tela Usuários. Depois da atribuição, entre com seu e-mail, senha e o perfil correspondente. Para uma conta com papel `ADMINISTRATOR`, escolha Administrador.
3. Escolha um dos agentes disponíveis no catálogo:
   - **Produção de Conteúdo:** descreva o objetivo, o público, o canal, o formato e informações verificadas. Para continuar uma conversa, use o histórico ou abra uma conversa existente.
   - **Prospecção e Qualificação de Leads:** informe o ICP, cadastre leads ou importe um CSV pela interface. Depois, solicite a qualificação de cada lead. Confira a análise antes de decidir próximos passos.
   - **SEO:** informe uma URL pública HTTPS e, se desejar, uma palavra-chave e contexto adicional. Veja os sinais identificados e as recomendações geradas.
   - **Mídias Sociais:** forneça a peça final, as regras técnicas de referência e os horários do calendário da plataforma para revisar o plano de publicação.
   - **Relatórios e KPIs:** informe os dados por produto e período, com a unidade, a origem e a meta de cada indicador.
   - **Tráfego:** envie o briefing, os criativos finalizados, o orçamento e as metas. Inclua os dados de desempenho para acompanhar os indicadores.
   - **Editais e Licitações:** informe o identificador do edital, órgão, modalidade, prazos, orçamento e documentos de habilitação para receber checklist e parecer de conformidade e riscos.
   - **Apoio Comercial:** informe a oportunidade, cliente, dores mapeadas, concorrentes e objeções para gerar estratégia consultiva de pitch e contorno.
4. Revise qualquer resultado gerado por IA antes de compartilhá-lo, publicá-lo ou usá-lo para tomar decisões.

O acesso ao gerenciamento de usuários exige o papel `ADMINISTRATOR`. A equipe responsável provisiona a primeira conta em uma execução não-web com `ADMIN_GEN_VALUE=true` e as variáveis `ADMIN_NAME`, `ADMIN_SURNAME`, `ADMIN_CPF`, `ADMIN_EMAIL` e `ADMIN_PASSWORD`. Depois, deixe `ADMIN_GEN_VALUE=false`. O bootstrap cria o administrador com departamento `MARKETING`. Um administrador antigo sem departamento mantém sua senha e recebe `MARKETING` após o primeiro login válido pelo perfil Administrador. A atualização do PostgreSQL permite contas pendentes e preserva os departamentos já preenchidos.

## Tecnologias

- Java 25 e Spring Boot 4.
- Spring MVC, Spring Security, validação Jakarta e Spring Data JPA.
- PostgreSQL para persistência.
- LangChain4j e Google Gemini para os agentes de IA.
- Jsoup e Apache HttpClient 5 para coleta e análise do HTML no agente SEO.
- Maven Wrapper para build e testes.

O backend segue uma arquitetura de **monólito modular**, organizada por capacidade de negócio. Consulte [ARCHITECTURE.md](ARCHITECTURE.md) para os limites dos módulos e as regras de dependência.

## Executar localmente

### Pré-requisitos

- JDK 25.
- PostgreSQL acessível e um banco criado para a aplicação.
- Chave de API válida para o Google Gemini.
- `curl` e `jq` para reproduzir os exemplos de integração abaixo.

### Configurar o ambiente

O backend lê suas configurações do ambiente do processo. Configure os valores para sua instalação antes de iniciar; não salve chaves, senhas ou segredos no README, no código ou em arquivos versionados.

Variáveis obrigatórias:

| Variável | Finalidade |
| --- | --- |
| `PG_URL` | URL JDBC do PostgreSQL, por exemplo `jdbc:postgresql://<host>:<porta>/<banco>`. |
| `PG_USERNAME` | Usuário do banco. |
| `PG_PASSWORD` | Senha do banco. |
| `JWT_SECRET` | Segredo Base64 com pelo menos 32 bytes aleatórios. |
| `GEMINI_API_KEY` | Chave usada pelo backend para chamar o Gemini. |

Variáveis opcionais:

| Variável | Padrão / finalidade |
| --- | --- |
| `COOKIE_SECURE` | `true` por padrão. Mantenha `true` quando houver HTTPS; use `false` somente para desenvolvimento local via HTTP. |
| `CORS_ALLOWED_ORIGINS` | Origens exatas permitidas, separadas por vírgula. Inclui `http://localhost:5173` e `https://incorp-front.onrender.com` por padrão. |
| `COOKIE_SAME_SITE` | Atributo `SameSite` dos cookies. Por padrão usa `None` com `COOKIE_SECURE=true` e `Lax` com `COOKIE_SECURE=false`; pode ser definido explicitamente. |

Exemplo de configuração no terminal Linux/macOS (substitua os valores pelos da sua máquina e não os compartilhe):

```bash
export PG_URL='jdbc:postgresql://<host>:<porta>/<banco>'
export PG_USERNAME='<usuario>'
export PG_PASSWORD='<senha>'
export JWT_SECRET="$(openssl rand -base64 32)"
export GEMINI_API_KEY='<chave-do-gemini>'
export COOKIE_SECURE=false
```

`COOKIE_SECURE=false` é apenas para uso local sobre HTTP. Em ambientes publicados, use HTTPS e cookies `Secure`. Guarde credenciais e chaves em um gerenciador de segredos ou variáveis protegidas do ambiente de execução.

### Publicar no Render com Docker

Crie um serviço Web ligado a este repositório, selecione o runtime **Docker** e mantenha o diretório raiz padrão. O Render usará o `Dockerfile` da raiz; a imagem lê a porta fornecida pelo Render (`PORT`) e inicia a API nessa porta.

Adicione no ambiente do serviço as variáveis obrigatórias listadas acima (`PG_URL`, `PG_USERNAME`, `PG_PASSWORD`, `JWT_SECRET` e `GEMINI_API_KEY`). Para o PostgreSQL do Render, configure `PG_URL` no formato JDBC `jdbc:postgresql://<host-interno>:<porta>/<banco>` e informe usuário e senha nas variáveis correspondentes. Mantenha `COOKIE_SECURE=true` e configure `CORS_ALLOWED_ORIGINS=https://incorp-front.onrender.com`. Com HTTPS, o backend usa `SameSite=None` por padrão; `COOKIE_SAME_SITE=None` pode ser definido explicitamente. Se o domínio do frontend mudar, substitua a origem pela URL pública exata, incluindo `https://`.

### Iniciar e testar

Na raiz deste projeto:

```bash
./mvnw spring-boot:run
```

A aplicação Spring Boot usa a porta `8080` por padrão. Para compilar e executar os testes:

```bash
./mvnw test
```

Para executar apenas a suíte isolada de serviços, regras de negócio e controllers dos agentes (sem necessidade de banco de dados ou chave de API externa ativa):

```bash
./mvnw test -Dtest="*ServiceTest,*StoreTest,*FetcherTest,*ControllerTest"
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

O Hibernate está configurado para atualizar o esquema (`ddl-auto=update`). Para o suporte a zeros iniciais no CPF, a API usa texto com 11 dígitos. O Hibernate mantém a coluna numérica existente e recompõe os 11 dígitos na leitura. Ao publicar, atualize primeiro o backend e depois o frontend.

## Autenticação e proteção CSRF

A API usa JWT em cookies `HttpOnly`, não tokens Bearer guardados pelo frontend. As rotas autenticadas leem o cookie `ACCESS_TOKEN`; o cookie `REFRESH_TOKEN` é usado para renovar a sessão. Requisições que alteram dados também exigem o token CSRF:

1. Faça `GET /csrf`, preservando os cookies da resposta. O JSON contém `headerName` e `token`.
2. Envie o token no cabeçalho indicado em todas as requisições `POST`, `PUT`, `PATCH` e `DELETE`, mantendo os cookies.
3. Faça login em `POST /auth/login` com o departamento atribuído pelo administrador ou com o perfil `ADMINISTRATOR` para uma conta administrativa. O servidor retorna `204 No Content` e define os cookies de sessão.

Exemplo de fluxo com `curl` e `jq` (use dados de teste):

```bash
API_URL='http://localhost:8080'
COOKIE_JAR="$(mktemp)"
trap 'rm -f "$COOKIE_JAR"' EXIT
CSRF_TOKEN="$(curl -fsS -c "$COOKIE_JAR" "$API_URL/csrf" | jq -r '.token')"

curl -sS -o /dev/null -w 'Cadastro: HTTP %{http_code}\n' -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H 'Content-Type: application/json' \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  -d '{"name":"Ana","surname":"Souza","cpf":"01234567890","email":"ana@example.com","password":"<senha-de-teste>"}' \
  "$API_URL/users/register"

# Continue após um administrador atribuir Marketing à conta da Ana.
curl -sS -o /dev/null -w 'Login: HTTP %{http_code}\n' -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H 'Content-Type: application/json' \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  -d '{"email":"ana@example.com","password":"<senha-de-teste>","department":"MARKETING"}' \
  "$API_URL/auth/login"

curl -fsS -b "$COOKIE_JAR" "$API_URL/auth/me"
```

Use uma senha descartável de desenvolvimento nos exemplos. O cabeçalho CSRF padrão usado pelo backend é `X-CSRF-TOKEN`. A interface web já realiza esse fluxo automaticamente. Em produção, configure `CORS_ALLOWED_ORIGINS` com a origem exata do frontend e mantenha HTTPS.

## Endpoints da API

Todos os caminhos abaixo são relativos à origem da API, normalmente `http://localhost:8080`. Exceto os endpoints públicos de autenticação/cadastro, é necessária uma sessão válida. Operações que alteram dados exigem CSRF.

### Contas e sessão

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/csrf` | Público | Obtém o nome do cabeçalho e o token CSRF. |
| `POST` | `/users/register` | Público | Cria uma conta `DEFAULT` que aguarda atribuição de departamento. Envie o CPF como texto com 11 dígitos, incluindo zeros iniciais. |
| `POST` | `/auth/login` | Público | Confere e-mail e senha. O campo `department` aceita `MARKETING`, `COMMERCIAL` ou `ADMINISTRATOR`. O último exige o papel administrativo existente. |
| `PATCH` | `/users/{id}` | Administrador | Atribui o departamento enviando `{"department":"MARKETING"}` ou `{"department":"COMMERCIAL"}`. |
| `POST` | `/auth/refresh` | Cookie de refresh | Renova a sessão e gira os tokens. |
| `POST` | `/auth/logout` | Cookie de refresh | Revoga o refresh token e limpa os cookies. |
| `GET` | `/auth/me` | Autenticado | Retorna os dados da conta atual, incluindo departamento. |
| `GET`, `PUT`, `PATCH`, `DELETE` | `/users/**` | `ADMINISTRATOR` | Gerencia contas de usuários. |

### Agentes

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/agents/content/produce` | Cria um rascunho e uma conversa, ou continua uma conversa do usuário autenticado. |
| `GET` | `/agents/content/conversations` | Lista as conversas do usuário autenticado. |
| `GET` | `/agents/content/conversations/{conversationId}/messages` | Lista mensagens em ordem cronológica, apenas da conversa do usuário autenticado. |
| `GET` | `/agents/leads` | Lista os leads do usuário autenticado. |
| `POST` | `/agents/leads` | Cadastra um lead. |
| `POST` | `/agents/leads/import` | Importa de 1 a 100 leads em JSON. |
| `POST` | `/agents/leads/{leadId}/qualify` | Compara um lead do usuário com o ICP informado. |
| `POST` | `/agents/seo/analyze` | Busca uma página HTTPS pública e retorna a auditoria SEO. |
| `POST` | `/agents/social/publication-plan` | Valida um plano de publicação orgânica e permite simular seu agendamento. |
| `POST` | `/agents/reports/kpis` | Consolida observações de indicadores e compara os resultados com as metas informadas. |
| `POST` | `/agents/traffic/campaign-plan` | Simula um rascunho de campanha e calcula indicadores a partir de observações opcionais. |

## Exemplos de uso dos agentes

Os exemplos abaixo mostram corpos JSON. Use a autenticação e o cabeçalho CSRF descritos acima.

### Produção de conteúdo

Campos obrigatórios: `objective`, `targetAudience`, `channel`, `format` e `keyInformation`. Para continuar uma conversa, envie o `conversationId` retornado na resposta anterior. Sem ele, o backend inicia uma conversa nova.

```json
{
  "objective": "Apresentar um serviço e incentivar contato",
  "targetAudience": "Pequenas empresas que querem melhorar sua presença digital",
  "channel": "LinkedIn",
  "format": "Publicação curta",
  "keyInformation": "Inclua apenas informações confirmadas sobre o serviço.",
  "tone": "Profissional e acessível",
  "callToAction": "Entre em contato para conversar",
  "language": "pt-BR"
}
```

A resposta contém `conversationId`, `draft` e `humanReviewRequired`. O último campo indica que o conteúdo deve ser revisado por uma pessoa antes do uso.

### Cadastro e qualificação de leads

Cadastro:

```json
{
  "name": "Marina Costa",
  "company": "Acme Tecnologia",
  "jobTitle": "Diretora de operações",
  "email": "marina@example.com",
  "industry": "Tecnologia",
  "companySize": "20 a 200 pessoas",
  "location": "São Paulo",
  "notes": "Informação fornecida pela equipe comercial."
}
```

Importação em lote usa `POST /agents/leads/import`:

```json
{
  "leads": [
    {
      "name": "Marina Costa",
      "company": "Acme Tecnologia",
      "industry": "Tecnologia"
    }
  ]
}
```

Qualificação usa `POST /agents/leads/{leadId}/qualify`:

```json
{
  "idealCustomerProfile": "Empresas B2B de tecnologia com 20 a 200 funcionários."
}
```

A qualificação retorna uma pontuação de 0 a 100, a classificação (`QUALIFIED`, `REVIEW` ou `NOT_QUALIFIED`), justificativa e próximo passo sugerido. Esses dados são recomendações geradas por IA, não decisões automáticas.

### Auditoria SEO

```json
{
  "url": "https://www.exemplo.com/servicos",
  "targetKeyword": "consultoria de marketing",
  "supplementalText": "Página voltada a pequenas empresas; o objetivo é gerar pedidos de contato."
}
```

O endereço precisa ser HTTPS e apontar para um host público; endereços privados/locais e portas personalizadas são recusados, redirecionamentos não são seguidos e o HTML tem limite de tamanho. A resposta combina sinais observados (título, meta description, headings, imagens e links) com recomendações e uma pontuação orientativa. O servidor envia os dados extraídos e o contexto opcional ao Gemini.

### Mídias Sociais

```json
{
  "platform": "LINKEDIN",
  "format": "POST",
  "contentReference": "post-piloto",
  "caption": "Conheça os resultados do piloto.",
  "technicalRules": {
    "reference": "Regras fictícias para o piloto interno",
    "maxCaptionCharacters": 300,
    "assetRequired": false,
    "allowedMimeTypes": ["image/png"]
  },
  "contentBrandVersion": "v1",
  "currentBrandVersion": "v1",
  "plannedAt": "2026-11-27T10:00:00-03:00",
  "approvedSlots": ["2026-11-27T10:00:00-03:00"],
  "occupiedSlots": [],
  "minimumSpacingMinutes": 15,
  "interactions": [
    {
      "type": "COMMENT",
      "reference": "comentario-1",
      "url": "https://example.org/comment/1",
      "pendingReply": true
    }
  ],
  "confirmed": false
}
```

- O plano aceita `POST`, `STORY` e `REEL` para `FACEBOOK` e `INSTAGRAM`, e `POST` para `LINKEDIN`.
- Envie os horários aprovados e ocupados para a plataforma selecionada, com fuso explícito, e informe um horário futuro. A comparação usa o instante e aplica o espaçamento informado em minutos.
- Havendo mídia, inclua `asset` com `mimeType`, `width`, `height` e `sizeBytes`. É possível incluir `requiredWidth`, `requiredHeight` e `maxAssetBytes` em `technicalRules`. A validação usa metadados declarados, sem baixar arquivos nem consultar regras atuais das redes.
- A resposta inclui plataforma, formato, horário, referência das regras e lista de pendências (`issues`). O status é `REVIEW_REQUIRED` com pendências, `READY_FOR_CONFIRMATION` quando a revisão passou e `SIMULATED` quando passou com `confirmed: true`.
- Em `SIMULATED`, `simulation` traz uma referência estável que pode ser registrada no relatório de teste, a referência da peça e a versão da marca. Essa referência identifica a simulação.
- A lista opcional `interactions` aceita `COMMENT` e `DIRECT_MESSAGE`. O backend devolve em `pendingInteractions` os itens marcados com `pendingReply: true`, com sua referência e link HTTPS opcional para triagem humana.
- O endpoint executa apenas a simulação a partir da requisição. A agenda, as regras e a versão da marca são fornecidas pelo chamador. O backend não armazena nem publica o conteúdo ou reserva horários.

### Relatórios e KPIs

```json
{
  "product": "Produto piloto",
  "startDate": "2026-11-01",
  "endDate": "2026-11-18",
  "metrics": [
    {
      "channel": "PAID",
      "name": "CPA",
      "unit": "BRL",
      "goal": {"value": 12, "direction": "AT_MOST"},
      "observations": [{"source": "TRAFFIC_AGENT", "reference": "pacote-trafego-1", "value": 10.20}]
    },
    {
      "channel": "ORGANIC",
      "name": "Impressões",
      "unit": "impressões",
      "observations": [],
      "contextReferences": ["social-sim-identificador-do-piloto"]
    }
  ],
  "includeNarrative": false
}
```

- Envie cada indicador uma vez por canal. Todas as observações devem referir-se ao produto, período e unidade do indicador e representar, cada uma, o valor consolidado do mesmo recorte. O endpoint não soma observações sobrepostas.
- Use `META`, `GOOGLE` ou `LINKEDIN` para `ORGANIC`, `TRAFFIC_AGENT` para `PAID`, `LEADS_AGENT` para `LEADS`, `BIDS_AGENT` para `BIDS` e `SEO_AGENT` para `SEO`. `contextReferences` aceita referências de apoio, como um log de publicação, sem tratá-las como observações de valor.
- A resposta preserva as observações e suas referências. Indicadores sem observação recebem `MISSING`. Valores divergentes recebem `CONFLICT`. Em ambos, o campo `value` é nulo. Indicadores com valor e sem meta recebem `NO_GOAL`.
- Quando existe uma meta, a API usa comparação decimal sem arredondamento adicional. `AT_MOST` indica um limite máximo, e `AT_LEAST` indica um mínimo. Os estados são `GOAL_MET` e `GOAL_NOT_MET`.
- Com `includeNarrative: true`, o modelo recebe os indicadores consolidados e suas referências para produzir uma leitura opcional, devolvida em `narrative`. Use dados fictícios no piloto. Os estados e números oficiais da resposta são calculados pelo backend. A API usa somente os dados da requisição, sem coleta externa ou armazenamento.
- Se o modelo estiver indisponível, repita a mesma requisição com `includeNarrative: false` para receber os indicadores e a comparação com metas.

### Tráfego

```json
{
  "campaignReference": "campanha-piloto",
  "product": "Produto piloto",
  "platform": "META_ADS",
  "objective": "Gerar pedidos de demonstração",
  "audience": "Equipes de tecnologia no Recife",
  "startDate": "2026-11-01",
  "endDate": "2026-11-18",
  "budget": {"currency": "BRL", "planned": 50, "ceiling": 60},
  "goals": {"maxCpa": 10, "minRoas": 3},
  "creatives": [{"reference": "peca-1", "validationReference": "revisao-1", "compatible": true}],
  "observation": {
    "reference": "campanha-piloto-exportacao-01",
    "startDate": "2026-11-01",
    "endDate": "2026-11-18",
    "impressions": 2000,
    "reach": 900,
    "clicks": 100,
    "conversions": 6,
    "spend": 60,
    "attributedRevenue": 150
  }
}
```

- Plataformas aceitas: `META_ADS`, `GOOGLE_ADS`, `LINKEDIN_ADS`. A moeda deve usar o código ISO de três letras. O briefing precisa de pelo menos uma meta de CPA ou ROAS, além dos criativos revisados previamente para a plataforma.
- O orçamento acima do teto, metas ausentes ou criativos incompatíveis produzem `REVIEW_REQUIRED` com os motivos em `issues`. Com `SIMULATED_DRAFT`, o modelo retorna em `strategy` uma proposta de segmentação, lances, distribuição de orçamento e teste A/B baseada no briefing. A API gera apenas recomendações. Ela não acessa plataformas de anúncios, mexe no orçamento, registra leads, publica, ativa ou pausa campanhas.
- `observation` é opcional. Se presente, informe uma referência que identifique a campanha e a amostra e um período contido no período da campanha. Os contadores aceitam até 16 dígitos. A API devolve em `performance` os indicadores informados e os calculados. CPA é gasto dividido por conversões, ROAS é receita atribuída dividida por gasto, CPC é gasto dividido por cliques e CTR é cliques divididos por impressões, multiplicado por 100. Operando ausente ou denominador zero deixa o valor nulo com estado `MISSING`.
- Valores derivados são arredondados para seis casas decimais, com empate para cima, antes da comparação com a meta. Quando o gasto atinge o teto informado, a resposta inclui um alerta. A amostra é fornecida pelo chamador e o endpoint não infere tendências de desempenho.

#### Levar o desempenho de Tráfego a Relatórios e KPIs

Para usar o CPA do exemplo acima, envie para `POST /agents/reports/kpis` o produto da resposta de Tráfego, as datas de `performance` e o indicador no canal `PAID`. Copie `name`, `unit` e `goal` do indicador e componha a observação usando `performance.source`, `performance.reference` e o `value` do indicador:

```json
{
  "product": "Produto piloto",
  "startDate": "2026-11-01",
  "endDate": "2026-11-18",
  "metrics": [
    {
      "channel": "PAID",
      "name": "CPA",
      "unit": "BRL",
      "goal": {"value": 10, "direction": "AT_MOST"},
      "observations": [{"source": "TRAFFIC_AGENT", "reference": "campanha-piloto-exportacao-01", "value": 10.000000}]
    }
  ],
  "includeNarrative": false
}
```

O indicador recebe `GOAL_MET`. Se o indicador vier sem valor no pacote de Tráfego, envie `observations: []` para preservar a lacuna.

### Editais e Licitações

```json
{
  "bidReference": "PE-42/2026",
  "issuingBody": "Ministério da Gestão e Inovação",
  "modality": "PREGAO_ELETRONICO",
  "objectDescription": "Contratação de plataforma em nuvem para inteligência e automação de processos.",
  "submissionDeadline": "2026-12-05",
  "openingDate": "2026-12-05",
  "budget": {
    "currency": "BRL",
    "estimatedValue": 350000.00,
    "maxAcceptableValue": 400000.00
  },
  "technicalRequirements": [
    "Atestado de capacidade técnica para sustentação de microsserviços em nuvem",
    "Certificação ISO 27001 para o ambiente de hospedagem"
  ],
  "qualificationDocuments": [
    {
      "documentName": "Contrato Social consolidado",
      "type": "HABILITACAO_JURIDICA",
      "provided": true
    },
    {
      "documentName": "CND Federal e Previdenciária",
      "type": "REGULARIDADE_FISCAL_TRABALHISTA",
      "provided": true
    },
    {
      "documentName": "Balanço Patrimonial do último exercício",
      "type": "QUALIFICACAO_ECONOMICO_FINANCEIRA",
      "provided": false,
      "remarks": "Pendente de assinatura contábil"
    }
  ],
  "noticeSummary": "Pregão com critério de julgamento menor preço por item. Exigência de amostra em até 5 dias úteis."
}
```

- Endpoint: `POST /agents/bids/analyze`.
- Modalidades aceitas: `PREGAO_ELETRONICO`, `CONCORRENCIA`, `DISPENSA`, `INEXIGIBILIDADE`, `DIALOGO_COMPETITIVO`, `CONCURSO`, `OUTROS`.
- Tipos de documento: `HABILITACAO_JURIDICA`, `REGULARIDADE_FISCAL_TRABALHISTA`, `QUALIFICACAO_TECNICA`, `QUALIFICACAO_ECONOMICO_FINANCEIRA`.
- O prazo de submissão não pode ser posterior à data de abertura. Se o prazo estiver expirado ou o valor estimado superar o teto, o edital recebe `HIGH_RISK`.
- Com `VIABLE` ou `REVIEW_REQUIRED`, a API retorna um checklist estruturado (`COMPLIANT`, `PENDING`, `MISSING`) e parecer do modelo com pontos de atenção, cláusulas de risco e recomendações para a proposta.

### Apoio Comercial

```json
{
  "opportunityReference": "OPP-2026-089",
  "clientName": "Grupo Varejo Brasil",
  "clientSegment": "Varejo Omnichannel",
  "decisionMakers": "Diretora de E-commerce e Gerente de TI",
  "productOffered": "Plataforma de Personalização de Vitrines",
  "stage": "NEGOTIATION",
  "dealValue": {
    "currency": "BRL",
    "amount": 85000.00
  },
  "painPoints": [
    "Taxa de conversão estagnada no mobile",
    "Dificuldade de integrar catálogo com o ERP legado"
  ],
  "competitors": [
    "Concorrente Alfa",
    "Solução interna legada"
  ],
  "objections": [
    {
      "category": "Preço",
      "description": "Cliente alega que o setup está 20% acima do orçamento previsto para este ano.",
      "currentAttempt": "Oferecemos parcelamento do setup em 3 vezes."
    },
    {
      "category": "Prazo de Implantação",
      "description": "Receio de impactar as vendas da Black Friday durante a virada.",
      "currentAttempt": "Apresentamos proposta de implementação faseada."
    }
  ],
  "notes": "Cliente demonstrou alto interesse no piloto mas necessita de validação do CFO."
}
```

- Endpoint: `POST /agents/commercial/advise`.
- Estágios aceitos: `PROSPECTING`, `DISCOVERY`, `PROPOSAL`, `NEGOTIATION`, `CLOSING`.
- Objeções em aberto durante a fase `CLOSING` produzem status `REVIEW_REQUIRED`.
- Retorna direcionamento de valor (`commercialStrategy`) com narrativa de pitch, diferenciais frente a concorrentes, perguntas de avanço e sugestão de tratamento para cada categoria de objeção.

## Privacidade e isolamento

- Conversas e leads são vinculados à conta autenticada; a API não permite acessar dados de outra conta.
- A sessão usa cookies `HttpOnly`, `SameSite` e `Secure` configurável; o token CSRF protege operações de escrita.
- A chave do Gemini permanece no ambiente do backend e não deve ser enviada pelo frontend.
- Todo conteúdo retornado por IA deve ser revisado e validado por uma pessoa.

## Estrutura do projeto

```text
src/
├── main/
│   ├── java/ai/incorp/cmiap/
│   │   ├── agent/content/    # Geração de conteúdo e histórico
│   │   ├── agent/leads/      # Cadastro e qualificação de leads
│   │   ├── agent/seo/        # Auditoria on-page de SEO
│   │   ├── agent/social/     # Validação e simulação de publicações orgânicas
│   │   ├── agent/reports/    # Consolidação de indicadores e leitura de resultados
│   │   ├── agent/traffic/    # Planejamento e desempenho de campanhas pagas
│   │   ├── agent/bids/       # Análise de editais e licitações públicas
│   │   ├── agent/commercial/ # Estratégia e apoio comercial a vendas
│   │   ├── security/         # Cookies, JWT, CSRF e CORS
│   │   └── user/             # Contas e administração de usuários
│   └── resources/
└── test/
```

## Documentação relacionada

- [ARCHITECTURE.md](ARCHITECTURE.md) — arquitetura modular e responsabilidades dos pacotes.
- [HELP.md](HELP.md) — referências do Spring Boot, configuração local e provisionamento administrativo.
- [Frontend Incorp AI](https://github.com/C-26hub/Incorp_front) — interface web da plataforma.

## Equipe

Davi Felipe Silva Clemente - Dev Backend
Gabriel Eduardo de Moura França - Dev Backend
Pedro Henrique Miranda Valença - Dev Backend
Marcelo barbosa - Dev Frontend
Lucas Mendes da Silva - Dev Fronted
Carolline Barbosa Ferreira -Dev Frontend
Maria Cecília de Lima e Silva- Dev Frontend
Marcos Vinicius Souza dos Santos - Dev FrontEnd
Caio Sabino Coelho - Dev Backend
