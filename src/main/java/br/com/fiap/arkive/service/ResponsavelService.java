package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ResponsavelRequest;
import br.com.fiap.arkive.dto.response.ResponsavelResponse;
import br.com.fiap.arkive.dto.response.ResponsavelLookupResponse;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class ResponsavelService {

	private static final Set<String> TIPOS = Set.of(
			"TUTOR",
			"FUNCIONARIO_CLINICA",
			"FUNCIONARIO_ZOO",
			"ONG",
			"INSTITUICAO",
			"OUTRO"
	);

	private final ResponsavelRepository responsavelRepository;

	public ResponsavelService(ResponsavelRepository responsavelRepository) {
		this.responsavelRepository = responsavelRepository;
	}

	public List<String> listarTipos() {
		return TIPOS.stream().sorted().toList();
	}

	@Transactional(readOnly = true)
	public Page<ResponsavelLookupResponse> buscarParaVinculo(String busca, Pageable pageable) {
		String texto = busca == null ? "" : busca.trim();
		if (texto.length() < 3 || texto.length() > 200) {
			throw new BusinessException("Informe de 3 a 200 caracteres do nome ou o email do responsavel.");
		}
		Pageable limitado = PageRequest.of(pageable.isPaged() ? pageable.getPageNumber() : 0,
				pageable.isPaged() ? Math.min(pageable.getPageSize(), 20) : 20,
				Sort.by("nome").and(Sort.by("id")));
		return responsavelRepository.buscarParaVinculo(texto, limitado)
				.map(ResponsavelLookupResponse::fromEntity);
	}

	@Transactional
	public ResponsavelResponse criar(ResponsavelRequest request) {
		Responsavel responsavel = new Responsavel();
		aplicarDados(responsavel, request, true);
		return ResponsavelResponse.fromEntity(responsavelRepository.save(responsavel));
	}

	@Transactional(readOnly = true)
	public Page<ResponsavelResponse> listar(String nome, String documento, String tipo, String ativo, Pageable pageable) {
		validarTipoQuandoInformado(tipo);
		validarSNQuandoInformado(ativo, "Ativo");
		return responsavelRepository.buscar(
				vazioParaNulo(nome),
				vazioParaNulo(documento),
				vazioParaNulo(tipo),
				vazioParaNulo(ativo),
				pageable
		).map(ResponsavelResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<ResponsavelResponse> listarPorTexto(String busca, String ativo, Pageable pageable) {
		validarSNQuandoInformado(ativo, "Ativo");
		return responsavelRepository.buscarPorTexto(
				vazioParaNulo(busca),
				vazioParaNulo(ativo),
				pageable
		).map(ResponsavelResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public ResponsavelResponse buscarPorId(Long id) {
		return ResponsavelResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public ResponsavelResponse atualizar(Long id, ResponsavelRequest request) {
		Responsavel responsavel = buscarEntidade(id);
		aplicarDados(responsavel, request, false);
		return ResponsavelResponse.fromEntity(responsavelRepository.save(responsavel));
	}

	@Transactional
	public void excluir(Long id) {
		Responsavel responsavel = buscarEntidade(id);
		responsavel.setAtivo("N");
		responsavelRepository.save(responsavel);
	}

	private Responsavel buscarEntidade(Long id) {
		return responsavelRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private void aplicarDados(Responsavel responsavel, ResponsavelRequest request, boolean criando) {
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		String notificacao = criando && request.notificacao() == null ? "S" : request.notificacao();
		LocalDate dataCadastro = criando && request.dataCadastro() == null ? LocalDate.now() : request.dataCadastro();
		validarTipoQuandoInformado(request.tipo());
		validarSNQuandoInformado(ativo, "Ativo");
		validarSNQuandoInformado(notificacao, "Notificacao");
		responsavel.setNome(request.nome());
		responsavel.setDocumento(request.documento());
		responsavel.setEmail(request.email());
		responsavel.setTelefone(request.telefone());
		responsavel.setTipo(request.tipo());
		responsavel.setDataCadastro(dataCadastro == null ? responsavel.getDataCadastro() : dataCadastro);
		responsavel.setNotificacao(notificacao == null ? responsavel.getNotificacao() : notificacao);
		responsavel.setAtivo(ativo == null ? responsavel.getAtivo() : ativo);
	}

	private void validarTipoQuandoInformado(String tipo) {
		if (tipo != null && !tipo.isBlank() && !TIPOS.contains(tipo)) {
			throw new BusinessException("Tipo de responsavel invalido.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
