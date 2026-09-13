/*
 * ArkIve
 * FIAP - DevOps Tools & Cloud Computing - 3o Sprint
 *
 * Banco alvo: Azure SQL Database | Dialeto: T-SQL
 *
 * Este arquivo documenta o DDL consolidado do schema FINAL da aplicacao apos as
 * migrations Flyway V1 a V7 (src/main/resources/db/migration). script_bd.sql e a
 * entrega/documentacao do DDL exigida pela rubrica de DevOps Tools & Cloud
 * Computing; o Flyway continua sendo o mecanismo real usado pela aplicacao
 * Spring Boot para criar e evoluir o schema em tempo de execucao
 * (spring.flyway.enabled=true, ddl-auto=validate). Este script NAO substitui as
 * migrations Flyway.
 *
 * V7 foi adicionada para recriar TB_ARKIVE_ADESAO_PRESCRICAO (ja definida em V1)
 * apos essa tabela ter sido reportada ausente pela validacao do Hibernate contra
 * o Azure SQL Database real, com V1-V6 ja aplicadas e imutaveis nesse ponto; V7
 * so cria a tabela caso ela ainda nao exista, entao o resultado final e
 * identico ao que V1 ja descrevia.
 * ============================================================================
 * PARTE 1 - CADASTROS E CATALOGOS DE APOIO
 * ----------------------------------------------------------------------------
 * Tabelas de dominio/cadastro que sustentam o nucleo clinico (especie, raca,
 * responsaveis, clinicas, veterinarios e catalogos de doenca/protocolo).
 * Nao sao o centro do modelo, mas sao pre-requisito para as chaves
 * estrangeiras do NUCLEO CLINICO (PARTE 3).
 * ============================================================================
 */

-- ============================================================
-- TB_ARKIVE_ESPECIE
-- Catalogo de especies animais (ex.: Cao, Gato, Ave).
-- ============================================================
CREATE TABLE TB_ARKIVE_ESPECIE (
    ID_ESPECIE BIGINT IDENTITY(1,1) NOT NULL,          -- Identificador unico da especie
    NM_ESPECIE VARCHAR(50) NOT NULL,
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',              -- S = ativo, N = inativo (exclusao logica)

    CONSTRAINT PK_ARKIVE_ESPECIE PRIMARY KEY (ID_ESPECIE),
    CONSTRAINT UQ_ARKIVE_ESPECIE_NOME UNIQUE (NM_ESPECIE),
    CONSTRAINT CK_ARKIVE_ESPECIE_ATIVO CHECK (ST_ATIVO IN ('S', 'N'))
);

-- ============================================================
-- TB_ARKIVE_RACA
-- Catalogo de racas, sempre vinculadas a uma especie.
-- ============================================================
CREATE TABLE TB_ARKIVE_RACA (
    ID_RACA BIGINT IDENTITY(1,1) NOT NULL,              -- Identificador unico da raca
    NM_RACA VARCHAR(50) NOT NULL,
    ID_ESPECIE BIGINT NOT NULL,
    TP_PORTE VARCHAR(20) NULL,                          -- PEQUENO, MEDIO ou GRANDE, quando informado
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_RACA PRIMARY KEY (ID_RACA),
    CONSTRAINT UQ_ARKIVE_RACA_ESPECIE_NOME UNIQUE (ID_ESPECIE, NM_RACA),
    CONSTRAINT CK_ARKIVE_RACA_PORTE CHECK (TP_PORTE IS NULL OR TP_PORTE IN ('PEQUENO', 'MEDIO', 'GRANDE')),
    CONSTRAINT CK_ARKIVE_RACA_ATIVO CHECK (ST_ATIVO IN ('S', 'N')),
    CONSTRAINT FK_RACA_ESPECIE FOREIGN KEY (ID_ESPECIE)
        REFERENCES TB_ARKIVE_ESPECIE (ID_ESPECIE)
);

CREATE INDEX IX_ARKIVE_RACA_ESPECIE ON TB_ARKIVE_RACA (ID_ESPECIE);

-- ============================================================
-- TB_ARKIVE_RESPONSAVEL
-- Pessoa, instituicao ou contato que pode se relacionar com um animal
-- (tutor, funcionario de clinica/zoo, ONG, instituicao, etc.).
-- ============================================================
CREATE TABLE TB_ARKIVE_RESPONSAVEL (
    ID_RESPONSAVEL BIGINT IDENTITY(1,1) NOT NULL,       -- Identificador unico do responsavel
    NM_RESPONSAVEL VARCHAR(50) NOT NULL,
    DC_CPF_RG VARCHAR(20) NOT NULL,                     -- CPF, CNPJ ou outro documento
    DS_EMAIL VARCHAR(200) NULL,
    NR_CONTATO VARCHAR(20) NULL,
    TP_RESPONSAVEL VARCHAR(30) NOT NULL,                -- TUTOR, FUNCIONARIO_CLINICA, FUNCIONARIO_ZOO, ONG, INSTITUICAO, OUTRO
    DT_CADASTRO DATE NOT NULL DEFAULT GETDATE(),
    ST_NOTIFICACAO CHAR(1) NOT NULL DEFAULT 'S',        -- Aceita notificacoes: S ou N
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_RESPONSAVEL PRIMARY KEY (ID_RESPONSAVEL),
    CONSTRAINT UQ_ARKIVE_RESPONSAVEL_DOC UNIQUE (DC_CPF_RG),
    CONSTRAINT CK_ARKIVE_RESP_TIPO CHECK (TP_RESPONSAVEL IN ('TUTOR', 'FUNCIONARIO_CLINICA', 'FUNCIONARIO_ZOO', 'ONG', 'INSTITUICAO', 'OUTRO')),
    CONSTRAINT CK_ARKIVE_RESP_NOTIF CHECK (ST_NOTIFICACAO IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_RESP_ATIVO CHECK (ST_ATIVO IN ('S', 'N'))
);

-- ============================================================
-- TB_ARKIVE_CLINICA
-- Clinica ou hospital veterinario parceiro.
-- ============================================================
CREATE TABLE TB_ARKIVE_CLINICA (
    ID_CLINICA BIGINT IDENTITY(1,1) NOT NULL,           -- Identificador unico da clinica
    NM_CLINICA VARCHAR(150) NOT NULL,
    DC_CNPJ VARCHAR(14) NOT NULL,
    DS_ENDERECO VARCHAR(255) NULL,
    NR_CONTATO VARCHAR(20) NULL,
    DS_EMAIL VARCHAR(200) NULL,
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_CLINICA PRIMARY KEY (ID_CLINICA),
    CONSTRAINT UQ_ARKIVE_CLINICA_CNPJ UNIQUE (DC_CNPJ),
    CONSTRAINT CK_ARKIVE_CLINICA_ATIVO CHECK (ST_ATIVO IN ('S', 'N'))
);

-- ============================================================
-- TB_ARKIVE_RESPONSAVEL_CLINICA
-- Relacionamento N:N entre responsaveis e clinicas (vinculo operacional,
-- clinico ou de atendimento).
-- ============================================================
CREATE TABLE TB_ARKIVE_RESPONSAVEL_CLINICA (
    ID_RESPONSAVEL BIGINT NOT NULL,
    ID_CLINICA BIGINT NOT NULL,
    DT_VINCULO DATE NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_ARKIVE_RESP_CLINICA PRIMARY KEY (ID_RESPONSAVEL, ID_CLINICA),
    CONSTRAINT FK_RESPCLDS_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL),
    CONSTRAINT FK_RESPCLDS_CLINICA FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA)
);

CREATE INDEX IX_ARKIVE_RESPCLDS_CLINICA ON TB_ARKIVE_RESPONSAVEL_CLINICA (ID_CLINICA);

-- ============================================================
-- TB_ARKIVE_VETERINARIO
-- Profissional veterinario, vinculado ou nao a uma clinica (autonomo).
-- ============================================================
CREATE TABLE TB_ARKIVE_VETERINARIO (
    ID_VETERINARIO BIGINT IDENTITY(1,1) NOT NULL,       -- Identificador unico do veterinario
    NM_VETERINARIO VARCHAR(50) NOT NULL,
    DC_CRMV VARCHAR(10) NOT NULL,                       -- Registro profissional
    DS_ESPECIALIDADE VARCHAR(50) NULL,
    DS_EMAIL VARCHAR(200) NULL,
    ID_CLINICA BIGINT NULL,                             -- Nulo quando o veterinario e autonomo
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_VETERINARIO PRIMARY KEY (ID_VETERINARIO),
    CONSTRAINT UQ_ARKIVE_CRMV UNIQUE (DC_CRMV),
    CONSTRAINT CK_ARKIVE_VET_ATIVO CHECK (ST_ATIVO IN ('S', 'N')),
    CONSTRAINT FK_VET_CLINICA FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA)
);

CREATE INDEX IX_ARKIVE_VET_CLINICA ON TB_ARKIVE_VETERINARIO (ID_CLINICA);

-- ============================================================
-- TB_ARKIVE_CATEGORIA_DOENCA
-- Dominio de categorias clinicas de doencas.
-- ============================================================
CREATE TABLE TB_ARKIVE_CATEGORIA_DOENCA (
    ID_CATEGORIA BIGINT IDENTITY(1,1) NOT NULL,         -- Identificador unico da categoria
    NM_CATEGORIA VARCHAR(100) NOT NULL,
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_CATEGORIA_DOENCA PRIMARY KEY (ID_CATEGORIA),
    CONSTRAINT UQ_ARKIVE_CATEGORIA_NOME UNIQUE (NM_CATEGORIA),
    CONSTRAINT CK_ARKIVE_CATDOENCA_ATIVO CHECK (ST_ATIVO IN ('S', 'N'))
);

-- ============================================================
-- TB_ARKIVE_DOENCA
-- Catalogo de doencas e condicoes clinicas usadas no diagnostico.
-- ============================================================
CREATE TABLE TB_ARKIVE_DOENCA (
    ID_DOENCA BIGINT IDENTITY(1,1) NOT NULL,            -- Identificador unico da doenca
    NM_DOENCA VARCHAR(100) NOT NULL,
    ID_CATEGORIA BIGINT NULL,
    DS_DOENCA VARCHAR(MAX) NULL,
    CD_CID_VET VARCHAR(20) NULL,                        -- Codigo veterinario, quando aplicavel
    DS_SINTOMAS VARCHAR(MAX) NULL,                      -- Palavras-chave usadas pelo motor de regras
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_DOENCA PRIMARY KEY (ID_DOENCA),
    CONSTRAINT UQ_ARKIVE_DOENCA_NOME UNIQUE (NM_DOENCA),
    CONSTRAINT CK_ARKIVE_DOENCA_ATIVO CHECK (ST_ATIVO IN ('S', 'N')),
    CONSTRAINT FK_DOENCA_CATEGORIA FOREIGN KEY (ID_CATEGORIA)
        REFERENCES TB_ARKIVE_CATEGORIA_DOENCA (ID_CATEGORIA)
);

CREATE INDEX IX_ARKIVE_DOENCA_CATEGORIA ON TB_ARKIVE_DOENCA (ID_CATEGORIA);

-- ============================================================
-- TB_ARKIVE_PREDISPOSICAO
-- Relacionamento N:N entre especie/raca e doencas com predisposicao
-- genetica conhecida.
-- ============================================================
CREATE TABLE TB_ARKIVE_PREDISPOSICAO (
    ID_PREDISPOSICAO BIGINT IDENTITY(1,1) NOT NULL,
    ID_ESPECIE BIGINT NOT NULL,
    ID_RACA BIGINT NULL,                                -- Nulo quando a predisposicao vale para toda a especie
    ID_DOENCA BIGINT NOT NULL,

    -- Coluna computada persistida usada para preservar a regra de unicidade
    -- quando ID_RACA for nulo.
    ID_RACA_COALESCE AS (ISNULL(ID_RACA, 0)) PERSISTED,

    CONSTRAINT PK_ARKIVE_PREDISPOSICAO PRIMARY KEY (ID_PREDISPOSICAO),
    CONSTRAINT FK_PRED_ESPECIE FOREIGN KEY (ID_ESPECIE)
        REFERENCES TB_ARKIVE_ESPECIE (ID_ESPECIE),
    CONSTRAINT FK_PRED_RACA FOREIGN KEY (ID_RACA)
        REFERENCES TB_ARKIVE_RACA (ID_RACA),
    CONSTRAINT FK_PRED_DOENCA FOREIGN KEY (ID_DOENCA)
        REFERENCES TB_ARKIVE_DOENCA (ID_DOENCA)
);

CREATE UNIQUE INDEX UX_ARKIVE_PREDISPOSICAO
    ON TB_ARKIVE_PREDISPOSICAO (ID_ESPECIE, ID_RACA_COALESCE, ID_DOENCA);

CREATE INDEX IX_ARKIVE_PRED_DOENCA ON TB_ARKIVE_PREDISPOSICAO (ID_DOENCA);

-- ============================================================
-- TB_ARKIVE_PROTOCOLO_PREVENTIVO
-- Template de protocolos preventivos (vacina, vermifugo, check-up,
-- antiparasitario) por especie ou raca.
-- ============================================================
CREATE TABLE TB_ARKIVE_PROTOCOLO_PREVENTIVO (
    ID_PROTOCOLO BIGINT IDENTITY(1,1) NOT NULL,
    NM_PROTOCOLO VARCHAR(50) NOT NULL,
    TP_PROTOCOLO VARCHAR(50) NOT NULL,                  -- VACINA, VERMIFUGO, CHECK-UP, ANTIPARASITARIO
    DS_PROTOCOLO VARCHAR(MAX) NULL,
    NR_INTERVALO INT NOT NULL,                          -- Intervalo recomendado, em dias
    NR_IDADE_MIN INT NOT NULL DEFAULT 0,                 -- Idade minima recomendada, em meses
    ID_ESPECIE BIGINT NULL,                             -- Nulo quando o protocolo e geral
    ID_RACA BIGINT NULL,
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_PROTOCOLO_PREV PRIMARY KEY (ID_PROTOCOLO),
    CONSTRAINT CK_ARKIVE_PROT_TIPO CHECK (TP_PROTOCOLO IN ('VACINA', 'VERMIFUGO', 'CHECK-UP', 'ANTIPARASITARIO')),
    CONSTRAINT CK_ARKIVE_PROT_INTERVALO CHECK (NR_INTERVALO > 0),
    CONSTRAINT CK_ARKIVE_PROT_IDADE CHECK (NR_IDADE_MIN >= 0),
    CONSTRAINT CK_ARKIVE_PROT_ATIVO CHECK (ST_ATIVO IN ('S', 'N')),
    CONSTRAINT FK_PROT_ESPECIE FOREIGN KEY (ID_ESPECIE)
        REFERENCES TB_ARKIVE_ESPECIE (ID_ESPECIE),
    CONSTRAINT FK_PROT_RACA FOREIGN KEY (ID_RACA)
        REFERENCES TB_ARKIVE_RACA (ID_RACA)
);

CREATE INDEX IX_ARKIVE_PROT_ESPECIE ON TB_ARKIVE_PROTOCOLO_PREVENTIVO (ID_ESPECIE);
CREATE INDEX IX_ARKIVE_PROT_RACA ON TB_ARKIVE_PROTOCOLO_PREVENTIVO (ID_RACA);

/*
 * ============================================================================
 * PARTE 2 - SEGURANCA E ACESSO (NAO E O MODELO CENTRAL)
 * ----------------------------------------------------------------------------
 * Tabela de credenciais/perfil de acesso. Existe para autenticacao e
 * autorizacao da aplicacao, mas NAO representa o dominio clinico avaliado
 * por esta disciplina - ver NUCLEO CLINICO na PARTE 3.
 * ============================================================================
 */

-- ============================================================
-- TB_ARKIVE_USUARIO
-- Credenciais de acesso dos perfis SYSADMIN, ADMIN_CLINICA, VETERINARIO
-- e RESPONSAVEL. Estado final apos V1 (base) + V2 (perfis/clinica) + V3
-- (ciclo de vida de senha).
-- ============================================================
CREATE TABLE TB_ARKIVE_USUARIO (
    ID_USUARIO BIGINT IDENTITY(1,1) NOT NULL,           -- Identificador unico do usuario
    TP_USUARIO VARCHAR(20) NOT NULL,                    -- SYSADMIN, ADMIN_CLINICA, VETERINARIO ou RESPONSAVEL
    NM_USUARIO VARCHAR(150) NOT NULL,                    -- Nome de exibicao para autenticacao/administracao
    ID_RESPONSAVEL BIGINT NULL,                         -- Preenchido somente quando TP_USUARIO = RESPONSAVEL
    ID_VETERINARIO BIGINT NULL,                         -- Preenchido somente quando TP_USUARIO = VETERINARIO
    ID_CLINICA BIGINT NULL,                             -- Preenchido somente quando TP_USUARIO = ADMIN_CLINICA
    DS_LOGIN VARCHAR(200) NOT NULL,
    DS_SENHA_HASH VARCHAR(255) NOT NULL,                -- Hash BCrypt; nunca texto puro
    DT_CADASTRO DATETIME2 NOT NULL DEFAULT GETDATE(),
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',
    ST_TROCA_SENHA CHAR(1) NOT NULL DEFAULT 'N',        -- S = deve trocar a senha no proximo acesso
    DT_ULTIMA_TROCA_SENHA DATETIME2 NULL,

    CONSTRAINT PK_ARKIVE_USUARIO PRIMARY KEY (ID_USUARIO),
    CONSTRAINT UQ_ARKIVE_USUARIO_LOGIN UNIQUE (DS_LOGIN),
    CONSTRAINT CK_ARKIVE_USUARIO_TIPO CHECK (TP_USUARIO IN ('SYSADMIN', 'ADMIN_CLINICA', 'VETERINARIO', 'RESPONSAVEL')),
    CONSTRAINT CK_ARKIVE_USUARIO_ATIVO CHECK (ST_ATIVO IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_USUARIO_TROCA_SENHA CHECK (ST_TROCA_SENHA IN ('S', 'N')),
    -- Garante que cada perfil preenche exatamente o vinculo esperado (e nenhum outro).
    CONSTRAINT CK_ARKIVE_USUARIO_REF CHECK (
        (
            TP_USUARIO = 'SYSADMIN'
            AND ID_RESPONSAVEL IS NULL
            AND ID_VETERINARIO IS NULL
            AND ID_CLINICA IS NULL
        )
        OR
        (
            TP_USUARIO = 'ADMIN_CLINICA'
            AND ID_RESPONSAVEL IS NULL
            AND ID_VETERINARIO IS NULL
            AND ID_CLINICA IS NOT NULL
        )
        OR
        (
            TP_USUARIO = 'VETERINARIO'
            AND ID_RESPONSAVEL IS NULL
            AND ID_VETERINARIO IS NOT NULL
            AND ID_CLINICA IS NULL
        )
        OR
        (
            TP_USUARIO = 'RESPONSAVEL'
            AND ID_RESPONSAVEL IS NOT NULL
            AND ID_VETERINARIO IS NULL
            AND ID_CLINICA IS NULL
        )
    ),
    CONSTRAINT FK_USUARIO_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL),
    CONSTRAINT FK_USUARIO_VET FOREIGN KEY (ID_VETERINARIO)
        REFERENCES TB_ARKIVE_VETERINARIO (ID_VETERINARIO),
    CONSTRAINT FK_USUARIO_CLINICA FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA)
);

-- ID_RESPONSAVEL e ID_VETERINARIO sao vinculos 1:1 opcionais (a maioria das
-- linhas tem um dos dois NULL). Ao contrario do Oracle, um UNIQUE constraint
-- comum do SQL Server so tolera UM NULL por tabela - o que quebraria assim
-- que existisse um segundo usuario do mesmo tipo. Indices unicos filtrados
-- (unicos apenas onde o valor esta presente) reproduzem o comportamento do
-- Oracle, em que NULLs nao sao comparados entre si.
CREATE UNIQUE INDEX UQ_ARKIVE_USUARIO_RESP ON TB_ARKIVE_USUARIO (ID_RESPONSAVEL)
    WHERE ID_RESPONSAVEL IS NOT NULL;
CREATE UNIQUE INDEX UQ_ARKIVE_USUARIO_VET ON TB_ARKIVE_USUARIO (ID_VETERINARIO)
    WHERE ID_VETERINARIO IS NOT NULL;

CREATE INDEX IX_ARKIVE_USUARIO_CLINICA ON TB_ARKIVE_USUARIO (ID_CLINICA);

/*
 * ============================================================================
 * PARTE 3 - NUCLEO CLINICO (CORE)
 * ----------------------------------------------------------------------------
 * Este e o modelo de dados central da aplicacao ArkIve, avaliado pela
 * disciplina: o paciente (animal), o atendimento (consulta), o diagnostico,
 * a prescricao e a adesao ao tratamento, a avaliacao de bem-estar e o
 * registro de eventos da jornada do paciente. E aqui que o CRUD completo
 * exigido pela rubrica deve ser demonstrado.
 * ============================================================================
 */

-- ============================================================
-- TB_ARKIVE_ANIMAL  [CORE]
-- Representa o paciente animal acompanhado pela plataforma. Nao exige
-- responsavel individual direto: pode ser cadastrado por clinica, hospital,
-- ONG, abrigo ou zoologico. Estado final apos V1 + V5 (veterinario que
-- cadastrou) + V6 (data de nascimento).
-- ============================================================
CREATE TABLE TB_ARKIVE_ANIMAL (
    ID_ANIMAL BIGINT IDENTITY(1,1) NOT NULL,            -- Identificador unico do animal
    NM_ANIMAL VARCHAR(50) NOT NULL,
    ID_ESPECIE BIGINT NOT NULL,
    ID_RACA BIGINT NULL,                                -- Raca, quando conhecida
    DS_SEXO CHAR(1) NULL,                               -- M ou F
    DS_CASTRADO CHAR(1) NOT NULL DEFAULT 'N',           -- S ou N
    DT_NASCIMENTO DATE NULL,
    ID_CLINICA BIGINT NULL,                             -- Clinica/instituicao que cadastrou o animal
    ID_VETERINARIO_CADASTRO BIGINT NULL,                -- Veterinario que cadastrou diretamente; nulo se cadastro administrativo/legado
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_ANIMAL PRIMARY KEY (ID_ANIMAL),
    CONSTRAINT CK_ARKIVE_ANIMAL_ATIVO CHECK (ST_ATIVO IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_ANIMAL_SEXO CHECK (DS_SEXO IN ('M', 'F')),
    CONSTRAINT CK_ARKIVE_ANIMAL_CASTRADO CHECK (DS_CASTRADO IN ('S', 'N')),
    CONSTRAINT FK_ANIMAL_ESPECIE FOREIGN KEY (ID_ESPECIE)
        REFERENCES TB_ARKIVE_ESPECIE (ID_ESPECIE),
    CONSTRAINT FK_ANIMAL_RACA FOREIGN KEY (ID_RACA)
        REFERENCES TB_ARKIVE_RACA (ID_RACA),
    CONSTRAINT FK_ANIMAL_CLINICA_CAD FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA),
    CONSTRAINT FK_ANIMAL_VET_CADASTRO FOREIGN KEY (ID_VETERINARIO_CADASTRO)
        REFERENCES TB_ARKIVE_VETERINARIO (ID_VETERINARIO)
);

CREATE INDEX IX_ARKIVE_ANIMAL_ESPECIE ON TB_ARKIVE_ANIMAL (ID_ESPECIE);
CREATE INDEX IX_ARKIVE_ANIMAL_RACA ON TB_ARKIVE_ANIMAL (ID_RACA);
CREATE INDEX IX_ARKIVE_ANIMAL_CLINICA_CAD ON TB_ARKIVE_ANIMAL (ID_CLINICA);
CREATE INDEX IX_ARKIVE_ANIMAL_VET_CADASTRO ON TB_ARKIVE_ANIMAL (ID_VETERINARIO_CADASTRO);

-- ============================================================
-- TB_ARKIVE_RESPONSAVEL_ANIMAL  [CORE]
-- Relacionamento historico entre animais e responsaveis: permite multiplos
-- responsaveis, um responsavel principal e substituicoes ao longo do tempo.
-- ============================================================
CREATE TABLE TB_ARKIVE_RESPONSAVEL_ANIMAL (
    ID_ANIMAL BIGINT NOT NULL,
    ID_RESPONSAVEL BIGINT NOT NULL,
    TP_VINCULO VARCHAR(40) NOT NULL,                    -- TUTOR_LEGAL, CUIDADOR, RESPONSAVEL_CLINICO, RESPONSAVEL_OPERACIONAL, CONTATO_EMERGENCIA
    DT_INICIO DATE NOT NULL DEFAULT GETDATE(),
    DT_FIM DATE NULL,                                   -- Preenchida quando o vinculo e encerrado
    ST_PRINCIPAL CHAR(1) NOT NULL DEFAULT 'N',          -- S = responsavel principal
    ST_ATIVO CHAR(1) NOT NULL DEFAULT 'S',

    CONSTRAINT PK_ARKIVE_ANIMAL_RESP PRIMARY KEY (ID_ANIMAL, ID_RESPONSAVEL, DT_INICIO),
    CONSTRAINT CK_ARKIVE_ANIRESP_VINCULO CHECK (TP_VINCULO IN ('TUTOR_LEGAL', 'CUIDADOR', 'RESPONSAVEL_CLINICO', 'RESPONSAVEL_OPERACIONAL', 'CONTATO_EMERGENCIA')),
    CONSTRAINT CK_ARKIVE_ANIRESP_PRINC CHECK (ST_PRINCIPAL IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_ANIRESP_ATIVO CHECK (ST_ATIVO IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_ANIRESP_DATAS CHECK (DT_FIM IS NULL OR DT_FIM >= DT_INICIO),
    CONSTRAINT FK_ANIRESP_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL),
    CONSTRAINT FK_ANIRESP_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL)
);

CREATE INDEX IX_ARKIVE_ANIRESP_RESP ON TB_ARKIVE_RESPONSAVEL_ANIMAL (ID_RESPONSAVEL);

-- ============================================================
-- TB_ARKIVE_CONSULTA  [CORE]
-- Registro de atendimento veterinario, presencial ou remoto. Estado final
-- apos V1 + V6 (endereco da consulta).
-- ============================================================
CREATE TABLE TB_ARKIVE_CONSULTA (
    ID_CONSULTA BIGINT IDENTITY(1,1) NOT NULL,          -- Identificador unico da consulta
    DT_HORA DATETIME2 NOT NULL,
    TP_MODALIDADE VARCHAR(20) NOT NULL,                 -- PRESENCIAL ou REMOTA
    DS_MOTIVO VARCHAR(MAX) NOT NULL,
    DS_SINTOMAS VARCHAR(MAX) NULL,
    DS_OBSERVACAO VARCHAR(MAX) NULL,
    DS_ENDERECO VARCHAR(255) NULL,                      -- Endereco do atendimento, quando aplicavel
    KG_PESO DECIMAL(6,2) NULL,
    DS_TRANSCRICAO VARCHAR(MAX) NULL,                   -- Texto bruto de transcricao de audio, quando houver
    ST_STATUS CHAR(2) NOT NULL DEFAULT 'AG',            -- AG=Agendada, EP=Em Progresso, AP=Aguardando Parecer, FI=Finalizada, CA=Cancelada
    ID_ANIMAL BIGINT NOT NULL,
    ID_VETERINARIO BIGINT NOT NULL,
    ID_CLINICA BIGINT NULL,                             -- Nulo para atendimento autonomo

    CONSTRAINT PK_ARKIVE_CONSULTA PRIMARY KEY (ID_CONSULTA),
    CONSTRAINT CK_ARKIVE_CONS_MODALIDADE CHECK (TP_MODALIDADE IN ('PRESENCIAL', 'REMOTA')),
    CONSTRAINT CK_ARKIVE_CONS_STATUS CHECK (ST_STATUS IN ('AG', 'EP', 'AP', 'FI', 'CA')),
    CONSTRAINT CK_ARKIVE_CONS_PESO CHECK (KG_PESO IS NULL OR KG_PESO > 0),
    CONSTRAINT FK_CONS_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL),
    CONSTRAINT FK_CONS_VET FOREIGN KEY (ID_VETERINARIO)
        REFERENCES TB_ARKIVE_VETERINARIO (ID_VETERINARIO),
    CONSTRAINT FK_CONS_CLINICA FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA)
);

CREATE INDEX IX_ARKIVE_CONS_ANIMAL ON TB_ARKIVE_CONSULTA (ID_ANIMAL);
CREATE INDEX IX_ARKIVE_CONS_VET ON TB_ARKIVE_CONSULTA (ID_VETERINARIO);
CREATE INDEX IX_ARKIVE_CONS_CLINICA ON TB_ARKIVE_CONSULTA (ID_CLINICA);

-- ============================================================
-- TB_ARKIVE_DIAGNOSTICO  [CORE]
-- Diagnostico gerado em consulta, com possibilidade de insight assistivo
-- de IA. Estado final apos V1 + V4 (fontes consultadas pela IA).
-- ============================================================
CREATE TABLE TB_ARKIVE_DIAGNOSTICO (
    ID_DIAGNOSTICO BIGINT IDENTITY(1,1) NOT NULL,       -- Identificador unico do diagnostico
    DS_DIAGNOSTICO VARCHAR(MAX) NOT NULL,
    TP_SEVERIDADE VARCHAR(20) NULL,                     -- LEVE, MODERADA ou GRAVE
    ST_CONFIRMADO CHAR(1) NOT NULL DEFAULT 'S',         -- S = confirmado pelo veterinario
    DS_INSIGHT_IA VARCHAR(MAX) NULL,                    -- Sugestao gerada pelo motor de regras/IA
    DS_FONTES_IA VARCHAR(MAX) NULL,                     -- Fontes consultadas pelo motor clinico externo (JSON serializado)
    PC_CONFIANCA DECIMAL(5,2) NULL,                     -- Score de confianca do insight, entre 0 e 100
    ST_VALIDACAO_VET CHAR(1) NULL,                      -- Aceite do veterinario sobre o insight: S ou N
    ID_CONSULTA BIGINT NOT NULL,
    ID_DOENCA BIGINT NULL,

    CONSTRAINT PK_ARKIVE_DIAGNOSTICO PRIMARY KEY (ID_DIAGNOSTICO),
    CONSTRAINT CK_ARKIVE_DIAG_SEVERIDADE CHECK (TP_SEVERIDADE IS NULL OR TP_SEVERIDADE IN ('LEVE', 'MODERADA', 'GRAVE')),
    CONSTRAINT CK_ARKIVE_DIAG_CONFIRMADO CHECK (ST_CONFIRMADO IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_DIAG_VALIDACAO CHECK (ST_VALIDACAO_VET IS NULL OR ST_VALIDACAO_VET IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_DIAG_CONFIANCA CHECK (PC_CONFIANCA IS NULL OR PC_CONFIANCA BETWEEN 0 AND 100),
    CONSTRAINT FK_DIAG_CONSULTA FOREIGN KEY (ID_CONSULTA)
        REFERENCES TB_ARKIVE_CONSULTA (ID_CONSULTA),
    CONSTRAINT FK_DIAG_DOENCA FOREIGN KEY (ID_DOENCA)
        REFERENCES TB_ARKIVE_DOENCA (ID_DOENCA)
);

CREATE INDEX IX_ARKIVE_DIAG_CONSULTA ON TB_ARKIVE_DIAGNOSTICO (ID_CONSULTA);
CREATE INDEX IX_ARKIVE_DIAG_DOENCA ON TB_ARKIVE_DIAGNOSTICO (ID_DOENCA);

-- ============================================================
-- TB_ARKIVE_PRESCRICAO  [CORE]
-- Medicamentos e tratamentos prescritos em uma consulta.
-- ============================================================
CREATE TABLE TB_ARKIVE_PRESCRICAO (
    ID_PRESCRICAO BIGINT IDENTITY(1,1) NOT NULL,        -- Identificador unico da prescricao
    NM_MEDICAMENTO VARCHAR(150) NOT NULL,
    DS_DOSAGEM VARCHAR(50) NOT NULL,
    DS_FREQUENCIA VARCHAR(100) NULL,
    TP_VIA_ADMINISTRACAO VARCHAR(50) NULL,              -- ORAL, INJETAVEL, TOPICO, OCULAR, OTOLOGICO, OUTRO
    DT_INICIO DATE NOT NULL,
    DT_FIM DATE NULL,
    DS_INSTRUCOES VARCHAR(MAX) NULL,
    ID_CONSULTA BIGINT NOT NULL,

    CONSTRAINT PK_ARKIVE_PRESCRICAO PRIMARY KEY (ID_PRESCRICAO),
    CONSTRAINT CK_ARKIVE_PRESC_VIA CHECK (TP_VIA_ADMINISTRACAO IS NULL OR TP_VIA_ADMINISTRACAO IN ('ORAL', 'INJETAVEL', 'TOPICO', 'OCULAR', 'OTOLOGICO', 'OUTRO')),
    CONSTRAINT CK_ARKIVE_PRESC_DATAS CHECK (DT_FIM IS NULL OR DT_FIM >= DT_INICIO),
    CONSTRAINT FK_PRESC_CONSULTA FOREIGN KEY (ID_CONSULTA)
        REFERENCES TB_ARKIVE_CONSULTA (ID_CONSULTA)
);

CREATE INDEX IX_ARKIVE_PRESC_CONSULTA ON TB_ARKIVE_PRESCRICAO (ID_CONSULTA);

-- ============================================================
-- TB_ARKIVE_ADESAO_PRESCRICAO  [CORE]
-- Registra se uma prescricao foi seguida ou nao, sustentando analises de
-- adesao terapeutica e abandono de tratamento.
-- Definida em V1; recriada de forma idempotente por V7 apos ausencia
-- constatada via validacao do Hibernate no ambiente Azure SQL real
-- (V1-V6 ja aplicadas e imutaveis nesse ponto). Estrutura identica em ambas.
-- ============================================================
CREATE TABLE TB_ARKIVE_ADESAO_PRESCRICAO (
    ID_ADESAO BIGINT IDENTITY(1,1) NOT NULL,            -- Identificador unico do registro de adesao
    ID_PRESCRICAO BIGINT NOT NULL,
    ID_RESPONSAVEL BIGINT NULL,                         -- Responsavel que informou a adesao, quando aplicavel
    ID_ANIMAL BIGINT NOT NULL,
    DT_REGISTRO DATETIME2 NOT NULL DEFAULT GETDATE(),
    ST_TOMOU CHAR(1) NOT NULL,                          -- S ou N
    DS_OBSERVACAO VARCHAR(MAX) NULL,

    CONSTRAINT PK_ARKIVE_ADESAO_PRESCRICAO PRIMARY KEY (ID_ADESAO),
    CONSTRAINT CK_ARKIVE_ADESAO_TOMOU CHECK (ST_TOMOU IN ('S', 'N')),
    CONSTRAINT FK_ADESAO_PRESCRICAO FOREIGN KEY (ID_PRESCRICAO)
        REFERENCES TB_ARKIVE_PRESCRICAO (ID_PRESCRICAO),
    CONSTRAINT FK_ADESAO_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL),
    CONSTRAINT FK_ADESAO_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL)
);

CREATE INDEX IX_ARKIVE_ADESAO_PRESC ON TB_ARKIVE_ADESAO_PRESCRICAO (ID_PRESCRICAO);
CREATE INDEX IX_ARKIVE_ADESAO_RESP ON TB_ARKIVE_ADESAO_PRESCRICAO (ID_RESPONSAVEL);
CREATE INDEX IX_ARKIVE_ADESAO_ANIMAL ON TB_ARKIVE_ADESAO_PRESCRICAO (ID_ANIMAL);

-- ============================================================
-- TB_ARKIVE_AVALIACAO_BEM_ESTAR  [CORE]
-- Avaliacoes recorrentes de bem-estar do animal (peso, apetite, atividade,
-- comportamento e observacoes longitudinais).
-- ============================================================
CREATE TABLE TB_ARKIVE_AVALIACAO_BEM_ESTAR (
    ID_AVALIACAO_BEM_ESTAR BIGINT IDENTITY(1,1) NOT NULL,
    ID_ANIMAL BIGINT NOT NULL,
    ID_RESPONSAVEL BIGINT NULL,
    ID_VETERINARIO BIGINT NULL,
    ID_CONSULTA BIGINT NULL,
    DT_AVALIACAO DATETIME2 NOT NULL DEFAULT GETDATE(),
    NR_IDADE DECIMAL(5,2) NULL,                         -- Idade real ou aproximada, em anos
    KG_PESO DECIMAL(6,2) NULL,
    DS_APETITE VARCHAR(20) NULL,                        -- SEM APETITE, REDUZIDO, NORMAL, AUMENTADO
    DS_ATIVIDADE VARCHAR(20) NULL,                      -- BAIXA, NORMAL, ALTA
    DS_COMPORTAMENTO VARCHAR(30) NULL,                  -- APATICO, NORMAL, ALTERADO, ANSIOSO, AGRESSIVO
    DS_OBSERVACAO VARCHAR(MAX) NULL,

    CONSTRAINT PK_ARKIVE_AVAL_BEM_ESTAR PRIMARY KEY (ID_AVALIACAO_BEM_ESTAR),
    CONSTRAINT CK_ARKIVE_AVAL_PESO CHECK (KG_PESO IS NULL OR KG_PESO > 0),
    CONSTRAINT CK_ARKIVE_AVAL_IDADE CHECK (NR_IDADE IS NULL OR NR_IDADE > 0),
    CONSTRAINT CK_ARKIVE_AVAL_APETITE CHECK (DS_APETITE IS NULL OR DS_APETITE IN ('SEM APETITE', 'REDUZIDO', 'NORMAL', 'AUMENTADO')),
    CONSTRAINT CK_ARKIVE_AVAL_ATIVIDADE CHECK (DS_ATIVIDADE IS NULL OR DS_ATIVIDADE IN ('BAIXA', 'NORMAL', 'ALTA')),
    CONSTRAINT CK_ARKIVE_AVAL_COMPORT CHECK (DS_COMPORTAMENTO IS NULL OR DS_COMPORTAMENTO IN ('APATICO', 'NORMAL', 'ALTERADO', 'ANSIOSO', 'AGRESSIVO')),
    -- Ao menos uma origem da avaliacao precisa estar preenchida.
    CONSTRAINT CK_ARKIVE_AVAL_AUTOR CHECK (ID_RESPONSAVEL IS NOT NULL OR ID_VETERINARIO IS NOT NULL OR ID_CONSULTA IS NOT NULL),
    CONSTRAINT FK_AVAL_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL),
    CONSTRAINT FK_AVAL_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL),
    CONSTRAINT FK_AVAL_VET FOREIGN KEY (ID_VETERINARIO)
        REFERENCES TB_ARKIVE_VETERINARIO (ID_VETERINARIO),
    CONSTRAINT FK_AVAL_CONSULTA FOREIGN KEY (ID_CONSULTA)
        REFERENCES TB_ARKIVE_CONSULTA (ID_CONSULTA)
);

CREATE INDEX IX_ARKIVE_AVAL_ANIMAL ON TB_ARKIVE_AVALIACAO_BEM_ESTAR (ID_ANIMAL);
CREATE INDEX IX_ARKIVE_AVAL_RESP ON TB_ARKIVE_AVALIACAO_BEM_ESTAR (ID_RESPONSAVEL);
CREATE INDEX IX_ARKIVE_AVAL_VET ON TB_ARKIVE_AVALIACAO_BEM_ESTAR (ID_VETERINARIO);
CREATE INDEX IX_ARKIVE_AVAL_CONSULTA ON TB_ARKIVE_AVALIACAO_BEM_ESTAR (ID_CONSULTA);

-- ============================================================
-- TB_ARKIVE_EVENTO_JORNADA  [CORE]
-- Tabela append-only de eventos relevantes da jornada do responsavel, pet,
-- veterinario e clinica. Serve de ponte para analytics e futura camada de
-- dados.
-- ============================================================
CREATE TABLE TB_ARKIVE_EVENTO_JORNADA (
    ID_EVENTO_JORN BIGINT IDENTITY(1,1) NOT NULL,
    TP_EVENTO VARCHAR(50) NOT NULL,                     -- Ex.: ANIMAL_CADASTRADO, ALERTA_LIDO, CONSULTA_CRIADA
    DT_EVENTO DATETIME2 NOT NULL DEFAULT GETDATE(),
    TP_ORIGEM VARCHAR(30) NOT NULL,                     -- APP, WEB, WHATSAPP, API, IA, SISTEMA
    TP_ATOR VARCHAR(30) NULL,                           -- RESPONSAVEL, VETERINARIO, CLINICA, SISTEMA, IA
    ID_RESPONSAVEL BIGINT NULL,
    ID_VETERINARIO BIGINT NULL,
    ID_ANIMAL BIGINT NULL,
    ID_CLINICA BIGINT NULL,
    DS_CANAL VARCHAR(30) NULL,                          -- Ex.: MOBILE, WEB, WHATSAPP, JOB
    DS_CONTEXTO VARCHAR(MAX) NULL,
    PAYLOAD_JSON VARCHAR(MAX) NULL,                     -- Dados complementares em JSON textual

    CONSTRAINT PK_ARKIVE_EVENTO_JORNADA PRIMARY KEY (ID_EVENTO_JORN),
    CONSTRAINT CK_ARKIVE_EVTJ_ORIGEM CHECK (TP_ORIGEM IN ('APP', 'WEB', 'WHATSAPP', 'API', 'IA', 'SISTEMA')),
    CONSTRAINT CK_ARKIVE_EVTJ_ATOR CHECK (TP_ATOR IS NULL OR TP_ATOR IN ('RESPONSAVEL', 'VETERINARIO', 'CLINICA', 'SISTEMA', 'IA')),
    -- Garante que PAYLOAD_JSON, quando preenchido, e um JSON valido.
    CONSTRAINT CK_ARKIVE_EVTJ_PAYLOAD_JSON CHECK (PAYLOAD_JSON IS NULL OR ISJSON(PAYLOAD_JSON) = 1),
    CONSTRAINT FK_EVTJ_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL),
    CONSTRAINT FK_EVTJ_VET FOREIGN KEY (ID_VETERINARIO)
        REFERENCES TB_ARKIVE_VETERINARIO (ID_VETERINARIO),
    CONSTRAINT FK_EVTJ_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL),
    CONSTRAINT FK_EVTJ_CLINICA FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA)
);

CREATE INDEX IX_ARKIVE_EVTJ_TIPO_DATA ON TB_ARKIVE_EVENTO_JORNADA (TP_EVENTO, DT_EVENTO);
CREATE INDEX IX_ARKIVE_EVTJ_RESP ON TB_ARKIVE_EVENTO_JORNADA (ID_RESPONSAVEL);
CREATE INDEX IX_ARKIVE_EVTJ_VET ON TB_ARKIVE_EVENTO_JORNADA (ID_VETERINARIO);
CREATE INDEX IX_ARKIVE_EVTJ_ANIMAL ON TB_ARKIVE_EVENTO_JORNADA (ID_ANIMAL);
CREATE INDEX IX_ARKIVE_EVTJ_CLINICA ON TB_ARKIVE_EVENTO_JORNADA (ID_CLINICA);

/*
 * ============================================================================
 * PARTE 4 - EVENTOS PREVENTIVOS, ALERTAS E FEEDBACK (APOIO OPERACIONAL)
 * ----------------------------------------------------------------------------
 * Tabelas ligadas ao nucleo clinico (referenciam ANIMAL/CONSULTA), mas de
 * natureza operacional/notificacao, nao clinica em si.
 * ============================================================================
 */

-- ============================================================
-- TB_ARKIVE_EVENTO_PREVENTIVO
-- Instancia de aplicacao ou vencimento de um protocolo preventivo para um
-- animal especifico.
-- ============================================================
CREATE TABLE TB_ARKIVE_EVENTO_PREVENTIVO (
    ID_EVENTO_PREV BIGINT IDENTITY(1,1) NOT NULL,
    DT_APLICACAO DATE NULL,                             -- Obrigatoria apenas quando ST_STATUS = REALIZADO
    DT_PROXIMO DATE NOT NULL,
    ST_STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',  -- REALIZADO, PENDENTE, ATRASADO
    ST_ALERTA CHAR(1) NOT NULL DEFAULT 'N',             -- S = alerta ja disparado
    DS_OBSERVACAO VARCHAR(MAX) NULL,
    ID_ANIMAL BIGINT NOT NULL,
    ID_PROTOCOLO BIGINT NOT NULL,
    ID_CONSULTA BIGINT NULL,

    CONSTRAINT PK_ARKIVE_EVENTO_PREV PRIMARY KEY (ID_EVENTO_PREV),
    CONSTRAINT CK_ARKIVE_EVTPREV_STATUS CHECK (ST_STATUS IN ('REALIZADO', 'PENDENTE', 'ATRASADO')),
    CONSTRAINT CK_ARKIVE_EVTPREV_ALERTA CHECK (ST_ALERTA IN ('S', 'N')),
    CONSTRAINT CK_ARKIVE_EVTPREV_APLIC CHECK (
        (ST_STATUS = 'REALIZADO' AND DT_APLICACAO IS NOT NULL)
        OR
        (ST_STATUS IN ('PENDENTE', 'ATRASADO') AND DT_APLICACAO IS NULL)
    ),
    CONSTRAINT CK_ARKIVE_EVTPREV_DATAS CHECK (DT_APLICACAO IS NULL OR DT_PROXIMO >= DT_APLICACAO),
    CONSTRAINT FK_EVTPREV_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL),
    CONSTRAINT FK_EVTPREV_PROTOCOLO FOREIGN KEY (ID_PROTOCOLO)
        REFERENCES TB_ARKIVE_PROTOCOLO_PREVENTIVO (ID_PROTOCOLO),
    CONSTRAINT FK_EVTPREV_CONSULTA FOREIGN KEY (ID_CONSULTA)
        REFERENCES TB_ARKIVE_CONSULTA (ID_CONSULTA)
);

CREATE INDEX IX_ARKIVE_EVTPREV_ANIMAL ON TB_ARKIVE_EVENTO_PREVENTIVO (ID_ANIMAL);
CREATE INDEX IX_ARKIVE_EVTPREV_PROTOCOLO ON TB_ARKIVE_EVENTO_PREVENTIVO (ID_PROTOCOLO);
CREATE INDEX IX_ARKIVE_EVTPREV_CONSULTA ON TB_ARKIVE_EVENTO_PREVENTIVO (ID_CONSULTA);

-- ============================================================
-- TB_ARKIVE_ALERTA
-- Notificacoes enviadas para responsaveis, clinicas ou equipe operacional.
-- ============================================================
CREATE TABLE TB_ARKIVE_ALERTA (
    ID_ALERTA BIGINT IDENTITY(1,1) NOT NULL,
    TP_ALERTA VARCHAR(50) NOT NULL,                     -- VACINA, RETORNO, MEDICAMENTO, CHECK-UP
    DS_MENSAGEM VARCHAR(MAX) NOT NULL,
    DT_ENVIO DATETIME2 NOT NULL DEFAULT GETDATE(),
    DT_LEITURA DATETIME2 NULL,
    ST_STATUS VARCHAR(20) NOT NULL DEFAULT 'ENVIADO',   -- ENVIADO, LIDO, IGNORADO
    TP_CANAL VARCHAR(20) NOT NULL,                      -- APP, WHATSAPP, EMAIL
    ID_ANIMAL BIGINT NOT NULL,
    ID_RESPONSAVEL BIGINT NULL,
    ID_CLINICA BIGINT NULL,
    ID_EVENTO_PREV BIGINT NULL,

    CONSTRAINT PK_ARKIVE_ALERTA PRIMARY KEY (ID_ALERTA),
    CONSTRAINT CK_ARKIVE_ALERTA_TIPO CHECK (TP_ALERTA IN ('VACINA', 'RETORNO', 'MEDICAMENTO', 'CHECK-UP')),
    CONSTRAINT CK_ARKIVE_ALERTA_STATUS CHECK (ST_STATUS IN ('ENVIADO', 'LIDO', 'IGNORADO')),
    CONSTRAINT CK_ARKIVE_ALERTA_CANAL CHECK (TP_CANAL IN ('APP', 'WHATSAPP', 'EMAIL')),
    CONSTRAINT CK_ARKIVE_ALERTA_LEITURA CHECK (DT_LEITURA IS NULL OR DT_LEITURA >= DT_ENVIO),
    -- Precisa de ao menos um destinatario (responsavel ou clinica).
    CONSTRAINT CK_ARKIVE_ALERTA_DESTINO CHECK (ID_RESPONSAVEL IS NOT NULL OR ID_CLINICA IS NOT NULL),
    CONSTRAINT FK_ALERTA_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL),
    CONSTRAINT FK_ALERTA_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL),
    CONSTRAINT FK_ALERTA_CLINICA FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA),
    CONSTRAINT FK_ALERTA_EVTPREV FOREIGN KEY (ID_EVENTO_PREV)
        REFERENCES TB_ARKIVE_EVENTO_PREVENTIVO (ID_EVENTO_PREV)
);

CREATE INDEX IX_ARKIVE_ALERTA_ANIMAL ON TB_ARKIVE_ALERTA (ID_ANIMAL);
CREATE INDEX IX_ARKIVE_ALERTA_RESP ON TB_ARKIVE_ALERTA (ID_RESPONSAVEL);
CREATE INDEX IX_ARKIVE_ALERTA_CLINICA ON TB_ARKIVE_ALERTA (ID_CLINICA);
CREATE INDEX IX_ARKIVE_ALERTA_EVTPREV ON TB_ARKIVE_ALERTA (ID_EVENTO_PREV);

-- ============================================================
-- TB_ARKIVE_FEEDBACK_NPS
-- Feedback de satisfacao (0 a 5 estrelas), relacionado ao app, clinica,
-- consulta, animal ou responsavel.
-- ============================================================
CREATE TABLE TB_ARKIVE_FEEDBACK_NPS (
    ID_FEEDBACK_NPS BIGINT IDENTITY(1,1) NOT NULL,
    ID_VETERINARIO BIGINT NULL,
    ID_RESPONSAVEL BIGINT NULL,
    ID_ANIMAL BIGINT NULL,
    ID_CLINICA BIGINT NULL,
    ID_CONSULTA BIGINT NULL,
    NR_NOTA INT NOT NULL,                               -- Escala de 0 a 5
    DS_COMENTARIO VARCHAR(MAX) NULL,
    DT_FEEDBACK DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_ARKIVE_FEEDBACK_NPS PRIMARY KEY (ID_FEEDBACK_NPS),
    CONSTRAINT CK_ARKIVE_NPS_NOTA CHECK (NR_NOTA BETWEEN 0 AND 5),
    -- Precisa de ao menos um contexto associado.
    CONSTRAINT CK_ARKIVE_NPS_CONTEXTO CHECK (ID_VETERINARIO IS NOT NULL OR ID_RESPONSAVEL IS NOT NULL OR ID_ANIMAL IS NOT NULL OR ID_CLINICA IS NOT NULL OR ID_CONSULTA IS NOT NULL),
    CONSTRAINT FK_NPS_VET FOREIGN KEY (ID_VETERINARIO)
        REFERENCES TB_ARKIVE_VETERINARIO (ID_VETERINARIO),
    CONSTRAINT FK_NPS_RESP FOREIGN KEY (ID_RESPONSAVEL)
        REFERENCES TB_ARKIVE_RESPONSAVEL (ID_RESPONSAVEL),
    CONSTRAINT FK_NPS_ANIMAL FOREIGN KEY (ID_ANIMAL)
        REFERENCES TB_ARKIVE_ANIMAL (ID_ANIMAL),
    CONSTRAINT FK_NPS_CLINICA FOREIGN KEY (ID_CLINICA)
        REFERENCES TB_ARKIVE_CLINICA (ID_CLINICA),
    CONSTRAINT FK_NPS_CONSULTA FOREIGN KEY (ID_CONSULTA)
        REFERENCES TB_ARKIVE_CONSULTA (ID_CONSULTA)
);

CREATE INDEX IX_ARKIVE_NPS_VET ON TB_ARKIVE_FEEDBACK_NPS (ID_VETERINARIO);
CREATE INDEX IX_ARKIVE_NPS_RESP ON TB_ARKIVE_FEEDBACK_NPS (ID_RESPONSAVEL);
CREATE INDEX IX_ARKIVE_NPS_ANIMAL ON TB_ARKIVE_FEEDBACK_NPS (ID_ANIMAL);
CREATE INDEX IX_ARKIVE_NPS_CLINICA ON TB_ARKIVE_FEEDBACK_NPS (ID_CLINICA);
CREATE INDEX IX_ARKIVE_NPS_CONSULTA ON TB_ARKIVE_FEEDBACK_NPS (ID_CONSULTA);

/*
 * ============================================================================
 * PARTE 5 - AUDITORIA TECNICA
 * ============================================================================
 */

-- ============================================================
-- TB_ARKIVE_LOG_ERRO
-- Auditoria tecnica de erros gerados por procedures e rotinas de banco.
-- ============================================================
CREATE TABLE TB_ARKIVE_LOG_ERRO (
    ID_LOG_ERRO BIGINT IDENTITY(1,1) NOT NULL,
    NM_PROCEDURE VARCHAR(100) NOT NULL,
    NM_USUARIO VARCHAR(100) NOT NULL DEFAULT USER_NAME(), -- Usuario do banco que executou a rotina
    DT_OCORRENCIA DATETIME2 NOT NULL DEFAULT GETDATE(),
    CD_ERRO INT NOT NULL,
    DS_MENSAGEM_ERRO VARCHAR(MAX) NOT NULL,

    CONSTRAINT PK_ARKIVE_LOG_ERRO PRIMARY KEY (ID_LOG_ERRO)
);

/*
 * ============================================================================
 * FIM DO SCRIPT
 * ----------------------------------------------------------------------------
 * Total de objetos representados (estado final apos V1..V7):
 *   23 tabelas                          (TB_ARKIVE_*)
 *   23 chaves primarias                 (uma por tabela)
 *   48 chaves estrangeiras              (FK_*)
 *    8 UNIQUE constraints                (UQ_ARKIVE_*, colunas NOT NULL)
 *    3 indices unicos explicitos         (UX_ARKIVE_PREDISPOSICAO;
 *                                          UQ_ARKIVE_USUARIO_RESP e
 *                                          UQ_ARKIVE_USUARIO_VET, ambos
 *                                          filtered indexes por serem
 *                                          vinculos 1:1 opcionais)
 *   55 CHECK constraints                 (CK_ARKIVE_*)
 *   43 indices explicitos ordinarios     (IX_ARKIVE_*)
 *    1 coluna computada persistida       (ID_RACA_COALESCE)
 * ============================================================================
 */

-- ============================================================
-- TABELAS CORE PARA DEMONSTRACAO DO CRUD
-- ============================================================
--
-- Tabela 1: TB_ARKIVE_CONSULTA
-- Tabela 2: TB_ARKIVE_DIAGNOSTICO
--
-- Relacionamento:
-- TB_ARKIVE_DIAGNOSTICO.ID_CONSULTA
--     -> TB_ARKIVE_CONSULTA.ID_CONSULTA
--        (FK_DIAG_CONSULTA)
--
-- Justificativa:
-- As duas tabelas pertencem diretamente ao nucleo clinico do ArkIve.
-- TB_ARKIVE_CONSULTA representa o atendimento veterinario e
-- TB_ARKIVE_DIAGNOSTICO registra a avaliacao clinica associada ao
-- atendimento.
--
-- ConsultaController (/api/consultas) e
-- DiagnosticoController (/api/diagnosticos) expoem CRUD REST completo:
-- POST, GET, PUT e DELETE.
--
-- Os respectivos services executam exclusao fisica no banco:
--   - ConsultaService.excluir() usa consultaRepository.delete(...).
--     A exclusao da consulta e permitida apenas enquanto seu status for
--     AG (agendada) e desde que nao existam registros dependentes.
--   - DiagnosticoService.excluir() usa diagnosticoRepository.delete(...).
--
-- Para a demonstracao de DELETE, a ordem correta e excluir primeiro o
-- diagnostico dependente e depois a consulta AG correspondente, respeitando
-- a chave estrangeira FK_DIAG_CONSULTA.
--
-- Na demonstracao final serao utilizados pelo menos dois registros
-- significativos em cada uma das duas tabelas, com comprovacao por SELECT
-- diretamente no Azure SQL apos as operacoes de inclusao, consulta,
-- alteracao e exclusao.
