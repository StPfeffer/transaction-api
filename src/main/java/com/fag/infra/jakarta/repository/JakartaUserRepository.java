package com.fag.infra.jakarta.repository;

import com.fag.domain.dtos.UserDTO;
import com.fag.domain.entities.UserBO;
import com.fag.domain.exceptions.UserException;
import com.fag.domain.mappers.UserMapper;
import com.fag.domain.repositories.IUserDataBaseRepository;
import com.fag.domain.repositories.IUserRepository;
import com.fag.infra.jakarta.mappers.JakartaUserMapper;
import com.fag.infra.jakarta.model.JakartaUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JakartaUserRepository extends SimpleJpaRepository<JakartaUser, Long> implements IUserDataBaseRepository, IUserRepository {

    private final EntityManager em;

    public JakartaUserRepository(EntityManager em) {
        super(JakartaUser.class, em);
        this.em = em;
    }

    @Override
    public UserBO persist(UserBO bo) {
        JakartaUser entity = JakartaUserMapper.toEntity(bo);

        em.persist(entity);
        em.flush();

        return JakartaUserMapper.toDomain(entity);
    }

    @Override
    public List<UserDTO> listAllUsers() {
        List<JakartaUser> users = this.findAll();

        return users.stream()
                .map(jakartaUser -> UserMapper.toDTO(JakartaUserMapper.toDomain(jakartaUser)))
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO findUserById(Long id) {
        JakartaUser user = this.findById(id).orElse(null);

        if (user == null) {
            throw new UserException("Não existe um usuário cadastrado para este ID", 404);
        }

        return UserMapper.toDTO(JakartaUserMapper.toDomain(user));
    }

    @Override
    public UserDTO findUserByDocument(String document) {
        TypedQuery<JakartaUser> query = em.createQuery("SELECT e FROM JakartaUser e WHERE e.document = :document", JakartaUser.class)
                .setParameter("document", document);

        try {
            return UserMapper.toDTO(JakartaUserMapper.toDomain(query.getSingleResult()));
        } catch (NoResultException e) {
            return null;
        }
    }

}
