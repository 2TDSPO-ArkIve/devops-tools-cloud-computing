package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.AdesaoPrescricao;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("!local-nodb")
public interface AdesaoPrescricaoRepository extends JpaRepository<AdesaoPrescricao, Long> {

	@Query("""
			select a from AdesaoPrescricao a
			where (:prescricaoId is null or a.prescricao.id = :prescricaoId)
			and (:animalId is null or a.animal.id = :animalId)
			and (:responsavelId is null or a.responsavel.id = :responsavelId)
			and (:tomou is null or a.tomou = :tomou)
			""")
	Page<AdesaoPrescricao> buscar(
			@Param("prescricaoId") Long prescricaoId,
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("tomou") String tomou,
			Pageable pageable
	);

	boolean existsByPrescricaoId(Long prescricaoId);

	@Query("""
			select a from AdesaoPrescricao a
			where a.prescricao.consulta.veterinario.id = :veterinarioId
			and (:prescricaoId is null or a.prescricao.id = :prescricaoId)
			and (:animalId is null or a.animal.id = :animalId)
			and (:responsavelId is null or a.responsavel.id = :responsavelId)
			and (:tomou is null or a.tomou = :tomou)
			""")
	Page<AdesaoPrescricao> buscarParaVeterinario(
			@Param("veterinarioId") Long veterinarioId,
			@Param("prescricaoId") Long prescricaoId,
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("tomou") String tomou,
			Pageable pageable
	);

	@Query("""
			select distinct a from AdesaoPrescricao a
			join AnimalResponsavel ar on ar.animal = a.prescricao.consulta.animal
			where a.responsavel.id = :responsavelAutenticadoId
			and ar.responsavel.id = :responsavelAutenticadoId
			and ar.ativo = 'S'
			and ar.id.dataInicio <= :dataAtual
			and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			and (:prescricaoId is null or a.prescricao.id = :prescricaoId)
			and (:animalId is null or a.animal.id = :animalId)
			and (:responsavelId is null or a.responsavel.id = :responsavelId)
			and (:tomou is null or a.tomou = :tomou)
			""")
	Page<AdesaoPrescricao> buscarParaResponsavel(
			@Param("responsavelAutenticadoId") Long responsavelAutenticadoId,
			@Param("dataAtual") java.time.LocalDate dataAtual,
			@Param("prescricaoId") Long prescricaoId,
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("tomou") String tomou,
			Pageable pageable
	);

	@Query("""
			select a from AdesaoPrescricao a
			where a.prescricao.consulta.clinica.id = :clinicaId
			and (:prescricaoId is null or a.prescricao.id = :prescricaoId)
			and (:animalId is null or a.animal.id = :animalId)
			and (:responsavelId is null or a.responsavel.id = :responsavelId)
			and (:tomou is null or a.tomou = :tomou)
			""")
	Page<AdesaoPrescricao> buscarParaClinica(
			@Param("clinicaId") Long clinicaId,
			@Param("prescricaoId") Long prescricaoId,
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("tomou") String tomou,
			Pageable pageable
	);

}
