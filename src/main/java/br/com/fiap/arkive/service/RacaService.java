package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RacaRequest;
import br.com.fiap.arkive.dto.response.RacaResponse;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Raca;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.RacaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Profile("!local-nodb")
public class RacaService {

	private static final Set<String> PORTES = Set.of("PEQUENO", "MEDIO", "GRANDE");

	private final RacaRepository racaRepository;
	private final EspecieService especieService;

	public RacaService(RacaRepository racaRepository, EspecieService especieService) {
		this.racaRepository = racaRepository;
		this.especieService = especieService;
	}

	@Transactional
	@CacheEvict(value = "racas", allEntries = true)
	public RacaResponse criar(RacaRequest request) {
		Raca raca = new Raca();
		aplicarDados(raca, request);
		return salvar(raca);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "racas", key = "'listar:' + (#nome == null ? '' : #nome) + ':' + (#especieId == null ? '' : #especieId) + ':' + #pageable")
	public Page<RacaResponse> listar(String nome, Long especieId, Pageable pageable) {
		return racaRepository.buscar(vazioParaNulo(nome), especieId, pageable).map(RacaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "racas", key = "'id:' + #id")
	public RacaResponse buscarPorId(Long id) {
		return RacaResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	@CacheEvict(value = "racas", allEntries = true)
	public RacaResponse atualizar(Long id, RacaRequest request) {
		Raca raca = buscarEntidade(id);
		aplicarDados(raca, request);
		return salvar(raca);
	}

	@Transactional
	@CacheEvict(value = "racas", allEntries = true)
	public void excluir(Long id) {
		Raca raca = buscarEntidade(id);
		raca.setAtivo("N");
		racaRepository.save(raca);
	}

	@Transactional(readOnly = true)
	public Raca buscarEntidade(Long id) {
		return racaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Raca nao encontrada."));
	}

	private void aplicarDados(Raca raca, RacaRequest request) {
		if (request.especieId() == null) {
			throw new BusinessException("Especie deve ser informada.");
		}
		String nome = request.nome() == null ? "" : request.nome().trim();
		if (nome.isBlank() || nome.length() > 50) {
			throw new BusinessException("Nome da raca deve ter entre 1 e 50 caracteres.");
		}
		validarPorte(request.porte());
		Especie especie = especieService.buscarEntidade(request.especieId());
		if (!"S".equals(especie.getAtivo())) {
			throw new BusinessException("Especie deve estar ativa.");
		}
		if (racaRepository.existeOutraComNome(especie.getId(), nome, raca.getId())) {
			throw new BusinessException("Ja existe uma raca com este nome nesta especie.", HttpStatus.CONFLICT);
		}
		raca.setNome(nome);
		raca.setPorte(vazioParaNulo(request.porte()));
		raca.setEspecie(especie);
	}

	private RacaResponse salvar(Raca raca) {
		try {
			return RacaResponse.fromEntity(racaRepository.saveAndFlush(raca));
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Raca nao pode ser salva. Verifique a especie e se o nome ja foi cadastrado.", HttpStatus.CONFLICT);
		}
	}

	private void validarPorte(String porte) {
		if (porte != null && !porte.isBlank() && !PORTES.contains(porte)) {
			throw new BusinessException("Porte deve ser PEQUENO, MEDIO ou GRANDE.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
