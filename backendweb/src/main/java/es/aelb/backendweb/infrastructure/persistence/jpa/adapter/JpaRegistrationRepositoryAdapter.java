package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.RegistrationJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringRegistrationJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaRegistrationRepositoryAdapter implements RegistrationRepository {

    private final SpringRegistrationJpaRepository jpaRepository;

    public JpaRegistrationRepositoryAdapter(SpringRegistrationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Registration registration) {
        jpaRepository.save(RegistrationJpaMapper.toJpa(registration));
    }

    @Override
    public Optional<Registration> findById(String id) {
        return jpaRepository.findById(id).map(RegistrationJpaMapper::toDomain);
    }

    @Override
    public boolean existsByUserAndChampionship(UserId userId, ChampionshipId championshipId) {
        return jpaRepository.existsByUserIdAndChampionshipId(userId.value(), championshipId.value());
    }

    @Override
    public List<Registration> findByUserAndChampionship(UserId userId, ChampionshipId championshipId) {
        return jpaRepository.findByUserIdAndChampionshipId(userId.value(), championshipId.value())
                .stream().map(RegistrationJpaMapper::toDomain).toList();
    }

    @Override
    public List<Registration> findByChampionship(ChampionshipId championshipId) {
        return jpaRepository.findByChampionshipId(championshipId.value())
                .stream().map(RegistrationJpaMapper::toDomain).toList();
    }

    @Override
    public List<Registration> findByUser(UserId userId) {
        return jpaRepository.findByUserId(userId.value())
                .stream().map(RegistrationJpaMapper::toDomain).toList();
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
}
