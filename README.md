![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Projeto Curso EBAC](https://img.shields.io/badge/Projeto--Curso--EBAC-navy?style=for-the-badge)

# ☕ Projeto EBAC – Módulo 30

Projeto de estudo desenvolvido em Java 17 para praticar persistência com JDBC e
PostgreSQL, sem Spring, JPA, Hibernate ou outro ORM.

O código parte de uma implementação JDBC antiga e foi reorganizado em
camadas, com responsabilidades bem definidas, DAOs reutilizáveis e testes
automatizados.

## 📍 Checkpoint atual

Atualizado em 14/09/2026. Escopo funcional planejado para a entrega concluído.

| Camada | Implementado |
|---|---|
| Domínio | Clientes, produtos, estoque, vendas e itens; reconstrução de vendas com preços históricos |
| Persistência | CRUD de clientes, produtos e estoque; criação, consulta, conclusão e cancelamento de vendas |
| Serviço | `ClientService`, `ProductService`, `StockService` e `SaleService` |
| Console | `Main` e `AppController` com submenus para clientes, produtos, estoque e vendas |
| Validação | 87 testes aprovados e fluxo de console conferido manualmente pelo autor |

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
- `StockDAO` implementa o CRUD usando o ID do produto e consulta os dados com
  `JOIN`; seus sete testes de integração foram aprovados.
- `StockService` delega as operações ao DAO, com proteção contra dependência nula.
- `SaleDAO.create` grava venda e itens na mesma transação, com commit e rollback.
  O ID só é atribuído ao objeto após o commit.
- `SaleDAO.findById` recupera cliente e itens e usa `Sale.restore` para preservar
  data, status e preços históricos.

- A conclusão da venda atualiza status e estoque na mesma transação, com
  rollback se faltar saldo em qualquer item.
- O cancelamento preserva os itens e não movimenta estoque. Apenas vendas
  iniciadas podem ser concluídas ou canceladas.
- O console oferece cadastro, busca, listagem, alteração e exclusão de clientes
  e produtos, além de manutenção de estoque e operações de venda.

A suíte completa passou com 87 testes, sem falhas ou erros. O autor também
confirmou manualmente o fluxo de cadastro, criação e consulta de venda,
conclusão com baixa de estoque e cancelamento pelo console.

## 🧰 Tecnologias

`Java 17` · `Maven` · `JDBC` · `PostgreSQL` · `DotEnv` · `JUnit 6`

## 🏗️ Arquitetura

```text
Main → AppController → Services

ClientService / ProductService → IGenericDAO → AbstractDAO → ClientDAO / ProductDAO
StockService                   → IGenericDAO → StockDAO
SaleService                    → SaleDAO → venda, itens e baixa transacional de estoque

Todos os DAOs → ConnectionFactory → PostgreSQL
```

`SaleDAO` possui operações específicas e não implementa o CRUD genérico.
O `Main` monta as dependências e inicia o `AppController`. O controller coleta
as entradas, chama os services e apresenta os resultados.

O contrato `IGenericDAO<T, ID>` define `create`, `findById`, `findAll`, `update`
e `deleteById`. O `AbstractDAO` implementa o ciclo JDBC desses métodos para
clientes e produtos, cujos DAOs concretos fornecem:

- SQL específico da entidade;
- parâmetros do `PreparedStatement`;
- mapeamento do `ResultSet`;
- leitura e atribuição do ID gerado.

Veja a implementação comentada em [GUIA_ABSTRACT_DAO.md](GUIA_ABSTRACT_DAO.md).

`StockDAO` implementa `IGenericDAO<Stock, Long>` diretamente porque o estoque
utiliza o ID do produto, sem gerar um identificador próprio.

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

Para executar apenas os testes de services e domínio, sem PostgreSQL:

```bash
mvn -Dtest=ClientServiceTest,ProductServiceTest,StockTest,SaleItemTest,SaleTest test
```

Os testes atuais cobrem os cenários descritos neste checkpoint, sem representar
cobertura exaustiva do domínio. Os próximos blocos terão no máximo dez testes
novos cada, priorizando fluxos principais, integridade e rollback. Testes
adicionais de validação do domínio ficam como melhoria futura.

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
- [x] Implementar `StockDAO` e validar com sete testes de integração.
- [x] Implementar a delegação do CRUD no `StockService`.
- [x] Criar e registrar os schemas de venda e itens.
- [x] Atualizar a preparação dos testes para as cinco tabelas.
- [x] Implementar `Sale.restore` e a leitura dos preços históricos.
- [x] Implementar `SaleDAO.create` com transação para venda e itens.
- [x] Implementar `SaleDAO.findById` com reconstrução da venda.
- [x] Validar persistência, consulta e rollback em dez testes de `SaleDAO`.
- [x] Implementar serviço de vendas e persistência das transições de status.
- [x] Integrar conclusão da venda e baixa de estoque na mesma transação.
- [x] Criar um `AppController` de console e iniciar o fluxo pelo `Main`.
- [x] Executar a suíte completa e finalizar a documentação de entrega.

- [x] Conferir manualmente o fluxo completo pelo console.

Melhorias futuras: ampliar validações de cadastro e seus testes, e introduzir
DTOs ou separar controllers se o fluxo da aplicação justificar.

## 🔐 Segurança

Credenciais reais não devem ser escritas no código ou adicionadas aos resources.
O `.env` deve permanecer somente no ambiente local de cada desenvolvedor.

----

### Fabio Peretti Guimarães | EBAC mod30 - PROJETO 03 | SET 2026
