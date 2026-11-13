package br.com.thindesk.dashboard.repository;

import br.com.thindesk.dashboard.model.Chamado;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChamadoRepository extends MongoRepository<Chamado, String> {

    Optional<Chamado> findByProtocolo(String protocolo);

    Optional<Chamado> findTopByOrderByProtocoloDesc();
}