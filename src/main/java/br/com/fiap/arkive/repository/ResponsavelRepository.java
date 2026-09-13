package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Responsavel;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Profile("!local-nodb")
public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {

	@Query("""
			select r from Responsavel r where r.ativo = 'S'
			and (locate(lower(:busca), lower(r.nome)) > 0 or lower(r.email) = lower(:busca))
			""")
	Page<Responsavel> buscarParaVinculo(@Param("busca") String busca, Pageable pageable);

	@Query("""
			select r from Responsavel r
			where (:nome is null or lower(r.nome) like lower(concat('%', :nome, '%')))
			and (:documento is null or lower(r.documento) like lower(concat('%', :documento, '%')))
			and (:tipo is null or r.tipo = :tipo)
			and (:ativo is null or r.ativo = :ativo)
			""")
	Page<Responsavel> buscar(
			@Param("nome") String nome,
			@Param("documento") String documento,
			@Param("tipo") String tipo,
			@Param("ativo") String ativo,
			Pageable pageable
	);

	@Query("""
			select r from Responsavel r
			where (
				:busca is null
				or lower(r.nome) like lower(concat('%', :busca, '%'))
				or lower(r.documento) like lower(concat('%', :busca, '%'))
				or lower(r.email) like lower(concat('%', :busca, '%'))
			)
			and (:ativo is null or r.ativo = :ativo)
			""")
	Page<Responsavel> buscarPorTexto(
			@Param("busca") String busca,
			@Param("ativo") String ativo,
			Pageable pageable
	);

	long countByAtivo(String ativo);

	List<Responsavel> findByAtivoOrderByNomeAsc(String ativo);

	@Query("""
			select r from Responsavel r
			where r.ativo = 'S'
			and not exists (
				select u from Usuario u
				where u.responsavel = r
			)
			order by r.nome asc
			""")
	List<Responsavel> findAtivosSemUsuarioOrderByNomeAsc();

	@Query("""
			select r from Responsavel r
			where (
				r.ativo = 'S'
				and not exists (
					select u from Usuario u
					where u.responsavel = r
					and u.id <> :usuarioId
				)
			)
			or exists (
				select atual from Usuario atual
				where atual.id = :usuarioId
				and atual.responsavel = r
			)
			order by r.nome asc
			""")
	List<Responsavel> findDisponiveisParaUsuarioOrderByNomeAsc(@Param("usuarioId") Long usuarioId);

}
