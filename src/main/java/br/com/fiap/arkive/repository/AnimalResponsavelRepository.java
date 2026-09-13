package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.AnimalResponsavelId;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;

@Profile("!local-nodb")
public interface AnimalResponsavelRepository extends JpaRepository<AnimalResponsavel, AnimalResponsavelId> {

	@Query("select ar from AnimalResponsavel ar where ar.animal.id = :animalId")
	Page<AnimalResponsavel> listarPorAnimal(@Param("animalId") Long animalId, Pageable pageable);

	@Query("select ar from AnimalResponsavel ar where ar.responsavel.id = :responsavelId")
	Page<AnimalResponsavel> listarPorResponsavel(@Param("responsavelId") Long responsavelId, Pageable pageable);

	@Query("select ar from AnimalResponsavel ar where ar.animal.id = :animalId and ar.ativo = :ativo")
	List<AnimalResponsavel> listarPorAnimalEAtivo(@Param("animalId") Long animalId, @Param("ativo") String ativo);

	@Query("""
			select count(ar) > 0 from AnimalResponsavel ar
			where ar.animal.id = :animalId
			and ar.responsavel.id = :responsavelId
			and ar.ativo = 'S'
			and ar.id.dataInicio <= :dataAtual
			and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			""")
	boolean existsVinculoAtivoVigente(
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("dataAtual") LocalDate dataAtual
	);

	@Query("""
			select ar from AnimalResponsavel ar
			where ar.animal.id = :animalId
			and ar.ativo = :ativo
			and ar.principal = :principal
			""")
	List<AnimalResponsavel> listarPrincipaisAtivos(
			@Param("animalId") Long animalId,
			@Param("ativo") String ativo,
			@Param("principal") String principal
	);

	@Query("""
			select ar from AnimalResponsavel ar
			where ar.animal.id = :animalId
			and ar.ativo = 'S'
			and ar.principal = 'S'
			and ar.id.dataInicio <= :dataAtual
			and (ar.dataFim is null or ar.dataFim >= :dataAtual)
			""")
	List<AnimalResponsavel> buscarResponsaveisPrincipaisAtivosVigentes(
			@Param("animalId") Long animalId,
			@Param("dataAtual") LocalDate dataAtual
	);

	@Query("""
			select ar from AnimalResponsavel ar
			where (:animalId is null or ar.animal.id = :animalId)
			and (:responsavelId is null or ar.responsavel.id = :responsavelId)
			and (:tipoVinculo is null or ar.tipoVinculo = :tipoVinculo)
			and (:ativo is null or ar.ativo = :ativo)
			""")
	Page<AnimalResponsavel> buscar(
			@Param("animalId") Long animalId,
			@Param("responsavelId") Long responsavelId,
			@Param("tipoVinculo") String tipoVinculo,
			@Param("ativo") String ativo,
			Pageable pageable
	);

}
