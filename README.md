# CMIAP - Sistema de Agentes Inteligentes

Este é um projeto visa o desenvolvimento de uma plataforma de Agentes Inteligentes para melhorias
nos processos de trabalho da empresa **Incorp Tech**.

## Funcionalidades implementadas

- Cadastro de usuário;
- Login de usuários;
- Agente de Produção de Conteúdo, com geração orientada por tema, formato, produto, canal, objetivo, público e referências;
- Agente de leitura e interpretação de editais (em progresso).

## Tecnologias
| Tecnologia                                   | Uso                                                      |
|----------------------------------------------|----------------------------------------------------------|
| Java 25                                      | Linguagem e versão configurada no Maven                  |
| Spring Boot 4.1.1                            | Inicialização e integração da aplicação                  |
| Spring MVC                                   | API REST e controllers                                   |
| Spring Security                              | Autorização de rotas, filtro JWT e codificação de senhas |
| Spring Data JPA / Hibernate                  | Persistência relacional                                  |
| PostgreSQL                                   | Banco de dados                                           |
| JJWT 0.13.0                                  | Criação e validação de tokens JWT                        |
| LangChain4j 1.22.0-beta32                    | Integração com o modelo de chat Gemini                   |
| MapStruct 1.6.3                              | Conversão entre modelo de domínio e entidade JPA         |
| Lombok                                       | Redução de código repetitivo                             |
| JUnit 5, AssertJ, Mockito e Spring Boot Test | Testes automatizados                                     |
| Maven Wrapper                                | Build e execução do projeto                              |

## Arquitetura e padrões
O código está organizado em camadas com separação inspirada em Domain-Driven Design (DDD) e na 
abordagem Ports and Adapters:
- `domain`: modelo `User`, enumeração `UserRole`, value object `Email` e contrato `UserRepository`.
- `application/useCase`: casos de uso para registro e autenticação, mantendo as regras do fluxo fora 
dos controllers.
- `infrastructure`: adaptador do repositório para Spring Data JPA, entidade de persistência e mapper;
contém também configuração de segurança, JWT e integração de IA.
- `presentation`: controllers REST e DTOs de entrada/saída.

O contrato `UserRepository` funciona como porta do domínio;  
`UserRepositoryAdapter` conecta essa porta ao repositório Spring Data JPA.  
`UserMapper` usa MapStruct para converter entre `User` e `UserSetup`.  
Os agentes de conteúdo e editais são declarados como interfaces LangChain4j com instruções de sistema;
seus controllers ficam na camada `presentation`.

Essa estrutura aplica alguns conceitos de DDD, como entidade, value object, caso de uso e repositório.
Não é uma implementação completa de DDD: não há, por exemplo, agregados e eventos de domínio explícitos ou módulos separados por bounded context.

### Estrutura atual

```text
src/
├── main/
│   ├── java/ai/incorptech/cmiap/
│   │   ├── CmiapApplication.java
│   │   ├── application/useCase/
│   │   │   ├── AuthenticateUserService.java
│   │   │   └── RegisterUserService.java
│   │   ├── application/modules/contentGenerator/ContentGeneratorService.java
│   │   ├── domain/
│   │   │   ├── model/ (User, UserRole)
│   │   │   ├── repository/UserRepository.java
│   │   │   └── valueObject/Email.java
│   │   ├── infrastructure/
│   │   │   ├── ai/memory/ (provider namespaced por agente)
│   │   │   ├── ai/modules/publicNotices/PublicNoticeService.java
│   │   │   ├── persistence/ (adapter, entidades, store de memória e mapper)
│   │   │   └── security/ (configuração, filtro e serviço JWT)
│   │   └── presentation/
│   │       ├── controller/ (AuthController, ContentGenerationController, PublicNoticeController)
│   │       └── controller/dto/ (auth, content)
│   └── resources/application.properties
└── test/
        ├── java/ai/incorptech/cmiap/
        └── resources/application.properties
```

## API

As rotas de autenticação são públicas. As demais rotas exigem o cabeçalho `Authorization: Bearer <token>`.

### Registrar usuário

`POST /auth/register` retorna `201 Created` sem corpo. Nome, e-mail e senha são obrigatórios; a senha deve ter pelo menos seis caracteres.

```json
{
    "name": "Maria Silva",
    "email": "maria@example.com",
    "password": "senha-segura"
}
```

### Autenticar

`POST /auth/login` retorna `200 OK` e um token Bearer.

```json
{
    "email": "maria@example.com",
    "password": "senha-segura"
}
```

Exemplo de resposta:

```json
{
    "token": "<jwt>",
    "type": "Bearer"
}
```

### Gerar conteúdo

`POST /ai/content/generate` recebe um JSON e retorna `200 OK` com o identificador da conversa
(`conversationId`) e o conteúdo gerado. A rota exige autenticação Bearer. Na primeira chamada,
omita `conversationId`; a API cria um UUID. Para continuar a mesma conversa, envie esse UUID nas
chamadas seguintes. O ID pertence ao usuário autenticado e não permite acessar a memória de outro
usuário.

Campos `topic` e `format` são obrigatórios. Os demais são opcionais. Os limites são: tema 500
caracteres, formato/produto/canal 100, objetivo/público 300 e referências 4.000.

```json
{
    "conversationId": "251b24d9-2686-4e68-aa72-193f3d66fa62",
    "topic": "Como a gestão documental integrada pode apoiar conselhos profissionais",
    "format": "Carrossel para LinkedIn",
    "product": "Incorp Next",
    "channel": "LinkedIn",
    "objective": "Explicar os benefícios de organizar documentos e processos em um fluxo integrado",
    "audience": "Gestores de conselhos profissionais",
    "referenceFacts": "O sistema permite configurar fluxos de trabalho e centralizar documentos."
}
```

Exemplo de resposta:

```json
{
    "conversationId": "251b24d9-2686-4e68-aa72-193f3d66fa62",
    "content": "Texto produzido conforme o tema, formato e contexto informados."
}
```

O campo `content` é texto gerado pela IA. O formato visual do texto não é fixado como HTML ou
README; informe o formato editorial desejado em `format`. O agente não publica o conteúdo em redes
sociais e não deve inventar dados ou funcionalidades ausentes das referências fornecidas.

Para apagar a memória da conversa, mantendo a autenticação do mesmo usuário:

```http
DELETE /ai/content/conversations/251b24d9-2686-4e68-aa72-193f3d66fa62
Authorization: Bearer <jwt>
```

### Memória de conversa dos agentes

O agente de conteúdo usa `@MemoryId` e um `ChatMemoryProvider` específico, conectado pelo modo
explícito de `@AiService`. O provider prefixa toda chave com o identificador fixo do agente
(`content-generator`); a parte restante identifica usuário e conversa. Portanto, mesmo que duas
conversas usem o mesmo UUID, usuários diferentes não dividem memória, e agentes diferentes não
compartilham mensagens.

Exemplo de teste: na primeira chamada, envie o prompt sem `conversationId` e guarde o ID devolvido.
Depois, envie-o junto a um pedido de continuação:

```bash
curl -X POST http://localhost:8080/ai/content/generate \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"topic":"Gestão documental integrada","format":"Post para LinkedIn"}'
```

Use o valor de `conversationId` recebido na primeira resposta:

```bash
curl -X POST http://localhost:8080/ai/content/generate \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"<conversationId>","topic":"Ajuste a sugestão anterior para gestores","format":"Post para LinkedIn"}'
```

O `PostgresChatMemoryStore` salva as mensagens serializadas pelo próprio LangChain4j na tabela
`chat_memories`. Como as demais entidades, ela é criada pelo Hibernate com a configuração atual de
`ddl-auto`. A memória mantém no máximo 20 mensagens por conversa; isso é uma janela de mensagens,
não uma contagem de turnos ou tokens. A memória é contexto para o modelo, não substitui um histórico
completo para exibição ao usuário.

Ao adicionar um agente, declare um bean `AgentChatMemoryProvider` com um namespace único, por
exemplo `public-notices`, e configure a interface com `@AiService(wiringMode = EXPLICIT,
chatModel = "googleAiGeminiChatModel", chatMemoryProvider = "publicNoticesChatMemoryProvider")`.
Passe também em `@MemoryId` um ID de conversa gerado pelo servidor e vinculado ao usuário
autenticado. Cada agente usa seu próprio provider e, portanto, seu próprio namespace, embora todos
possam compartilhar o mesmo `PostgresChatMemoryStore`.

## Configuração e execução
Pré-requisitos: JDK 25, PostgreSQL e uma chave de API do Google Gemini.
1. Crie um banco PostgreSQL para a aplicação. O `application.properties` atual aponta para 
`localhost:5432/cmiap`.
2. Configure credenciais do banco, segredo JWT e chave Gemini por variáveis de ambiente ou 
configuração externa. As propriedades correspondentes incluem `spring.datasource.username`, 
`spring.datasource.password`, `jwt.secret.key` e `langchain4j.google-ai-gemini.chat-model.api-key`.
3. Inicie a aplicação.

## Persistência e cuidados de configuração
A tabela `users` armazena UUID, nome, e-mail único, senha codificada e perfil. A configuração atual
de Hibernate usa `ddl-auto=create-drop`, que recria o esquema ao iniciar e o remove ao encerrar. 
É apropriada apenas para desenvolvimento/testes, não para um banco com dados que devam ser 
preservados.

O arquivo de configuração de execução contém atualmente credenciais e chaves diretamente no arquivo. 
Não reutilize nem publique esses valores: remova-os do arquivo versionado, injete segredos pelo 
ambiente/gerenciador de segredos e rotacione qualquer chave que já tenha sido exposta. Também 
desative logs SQL detalhados em ambientes com dados sensíveis.