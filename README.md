![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Projeto Curso EBAC](https://img.shields.io/badge/Projeto--Curso--EBAC-navy?style=for-the-badge)

# ☕ PersistentDAO 001 - Projeto DAO com persistência de Cliente e Produtos

Projeto DAO persistência em PostgreSQL, implementado puramente JDBC com separação de responsabilidades,
DAO e interfaces reutilizáveis e testes jUnit em um cenário mais próximo do real. Mas,
sem Spring, JPA, Hibernate ou outro ORM.

> Este projeto foi pensado e construído ao longo de dois meses, passando por reestruturações desde a fase inicial,
> seguindo soluções de outros projetos DAO e feito para se assemelhar o mais próximo possível de um projeto real.
> A intenção é que este projeto possa ser usado como portifólio e case de estudo para uso de JDBC.

### 🏛 Arquitetura do projeto️
| Camada | Implementado                                                                                      |
|---|---------------------------------------------------------------------------------------------------|
| Domínio | Clientes, produtos, estoque, vendas e itens com histórico de preço;                               |
| Persistência | CRUD de clientes, produtos e estoque, com registro, consulta, conclusão e cancelamento de vendas; |
| Serviço | `ClientService`, `ProductService`, `StockService` e `SaleService`                                 |
| Console | `Main` e `AppController` com submenus para clientes, produtos, estoque e vendas                   |
| Validação | 87 testes aprovados e fluxo de console conferido manualmente pelo autor                           |


<details><summary>Estrutura detalhada</summary>

```
src
├── main
│   ├── java (com.fabioperettig)
│   │   ├── .config
│   │   │     ├── ConnectionFactory
│   │   │     ├── DatabaseConfig
│   │   │     └── SchemaInitializer
│   │   │
│   │   ├── .controller
│   │   │     └── SchemaInitializer
│   │   │
│   │   ├── .dao
│   │   │     ├── AbstractDAO
│   │   │     ├── ClientDAO
│   │   │     ├── IGenericDAO
│   │   │     ├── ProductDAO
│   │   │     ├── SaleDAO
│   │   │     └── StockDAO
│   │   │
│   │   ├── .domain
│   │   │     ├── Client
│   │   │     ├── Product
│   │   │     ├── Sale
│   │   │     ├── SaleItem
│   │   │     ├── SaleStatus
│   │   │     └── Stock
│   │   │
│   │   ├── .exception
│   │   │     └── DataAccessException
│   │   │
│   │   ├── .service
│   │   │     ├── ClientService
│   │   │     ├── IGenericService
│   │   │     ├── ProductService
│   │   │     ├── SaleService
│   │   │     └── StockService
│   │   │
│   │   └── Main     
│   │
│   └── resources
│       └── database
│            ├── schemaClient.sql
│            ├── schemaProduct.sql
│            ├── schemaSale.sql
│            ├── schemaSaleItem.sql
│            └── schemaStock.sql
│
└── test
    └── java (com.fabioperettig)
        ├── .config
        │     └── DatabaseEnvironment
        │
        ├── .dao
        │     ├── ClientDAOTest
        │     ├── DaoIntegrationTestSupport
        │     ├── ProductDAOTest
        │     ├── SaleDAOTest
        │     └── StockDAOTest
        │
        ├── .domain
        │     ├── SaleTest
        │     ├── SaleItemTest
        │     └── StockTest
        │
        ├── .factory (pattern para Mocks)
        │     ├── ClientTestFactory
        │     └── ProductTestFactory
        │
        └── .service
              ├── ClientServiceTest
              └── ProductServiceTest

```

</details>

### ✨ Destaques do projeto

- `AbstractDAO<T, ID>` concentra o fluxo JDBC comum do CRUD.
- `Stock` separa o saldo da entidade `Product`.
- `ClientDAO` e `ProductDAO` mantêm apenas SQL, parâmetros e mapeamentos.
- `SaleDAO.findById` recupera cliente e itens e usa `Sale.restore` para preservar
  data, status e preços históricos.
- `ClientService` e `ProductService` delegam o CRUD pela interface `IGenericDAO`.
- O ambiente de integração inicializa e limpa o PostgreSQL de testes.
- Os testes usam DAOs falsos, com entidades Mock e sem acesso ao banco real.
- O console oferece um CRUD completo: cadastro, busca, listagem, alteração e exclusão de clientes
  e produtos, além de manutenção de estoque e operações de venda.

>Veja a implementação em detalhes em [GUIA_COMPLETO_PROJETO_JDBC.md](GUIA_COMPLETO_PROJETO_JDBC.md).

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

O saldo fica somente em `TB_STOCK`, sem duplicação em `TB_PRODUCT`.
`isAvailable()` deriva a disponibilidade da quantidade. A chave primária
`product_id` permite no máximo um estoque por produto, e a chave estrangeira
impede estoque para produto inexistente. Excluir o produto remove seu estoque
em cascata; excluir apenas o estoque preserva o produto.

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

`Sale.restore` reconstrói uma venda com ID, data, status e preços dos itens
fornecidos pelo DAO. A criação e a consulta por ID são validadas por testes
com PostgreSQL, incluindo preservação do preço histórico e rollback dos itens.

`Sale.complete()` e `Sale.cancel()` alteram o objeto em memória. No fluxo de
console, `SaleService` chama as operações persistidas do `SaleDAO` por ID.
A conclusão baixa o estoque e confirma o status em uma única transação;
se ocorrer falha, ambos são desfeitos. Uma conclusão repetida é rejeitada para
impedir uma segunda baixa. O cancelamento altera somente o status no banco.
Novas vendas só podem ser persistidas em `INITIATED`.

O console monta os itens antes de salvar a venda. A edição dos itens de uma
venda já persistida não faz parte do fluxo implementado.

## 🗄️ Banco de dados

O banco deve existir antes da execução. O `SchemaInitializer` cria, em uma única
transação, as estruturas abaixo:

- `TB_CLIENT`;
- `TB_PRODUCT`;
- `TB_STOCK`;
- `TB_SALE`;
- `TB_SALE_ITEM`.

`TB_SALE` referencia o cliente e restringe os valores de status. `TB_SALE_ITEM`
usa a chave composta `(sale_id, product_id)` e armazena quantidade positiva e
preço unitário não negativo. O total é calculado pelos itens.

A exclusão física da venda remove seus itens em cascata. Clientes com vendas
e produtos referenciados por itens de venda têm a exclusão bloqueada pelas
chaves estrangeiras. Cancelar uma venda preserva seu histórico.

As credenciais são carregadas pela `DatabaseConfig` e cada operação obtém uma
nova conexão pela `ConnectionFactory`.
Na criação de uma venda, a mesma conexão é compartilhada entre os INSERTs da
venda e de seus itens para permitir commit ou rollback do conjunto.

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

## ▶️ Executar a aplicação

Com o PostgreSQL disponível e o `.env` configurado, abra o projeto Maven no
IntelliJ e execute `com.fabioperettig.Main`. Use a raiz do projeto como diretório
de trabalho para carregar o `.env`. O `Main` usa `DB_*` (desenvolvimento),
inicializa os schemas e abre o menu:

```text
1 - Clientes
2 - Produtos
3 - Estoque
4 - Vendas
0 - Sair
```

- **Clientes e produtos:** cadastrar, buscar por ID, listar, alterar e excluir.
- **Estoque:** cadastrar saldo inicial, consultar por produto, listar, ajustar
  o saldo total e excluir o registro mantendo o produto.
- **Vendas:** buscar por ID, criar com vários itens, concluir e cancelar.

Para criar uma venda, informe o cliente e os produtos pelos IDs retornados nos
cadastros. Digite `0` ao terminar os itens. A criação não baixa estoque; a baixa
ocorre na conclusão. O ajuste manual de estoque informa o novo saldo total.
Os preços aceitam ponto ou vírgula decimal, sem separador de milhar.

Roteiro de demonstração validado: cadastrar cliente e produto a `10.00`, criar
estoque de oito unidades e vender três. Antes da conclusão, o estoque permanece
em oito; depois, a venda fica `COMPLETED` e o saldo passa a cinco. Criar outra
venda e cancelá-la preserva seus itens e mantém o saldo em cinco.

## ✅ Testes

```bash
mvn test
```

| Suíte | Tipo | Testes |
|---|---|---:|
| `DatabaseEnvironmentTest` | Ambiente | 2 |
| `ClientDAOTest` | Integração | 5 |
| `ProductDAOTest` | Integração | 5 |
| `StockDAOTest` | Integração | 7 |
| `SaleDAOTest` | Integração | 10 |
| `StockTest` | Unidade | 4 |
| `ClientServiceTest` | Unidade | 9 |
| `ProductServiceTest` | Unidade | 9 |
| `SaleItemTest` | Unidade | 15 |
| `SaleTest` | Unidade | 21 |
| **Total** |  | **87** |

`DaoIntegrationTestSupport` prepara os schemas e limpa as cinco tabelas em um
único `TRUNCATE` antes e depois de cada teste de integração.

`StockDAOTest` cobre criação e leitura, atualização, exclusão sem remover o
produto, exclusão em cascata, listagem com saldo zero, estoque duplicado e
produto inexistente.

`SaleDAOTest` cobre criação e consulta, preço histórico, rollback de inserção,
busca sem resultado, conclusão com baixa, rollback por estoque insuficiente,
conclusão repetida, cancelamento sem baixa e transições proibidas de status.
O console foi verificado manualmente; não há testes automatizados específicos
para o controller, `StockService` ou `SaleService`.

## 🔐 Segurança

Credenciais reais não devem ser escritas no código ou adicionadas aos resources.
O `.env` deve permanecer somente no ambiente local de cada desenvolvedor.

----

### Fabio Peretti Guimarães | EBAC mod30 - PROJETO 03 | SET 2026
