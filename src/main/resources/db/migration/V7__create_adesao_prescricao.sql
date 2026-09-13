-- Reconciliation migration: TB_ARKIVE_ADESAO_PRESCRICAO is defined in V1 and is required
-- by the AdesaoPrescricao JPA entity (@Table(name = "TB_ARKIVE_ADESAO_PRESCRICAO")), but
-- Hibernate schema validation reported it as missing after V1-V6 were applied to the
-- Azure SQL environment used for the rehearsal deploy. V1-V6 are treated as immutable at
-- this point (already applied); this migration only creates the table if it is not
-- already present, so it is safe to run regardless of the exact cause of the drift.
--
-- Column shape, types, constraints, indexes and naming mirror exactly what V1 already
-- defines for this table, so the resulting schema is identical either way.

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'TB_ARKIVE_ADESAO_PRESCRICAO' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE TB_ARKIVE_ADESAO_PRESCRICAO (
        ID_ADESAO BIGINT IDENTITY(1,1) NOT NULL,
        ID_PRESCRICAO BIGINT NOT NULL,
        ID_RESPONSAVEL BIGINT NULL,
        ID_ANIMAL BIGINT NOT NULL,
        DT_REGISTRO DATETIME2 NOT NULL DEFAULT GETDATE(),
        ST_TOMOU CHAR(1) NOT NULL,
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
END
GO

-- Separate batch: guarantees TB_ARKIVE_ADESAO_PRESCRICAO already exists (created above, or
-- pre-existing) before these indexes are compiled/created, avoiding the same same-batch
-- name-resolution issue already fixed in V2/V3/V5.
--
-- Each check is scoped to this specific table via OBJECT_ID(...): sys.indexes.name is
-- unique only per-table in SQL Server, not database-wide, so filtering by name alone
-- would not be a reliable existence test. This makes the migration idempotent whether
-- the table was just created above or already existed with some/all of its indexes.
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_ARKIVE_ADESAO_PRESC'
      AND object_id = OBJECT_ID('TB_ARKIVE_ADESAO_PRESCRICAO')
)
    CREATE INDEX IX_ARKIVE_ADESAO_PRESC ON TB_ARKIVE_ADESAO_PRESCRICAO (ID_PRESCRICAO);

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_ARKIVE_ADESAO_RESP'
      AND object_id = OBJECT_ID('TB_ARKIVE_ADESAO_PRESCRICAO')
)
    CREATE INDEX IX_ARKIVE_ADESAO_RESP ON TB_ARKIVE_ADESAO_PRESCRICAO (ID_RESPONSAVEL);

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_ARKIVE_ADESAO_ANIMAL'
      AND object_id = OBJECT_ID('TB_ARKIVE_ADESAO_PRESCRICAO')
)
    CREATE INDEX IX_ARKIVE_ADESAO_ANIMAL ON TB_ARKIVE_ADESAO_PRESCRICAO (ID_ANIMAL);

-- TB_ARKIVE_ADESAO_PRESCRICAO: Registra se uma prescricao foi seguida ou nao, sustentando analises de adesao terapeutica e abandono de tratamento.
-- TB_ARKIVE_ADESAO_PRESCRICAO.ID_ADESAO: Identificador unico do registro de adesao.
-- TB_ARKIVE_ADESAO_PRESCRICAO.ID_PRESCRICAO: Prescricao relacionada ao registro de adesao.
-- TB_ARKIVE_ADESAO_PRESCRICAO.ID_RESPONSAVEL: Responsavel que informou a adesao, quando aplicavel.
-- TB_ARKIVE_ADESAO_PRESCRICAO.ID_ANIMAL: Animal relacionado a prescricao.
-- TB_ARKIVE_ADESAO_PRESCRICAO.DT_REGISTRO: Data e hora do registro de adesao.
-- TB_ARKIVE_ADESAO_PRESCRICAO.ST_TOMOU: Indica se a dose/tratamento foi seguido: S ou N.
-- TB_ARKIVE_ADESAO_PRESCRICAO.DS_OBSERVACAO: Observacao opcional do responsavel, clinica ou sistema.
