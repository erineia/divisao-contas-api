package br.com.neia.divisaocontas.controller;

import br.com.neia.divisaocontas.dto.PessoaCreateRequest;
import br.com.neia.divisaocontas.dto.PessoaResponse;
import br.com.neia.divisaocontas.entity.Pessoa;
import br.com.neia.divisaocontas.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {

  private final PessoaRepository pessoaRepository;

  public PessoaController(PessoaRepository pessoaRepository) {
    this.pessoaRepository = pessoaRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PessoaResponse criar(@RequestBody PessoaCreateRequest req) {
    String nome = req.getNome() == null ? "" : req.getNome().trim();

    if (nome.isEmpty()) {
      throw new IllegalArgumentException("nome é obrigatório");
    }

    if (pessoaRepository.existsByNomeIgnoreCase(nome)) {
      throw new IllegalArgumentException("Já existe uma pessoa com esse nome.");
    }

    Pessoa salva = pessoaRepository.save(new Pessoa(nome));
    return new PessoaResponse(salva.getId(), salva.getNome());
  }

  @GetMapping
  public List<PessoaResponse> listar() {
    return pessoaRepository.findAll()
        .stream()
        .map(p -> new PessoaResponse(p.getId(), p.getNome()))
        .toList();
  }
}
