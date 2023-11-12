package com.fag.services;

import com.fag.domain.dtos.UserDTO;
import com.fag.domain.repositories.IUserRepository;
import com.fag.infra.jakarta.repository.JakartaTransactionRepository;
import com.fag.infra.jakarta.repository.JakartaUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements IUserRepository {

    @Autowired
    private JakartaUserRepository repository;

    @Autowired
    private JakartaTransactionRepository transactionRepository;

    @Override
    public List<UserDTO> listAllUsers() {
        return null;
    }

    @Override
    public UserDTO findUserById(Long id) {
        return null;
    }

    @Override
    public UserDTO findUserByDocument(String document) {
        return null;
    }

}