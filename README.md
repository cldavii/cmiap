# CMIAP - Sistema de Agentes Inteligentes

Este é um projeto visa o desenvolvimento de uma plataforma de Agentes Inteligentes para melhorias
nos processos de trabalho da empresa **Incorp Tech**.

## Funcionalidades implementadas

- Cadastro de usuário;
- Login de usuários;
- Agente para leitura de editais(Em progresso);

## Tecnologias
| Tecnologia | Uso |
| --- | --- |
| Java 25 | Linguagem e versão configurada no Maven |
| Spring Boot 4.1.1 | Inicialização e integração da aplicação |
| Spring MVC | API REST e controllers |
| Spring Security | Autorização de rotas, filtro JWT e codificação de senhas |
| Spring Data JPA / Hibernate | Persistência relacional |
| PostgreSQL | Banco de dados |
| JJWT 0.13.0 | Criação e validação de tokens JWT |
| LangChain4j 1.22.0-beta32 | Integração com o modelo de chat Gemini |
| MapStruct 1.6.3 | Conversão entre modelo de domínio e entidade JPA |
| Lombok | Redução de código repetitivo |
| JUnit 5, AssertJ, Mockito e Spring Boot Test | Testes automatizados |
| Maven Wrapper | Build e execução do projeto |

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
O serviço de editais é declarado como interface LangChain4j com instruções de sistema.

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
│   │   ├── domain/
│   │   │   ├── model/ (User, UserRole)
│   │   │   ├── repository/UserRepository.java
│   │   │   └── valueObject/Email.java
│   │   ├── infrastructure/
│   │   │   ├── ai/modules/publicNotices/PublicNoticeService.java
│   │   │   ├── persistence/ (adapter, entidade e mapper)
│   │   │   └── security/ (configuração, filtro e serviço JWT)
│   │   └── presentation/
│   │       ├── controller/ (AuthController, PublicNoticeController)
│   │       └── controller/dto/auth/
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

## Configuração e execução
Pré-requisitos: JDK 25, PostgreSQL e uma chave de API do Google Gemini.

1. Crie um banco PostgreSQL para a aplicação. O `application.properties` atual aponta para 
`localhost:5432/cmiap2`.
2. Configure credenciais do banco, segredo JWT e chave Gemini por variáveis de ambiente ou 
configuração externa. As propriedades correspondentes incluem `spring.datasource.username`, 
`spring.datasource.password`, `jwt.secret.key` e `langchain4j.google-ai-gemini.chat-model.api-key`.
3. Inicie a aplicação:

## Persistência e cuidados de configuração
A tabela `users` armazena UUID, nome, e-mail único, senha codificada e perfil. A configuração atual
de Hibernate usa `ddl-auto=create-drop`, que recria o esquema ao iniciar e o remove ao encerrar. 
É apropriada apenas para desenvolvimento/testes, não para um banco com dados que devam ser 
preservados.

O arquivo de configuração de execução contém atualmente credenciais e chaves diretamente no arquivo. 
Não reutilize nem publique esses valores: remova-os do arquivo versionado, injete segredos pelo 
ambiente/gerenciador de segredos e rotacione qualquer chave que já tenha sido exposta. Também 
desative logs SQL detalhados em ambientes com dados sensíveis.