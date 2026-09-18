# Docker Foundations Study

<p>
  <img alt="Docker" src="https://img.shields.io/badge/Docker-Study-2496ED?logo=docker&logoColor=ffffff">
  <img alt="Docker Compose" src="https://img.shields.io/badge/Docker%20Compose-Study-2496ED?logo=docker&logoColor=ffffff">
</p>

Docker / Docker Compose の基礎を、ハンズオンを通して体系的に学習するためのリポジトリ。  
Image / Container の基本操作から、Port、Volume、Dockerfile、Network、Compose、React / Spring Boot / PostgreSQL を使った複数 Container 構成、基本的な Best Practice までを段階的に扱う。  
各 Unit に学習用ドキュメントと必要な実践ファイルを配置し、概念と実際の操作・構成を対応づけながら見返せる形で整理している。

---

## このリポジトリの位置づけ

このリポジトリは、Web Application 開発で Docker を利用するために必要となる基礎を、ほぼゼロから段階的に学習するための `docker-foundations-study` である。

前半では Docker / Image / Container の基本、Port、Bind Mount、Volume、Dockerfile などを個別に確認する。  
中盤では React / Vite、Spring Boot、PostgreSQL をそれぞれ Container 化し、Docker Network と Docker Compose を使って複数 Container を扱う。  
後半では Application + Database の 2 Container 構成、Frontend + Backend + Database の 3 Container 構成まで発展させ、最後に Multi-stage Build、Healthcheck、Restart Policy、non-root などの基本的な改善観点を確認する。

Container Infrastructure や Production 運用全般を網羅することは目的とせず、Application Engineer として Docker を利用するときの基礎を対象とする。  
Kubernetes などの Orchestration、CI/CD からの自動 Build / Push、本格的な Production Security などは学習範囲に含めない。

---

## 学習目的

このリポジトリでは、主に次の内容を目的として学習を行う。

- Docker / Image / Container の関係と基本的な Lifecycle を理解する
- Docker CLI を使って Image / Container の作成、起動、停止、確認、削除を行えるようにする
- Host と Container の Port Mapping、Bind Mount、Named Volume の役割を理解する
- Dockerfile から Custom Image を Build し、Instruction と Layer / Build Context の関係を理解する
- Docker Network と Service 名による Container 間通信を理解する
- Docker Compose で複数 Service / Network / Volume / Environment Variable をまとめて管理できるようにする
- Spring Boot + PostgreSQL の 2 Container 構成を構築し、Database 接続と Data 永続化を確認できるようにする
- React + Spring Boot + PostgreSQL の 3 Container 構成を構築し、Browser / Host / Docker Network から見た接続先の違いを説明できるようにする
- Multi-stage Build、Build Cache、`.dockerignore`、Healthcheck、Restart Policy、non-root など、動作後の基本的な改善観点を理解する
- Docker Hub への Tag / Push を通して、Build 済み Image を Registry へ配置する基本的な流れを確認する

---

## 学習範囲

このリポジトリで扱う Unit は次の通り。

| Unit | 内容                                                                                                    |
| ---- | ------------------------------------------------------------------------------------------------------- |
| 01   | [Docker・Container の基本概念と環境確認](units/01-docker-container-basics/README.md)                    |
| 02   | [Image・Container の基本操作とライフサイクル](units/02-image-container-operations/README.md)            |
| 03   | [Port・Bind Mount・Volume](units/03-ports-bind-mounts-volumes/README.md)                                |
| 04   | [Dockerfile と Image Build](units/04-dockerfile-image-build/README.md)                                  |
| 05   | [React / Vite アプリの Docker 化](units/05-react-vite-docker/README.md)                                 |
| 06   | [Spring Boot アプリの Docker 化](units/06-spring-boot-docker/README.md)                                 |
| 07   | [PostgreSQL Container とデータ永続化](units/07-postgresql-container/README.md)                          |
| 08   | [Docker Network と Container 間通信](units/08-docker-network/README.md)                                 |
| 09   | [Docker Compose の基本](units/09-docker-compose/README.md)                                              |
| 10   | [Spring Boot + PostgreSQL の 2 Container 構成](units/10-spring-boot-postgresql/README.md)               |
| 11   | [React + Spring Boot + PostgreSQL の 3 Container 構成](units/11-react-spring-boot-postgresql/README.md) |
| 12   | [Dockerfile・Compose の改善と基本的な Best Practice](units/12-docker-best-practices/README.md)          |

### 各 Unit の位置づけ

- **Unit 01: Docker・Container の基本概念と環境確認**  
  Docker、Image、Container、Host、Registry などの基本概念を整理し、`hello-world` を使って最初の Container を起動する。

- **Unit 02: Image・Container の基本操作とライフサイクル**  
  Image の取得・確認・削除、Container の作成・起動・停止・再起動・削除、Logs、`exec`、`inspect` など、Docker CLI の基本操作を確認する。

- **Unit 03: Port・Bind Mount・Volume**  
  Host / Container の Port、Port Publishing、Bind Mount、Named Volume を扱い、外部からの接続と Data / File の共有・永続化を確認する。

- **Unit 04: Dockerfile と Image Build**  
  `FROM`、`WORKDIR`、`COPY`、`RUN`、`ENV`、`EXPOSE`、`CMD` などを使い、Dockerfile から Custom Image を Build する流れを確認する。

- **Unit 05: React / Vite アプリの Docker 化**  
  最小構成の React / Vite Application を Container 上で起動し、Vite Development Server と Host からの接続を確認する。

- **Unit 06: Spring Boot アプリの Docker 化**  
  Spring Boot の実行可能 JAR を Runtime Image へ配置し、Environment Variable を利用しながら API を Container 上で動かす。

- **Unit 07: PostgreSQL Container とデータ永続化**  
  PostgreSQL Official Image、初期設定用 Environment Variable、接続、Named Volume を扱い、Container を再作成しても Data が保持されることを確認する。

- **Unit 08: Docker Network と Container 間通信**  
  User-defined Bridge Network を作成し、同じ Network 上の Container が名前解決を使って通信する仕組みと `localhost` の意味を確認する。

- **Unit 09: Docker Compose の基本**  
  `compose.yaml` を使い、Service、Network、Volume、Port、Environment Variable、`depends_on` などを宣言的に管理する基本を確認する。

- **Unit 10: Spring Boot + PostgreSQL の 2 Container 構成**  
  Spring Boot から PostgreSQL Service 名を使って Database へ接続し、API から Data を取得する Application + Database 構成を構築する。

- **Unit 11: React + Spring Boot + PostgreSQL の 3 Container 構成**  
  Browser → React → Spring Boot → PostgreSQL の通信経路を構築し、Browser から見た URL と Docker Network 内の Service 名、CORS の違いを確認する。

- **Unit 12: Dockerfile・Compose の改善と基本的な Best Practice**  
  Multi-stage Build、Build Cache、`.dockerignore`、Base Image / Tag、Healthcheck、Restart Policy、non-root、Image Size、Docker Hub Push を扱い、動作する構成を改善する観点を確認する。

---

## 学習の進め方

基本的な進め方は次の通り。

1. `units/` 配下の対象 Unit の `README.md` を開く
2. `この項目の目的` と `学習内容` を読み、対象となる概念と今回のハンズオンの位置づけを確認する
3. `使用するもの` と `事前準備` を確認する
4. `ハンズオン` の手順に沿って Docker / Docker Compose のコマンドや Application の Build・起動を実行する
5. `docker container ls`、`docker image ls`、`docker network inspect`、`docker volume ls`、`docker compose ps`、`docker compose logs` などを使い、操作前後の状態を確認する
6. Browser、`curl`、Container 内の Command などを使い、Port、Network、Environment Variable、Volume、Application 間通信の結果を確認する
7. `動作・確認ポイント` と実際の結果を照らし合わせる
8. `学習ポイント` を読み、操作と Docker の仕組みを対応づけて整理する
9. `完了条件` を確認し、対象 Unit の学習を完了する

各 Unit の Application Code は、React / Spring Boot / PostgreSQL 自体の機能を深く学ぶためではなく、Docker の仕組みを確認するための最小構成としている。  
Docker に直接関係する設定・処理は、ソースコードや設定ファイル内のコメントからも意図を確認できるようにしている。

---

## 前提環境

- Windows
- Docker Desktop
  - WSL 2 backend
  - Linux Containers
- Git Bash

Docker の学習操作は、基本的に Windows 上の Git Bash から実行する。  
WSL 2 は主に Docker Desktop の Backend として利用し、通常の学習操作のために WSL / Ubuntu Shell へ入る構成にはしていない。

Unit によって React / Vite、Spring Boot、PostgreSQL などを利用するため、必要な Runtime / Build Tool は各 Unit の `README.md` を参照する。

---

## 使用技術・ツール

### Docker

- Docker
- Docker Compose
- Docker Desktop
- Docker Hub

### Frontend

- Node.js 24
- npm
- React 19
- Vite 8
- Nginx 1.30

### Backend

- Java 21
- Spring Boot 4
- Maven 3.9

### Database

- PostgreSQL 18

### Development

- Git Bash

Version は学習時に使用した主要 Version を記載している。  
詳細な Image Tag や Unit ごとの Version は各 Unit の Dockerfile / `compose.yaml` / `README.md` を参照する。

---

## セットアップ

Docker Desktop を起動した状態で、Git Bash から Docker / Docker Compose が利用できることを確認する。

```bash
docker version
docker compose version
```

Repository Root から学習する Unit へ移動する。

```bash
cd units/<unit-directory>
```

各 Unit で必要な Build、Image 取得、Container 起動、`.env` 作成などの手順は、それぞれの `README.md` に記載している。

例えば Unit 01 から開始する場合は、次を参照する。

```text
units/01-docker-container-basics/README.md
```

各 Unit は学習履歴を独立して残すため、後続 Unit から過去 Unit の実装を直接変更する前提にはしていない。  
複数の技術を組み合わせる Unit では、その Unit 配下に必要な実践用ファイルをまとめて配置している。

---

## リポジトリ構成

主要な構成は次の通り。

```text
docker-foundations-study/
├─ docs/
│  └─ planning/
│     ├─ learning-curriculum.md
│     └─ learning-operation.md
│
├─ units/
│  ├─ 01-docker-container-basics/
│  ├─ 02-image-container-operations/
│  ├─ 03-ports-bind-mounts-volumes/
│  ├─ 04-dockerfile-image-build/
│  ├─ 05-react-vite-docker/
│  ├─ 06-spring-boot-docker/
│  ├─ 07-postgresql-container/
│  ├─ 08-docker-network/
│  ├─ 09-docker-compose/
│  ├─ 10-spring-boot-postgresql/
│  ├─ 11-react-spring-boot-postgresql/
│  └─ 12-docker-best-practices/
│
├─ .gitignore
└─ README.md
```

### 各ディレクトリ・ファイルの役割

#### `docs/planning/`

Repository 全体の学習計画・運用方針を配置する。

- `learning-curriculum.md`  
  Unit 01〜12 の学習範囲、目的、主な学習内容、サンプル方針などを整理する。
- `learning-operation.md`  
  Repository 構成、Unit の進め方、ドキュメント、ソースコード / 設定ファイルなどの全体方針を整理する。

#### `units/`

実際の学習を行う Unit を配置する。  
各 Unit は独立したディレクトリとし、基本的に `README.md` と、その Unit のハンズオンに必要なソースコード / Dockerfile / `compose.yaml` / 設定ファイル などを配置する。

各 Unit の `README.md` は、基本的に次の構成としている。

```text
この項目の目的
学習内容
使用するもの
事前準備
ハンズオン
動作・確認ポイント
学習ポイント
完了条件
```

---

## ドキュメント

### 学習カリキュラム

[`docs/planning/learning-curriculum.md`](docs/planning/learning-curriculum.md)

Docker の基礎から複数 Container 構成、基本的な Best Practice まで、Unit 01〜12 の学習内容と順序を整理している。

### 学習・運用方針

[`docs/planning/learning-operation.md`](docs/planning/learning-operation.md)

Repository 構成、Unit ごとのドキュメント、サンプルの独立性、ソースコード / 設定ファイルのコメント方針などを整理している。

### Unit ドキュメント

各 Unit の詳細な学習内容、ハンズオン、確認ポイントは `units/<unit>/README.md` に記載している。  
Root `README.md` は Repository 全体の入口とし、Docker の個別概念やコマンドの詳細は各 Unit のドキュメントから確認する。

---

## このリポジトリで確認できること

- Docker / Image / Container の基本概念と Lifecycle
- Docker CLI による Image / Container の基本操作
- Host / Container の Port Mapping
- Bind Mount / Named Volume と Data 永続化
- Dockerfile による Custom Image Build
- Docker Build Context / Layer / Cache の基本
- React / Vite Application の Container 化
- Spring Boot Application の Container 化
- PostgreSQL Official Image の利用と Data 永続化
- User-defined Bridge Network と Service / Container 名による通信
- Docker Compose による Service / Network / Volume / Environment Variable の管理
- Spring Boot + PostgreSQL の 2 Container 構成
- React + Spring Boot + PostgreSQL の 3 Container 構成
- Browser / Host / Docker Network 内部から見た接続先の違い
- React → Spring Boot の API 通信と CORS
- Multi-stage Build による Build / Runtime の分離
- `.dockerignore` と Build Cache の基本的な改善
- Base Image / Version Tag / Image Size の基本的な考え方
- Healthcheck と `depends_on.condition: service_healthy`
- Restart Policy
- `USER` を使った Spring Boot Container の non-root 実行
- Docker Hub への Login / Tag / Push と Registry の基本
