![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Projeto Curso EBAC](https://img.shields.io/badge/Projeto--Curso--EBAC-navy?style=for-the-badge)

# ☕ Projeto EBAC – Módulo 30

Projeto de estudo desenvolvido em Java 17 para praticar persistência com JDBC e
PostgreSQL, sem Spring, JPA, Hibernate ou outro ORM.

O código parte de uma implementação JDBC antiga e está sendo reorganizado em
camadas, com responsabilidades bem definidas, DAOs reutilizáveis e testes
automatizados.

## 📍 Checkpoint atual

Atualizado em 12/09/2026.

| Camada | Concluído | Próximo passo |
|---|---|---|
| Domínio | `Client`, `Product`, `Stock`, `SaleItem`, `Sale` e `SaleStatus` | Evoluir as regras de estoque e venda |
| Persistência | CRUD de clientes e produtos; schemas de cliente, produto e estoque | Persistir estoque e vendas |
| Serviço | `ClientService` e `ProductService` com todos os métodos do CRUD | Criar o serviço de estoque |
| Testes | 70 testes de ambiente, integração e unidade | Complementar validações de entrada do domínio e cobrir os próximos services e DAOs |

### Destaques deste checkpoint

- `AbstractDAO<T, ID>` concentra o fluxo JDBC comum do CRUD.
- `ClientDAO` e `ProductDAO` mantêm apenas SQL, parâmetros e mapeamentos.
- `Stock` separa o saldo da entidade `Product`.
- `ClientService` e `ProductService` delegam o CRUD pelo contrato `IGenericDAO`,
  com proteção contra DAO nulo no construtor.
- Os testes dos dois services usam DAOs falsos, sem acesso ao banco.
- `SaleItemTest` cobre subtotal, preservação do preço, quantidades inválidas,
  aumento, redução e proteção contra overflow.
- `SaleTest` cobre itens, totais, remoções inválidas, conclusão, cancelamento
  e bloqueios de operações após o encerramento da venda.
- O ambiente de integração inicializa e limpa o PostgreSQL de testes.

## 🧰 Tecnologias

`Java 17` · `Maven` · `JDBC` · `PostgreSQL` · `DotEnv` · `JUnit 6`

## 🏗️ Arquitetura

```text
Main / Controller
       ↓
    Service
       ↓
 IGenericDAO<T, ID>
       ↓
 AbstractDAO<T, ID>
       ↓
ClientDAO / ProductDAO
       ↓
ConnectionFactory
       ↓
   PostgreSQL
```

O contrato `IGenericDAO<T, ID>` define `create`, `findById`, `findAll`, `update`
e `deleteById`. O `AbstractDAO` implementa o ciclo JDBC desses métodos, enquanto
os DAOs concretos fornecem:

- SQL específico da entidade;
- parâmetros do `PreparedStatement`;
- mapeamento do `ResultSet`;
- leitura e atribuição do ID gerado.

Veja a implementação comentada em [GUIA_ABSTRACT_DAO.md](GUIA_ABSTRACT_DAO.md).

## 🧩 Domínio

### `Client`

Representa o cliente com `id`, `name`, `cpf` e `contact`. O CPF é único no
banco.

### `Product`

Representa o produto com `id`, `name`, `code` e `price`. O código é único no
banco e o preço é armazenado como `BigDecimal`. O banco rejeita preço negativo;
o setter e o service ainda não fazem essa validação. Preço zero é permitido
pela regra atual.

### `Stock`

Mantém a quantidade disponível de um produto já persistido. Permite aumentar e
reduzir o saldo, rejeitando valores inválidos e estoque insuficiente.

### `Sale` e `SaleItem`

A venda mantém cliente, data, status e itens indexados pelo ID do produto. Ela
calcula quantidade e valor total e só pode ser alterada enquanto estiver em
`INITIATED`.

Cada item registra uma quantidade positiva e preserva o preço unitário do
momento da inclusão, mesmo que o preço atual do produto seja alterado.

Adicionar novamente um produto com o mesmo ID acumula a quantidade no item
existente. Remover todas as unidades retira o item da venda; uma remoção parcial
mantém o item com quantidade positiva.

A conclusão exige pelo menos um item. O cancelamento preserva os itens e os
valores para consulta do histórico. Vendas em `COMPLETED` ou `CANCELLED` rejeitam
inclusões, remoções e novas chamadas de conclusão ou cancelamento.

Essas operações ainda ocorrem somente em memória, sem persistência de vendas
ou movimentação de estoque.

## 🗄️ Banco de dados

O banco deve existir antes da execução. O `SchemaInitializer` cria, em uma única
transação, as estruturas abaixo:

- `TB_CLIENT`;
- `TB_PRODUCT`;
- `TB_STOCK`.

As credenciais são carregadas pela `DatabaseConfig` e cada operação obtém uma
nova conexão pela `ConnectionFactory`.

## ⚙️ Configuração local

Crie um `.env` na raiz do projeto com bancos separados para desenvolvimento e
testes:

```text
DB_URL=jdbc:postgresql://localhost:5432/projebac3_dev
DB_USER=postgres
DB_PASSWORD=sua_senha

TEST_DB_URL=jdbc:postgresql://localhost:5432/projebac3_test
TEST_DB_USER=postgres
TEST_DB_PASSWORD=sua_senha
```

O arquivo contém dados locais, está ignorado pelo Git e não deve ser enviado ao
repositório. A configuração também rejeita o uso da mesma URL para os dois
ambientes.

## ✅ Testes

```bash
mvn test
```

| Suíte | Tipo | Testes |
|---|---|---:|
| `DatabaseEnvironmentTest` | Ambiente | 2 |
| `ClientDAOTest` | Integração | 5 |
| `ProductDAOTest` | Integração | 5 |
| `StockTest` | Unidade | 4 |
| `ClientServiceTest` | Unidade | 9 |
| `ProductServiceTest` | Unidade | 9 |
| `SaleItemTest` | Unidade | 15 |
| `SaleTest` | Unidade | 21 |
| **Total** |  | **70** |

`DaoIntegrationTestSupport` prepara o schema e limpa `TB_STOCK`, `TB_CLIENT` e
`TB_PRODUCT` antes e depois de cada teste de integração.

Para executar apenas os testes de services e domínio, sem PostgreSQL:

```bash
mvn -Dtest=ClientServiceTest,ProductServiceTest,StockTest,SaleItemTest,SaleTest test
```

Os testes atuais cobrem os cenários descritos neste checkpoint, sem representar
cobertura exaustiva do domínio. Ainda podemos complementar casos como código
de venda inválido, cliente sem ID e produto nulo.

## 🗺️ Roadmap

- [x] Criar as entidades de domínio `Client` e `Product`.
- [x] Criar os schemas de cliente e produto.
- [x] Definir a interface genérica de CRUD.
- [x] Implementar o ciclo JDBC no `AbstractDAO`.
- [x] Implementar `ClientDAO` e `ProductDAO`.
- [x] Criar `ConnectionFactory` e `DataAccessException`.
- [x] Adicionar DotEnv e proteger o `.env` no Git.
- [x] Criar `DatabaseConfig` para carregar o `.env`.
- [x] Criar `SchemaInitializer` para executar os schemas uma vez.
- [x] Criar e validar banco PostgreSQL exclusivo para testes.
- [x] Implementar factories para os dados dos testes.
- [x] Implementar testes de integração dos DAOs.
- [x] Modelar `SaleStatus`, `SaleItem` e `Sale`.
- [x] Criar `Stock` e separar o saldo de `Product`.
- [x] Criar o schema de estoque com chave estrangeira para produto.
- [x] Implementar e testar o `ClientService`.
- [x] Implementar e testar o `ProductService`.
- [x] Testar cálculo, preço e alterações de quantidade de `SaleItem`.
- [x] Testar itens, totais e transições de status de `Sale`.
- [ ] Complementar testes de validação de entrada do domínio.
- [ ] Definir e implementar validações de cadastro nos services.
- [ ] Implementar persistência e serviço de estoque.
- [ ] Criar os schemas de venda e itens.
- [ ] Implementar persistência transacional de vendas e estoque.
- [ ] Implementar services com as regras de negócio de venda.
- [ ] Criar DTOs e controllers.
- [ ] Montar as dependências e iniciar a aplicação pelo `Main`.
- [ ] Criar testes dos próximos services e controllers.

## 🔐 Segurança

Credenciais reais não devem ser escritas no código ou adicionadas aos resources.
O `.env` deve permanecer somente no ambiente local de cada desenvolvedor.

----

### Fabio Peretti Guimarães | EBAC mod30 - PROJETO 03 | SET 2026
