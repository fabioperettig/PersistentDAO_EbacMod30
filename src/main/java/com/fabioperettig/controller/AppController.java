package com.fabioperettig.controller;

import com.fabioperettig.domain.Client;
import com.fabioperettig.domain.Product;
import com.fabioperettig.exception.DataAccessException;
import com.fabioperettig.service.ClientService;
import com.fabioperettig.service.ProductService;
import com.fabioperettig.service.SaleService;
import com.fabioperettig.service.StockService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class AppController {

    private final ClientService clientService;
    private final ProductService productService;
    private final StockService stockService;
    private final SaleService saleService;
    private final Scanner scanner;

    public AppController(
            ClientService clientService,
            ProductService productService,
            StockService stockService,
            SaleService saleService,
            Scanner scanner
    ) {
        this.clientService = Objects.requireNonNull(
                clientService, "Client service is required"
        );
        this.productService = Objects.requireNonNull(
                productService, "Product service is required"
        );
        this.stockService = Objects.requireNonNull(
                stockService, "Stock service is required"
        );
        this.saleService = Objects.requireNonNull(
                saleService, "Sale service is required"
        );
        this.scanner = Objects.requireNonNull(
                scanner, "Scanner is required"
        );
    }

    public void start() {
        while (true) {
            System.out.println("""
                === Sistema de vendas ===
                1 - Clientes
                2 - Produtos
                3 - Estoque
                4 - Vendas
                0 - Sair
                """);
            System.out.print("Opção: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            String option = scanner.nextLine().trim();

            try {
                switch (option) {
                    case "1" -> clientsMenu();
                    case "2" -> productsMenu();
                    case "3" -> stockMenu();
                    case "0" -> {
                        System.out.println("Até logo!");
                        return;
                    }
                    default -> System.out.println("Opção inválida.");
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                System.out.println("Não foi possível concluir: " + exception.getMessage());
            } catch (DataAccessException exception) {
                System.out.println(
                        "Não foi possível acessar ou gravar os dados. "
                                + "Confira os dados informados e a conexão com o banco."
                );
            }
        }
    }

    private String readRequired(String prompt) {
        System.out.print(prompt);

        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Entrada encerrada.");
        }

        String value = scanner.nextLine().trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException("O campo não pode ficar vazio.");
        }

        return value;
    }

    /// MENUS
    private void clientsMenu() {
        while (true) {
            System.out.println("""
                
                === Clientes ===
                1 - Cadastrar
                2 - Buscar por ID
                3 - Listar todos
                4 - Alterar cadastro
                5 - Excluir
                0 - Voltar
                """);
            System.out.print("Opção: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            switch (scanner.nextLine().trim()) {
                case "1" -> registerClient();
                case "2" -> findClientById();
                case "3" -> findAllClients();
                case "4" -> updateClient();
                case "5" -> deleteClientById();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void productsMenu() {
        while (true) {
            System.out.println("""
                
                === Produtos ===
                1 - Cadastrar
                2 - Buscar por ID
                3 - Listar todos
                4 - Alterar cadastro
                5 - Excluir
                0 - Voltar
                """);
            System.out.print("Opção: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            switch (scanner.nextLine().trim()) {
                case "1" -> registerProduct();
                case "2" -> findProductById();
                case "3" -> findAllProducts();
                case "4" -> updateProduct();
                case "5" -> deleteProductById();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void stockMenu() {
        while (true) {
            System.out.println("""
                
                === Estoque ===
                1 - Cadastrar saldo inicial
                2 - Consultar por produto
                3 - Listar estoque
                4 - Ajustar saldo
                5 - Excluir registro
                0 - Voltar
                """);
            System.out.print("Opção: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            switch (scanner.nextLine().trim()) {
                case "1" -> registerInitialBalance();
                case "2" -> findByProduct();
                case "3" -> findAllStocks();
                case "4" -> adjustBalance();
                case "5" -> deleteRegister();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    /// CRUD Clientes
    private void registerClient() {
        Client client = new Client();
        client.setName(readRequired("Nome: "));
        client.setCpf(readRequired("CPF (somente números): "));
        client.setContact(readRequired("Contato: "));

        Client createdClient = clientService.create(client);

        System.out.println("Cliente cadastrado. ID: " + createdClient.getId());
    }

    private void findClientById() {
        Long id = readId("ID do cliente: ");

        clientService.findById(id).ifPresentOrElse(
                client -> showClient(client),
                () -> System.out.println("Cliente não encontrado.")
        );
    }

    private void findAllClients() {
        List<Client> clients = clientService.findAll();

        if (clients.isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.");
            return;
        }

        for (Client client : clients) {
            showClient(client);
        }
    }

    private void updateClient() {
        Long id = readId("ID do cliente: ");

        Client client = clientService.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Cliente não encontrado.")
        );

        showClient(client);

        client.setName(readRequired("Novo nome: "));
        client.setCpf(readRequired("Novo CPF (somente números): "));
        client.setContact(readRequired("Novo contato: "));

        boolean updated = clientService.update(client);

        System.out.println(
                updated ? "Cliente atualizado." : "Cliente não encontrado para atualização."
        );
    }

    private void deleteClientById() {
        Long id = readId("ID do cliente: ");

        Client client = clientService.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Cliente não encontrado.")
        );

        showClient(client);

        String confirmation = readRequired("Confirmar exclusão? (s/n): ");

        if (!confirmation.equalsIgnoreCase("s")) {
            System.out.println("Exclusão cancelada.");
            return;
        }

        boolean deleted = clientService.deleteById(id);

        System.out.println(
                deleted ? "Cliente excluído." : "Cliente não encontrado para exclusão."
        );
    }

    /// CRUD Produtos
    private void registerProduct() {
        Product product = new Product();
        product.setName(readRequired("Nome do produto: "));
        product.setCode(readRequired("Código (até 8 caracteres): "));
        product.setPrice(readPrice("Preço (ex.: 25.50 ou 25,50): "));

        Product createdProduct = productService.create(product);

        System.out.println("Produto cadastrado. ID: " + createdProduct.getId());
    }

    private void findProductById() {
        Long id = readId("ID do produto: ");

        productService.findById(id).ifPresentOrElse(
                product -> showProduct(product),
                () -> System.out.println("Produto não encontrado.")
        );
    }

    private void findAllProducts() {
        List<Product> products = productService.findAll();

        if (products.isEmpty()) {
            System.out.println("Nenhum produto cadastrado.");
            return;
        }

        for (Product product : products) {
            showProduct(product);
        }
    }

    private void updateProduct() {
        Long id = readId("ID do produto: ");

        Product product = productService.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Produto não encontrado.")
        );

        showProduct(product);

        product.setName(readRequired("Novo nome: "));
        product.setCode(readRequired("Novo código: "));
        product.setPrice(readPrice("Novo preço (ex.: 25.50 ou 25,50): "));

        boolean updated = productService.update(product);

        System.out.println(
                updated ? "Produto atualizado." : "Produto não encontrado para atualização."
        );
    }

    private void deleteProductById() {
        Long id = readId("ID do produto: ");

        Product product = productService.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Produto não encontrado.")
        );

        showProduct(product);

        String confirmation = readRequired("Excluir produto e seu registro de estoque? (s/n): ");

        if (!confirmation.equalsIgnoreCase("s")) {
            System.out.println("Exclusão cancelada.");
            return;
        }

        boolean deleted = productService.deleteById(id);

        System.out.println(
                deleted ? "Produto excluído." : "Produto não encontrado para exclusão."
        );
    }

    ///métodos auxiliares
    private Long readId(String prompt) {
        long id = Long.parseLong(readRequired(prompt));

        if (id <= 0) {
            throw new IllegalArgumentException("O ID deve ser positivo.");
        }

        return id;
    }

    private void showClient(Client client) {
        System.out.printf(
                "ID: %d | Nome: %s | CPF: %s | Contato: %s%n",
                client.getId(),
                client.getName(),
                client.getCpf(),
                client.getContact()
        );
    }

    private void showProduct(Product product) {
        System.out.printf(
                "ID: %d | Nome: %s | Código: %s | Preço: %s%n",
                product.getId(),
                product.getName(),
                product.getCode(),
                product.getPrice().toPlainString()
        );
    }

    private BigDecimal readPrice(String prompt) {
        String text = readRequired(prompt);
        BigDecimal price = new BigDecimal(text.replace(',', '.'));

        if (price.signum() < 0) {
            throw new IllegalArgumentException("O preço não pode ser negativo.");
        }

        return price;
    }
}
