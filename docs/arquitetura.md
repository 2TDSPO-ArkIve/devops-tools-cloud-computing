# Arquitetura da Solução — ArkIve

## Visão geral

O ArkIve é uma aplicação Spring Boot de apoio clínico veterinário. O sistema organiza dados administrativos, clínicos, preventivos e terapêuticos e oferece suporte assistivo à decisão do veterinário.

A aplicação reúne controllers Spring MVC e REST, frontend web com Thymeleaf, camada de serviços, repositories Spring Data JPA, persistência Oracle, migrations Flyway e autenticação e autorização com Spring Security. O fluxo clínico consulta um motor externo de apoio e pode usar Azure Speech, com FFmpeg para conversão de áudio, quando a transcrição estiver configurada.

O apoio produzido pela inteligência artificial é provisório. Ele não substitui o veterinário, não confirma autonomamente diagnósticos e não prescreve medicamentos.

## Camadas

| Camada | Responsabilidade |
| --- | --- |
| Controllers | Recebem requisições HTTP. Os controllers REST expõem `/api/**`; os controllers MVC preparam modelos e retornam templates Thymeleaf. |
| Services | Implementam casos de uso, transações, validações de negócio, autorização por escopo e orquestração de integrações. |
| Repositories | Usam Spring Data JPA para persistência, filtros, paginação e consultas autorizadas. |
| Entities | Mapeiam com JPA as tabelas e relacionamentos do schema Oracle `TB_ARKIVE_*`. |
| DTOs | Definem contratos de entrada, saída e formulários, incluindo Bean Validation. |
| Security e configuração | Configuram autenticação, BCrypt, proteção de rotas, ciclo de senha, CORS, OpenAPI e clientes externos. |
| Templates e arquivos estáticos | Implementam as páginas Thymeleaf, fragmentos reutilizáveis, CSS, JavaScript e imagens. |
| Migrations | Versionam a criação e a evolução do banco em `src/main/resources/db/migration`. |

O tratamento de exceções é centralizado por `GlobalExceptionHandler`. Recursos de catálogo usam cache, com invalidação nas operações de alteração.

## Segurança

Os usuários são persistidos no banco e carregados por `ArkiveUserDetailsService`. A autenticação usa `DaoAuthenticationProvider` e senhas com hash BCrypt.

O frontend web utiliza form login em `/login` e sessão HTTP. Clientes REST podem usar HTTP Basic. O Spring Security trabalha com quatro perfis:

- `SYSADMIN`: administração global, incluindo clínicas, usuários, veterinários e responsáveis;
- `ADMIN_CLINICA`: administração e visualização limitadas à própria clínica;
- `VETERINARIO`: operações clínicas nas consultas sob sua responsabilidade;
- `RESPONSAVEL`: acesso aos animais e dados clínicos vinculados e registro da própria adesão.

As rotas `/sysadmin/**` e `/admin/**` possuem restrições próprias. A API também tem regras por recurso, método HTTP e perfil; a administração REST de clínicas e veterinários, por exemplo, exige `SYSADMIN`.

As regras de rota são complementadas por `ClinicalAccessService` e por consultas filtradas nos services e repositories. Essas verificações impedem acesso clínico fora da clínica, consulta, veterinário ou vínculo do responsável aplicável.

Contas provisionadas com credencial temporária e contas cuja senha foi redefinida ficam obrigadas a alterar a senha. O filtro `MandatoryPasswordChangeFilter` restringe a navegação até a conclusão dessa etapa.

## Banco de dados

O banco oficial da aplicação é Oracle. Spring Data JPA e Hibernate realizam o mapeamento e o acesso aos dados.

O Flyway controla o schema por meio das migrations `V1` a `V6`:

- `V1`: cria o schema-base, tabelas, constraints, relacionamentos e índices;
- `V2`: evolui usuários para os quatro perfis e adiciona o vínculo administrativo com clínica;
- `V3`: adiciona o ciclo de troca de senha;
- `V4`: adiciona as fontes consultadas pela IA ao diagnóstico;
- `V5`: registra o veterinário responsável pelo cadastro do animal;
- `V6`: adiciona data de nascimento do animal e endereço da consulta.

No perfil `oracle`, o Flyway é habilitado e o Hibernate usa `spring.jpa.hibernate.ddl-auto=validate`. Assim, as migrations criam e evoluem o schema, enquanto o Hibernate apenas verifica sua compatibilidade com as entities.

## Diagramas da solução

### Diagrama simplificado de entidades

<p align="center">
  <img src="images/arkive-diagrama-entidades.png" alt="Diagrama simplificado das entidades do ArkIve" width="900">
</p>

### Modelo entidade-relacionamento

<p align="center">
  <img src="images/arkive-mer.png" alt="Modelo entidade-relacionamento do ArkIve" width="900">
</p>

## Fluxo clínico

A consulta clínica assistida segue o fluxo principal `AG → EP → AP → FI`:

- `AG`: consulta agendada;
- `EP`: atendimento em progresso, com narrativa clínica;
- `AP`: apoio clínico externo persistido e aguardando parecer;
- `FI`: consulta finalizada com conclusão confirmada pelo veterinário.

O motor clínico externo retorna hipótese, severidade, confiança, insight e fontes. Esse registro permanece marcado como provisório. Somente o veterinário responsável informa a conclusão confirmada e finaliza a consulta. As transições e ações principais geram eventos de jornada.

Depois de uma consulta `FI`, o veterinário responsável pode criar uma prescrição. O responsável vinculado ao animal registra a adesão durante o período válido do tratamento. O histórico de adesão é imutável e também produz evento de jornada.

## Frontend

O projeto Java possui frontend Thymeleaf para administração e visualização clínica.

A área `SYSADMIN` oferece dashboard e páginas de gestão de clínicas, usuários, veterinários e responsáveis. A área `ADMIN_CLINICA` oferece dashboard, gestão de animais e visualização de consultas, narrativa, apoio da IA, conclusão veterinária, prescrições e adesões dentro do escopo da clínica.

Os formulários usam Bean Validation, `BindingResult`, mensagens `th:errors` e token CSRF. A navegação usa Thymeleaf Extras Spring Security para exibir opções conforme o perfil.

Não existem fluxos Thymeleaf completos para `VETERINARIO` ou `RESPONSAVEL` nesta aplicação. Esses perfis autenticam pelo mesmo Spring Security, mas seus casos de uso operacionais atuais são atendidos pela API e pelos clientes que a consomem.

## Integrações

- **Motor clínico ArkIve:** acessado com `RestClient` para gerar apoio clínico provisório a partir de uma consulta em progresso.
- **Azure Speech:** integração opcional para transformar áudio clínico em texto editável. A transcrição não inicia consulta, não persiste narrativa e não aciona o motor clínico.
- **FFmpeg:** usado localmente para converter formatos compactados, como M4A, AAC e WebM, para WAV antes do envio ao Azure Speech. A imagem Docker já instala essa dependência.
