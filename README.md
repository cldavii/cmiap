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

Para completar os **8 Agentes**, planejamos adicionar os seguintes módulos (sugestão inicial):
3.  **Agente Financeiro (`modules/financeiro`)**: Para análise de relatórios, orçamentos e custos.
4.  **Agente de RH (`modules/rh`)**: Auxílio em triagem de currículos e dúvidas de colaboradores.
5.  **Agente de Atendimento/Suporte (`modules/suporte`)**: SAC automatizado para resolver problemas de clientes.
6.  **Agente de Vendas (`modules/vendas`)**: Auxilia na qualificação de leads e roteiros de negociação.
7.  **Agente Jurídico (`modules/juridico`)**: Análise de contratos, termos e adequação à legislação (LGPD).
8.  **Agente de Marketing (`modules/marketing`)**: Geração de copys, ideias de campanhas e análise de SEO.
