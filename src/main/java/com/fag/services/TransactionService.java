package com.fag.services;

import com.fag.domain.dtos.TransactionDTO;
import com.fag.domain.dtos.TransactionRequestDTO;
import com.fag.domain.dtos.UserDTO;
import com.fag.domain.exceptions.TransactionException;
import com.fag.domain.mappers.TransactionMapper;
import com.fag.domain.repositories.ITransactionRepository;
import com.fag.domain.usecases.CreateTransaction;
import com.fag.infra.jakarta.mappers.JakartaTransactionMapper;
import com.fag.infra.jakarta.model.JakartaTransaction;
import com.fag.infra.jakarta.repository.JakartaTransactionRepository;
import com.fag.infra.mocky.repository.MockyTransaction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService implements ITransactionRepository {

    @Autowired
    private UserService userService;

    @Autowired
    private JakartaTransactionRepository repository;

    @Autowired
    private MockyTransaction authorization;

    @Autowired
    private NotificationService notificationService;

    /**
     * Cria uma nova transação entre usuários.
     *
     * @param request O objeto TransactionRequestDTO contendo detalhes da transação.
     * @return Um objeto TransactionDTO representando a transação criada.
     * @throws TransactionException Se a transação não for autorizada, uma exceção será lançada.
     */
    @Transactional
    public TransactionDTO createTransaction(TransactionRequestDTO request) {
        UserDTO sender = userService.findUserById(request.getSenderId());
        UserDTO receiver = userService.findUserById(request.getReceiverId());

        userService.validateTransaction(sender, request.getValue());

        this.authorizeTransaction();
        this.updateBalance(sender, receiver, request.getValue());

        TransactionDTO dto = new TransactionDTO(
                null,
                request.getValue(),
                sender,
                receiver,
                LocalDateTime.now(),
                true
        );


        try {
            CreateTransaction createTransaction = new CreateTransaction(repository);

            TransactionDTO transaction =  createTransaction.execute(dto);

            this.sendTransactionNotifications(sender, receiver);

            return transaction;
        } catch (Exception e) {
            this.handleTransactionFailure(sender, receiver, request.getValue());

            throw new TransactionException("Falha ao realizar a transação", 500);
        }
    }

    /**
     * Verifica se a transação está autorizada usando o serviço mocky.
     *
     * @throws TransactionException Se a transação não estiver autorizada.
     */
    private void authorizeTransaction() {
        if (!authorization.authorizeTransaction()) {
            throw new TransactionException("Transação não autorizada pelo serviço", 500);
        }
    }

    /**
     * Envia notificações de sucesso para o remetente e o destinatário após uma
     * transação bem-sucedida.
     *
     * @param sender   O remetente da transação.
     * @param receiver O destinatário da transação.
     */
    private void sendTransactionNotifications(UserDTO sender, UserDTO receiver) {
        notificationService.sendNotification(sender, "Transação realizada com sucesso!");
        notificationService.sendNotification(receiver, "Transação recebida com sucesso!");
    }

    /**
     * Manipula a falha de uma transação, atualizando saldos e enviando notificações.
     *
     * @param sender   O remetente da transação.
     * @param receiver O destinatário da transação.
     * @param value    O valor da transação.
     */
    private void handleTransactionFailure(UserDTO sender, UserDTO receiver, BigDecimal value) {
        this.updateBalance(receiver, sender, value);
        notificationService.sendNotification(sender, "Não foi possível realizar a transação");
    }

    /**
     * Salva uma transação no banco de dados.
     *
     * @param transaction O objeto TransactionDTO a ser salvo.
     */
    @Transactional
    public void saveTransaction(TransactionDTO transaction) {
        JakartaTransaction entity = JakartaTransactionMapper.toEntity(TransactionMapper.toBO(transaction));
        this.repository.save(entity);
    }

    /**
     * Busca uma transação pelo seu ID.
     *
     * @param id O ID da transação a ser encontrada.
     * @return Um objeto TransactionDTO representando a transação encontrada.
     */
    @Override
    public TransactionDTO findTransactionById(Long id) {
        return this.repository.findTransactionById(id);
    }

    /**
     * Lista todas as transações no banco de dados.
     *
     * @return Uma lista de objetos TransactionDTO representando todas as transações.
     */
    @Override
    public List<TransactionDTO> listAllTransactions() {
        return this.repository.listAllTransactions();
    }

    /**
     * Lista todas as transações associadas a um usuário.
     *
     * @param sender O objeto UserDTO para o qual as transações estão associadas.
     * @return Uma lista de objetos TransactionDTO representando as transações associadas ao usuário.
     */
    public List<TransactionDTO> listAllTransactionsByUser(UserDTO sender) {
        return this.repository.listAllTransactionsByUser(sender);
    }

    /**
     * Lista todas as transações em que um usuário é o remetente.
     *
     * @param sender O objeto UserDTO para o qual as transações de envio estão associadas.
     * @return Uma lista de objetos TransactionDTO representando as transações de envio associadas ao usuário.
     */
    @Override
    public List<TransactionDTO> listAllTransactionsBySender(UserDTO sender) {
        return this.repository.listAllTransactionsBySender(sender);
    }

    /**
     * Lista todas as transações em que um usuário é o destinatário.
     *
     * @param receiver O objeto UserDTO para o qual as transações de recebimento estão associadas.
     * @return Uma lista de objetos TransactionDTO representando as transações de recebimento associadas ao usuário.
     */
    @Override
    public List<TransactionDTO> listAllTransactionsByReceiver(UserDTO receiver) {
        return this.repository.listAllTransactionsByReceiver(receiver);
    }

    /**
     * Recusa uma transação pelo seu ID, revertendo os saldos dos usuários envolvidos.
     *
     * @param id O ID da transação a ser recusada.
     * @return Um objeto TransactionDTO representando a transação recusada.
     */
    public TransactionDTO refuseTransactionById(Long id) {
        TransactionDTO transaction = this.findTransactionById(id);

        UserDTO sender = transaction.getSender();
        UserDTO receiver = transaction.getReceiver();

        // Reverte o saldo dos usuários
        this.updateBalance(receiver, sender, transaction.getAmount());

        transaction.setSuccess(false);

        this.saveTransaction(transaction);
        return transaction;
    }

    /**
     * Atualiza os saldos dos usuários envolvidos em uma transação.
     *
     * @param sender   O remetente da transação.
     * @param receiver O destinatário da transação.
     * @param value    O valor da transação.
     */
    private void updateBalance(UserDTO sender, UserDTO receiver, BigDecimal value) {
        sender.setBalance(sender.getBalance().subtract(value));
        receiver.setBalance(receiver.getBalance().add(value));

        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);
    }

}
