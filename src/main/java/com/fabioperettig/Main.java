package com.fabioperettig;

import com.fabioperettig.config.ConnectionFactory;
import com.fabioperettig.config.DatabaseConfig;
import com.fabioperettig.config.SchemaInitializer;
import com.fabioperettig.controller.AppController;
import com.fabioperettig.dao.ClientDAO;
import com.fabioperettig.dao.ProductDAO;
import com.fabioperettig.dao.SaleDAO;
import com.fabioperettig.dao.StockDAO;
import com.fabioperettig.service.ClientService;
import com.fabioperettig.service.ProductService;
import com.fabioperettig.service.SaleService;
import com.fabioperettig.service.StockService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        DatabaseConfig config = DatabaseConfig.load();
        ConnectionFactory connectionFactory = config.createConnectionFactory();

        new SchemaInitializer(connectionFactory).initialize();

        ClientService clientService = new ClientService(new ClientDAO(connectionFactory));
        ProductService productService = new ProductService(new ProductDAO(connectionFactory));
        StockService stockService = new StockService(new StockDAO(connectionFactory));
        SaleService saleService = new SaleService(new SaleDAO(connectionFactory));

        try (Scanner scanner = new Scanner(System.in)) {
            AppController controller = new AppController(
                    clientService, productService, stockService, saleService, scanner
            );

            controller.start();
        }
    }
}
