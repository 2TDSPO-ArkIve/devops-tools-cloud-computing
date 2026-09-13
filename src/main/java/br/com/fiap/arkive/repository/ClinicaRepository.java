package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Clinica;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Profile("!local-nodb")
public interface ClinicaRepository extends JpaRepository<Clinica, Long> {

	@Query("""
			select c from Clinica c
			where (:nome is null or lower(c.nome) like lower(concat('%', :nome, '%')))
			and (:ativo is null or c.ativo = :ativo)
			""")
	Page<Clinica> buscar(@Param("nome") String nome, @Param("ativo") String ativo, Pageable pageable);

	@Query("""
			select c from Clinica c
			where (
				:busca is null
				or lower(c.nome) like lower(concat('%', :busca, '%'))
				or lower(c.cnpj) like lower(concat('%', :busca, '%'))
				or lower(c.email) like lower(concat('%', :busca, '%'))
			)
			and (:ativo is null or c.ativo = :ativo)
			""")
	Page<Clinica> buscarPorTexto(@Param("busca") String busca, @Param("ativo") String ativo, Pageable pageable);

	long countByAtivo(String ativo);

	List<Clinica> findByAtivoOrderByNomeAsc(String ativo);

}
