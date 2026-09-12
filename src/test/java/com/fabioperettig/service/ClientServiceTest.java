package com.fabioperettig.service;

import com.fabioperettig.dao.IGenericDAO;
import com.fabioperettig.domain.Client;
import com.fabioperettig.factory.ClientTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class ClientServiceTest {

    private FakeClientDAO clientDAO;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clientDAO = new FakeClientDAO();
        service = new ClientService(clientDAO);
    }

    @Test
    void shouldRejectNullClientDAO() {
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class, () -> new ClientService(null)
        );

        Assertions.assertEquals("Client DAO is required", exception.getMessage());
    }

    @Test
    void shouldDelegateClientCreationToDAO() {
        Client client = ClientTestFactory.create("12345678901");
        Client createdClient = service.create(client);

        Assertions.assertSame(client, clientDAO.receivedClient);
        Assertions.assertSame(client, createdClient);
    }

    @Test
    void shouldDelegateFindByIdToDAO() {
        Client expectedClient = ClientTestFactory.create("23456789012");
        clientDAO.clientToFind = expectedClient;

        Optional<Client> result = service.findById(10L);

        Assertions.assertEquals(10L, clientDAO.receivedId);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(expectedClient, result.orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenClientIsNotFound() {
        Optional<Client> result = service.findById(99L);

        Assertions.assertEquals(99L, clientDAO.receivedId);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldDelegateFindAllToDAO() {
        Client firstClient = ClientTestFactory.create("34567890123");
        Client secondClient = ClientTestFactory.create("45678901234");

        List<Client> expectedClients = List.of(firstClient, secondClient);
        clientDAO.clientsToFind = expectedClients;

        List<Client> result = service.findAll();

        Assertions.assertSame(expectedClients, result);
    }

    @Test
    void shouldReturnTrueWhenClientIsUpdated() {
        Client client = ClientTestFactory.create("56789012345");
        clientDAO.updateResult = true;

        boolean result = service.update(client);

        Assertions.assertSame(client, clientDAO.receivedClientForUpdate);
        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenClientIsNotUpdated() {
        Client client = ClientTestFactory.create("67890123456");
        clientDAO.updateResult = false;

        boolean result = service.update(client);

        Assertions.assertSame(client, clientDAO.receivedClientForUpdate);
        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenClientIsDeleted() {
        clientDAO.deleteResult = true;
        boolean result = service.deleteById(10L);

        Assertions.assertEquals(10L, clientDAO.receivedIdForDeletion);
        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenClientIsNotDeleted() {
        clientDAO.deleteResult = false;
        boolean result = service.deleteById(99L);

        Assertions.assertEquals(99L, clientDAO.receivedIdForDeletion);
        Assertions.assertFalse(result);
    }

    private static class FakeClientDAO implements IGenericDAO<Client, Long> {
        private Client receivedClient;
        private Client clientToFind;
        private Client receivedClientForUpdate;
        private Long receivedId;
        private Long receivedIdForDeletion;
        private List<Client> clientsToFind = List.of();
        private boolean updateResult;
        private boolean deleteResult;

        @Override
        public Client create(Client entity) {
            receivedClient = entity;
            return entity;
        }

        @Override
        public Optional<Client> findById(Long id) {
            receivedId = id;
            return Optional.ofNullable(clientToFind);
        }

        @Override
        public List<Client> findAll() {
            return clientsToFind;
        }

        @Override
        public boolean update(Client entity) {
            receivedClientForUpdate = entity;
            return updateResult;
        }

        @Override
        public boolean deleteById(Long id) {
            receivedIdForDeletion = id;
            return deleteResult;
        }
    }

}
