package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Diagnostico;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Profile("!local-nodb")
public interface DiagnosticoRepository extends JpaRepository<Diagnostico, Long> {

	@Query("""
			select d from Diagnostico d
			where (:consultaId is null or d.consulta.id = :consultaId)
			and (:doencaId is null or d.doenca.id = :doencaId)
			and (:severidade is null or d.severidade = :severidade)
			and (:confirmado is null or d.confirmado = :confirmado)
			""")
	Page<Diagnostico> buscar(
			@Param("consultaId") Long consultaId,
			@Param("doencaId") Long doencaId,
			@Param("severidade") String severidade,
			@Param("confirmado") String confirmado,
			Pageable pageable
	);

	@Query("""
			select d from Diagnostico d
			where d.consulta.veterinario.id = :veterinarioId
			and (:consultaId is null or d.consulta.id = :consultaId)
			and (:doencaId is null or d.doenca.id = :doencaId)
			and (:severidade is null or d.severidade = :severidade)
			and (:confirmado is null or d.confirmado = :confirmado)
			""")
	Page<Diagnostico> buscarParaVeterinario(
			@Param("veterinarioId") Long veterinarioId,
			@Param("consultaId") Long consultaId,
			@Param("doencaId") Long doencaId,
			@Param("severidade") String severidade,
			@Param("confirmado") String confirmado,
			Pageable pageable
	);

	@Query("""
			select distinct d from Diagnostico d
			join AnimalResponsavel ar on ar.animal = d.consulta.animal
			where ar.responsavel.id = :responsavelId
			and ar.ativo = 'S'
			and ar.id.dataInicio <= :dataAtual
			and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			and (:consultaId is null or d.consulta.id = :consultaId)
			and (:doencaId is null or d.doenca.id = :doencaId)
			and (:severidade is null or d.severidade = :severidade)
			and (:confirmado is null or d.confirmado = :confirmado)
			""")
	Page<Diagnostico> buscarParaResponsavel(
			@Param("responsavelId") Long responsavelId,
			@Param("dataAtual") LocalDate dataAtual,
			@Param("consultaId") Long consultaId,
			@Param("doencaId") Long doencaId,
			@Param("severidade") String severidade,
			@Param("confirmado") String confirmado,
			Pageable pageable
	);

	@Query("""
			select d from Diagnostico d
			where d.consulta.clinica.id = :clinicaId
			and (:consultaId is null or d.consulta.id = :consultaId)
			and (:doencaId is null or d.doenca.id = :doencaId)
			and (:severidade is null or d.severidade = :severidade)
			and (:confirmado is null or d.confirmado = :confirmado)
			""")
	Page<Diagnostico> buscarParaClinica(
			@Param("clinicaId") Long clinicaId,
			@Param("consultaId") Long consultaId,
			@Param("doencaId") Long doencaId,
			@Param("severidade") String severidade,
			@Param("confirmado") String confirmado,
			Pageable pageable
	);

	@Query("""
			select d from Diagnostico d
			where d.consulta.id = :consultaId
			and d.confirmado = 'N'
			and d.validacaoVet = 'N'
			and d.insightIa is not null
			order by d.id desc
			""")
	List<Diagnostico> buscarSuportesClinicos(@Param("consultaId") Long consultaId, Pageable pageable);

	@Query("""
			select d from Diagnostico d
			where d.consulta.id = :consultaId
			and d.confirmado = 'S'
			and d.validacaoVet = 'S'
			order by d.id desc
			""")
	List<Diagnostico> buscarDiagnosticosConfirmadosVeterinario(@Param("consultaId") Long consultaId, Pageable pageable);

}
