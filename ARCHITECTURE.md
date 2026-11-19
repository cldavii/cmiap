# Backend architecture

The backend is organized as a **modular monolith**, grouped by business capability rather than by technical layer. This keeps the current deployment and development model simple while giving each capability a clear ownership boundary.

## Current modules

| Package | Responsibility |
| --- | --- |
| `user` | User accounts, registration, administration, persistence, and user-facing account data |
| `security` | Authentication endpoints, cookies, JWT/CSRF/CORS configuration, and refresh-token lifecycle |
| `agent.content` | Content-generation use case, conversation history, LangChain4j integration, and its persistence |
| `agent.leads` | User-scoped lead records, CSV import API, and AI-assisted qualification against a supplied ideal-customer profile |
| `agent.seo` | Auditoria de SEO e recomendações para páginas HTTPS públicas |
| `agent.social` | Validação e simulação de publicações orgânicas a partir de insumos fornecidos na requisição |

Each feature package owns its API, application logic, and persistence code. We avoid creating empty `controller`, `service`, `repository`, or `entity` packages shared across unrelated features.

## Module boundaries

- Controllers translate HTTP requests and responses; they delegate application work to the owning feature.
- `UserService` is the user module's application facade. Other modules use narrow projections (`UserIdentity`, `UserCredentials`) instead of accessing the user repository or persistence entity.
- `security` owns authentication and token policy. User data and user account persistence remain owned by `user`.
- `agent.content` owns content conversations and their chat-memory adapter. Conversation access is scoped to the authenticated user.
- `agent.leads` owns lead records and qualification; every query is scoped to the authenticated user. Qualification uses only data supplied by the user and Gemini; it does not enrich or contact leads.
- Cross-cutting Spring configuration stays with the concern it configures; bootstrap administrator provisioning belongs to the `user` feature.

## Dependency direction

```text
HTTP / Spring Security adapters
          ↓
Feature application services
          ↓
Feature-owned repositories and integrations
```

Keep dependencies between features at their public service/projection boundary. Do not have a feature query another feature's repository directly.

## Growing the codebase

Keep a feature package compact while it has only a few classes. When a capability grows, split **inside that feature** into packages such as `api`, `application`, `domain`, and `infrastructure`; introduce interfaces/ports when there are multiple implementations or a real testing boundary. Avoid splitting the deployment into microservices until independent scaling, ownership, or release requirements justify that operational cost.
