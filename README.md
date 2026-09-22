# CMIAP - Sistema de Agentes de IA

Este é um projeto desenvolvido com Spring Boot, focado em criar um ecossistema de Agentes Inteligentes integrados utilizando Langchain4j e a API do Google Gemini.

## Arquitetura Atual (DDD - Domain-Driven Design)

Atualmente, o projeto está estruturado em módulos baseados em domínios (Domain-Driven Design), separando responsabilidades por contexto delimitado (Bounded Contexts).

### Árvore do Projeto (Current Project Tree)

```text
cmiap/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/ai/incorptech/cmiap/
    │   │   ├── CmiapApplication.java
    │   │   ├── TerminalTestRunner.java
    │   │   ├── config/
    │   │   │   └── AiAgentsConfig.java
    │   │   └── modules/
    │   │       ├── editais/
    │   │       │   ├── controller/EditalController.java
    │   │       │   └── service/EditalAiService.java
    │   │       └── tutor/
    │   │           ├── controller/AssistantController.java
    │   │           └── service/AssistantAiService.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/ai/incorptech/cmiap/
            └── CmiapApplicationTests.java
```

## Bibliotecas e Dependências do Projeto

O projeto utiliza o **Java 25** e as seguintes bibliotecas principais definidas no `pom.xml`:

*   **Spring Boot (4.1.1)**
    *   `spring-boot-starter-webmvc`: Para a criação de APIs REST.
    *   `spring-boot-starter-webmvc-test`: Para testes automatizados da camada web.
*   **Langchain4j (1.20.0)**
    *   `langchain4j-spring-boot4-starter`: Integração oficial do Langchain4j com o Spring Boot 4.
    *   `langchain4j-google-ai-gemini`: Integração específica para utilizar o modelo Google Gemini.
*   **Lombok**: Para reduzir boilerplate (código repetitivo como Getters, Setters, Construtores).

## O Que Faremos no Futuro (Expansão para 8 Agentes)

No futuro, a arquitetura modular baseada em DDD nos permitirá expandir o sistema para suportar **8 Agentes de IA Especializados**. 

Atualmente já temos as bases para **2 Agentes**:
1.  **Agente Tutor (`modules/tutor`)**: Especialista em ensino, dúvidas e suporte educativo.
2.  **Agente de Editais (`modules/editais`)**: Especialista em análise e geração de informações sobre editais.
