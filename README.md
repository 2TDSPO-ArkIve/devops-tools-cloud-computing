# ArkIve — DevOps Tools & Cloud Computing

Esta é a entrega da disciplina FIAP DevOps Tools & Cloud Computing — 3º Sprint, uma cópia independente da aplicação Spring Boot ArkIve (originalmente desenvolvida na disciplina Java Advanced), adaptada e reimplantada com uma arquitetura de nuvem provisionada via Azure CLI.

## Descrição da Solução

A ArkIve é uma plataforma de apoio clínico veterinário. A aplicação centraliza dados de animais, responsáveis, clínicas, consultas, diagnósticos, prescrições e adesão ao tratamento, oferecendo uma visão longitudinal da jornada de saúde de cada pet.

Um motor de inteligência artificial atua apenas como apoio assistivo — produz hipóteses, severidade, confiança e insights durante a consulta. Ele não substitui o veterinário, não confirma diagnósticos de forma autônoma e não prescreve medicamentos; a conclusão clínica final é sempre do profissional.

## Benefícios para o Negócio

- histórico clínico centralizado e longitudinal por animal, em vez de registros fragmentados entre clínicas e atendimentos;
- acesso mais rápido ao contexto clínico do paciente (consultas, diagnósticos, prescrições e avaliações anteriores);
- apoio à tomada de decisão do veterinário durante o atendimento;
- rastreabilidade de consultas, diagnósticos e prescrições ao longo do tempo;
- ganho operacional para clínicas veterinárias, com redução de retrabalho no registro e na busca de informações do paciente.

## Arquitetura da Solução

A arquitetura desta Sprint é de **infraestrutura em nuvem**, não de camadas internas da aplicação. Os recursos Azure envolvidos são:

- **Azure Resource Group** (`rg-arkive-rm561408`) — agrupa e isola todos os recursos desta entrega, permitindo criação e exclusão como uma unidade;
- **Azure App Service Plan** (`plan-arkive-rm561408`, Linux, SKU `F1`) — o plano de computação (tier) que hospeda o Web App;
- **Azure App Service / Web App** (`webapp-arkive-rm561408`) — executa o JAR Spring Boot diretamente sobre o runtime Java 17 gerenciado do App Service Linux, sem imagem de container e sem Docker;
- **Azure SQL logical server** (`sqlserver-arkive-rm561408`) — servidor lógico gerenciado que hospeda o banco;
- **Azure SQL Database** (`arkivedb`, PaaS, service objective `Basic`) — o banco relacional da aplicação, com schema versionado pelo Flyway.

Comunicação entre os componentes:

- o usuário (navegador ou cliente REST) acessa o Web App via **HTTPS**, na URL pública do App Service;
- o Web App se conecta ao Azure SQL Database via **JDBC sobre TLS** (`encrypt=true`), usando o driver `mssql-jdbc`;
- nenhum componente desta arquitetura é containerizado — a aplicação roda como um JAR executável padrão dentro do runtime Java nativo do App Service Linux.

> Diagrama de infraestrutura Azure (a ser adicionado):
>
> ![Arquitetura Azure do ArkIve](docs/images/arquitetura-azure.png)

## Tecnologias Utilizadas

| Tecnologia | Papel nesta entrega |
| --- | --- |
| Java 17 | Linguagem e runtime da aplicação |
| Spring Boot | Framework da aplicação (web, segurança, dados) |
| Spring MVC / Thymeleaf | Controllers REST e frontend web server-side |
| Spring Data JPA / Hibernate | Persistência; `ddl-auto=validate` (não gera schema) |
| Spring Security | Autenticação e autorização por perfil |
| Flyway | Versionamento e evolução do schema do banco |
| Maven Wrapper | Build reproduzível sem instalação manual do Maven |
| Azure App Service (Linux) | Hospedagem da aplicação, sem containers |
| Azure SQL Database | Banco de dados relacional gerenciado (PaaS) |
| Azure CLI | Provisionamento de toda a infraestrutura desta Sprint |

Integrações opcionais da própria aplicação (não são infraestrutura exigida por esta Sprint): Azure Speech SDK (transcrição de áudio clínico), um motor clínico externo consultado via `RestClient`, Apache PDFBox (geração de PDFs) e Springdoc OpenAPI (documentação da API). A aplicação inicia e funciona normalmente sem nenhuma delas configurada.

## Estratégia de Deploy

Fluxo adotado, do zero até a aplicação pública:

```text
git clone → az login → provisionamento via Azure CLI →
configuração das variáveis de ambiente do App Service →
build Maven (./mvnw clean package) →
deploy manual do JAR (az webapp deploy) →
URL pública do App Service
```

## Banco de Dados em Nuvem

- **Azure SQL Database** é o banco de dados entregue e usado em nuvem (perfil Spring `azure`).
- **H2** é usado **somente pelos testes automatizados** (`./mvnw test`); não é o banco entregue nem o banco de produção desta Sprint.
- O **Flyway** é o dono do versionamento e da evolução do schema; as migrations `V1` a `V6` (`src/main/resources/db/migration`) foram reescritas em **T-SQL**, compatíveis com Azure SQL / SQL Server, e devem ser executadas em sequência, do zero, contra um banco Azure SQL vazio.
- O Hibernate usa `spring.jpa.hibernate.ddl-auto=validate`: ele **valida** o schema criado pelo Flyway e nunca cria ou altera tabelas.
- **Importante:** a configuração **não** usa `spring.flyway.baseline-on-migrate=true`. Um Azure SQL Database recém-provisionado (vazio) deve executar as migrations `V1` a `V6` normalmente, do zero, sem baseline.
- Um script `script_bd.sql` (deliverável específico da rubrica desta disciplina, com o DDL documentado) será adicionado à raiz do repositório. O Flyway continua sendo o mecanismo real de versionamento em tempo de execução da aplicação; `script_bd.sql` é a documentação/entrega do DDL, não substitui as migrations.


## Pré-requisitos

- JDK 17 disponível em `JAVA_HOME` ou no `PATH`;
- Azure CLI instalado e autenticado (`az login`), com uma assinatura Azure ativa;
- acesso à internet no primeiro uso do Maven Wrapper, para baixar dependências.

## Variáveis de Ambiente

Nenhum segredo é commitado neste repositório. Toda credencial existe apenas no shell local do operador ou nas configurações do Azure App Service.

### Credenciais de infraestrutura (Azure CLI)

A senha do administrador do Azure SQL Server nunca é hardcoded; ela é sempre lida do ambiente:

```bash
export SQL_ADMIN_PASSWORD="<defina-localmente>"
```

### Variáveis da aplicação (App Service)



## Provisionamento da Infraestrutura via Azure CLI

Toda a infraestrutura é criada por Azure CLI, sem Portal.

```bash
az login
```

**Resource Group** — `scripts/01-resource-group.sh`

```bash
az group create \
  --name rg-arkive-rm561408 \
  --location chilecentral
```

**Azure SQL logical server** — `scripts/02-azure-sql.sh`

```bash
az sql server create \
  --name sqlserver-arkive-rm561408 \
  --resource-group rg-arkive-rm561408 \
  --location chilecentral \
  --admin-user fiapadmin \
  --admin-password "$SQL_ADMIN_PASSWORD"
```

**Azure SQL Database** — `scripts/02-azure-sql.sh`

```bash
az sql db create \
  --resource-group rg-arkive-rm561408 \
  --server sqlserver-arkive-rm561408 \
  --name arkivedb \
  --service-objective Basic
```

**Firewall do Azure SQL** — `scripts/02-azure-sql.sh`

```bash
az sql server firewall-rule create \
  --resource-group rg-arkive-rm561408 \
  --server sqlserver-arkive-rm561408 \
  --name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

Opcional (mesmo script, somente se `MY_PUBLIC_IP` estiver definido no shell), para permitir acesso direto ao Azure SQL a partir desta máquina durante a demonstração de CRUD:

```bash
az sql server firewall-rule create \
  --resource-group rg-arkive-rm561408 \
  --server sqlserver-arkive-rm561408 \
  --name AllowLocalMachine \
  --start-ip-address "$MY_PUBLIC_IP" \
  --end-ip-address "$MY_PUBLIC_IP"
```

**App Service Plan** — `scripts/03-app-service-plan.sh`

```bash
az appservice plan create \
  --name plan-arkive-rm561408 \
  --resource-group rg-arkive-rm561408 \
  --location chilecentral \
  --sku F1 \
  --is-linux
```

**Web App** — `scripts/04-webapp.sh`

```bash
az webapp create \
  --resource-group rg-arkive-rm561408 \
  --plan plan-arkive-rm561408 \
  --name webapp-arkive-rm561408 \
  --runtime "JAVA:17-java17"
```

**App Settings** — `scripts/04-webapp.sh`

```bash
az webapp config appsettings set \
  --resource-group rg-arkive-rm561408 \
  --name webapp-arkive-rm561408 \
  --settings \
    SPRING_PROFILES_ACTIVE="azure" \
    SPRING_DATASOURCE_URL="jdbc:sqlserver://sqlserver-arkive-rm561408.database.windows.net:1433;database=arkivedb;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;" \
    SPRING_DATASOURCE_USERNAME="fiapadmin" \
    SPRING_DATASOURCE_PASSWORD="$SQL_ADMIN_PASSWORD"
```

O mesmo script aplica, condicionalmente, três blocos opcionais de `az webapp config appsettings set` (motor clínico externo, transcrição Azure Speech e bootstrap do primeiro SysAdmin) — cada um só é executado se as variáveis correspondentes já estiverem definidas no shell (ver seção anterior).



## Deploy no Azure App Service

Deploy manual do JAR via Azure CLI — `scripts/05-deploy.sh`:

```bash
az webapp deploy \
  --resource-group rg-arkive-rm561408 \
  --name webapp-arkive-rm561408 \
  --src-path target/arkive-0.0.1-SNAPSHOT.jar \
  --type jar
```

Após o deploy, a aplicação fica disponível em:

```text
https://webapp-arkive-rm561408.azurewebsites.net
```

## Validação da Aplicação e Persistência

Roteiro de validação a ser seguido na demonstração/gravação:

1. acessar a URL pública do App Service;
2. **CREATE**: criar um registro na primeira tabela CORE (ver [CRUD Avaliado](#crud-avaliado));
3. verificar com `SELECT` diretamente no Azure SQL que o registro foi persistido;
4. **READ**: consultar o registro pela aplicação;
5. **UPDATE**: alterar um campo do registro pela aplicação;
6. verificar com `SELECT` diretamente no Azure SQL que a alteração foi persistida;
7. **DELETE**: remover o registro pela aplicação;
8. verificar com `SELECT` diretamente no Azure SQL o resultado da exclusão (remoção física ou `ST_ATIVO = 'N'`, conforme a tabela);
9. repetir os passos 2–8 para a segunda tabela CORE relacionada;
10. garantir que pelo menos dois registros significativos foram demonstrados em cada tabela.

## Exclusão dos Recursos

Após os testes/demonstração, todo o Resource Group pode ser removido com Azure CLI — `scripts/06-delete-resource-group.sh`, que pede confirmação interativa (digitar `DELETE`) antes de executar:

```bash
az group delete \
  --name rg-arkive-rm561408 \
  --yes
```

## Estrutura do Repositório

```text
scripts/
├── 01-resource-group.sh
├── 02-azure-sql.sh
├── 03-app-service-plan.sh
├── 04-webapp.sh
├── 05-deploy.sh
├── 06-delete-resource-group.sh
└── env.example.sh
script_bd.sql            (a adicionar: DDL documentado, entrega da rubrica)
src/
├── main/
│   ├── java/br/com/fiap/arkive/
│   │   ├── bootstrap/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── domain/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   └── resources/
│       ├── db/migration/   (V1–V6, T-SQL para Azure SQL)
│       ├── static/
│       └── templates/
└── test/
    ├── java/br/com/fiap/arkive/
    └── resources/fixtures/
```

## Integrantes

Gustavo Crevelari Monteiro Porto — RM561408

Lucca de Araujo Gomes — RM561996

Rafaela Ferreira Santos — RM561671

Victor Sabelli Rocha Batista — RM566224
