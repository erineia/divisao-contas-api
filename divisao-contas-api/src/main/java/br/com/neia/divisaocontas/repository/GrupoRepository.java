package br.com.neia.divisaocontas.repository;

import br.com.neia.divisaocontas.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório principal da entidade Grupo.
 */
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    Optional<Grupo> findByNome(String nome);
}
