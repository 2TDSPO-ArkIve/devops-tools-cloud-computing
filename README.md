# ArkIve

O **ArkIve** é uma plataforma de apoio clínico veterinário desenvolvida em Spring Boot. A aplicação organiza dados de animais, responsáveis, clínicas, consultas, diagnósticos, prescrições, adesão ao tratamento e outros eventos da jornada de saúde do pet.

O motor de inteligência artificial atua como ferramenta assistiva: produz hipóteses, indicação de severidade, confiança, fontes e insights para apoiar a análise. Ele não substitui o veterinário, não confirma autonomamente um diagnóstico e não prescreve medicamentos. A conclusão clínica e a decisão final continuam sob responsabilidade do profissional veterinário.

## Objetivo do projeto

O ArkIve busca reduzir a fragmentação do histórico veterinário. A solução reúne informações clínicas, preventivas e terapêuticas em uma base longitudinal, aplica regras de acesso conforme o perfil do usuário e oferece apoio à tomada de decisão sem retirar a autonomia do veterinário.

## Sprint 3 — Java Advanced

Esta aplicação Spring Boot foi preparada para os requisitos da **FIAP Java Advanced — 3º Sprint**, com ênfase em:

- frontend web e camada de visualização com Thymeleaf;
- versionamento do banco Oracle com Flyway;
- autenticação e autorização com Spring Security;
- quatro perfis com permissões efetivamente diferentes;
- dois fluxos funcionais completos além de CRUD;
- validação de formulários, DTOs e regras de negócio.

| Requisito da Sprint | Evidência no projeto |
| --- | --- |
| Frontend | Controllers MVC, templates Thymeleaf, dashboards, formulários e páginas de consulta de dados |
| Flyway | Migrations Oracle `V1` a `V6` em `src/main/resources/db/migration` |
| Spring Security | Usuários persistidos, BCrypt, form login, HTTP Basic, quatro perfis e proteção de rotas |
| Fluxos além de CRUD | Consulta clínica assistida e prescrição com registro de adesão |
| Validação | Bean Validation, `BindingResult`, regras nos services e constraints Oracle |

## Tecnologias

| Tecnologia | Uso no projeto |
| --- | --- |
| Java 17 | Linguagem e versão de compilação |
| Spring Boot 3.5.14 | Inicialização, configuração e empacotamento da aplicação |
| Spring MVC / Spring Web | Controllers web e API REST |
| Thymeleaf | Renderização server-side do frontend |
| Thymeleaf Extras Spring Security 6 | Navegação condicionada ao perfil autenticado |
| Spring Security | Form login, HTTP Basic, autorização por perfil e sessão web |
| BCrypt | Hash das senhas persistidas |
| Spring Data JPA / Hibernate | Persistência e consultas ao banco |
| Bean Validation | Validação declarativa de DTOs e formulários |
| Flyway | Criação e evolução versionada do schema |
| Oracle Database / `ojdbc11` | Banco da aplicação no perfil padrão `oracle` |
| Maven Wrapper 3.9.15 | Build reproduzível sem instalação manual do Maven |
| Springdoc OpenAPI 2.8.9 | Documentação complementar da API |
| Apache PDFBox 3.0.8 | Geração de históricos e resumos clínicos em PDF |
| Azure Speech SDK 1.51.0 | Transcrição opcional de áudio clínico |
| FFmpeg | Conversão temporária de M4A, AAC e WebM para WAV na transcrição |
| Docker | Build em múltiplos estágios e imagem Java 17 com dependências de áudio |
| JUnit, MockMvc, Spring Security Test e H2 | Testes automatizados sem depender de uma instância Oracle |

O suporte clínico usa ainda o `RestClient` do Spring para consultar o motor clínico externo configurado por `ARKIVE_CLINICAL_ENGINE_URL`.

## Arquitetura

O projeto separa apresentação, regras de negócio e persistência. Os mesmos services são usados pelos controllers REST e, quando aplicável, pelos controllers MVC.

```text
Navegador / cliente REST
          │
          ▼
Controller MVC + Thymeleaf / REST Controller
          │
          ▼
Service + regras de negócio + autorização por escopo
          │
          ▼
Spring Data Repository ──► JPA/Hibernate ──► Oracle
          │
          ├──► motor clínico externo
          └──► Azure Speech + FFmpeg (transcrição opcional)
```

| Área | Responsabilidade |
| --- | --- |
| `controller` | Endpoints REST e controllers web MVC |
| `service` | Casos de uso, transações, validações e integrações |
| `repository` | Consultas e persistência com Spring Data JPA |
| `entity` | Mapeamento das tabelas Oracle |
| `dto` | Contratos de entrada, saída e formulários |
| `security` / `config` | Autenticação, autorização, senha, CORS e integrações |
| `templates` / `static` | Páginas Thymeleaf, CSS, JavaScript e imagens |
| `db/migration` | Evolução do schema controlada pelo Flyway |

## Perfis de usuário e Spring Security

| Perfil | Principais permissões |
| --- | --- |
| `SYSADMIN` | Administração global; dashboards; gestão de clínicas, usuários, veterinários e responsáveis; acesso administrativo aos dados do sistema |
| `ADMIN_CLINICA` | Dashboard e gestão de animais no escopo da própria clínica; visualização das consultas, prescrições e adesões vinculadas à clínica |
| `VETERINARIO` | Operações clínicas nas próprias consultas; narrativa, suporte clínico, conclusão, prescrições, transcrição e acesso aos pacientes permitidos |
| `RESPONSAVEL` | Leitura dos animais, consultas e prescrições aos quais está vinculado; registro da própria adesão e interações destinadas ao responsável |

Os usuários são carregados do banco por um `UserDetailsService` e autenticados por um `DaoAuthenticationProvider`. As senhas são persistidas somente como hashes BCrypt. O navegador usa form login em `/login`, enquanto clientes REST podem usar HTTP Basic.

O `SecurityConfig` restringe rotas por perfil. As áreas `/sysadmin/**` exigem `SYSADMIN`; `/admin/**` aceita `SYSADMIN` ou `ADMIN_CLINICA`. A matriz REST também protege explicitamente recursos administrativos: por exemplo, `/api/clinicas/**` e `/api/veterinarios/**` são exclusivos de `SYSADMIN`, e catálogos podem permitir leitura a outros perfis sem liberar suas mutações.

Além da proteção de rota, `ClinicalAccessService` e os services clínicos validam propriedade e escopo. Assim, o veterinário só altera consultas e prescrições sob sua responsabilidade, o administrador visualiza dados da própria clínica e o responsável acessa vínculos permitidos.

Contas provisionadas automaticamente para clínica ou veterinário recebem uma credencial inicial temporária e ficam marcadas para troca obrigatória. O `MandatoryPasswordChangeFilter` impede o restante da navegação até a alteração em `/alterar-senha`. A nova senha deve ter de 8 a 72 caracteres, com pelo menos uma letra maiúscula, uma minúscula e um número.

## Frontend Web com Thymeleaf

O projeto não é apenas uma API REST. A aplicação possui controllers MVC e páginas Thymeleaf que renderizam dados do backend, enviam formulários para endpoints Spring MVC e exibem mensagens de sucesso ou validação.

| Área | Rotas e telas principais |
| --- | --- |
| Autenticação | `/login` e `/alterar-senha` |
| Início | `/`, com redirecionamento conforme o perfil |
| SysAdmin | `/sysadmin/dashboard` |
| Clínicas | `/sysadmin/clinicas`, criação, edição e desativação |
| Usuários | `/sysadmin/usuarios`, criação, edição, ativação, desativação e redefinição de senha |
| Veterinários | `/sysadmin/veterinarios`, criação, edição e desativação |
| Responsáveis | `/sysadmin/responsaveis`, criação, edição e desativação |
| Administração da clínica | `/admin/dashboard` |
| Animais | `/admin/animais`, criação, detalhe, edição, ativação e desativação |
| Consultas | `/admin/consultas` e `/admin/consultas/{id}` |
| Prescrições | `/admin/prescricoes` e `/admin/prescricoes/{id}` |
| Adesão | `/admin/adesoes` e `/admin/adesoes/{id}` |

Os dashboards mostram indicadores calculados com dados reais do backend. O detalhe da consulta apresenta narrativa, evolução do status, apoios provisórios da IA, parecer veterinário, prescrições e adesões relacionadas.

Os formulários usam `th:object`, `th:field` e `th:errors`; erros de campos e de negócio retornam à página. As submissões web incluem token CSRF. A barra lateral usa as autoridades do Spring Security para mostrar somente a navegação compatível com o perfil.

Atualmente, as áreas web completas são destinadas a `SYSADMIN` e `ADMIN_CLINICA`. `VETERINARIO` e `RESPONSAVEL` autenticam pelo mesmo mecanismo, mas são direcionados a `/acesso-web-restrito`; seus casos de uso operacionais são consumidos pela API/cliente. O README não atribui a esses dois perfis telas Thymeleaf clínicas que não existem no repositório.

## Fluxos funcionais além de CRUD

### Fluxo 1 — Consulta clínica assistida

```text
AG (Agendada)
  → EP (Em Progresso)
  → narrativa clínica
  → suporte clínico assistido por IA
  → AP (Aguardando Parecer)
  → conclusão do veterinário
  → FI (Finalizada)
```

1. Uma consulta é criada/agendada, normalmente com status `AG`.
2. O veterinário responsável inicia o atendimento; o service valida o vínculo, os recursos ativos e a transição `AG → EP`.
3. A narrativa clínica é registrada e pode ser corrigida enquanto a consulta está em `EP` ou `AP`.
4. Em `EP`, o veterinário solicita suporte clínico. A aplicação consulta o motor externo e valida a resposta.
5. A hipótese, severidade, confiança, insight e fontes retornadas são persistidas como apoio provisório, com `confirmado=N` e `validacaoVet=N`; a consulta avança para `AP`.
6. O veterinário informa seu próprio diagnóstico e conclusão. Esse parecer é persistido como confirmado e validado pelo profissional.
7. A consulta muda para `FI`, e eventos relevantes da jornada são registrados.

Comandos:

```http
POST  /api/consultas/{id}/iniciar
PATCH /api/consultas/{id}/narrativa
POST  /api/consultas/{id}/suporte-clinico
GET   /api/consultas/{id}/suporte-clinico
POST  /api/consultas/{id}/finalizar
```

Esse fluxo não é CRUD simples porque contém uma máquina de estados, regras de transição, autorização pelo veterinário responsável, chamada externa, validação e persistência do resultado provisório, distinção entre hipótese da IA e conclusão profissional e registro de eventos da jornada.

### Fluxo 2 — Prescrição e adesão

Fluxo principal:

```text
consulta FI → prescrição veterinária → adesão do responsável → histórico terapêutico
```

1. A consulta precisa estar finalizada (`FI`).
2. O veterinário responsável cria a prescrição com medicamento, dosagem, frequência, via, período e instruções.
3. O sistema valida a autoria clínica, o estado da consulta, a via de administração e as datas do tratamento.
4. O responsável autenticado e vinculado ao animal registra se houve adesão, informando a prescrição, `S` ou `N` e uma observação opcional.
5. O servidor deriva o responsável e o animal da identidade autenticada e da prescrição, valida o vínculo e confirma que o registro está dentro do período do tratamento.
6. A adesão é persistida com data definida pelo servidor e gera um evento `ADESAO_REGISTRADA`.
7. O histórico de adesão não pode ser editado nem excluído; uma prescrição com adesão registrada também não pode ser alterada ou removida.

Endpoints principais:

```http
POST /api/prescricoes
GET  /api/prescricoes
POST /api/adesoes-prescricao
GET  /api/adesoes-prescricao
```

Esse fluxo não é CRUD simples porque depende do encerramento de outro processo, impõe autoria e vínculo, valida a janela terapêutica, completa dados pelo contexto autenticado, produz evento de jornada e preserva um histórico imutável.

## Validações

A aplicação valida dados em camadas complementares:

- DTOs com `@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Positive`, `@Min`, `@Max`, `@DecimalMin` e `@DecimalMax`;
- controllers REST e MVC com `@Valid`;
- formulários MVC com `BindingResult`, retorno à mesma view e mensagens `th:errors`;
- validações de negócio nos services, como transições de consulta, recursos ativos, propriedade clínica, vínculos, datas de tratamento e imutabilidade da adesão;
- constraints Oracle para chaves, unicidade, referências, domínios `CHECK`, datas e JSON.

Entradas inválidas da API recebem respostas pelo tratamento centralizado de exceções. Nos formulários web, os valores preenchidos e os erros compreensíveis são apresentados novamente na página.

## Flyway e banco de dados

O **Flyway controla o versionamento do schema Oracle**. As migrations ficam em `src/main/resources/db/migration` e são aplicadas em ordem durante a inicialização do perfil `oracle`.

| Versão | Migration | Objetivo |
| --- | --- | --- |
| V1 | `V1__create_arkive_schema.sql` | Cria o schema-base com 23 tabelas, chaves, constraints, relacionamentos, colunas identity, comentários e índices |
| V2 | `V2__evolve_usuario_for_security.sql` | Evolui usuários para os quatro perfis, adiciona nome e vínculo de clínica e ajusta constraints de segurança |
| V3 | `V3__add_usuario_password_lifecycle.sql` | Adiciona controle de troca obrigatória e data da última troca de senha |
| V4 | `V4__add_diagnostico_fontes_ia.sql` | Adiciona as fontes consultadas pelo motor clínico ao diagnóstico |
| V5 | `V5__add_animal_veterinario_cadastro.sql` | Registra o veterinário que cadastrou o animal, com chave estrangeira e índice |
| V6 | `V6__add_animal_birth_date_and_consulta_address.sql` | Adiciona data de nascimento do animal e endereço da consulta |

No perfil Oracle:

- `spring.flyway.enabled=true`;
- `spring.flyway.locations=classpath:db/migration`;
- `spring.flyway.baseline-on-migrate=true`, com versão-base `1`;
- `spring.jpa.hibernate.ddl-auto=validate`, portanto o Hibernate valida o modelo e não gera o schema;
- `spring.sql.init.mode=never`, evitando inicialização paralela por scripts SQL do Spring.

As migrations não inserem usuários ou credenciais de demonstração.

## Pré-requisitos

### Obrigatórios para a aplicação completa

- JDK 17 disponível em `JAVA_HOME` ou no `PATH`;
- acesso à internet no primeiro uso do Maven Wrapper e para baixar dependências;
- uma instância Oracle e um usuário/schema da aplicação;
- acesso ao motor clínico externo para demonstrar a etapa de suporte por IA.

O repositório não fixa uma versão específica do Oracle. A instância deve aceitar os recursos usados nas migrations, como colunas `GENERATED BY DEFAULT AS IDENTITY`, constraints com `IS JSON`, CLOBs, índices e chaves estrangeiras.

### Opcionais

- Docker, caso a execução seja feita pela imagem do `Dockerfile`;
- Azure Speech configurado para usar `/api/transcricoes`;
- FFmpeg no `PATH` para converter áudio M4A, AAC ou WebM em execução local. A imagem Docker já instala o FFmpeg.

A narrativa da consulta pode ser enviada como texto sem Azure Speech. Assim, Azure Speech e FFmpeg não são necessários para iniciar a aplicação nem para executar a consulta assistida sem transcrição de áudio.

## Variáveis de ambiente

Nenhum valor real de credencial deve ser versionado. O arquivo `.env.example` serve apenas como referência de nomes.

| Variável | Finalidade | Obrigatória |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Seleciona o perfil; `oracle` já é o perfil padrão | Não, mas recomendada explicitamente |
| `ARKIVE_DB_URL` | URL JDBC Oracle | Sim no perfil `oracle` |
| `ARKIVE_DB_USERNAME` | Usuário/schema Oracle | Sim no perfil `oracle` |
| `ARKIVE_DB_PASSWORD` | Senha do usuário Oracle | Sim no perfil `oracle` |
| `ARKIVE_JPA_SHOW_SQL` | Habilita log SQL; padrão `false` | Não |
| `ARKIVE_CLINICAL_ENGINE_URL` | URL-base do motor clínico externo; existe uma URL padrão configurada | Não, se a URL padrão for usada |
| `ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED` | Habilita a criação inicial do SysAdmin; padrão `false` | Não |
| `ARKIVE_BOOTSTRAP_SYSADMIN_NAME` | Nome do primeiro SysAdmin | Sim quando o bootstrap está habilitado |
| `ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN` | Login/e-mail do primeiro SysAdmin | Sim quando o bootstrap está habilitado |
| `ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD` | Senha inicial do primeiro SysAdmin | Sim quando o bootstrap está habilitado |
| `AZURE_SPEECH_ENDPOINT` | Endpoint do recurso Azure Speech | Somente para transcrição |
| `AZURE_SPEECH_API_KEY` | Chave do Azure Speech | Somente para transcrição |
| `PORT` | Porta HTTP; padrão `8080` | Não |

## Configuração inicial do banco

1. Solicite ou crie um usuário/schema Oracle destinado à aplicação.
2. Conceda a esse usuário permissão de conexão e permissões para criar e evoluir os objetos presentes nas migrations, incluindo tabelas, índices, constraints, chaves estrangeiras e comentários.
3. Configure `ARKIVE_DB_URL`, `ARKIVE_DB_USERNAME` e `ARKIVE_DB_PASSWORD` fora do repositório.
4. Inicie a aplicação com o perfil `oracle`.
5. O Flyway cria a tabela de histórico e aplica automaticamente as migrations pendentes. O Hibernate valida o resultado antes de a aplicação ficar disponível.

O repositório não define um script de criação do usuário Oracle nem exige comandos DBA específicos. Não é necessário executar as migrations manualmente.

## Primeiro acesso / criação do SYSADMIN

Em um banco limpo, as migrations criam o schema, mas não criam usuários da aplicação. O primeiro `SYSADMIN` pode ser provisionado pelo `InitialSysAdminBootstrap`.

No PowerShell, defina valores próprios antes da primeira inicialização:

```powershell
$env:ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED="true"
$env:ARKIVE_BOOTSTRAP_SYSADMIN_NAME="<nome-do-administrador>"
$env:ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN="<login-ou-email>"
$env:ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD="<senha-segura>"
```

Ao iniciar a aplicação:

1. o bootstrap verifica se está habilitado;
2. se já existir um `SYSADMIN` ativo, nenhuma conta é criada;
3. caso contrário, nome, login e senha precisam estar preenchidos;
4. a senha é armazenada como hash BCrypt;
5. o novo usuário pode acessar `/login`.

O bootstrap exige os três campos não vazios. Para manter consistência com a política de troca de senha do projeto, use de 8 a 72 caracteres, com maiúscula, minúscula e número. Depois da criação bem-sucedida, desabilite o bootstrap e remova a senha do ambiente:

```powershell
$env:ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED="false"
Remove-Item Env:ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD
```

O `SYSADMIN` criado pelo bootstrap usa a senha definida pelo operador. A troca obrigatória se aplica às contas provisionadas com credencial temporária e às senhas redefinidas pelo painel.

## Como executar

### Windows / PowerShell

Configure primeiro as variáveis Oracle no ambiente ou na configuração de execução da IDE. Não coloque valores reais no README nem em arquivos versionados.

Testes:

```powershell
.\mvnw.cmd test
```

Build sem repetir os testes:

```powershell
.\mvnw.cmd package -DskipTests
```

Execução com Oracle e Flyway:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=oracle"
```

Execução do JAR empacotado:

```powershell
java -jar target\arkive-0.0.1-SNAPSHOT.jar --spring.profiles.active=oracle
```

### Linux / macOS

```bash
./mvnw test
./mvnw package -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=oracle
```

O perfil `local-nodb` existe somente para uma inicialização técnica sem banco e desabilita DataSource, JPA e Flyway:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-nodb"
```

Esse perfil não permite demonstrar o frontend autenticado nem os fluxos persistidos.

### Docker opcional

O `Dockerfile` compila a aplicação com Java 17 e Maven e gera uma imagem de runtime Java 17 com FFmpeg e bibliotecas do Azure Speech.

```powershell
docker build -t arkive .
docker run --rm -p 8080:8080 --env-file .env arkive
```

Crie o `.env` localmente a partir de `.env.example`, substitua os placeholders e não versione o arquivo com valores reais.

## Acesso à aplicação

Com a execução local na porta padrão:

| Recurso | URL |
| --- | --- |
| Aplicação web / login | [http://localhost:8080/login](http://localhost:8080/login) |
| Entrada da aplicação | [http://localhost:8080/](http://localhost:8080/) |
| Dashboard SysAdmin | [http://localhost:8080/sysadmin/dashboard](http://localhost:8080/sysadmin/dashboard) |
| Dashboard da clínica | [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard) |
| Health check | [http://localhost:8080/api/health](http://localhost:8080/api/health) |

Após o login, `/` redireciona `SYSADMIN` para `/sysadmin/dashboard`, `ADMIN_CLINICA` para `/admin/dashboard` e os demais perfis para `/acesso-web-restrito`.

Deploy atual no Render:

- aplicação: [https://arkive-b7v2.onrender.com/](https://arkive-b7v2.onrender.com/)
- login direto: [https://arkive-b7v2.onrender.com/login](https://arkive-b7v2.onrender.com/login)

O Swagger é documentação complementar para desenvolvimento e demonstração da API; a interface principal para a avaliação web é o frontend Thymeleaf:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

O repositório não contém contas de demonstração ou senhas públicas. Use o bootstrap inicial ou contas provisionadas por um `SYSADMIN`.

## Testes

A última execução completa após o endurecimento da autorização REST apresentou:

```text
Tests run: 561
Failures: 0
Errors: 0
Skipped: 0
```

Também foi validado o empacotamento em um JAR executável Spring Boot com:

```powershell
.\mvnw.cmd package -DskipTests
```

Os testes usam H2 e cobrem controllers MVC/REST, Spring Security, ciclo de senha, serviços, autorização clínica, fluxos de consulta, prescrição/adesão, repositories, PDFs, transcrição e integração simulada com o motor clínico. Eles não substituem uma validação manual contra a instância Oracle configurada.


## Estrutura do projeto

```text
src/
├── main/
│   ├── java/br/com/fiap/arkive/
│   │   ├── bootstrap/
│   │   ├── config/
│   │   ├── controller/
│   │   │   └── web/
│   │   ├── domain/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   │       ├── clinical/
│   │       └── speech/
│   └── resources/
│       ├── db/migration/
│       ├── static/
│       └── templates/
└── test/
    ├── java/br/com/fiap/arkive/
    └── resources/fixtures/
```

Documentação complementar existente:

- [Arquitetura da solução](docs/arquitetura.md)
- [Cronograma de desenvolvimento](docs/cronograma-desenvolvimento.md)
- [Collection Postman](docs/postman/arkive-collection.json)
- [Modelo de dados Oracle](docs/database/Arkive_modelo_oracle_v6.sql)

## Integrantes

Gustavo Crevelari Monteiro Porto — RM561408

Lucca de Araujo Gomes — RM561996

Rafaela Ferreira Santos — RM561671

Victor Sabelli Rocha Batista — RM566224
