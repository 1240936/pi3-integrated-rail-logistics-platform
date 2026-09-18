

# pi3-integrated-rail-logistics-platform

Plataforma integrada de logística ferroviária desenvolvida no **Projeto Integrador do 3.º semestre** do curso de **Licenciatura em Engenharia Informática (LEI)** do **ISEP**, pela **Equipa g052** (ano letivo 2025/26).

O projeto modela um operador logístico ferroviário e intermodal, integrando gestão de armazém, análise de redes ferroviárias, operações de carga e controlo embebido de estações.

**Repositório:** [github.com/1240936/pi3-integrated-rail-logistics-platform](https://github.com/1240936/pi3-integrated-rail-logistics-platform)

---

## Índice

- [Visão geral](#visão-geral)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e execução](#instalação-e-execução)
- [Base de dados Oracle](#base-de-dados-oracle)
- [Testes](#testes)
- [Equipa](#equipa)
- [Documentação adicional](#documentação-adicional)

---

## Visão geral


| Módulo          | Tecnologia           | Descrição                                                                     |
| --------------- | -------------------- | ----------------------------------------------------------------------------- |
| `warehouseMngt` | Java 11, Maven       | Inventário, picking, indexação espacial (AVL, KD-tree) e algoritmos de grafos |
| `freightMngt`   | Java 11, Oracle JDBC | Planificação de rotas, montagem de comboios, scheduling e dispatch            |
| `stationMngt`   | C, RISC-V Assembly   | Sistema embebido multi-componente para controlo de estação                    |
| `sql-scripts`   | Oracle SQL/PL/SQL    | Modelo relacional, triggers e procedimentos armazenados                       |
| `doc`           | Markdown, SVG        | Especificações e modelo físico da base de dados                               |
| `scrum`         | —                    | Relatórios de sprint e registos de daily standups                             |


### Funcionalidades principais

- **Armazém:** inventário FEFO/FIFO, alocação de encomendas, planos de picking e gestão de devoluções
- **Rede ferroviária:** consultas espaciais, MST, centralidade de hubs, fluxo máximo e caminhos com risco
- **Carga e comboios:** rotas, composição de material circulante e dispatch com deteção de cruzamentos
- **Estações:** sensores, sinalização, atribuição de vias e logging de ações

---

## Estrutura do repositório

```
pi3-integrated-rail-logistics-platform/
├── warehouseMngt/          # Aplicação Java — armazém e algoritmos (USEI01–USEI15)
│   ├── src/main/           # Código-fonte
│   ├── test_cases/         # Testes JUnit
│   ├── testFiles/          # Ficheiros CSV de teste
│   └── pom.xml
├── freightMngt/            # Aplicação Java — gestão de carga (USLP08–USLP10)
│   ├── src/main/
│   └── pom.xml
├── stationMngt/            # Sistema embebido (USAC01–USAC16)
│   ├── sprint3/            # Componentes Manager, UI, Board, Sensors, LightSigns
│   └── USAC01–09/          # Funções em assembly RISC-V
├── sql-scripts/            # Scripts Oracle por user story (USBD31–USBD45, USLP08–10)
├── doc/                    # Documentação e modelo físico
├── scrum/                  # Relatórios Scrum
└── README.md
```

---

## Pré-requisitos


| Ferramenta                   | Versão       | Necessário para                                     |
| ---------------------------- | ------------ | --------------------------------------------------- |
| **JDK**                      | 11+          | `warehouseMngt`, `freightMngt`                      |
| **Maven**                    | 3.6+         | Compilar e testar módulos Java                      |
| **Oracle Database**          | 19c+ (ou XE) | `freightMngt` e `sql-scripts`                       |
| **SQL*Plus / SQL Developer** | —            | Executar scripts SQL                                |
| **GCC + Make**               | —            | Compilar `stationMngt` (Linux/WSL)                  |
| **Toolchain RISC-V**         | Bootlin      | Emulação dos componentes embebidos (`qemu-riscv32`) |
| **Graphviz**                 | —            | Exportação de grafos (USEI12, opcional)             |


---

## Instalação e execução

### 1. Clonar o repositório

```bash
git clone https://github.com/1240936/pi3-integrated-rail-logistics-platform.git
cd pi3-integrated-rail-logistics-platform
```

### 2. Warehouse Management (`warehouseMngt`)

Aplicação de consola para operações de armazém e algoritmos de rede. Não requer base de dados.

```bash
cd warehouseMngt
mvn compile
mvn exec:java -Dexec.mainClass="main.Main"
```

Alternativa via IDE (IntelliJ / VS Code): executar a classe `main.Main`.

**Testes:**

```bash
mvn test
```

**Dados de teste:** ficheiros CSV em `warehouseMngt/testFiles/` (ex.: `stations.csv`, `lines.csv`).

---

### 3. Freight Management (`freightMngt`)

Aplicação de consola ligada à base de dados Oracle. Requer que o schema esteja criado (ver secção [Base de dados Oracle](#base-de-dados-oracle)).

```bash
cd freightMngt
mvn compile
mvn exec:java -Dexec.mainClass="main.Main"
```

Na primeira execução, a aplicação pede credenciais de ligação à base de dados (utilizador, password, URL JDBC).

**Exemplo de URL JDBC:**

```
jdbc:oracle:thin:@localhost:1521/XEPDB1
```

---

### 4. Station Management (`stationMngt`)

Sistema embebido em C com funções RISC-V Assembly. Destinado a ambiente Linux/WSL com toolchain Bootlin.

```bash
cd stationMngt/sprint3
make build
make run
```

Para testes unitários das funções de assembly (Sprint 2):

```bash
cd stationMngt/USAC01
make test
```

> **Nota:** A execução completa do Sprint 3 usa `qemu-riscv32` e comunicação serial simulada entre componentes (UI, Manager, Board).

---

## Base de dados Oracle

Executar os scripts por ordem numa sessão SQL*Plus ou SQL Developer:

1. **Schema principal:** `sql-scripts/USBD31/USBD31.sql`
2. **Dados iniciais:** `sql-scripts/USBD32/USBD32.sql`, `sql-scripts/USBD33/USBD33.sql`
3. **Procedimentos para a app Java:** `sql-scripts/USLP08/`, `USLP09/`, `USLP10/`

```bash
sqlplus utilizador/password@localhost:1521/XEPDB1 @sql-scripts/USBD31/USBD31.sql
```

Regras de negócio adicionais documentadas em `doc/USBD31/SUPPLEMENTAL_SPECIFICATION.md`.

---

## Testes


| Módulo          | Comando                | Framework |
| --------------- | ---------------------- | --------- |
| `warehouseMngt` | `mvn test`             | JUnit 4   |
| `freightMngt`   | `mvn test`             | JUnit 4   |
| `stationMngt`   | `make test` (por USAC) | Unity (C) |


---

## Equipa


| N.º Estudante | Nome                  |
| ------------- | --------------------- |
| 1240892       | Maria Pinto           |
| 1240934       | Francisco Vasconcelos |
| 1240935       | Gabriel Inácio        |
| 1240936       | Gonçalo Azevedo       |
| 1240941       | Paulo Ferreira        |


---

## Documentação adicional

- `doc/auth.md` — identificação da equipa
- `doc/USBD31/Physical Model.svg` — modelo físico da base de dados
- `doc/USBD31/SUPPLEMENTAL_SPECIFICATION.md` — regras de negócio
- `warehouseMngt/temporalanalysis/` — análise de complexidade temporal (USEI11–15)
- `scrum/SprintReport/` — relatórios de sprint

---

## Licença

Projeto académico desenvolvido no âmbito do curso LEI-ISEP. Consultar a equipa antes de reutilização externa.
