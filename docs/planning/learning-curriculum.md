# Docker Foundations Study - Learning Curriculum

## 1. 学習目的

本リポジトリでは、Docker / Container について体系的な学習経験や十分な実務経験がない状態から、実際に手を動かしながら基礎を一周し、アプリケーション開発で Docker を扱うための土台を身につけることを目的とする。  
単に Docker コマンドを覚えるのではなく、Image / Container / Port / Volume / Network / Dockerfile / Docker Compose などの役割と関係を段階的に理解し、最終的には複数 Container で構成された Web システムを自分で起動・確認できる状態を目指す。  
学習では Docker 自体の理解と操作を中心とし、React / Spring Boot / PostgreSQL などは Docker を学ぶための題材として必要最小限の構成で扱う。

## 2. 到達目標

本カリキュラム完了時に、以下を自分で説明・操作できる状態を目指す。

- Docker / Container が何のための仕組みか説明できる。
- Image と Container の違いと関係を説明できる。
- Docker CLI を使って Image / Container の基本操作ができる。
- Host と Container の Port の関係を理解し、Port Mapping を設定できる。
- Bind Mount と Volume の違いを理解し、用途に応じて使い分けられる。
- Dockerfile を読み書きし、自分で Image を Build できる。
- React / Vite や Spring Boot などの小さなアプリを Container 化できる。
- PostgreSQL Container を起動し、Volume を使ってデータを永続化できる。
- Docker Network を使って Container 間通信を構成できる。
- Docker Compose を使って複数 Container をまとめて定義・起動・停止できる。
- Spring Boot + PostgreSQL の 2 Container 構成を構築できる。
- React + Spring Boot + PostgreSQL の 3 Container 構成を構築できる。
- Container が動かない場合に、Logs / Inspect / Port / Network / Environment などを確認しながら基本的な切り分けができる。
- Multi-stage Build、Build Cache、Healthcheck など、Docker をより適切に使うための基本的な改善観点を理解できる。
- 自分で Build した Image に Tag を付け、Docker Hub へ Push する基本的な流れを経験している。

## 3. 学習方針

### 3.1 ハンズオンを中心にする

概念の説明だけで終わらせず、実際に Docker コマンドを実行し、Container の状態・ブラウザ表示・API レスポンス・DB 接続・Volume の永続化などを確認しながら理解する。

### 3.2 小さな題材から段階的に進める

最初から複雑な Web システムを扱わず、以下のように段階を上げていく。

1. 既存 Image を使った最小の Container 実行
2. Image / Container の基本操作
3. Port / Bind Mount / Volume
4. Dockerfile からの Image Build
5. React / Vite の単体 Container 化
6. Spring Boot の単体 Container 化
7. PostgreSQL Container
8. Docker Network
9. Docker Compose
10. Spring Boot + PostgreSQL
11. React + Spring Boot + PostgreSQL
12. Dockerfile / Compose の改善と基本的な Best Practice

### 3.3 Docker 以外の実装を作り込みすぎない

React / Spring Boot / PostgreSQL は Docker 学習の題材として使用する。  
そのため、以下のような Docker 学習に直接関係しない実装は最小限に留める。

- 複雑な UI
- 状態管理
- 認証・認可
- 本格的な CRUD
- 複雑な DB 設計
- 高度な API 設計
- アプリ固有のドメインロジック

### 3.4 前の学習内容を次の学習につなげる

各 Unit は独立したテーマとして進めるが、前の Unit で学んだ内容を次の Unit で再利用・発展させる。  
たとえば、Volume は PostgreSQL のデータ永続化で再確認し、Network は Docker Compose の複数 Service 構成で再確認する。

## 4. 学習対象範囲

本カリキュラムでは、以下を主な学習対象とする。

### 4.1 Docker / Container の基本概念

- Docker
- Container
- Image
- Host
- Container と仮想マシンの違い
- Docker Engine
- Docker CLI
- Docker Desktop
- Windows / WSL 2 / Linux Container の大まかな関係
- Registry
- Docker Hub

### 4.2 Image / Container の基本操作

- Image の取得・一覧・削除
- Image Tag
- Container の作成・起動・停止・再起動・削除
- Container 名
- バックグラウンド実行
- 対話的実行
- Logs
- Container 内部でのコマンド実行
- Inspect
- Container のライフサイクル
- 不要リソースの整理

### 4.3 Host と Container の接続・データ

- Port Mapping
- Host 側 Port / Container 側 Port
- localhost
- Bind Mount
- Volume
- Named Volume
- Container を削除しても保持されるデータ

### 4.4 Dockerfile / Image Build

- Dockerfile
- Base Image
- FROM
- WORKDIR
- COPY
- RUN
- CMD
- ENV
- EXPOSE
- ENTRYPOINT
- ARG
- Build Context
- Image Build
- Image Layer
- Build Cache
- .dockerignore

### 4.5 アプリケーションの Container 化

- React / Vite
- Spring Boot
- PostgreSQL
- Node.js Base Image
- Java Base Image
- PostgreSQL 公式 Image
- アプリケーション Port
- 環境変数
- JAR と Container の関係
- DB データ永続化

### 4.6 Container 間通信

- Docker Network
- Bridge Network
- Container 間通信
- Container / Service 名による名前解決
- Host → Container と Container → Container の違い
- Container 内部における localhost の意味

### 4.7 Docker Compose

- compose.yaml
- Project
- Service
- Container
- image
- build
- ports
- environment
- volumes
- networks
- depends_on
- Compose による起動・停止・削除・Build・Logs
- .env
- 複数 Container 構成

### 4.8 基本的な改善・Best Practice

- Multi-stage Build
- Build Cache を意識した Dockerfile
- Base Image の選択
- Version Tag
- Healthcheck
- Restart Policy
- non-root Container の考え方
- Image サイズ
- Docker Hub への Image Push

## 5. 学習対象外

本カリキュラムでは、Docker の基礎からアプリケーション開発での利用までを対象とし、以下は扱わない。

- Kubernetes
- Docker Swarm
- Amazon ECS などの Container Orchestration
- 本格的な Production Deploy
- CI/CD と Docker の統合
- GitHub Container Registry など複数 Registry の運用
- Private Registry の構築・運用
- Multi-platform Build の深掘り
- Docker Buildx の高度な利用
- Docker Engine API / SDK
- Docker Plugin
- 高度な Network Driver
  - macvlan
  - overlay
  - host network の深掘り
- 高度な Storage Driver
- Rootless Docker
- Container 内部実装の深掘り
  - namespace
  - cgroups
- 本格的な Security Hardening
- Image 署名
- SBOM
- Vulnerability Scan の深掘り
- 本格的な Logging / Monitoring 基盤
- Docker Secrets / Build Secrets の実践的運用

これらは本カリキュラム完了後、必要に応じて別の学習テーマとして扱う。

## 6. 学習カリキュラム

### Unit 01. Docker・Container の基本概念と環境確認

#### 目的

Docker を操作する前提として、Docker / Container / Image が何を意味するのかを理解し、Windows 上で Docker を利用できる状態を確認する。

#### 主な学習内容

- Docker とは何か
- Container とは何か
- Image とは何か
- Image と Container の関係
- Container と仮想マシンの違い
- Host と Container
- Docker Engine
- Docker CLI
- Docker Desktop
- Windows / WSL 2 / Linux Container の大まかな関係
- Registry
- Docker Hub
- Image から Container が実行される基本的な流れ

#### ハンズオンの大枠

- Docker が利用できる状態を確認する。
- 最小の既存 Image を使って Container を実行する。
- Container がどの Image から作られたのかを確認する。

#### 到達状態

Docker Hub などの Registry に存在する Image を取得し、その Image を基に Docker が Container を作成・実行する、という基本的な流れを説明できる。

### Unit 02. Image・Container の基本操作とライフサイクル

#### 目的

Docker CLI を使って Image / Container の基本操作を行い、両者を区別して扱えるようにする。

#### 主な学習内容

##### Image

- Image の取得
- Image 一覧
- Image 削除
- Image Tag
- Image 情報確認

##### Container

- Container の作成・起動
- run
- 一覧確認
- 停止
- 再開
- 再起動
- 削除
- Container 名
- バックグラウンド実行
- 対話的実行
- Logs
- Container 内部でのコマンド実行
- Inspect

##### ライフサイクル

- Image から Container を作成する流れ
- Container の起動・停止・再実行
- Container を削除しても Image は残ること
- 不要リソースの整理
- prune 系操作の概要

#### ハンズオンの大枠

Linux 系 Image や Nginx などの既存 Image を利用し、Docker CLI で基本操作を繰り返す。

#### 到達状態

今扱っている対象が Image なのか Container なのかを区別しながら、基本的な Docker CLI 操作を行える。

### Unit 03. Port・Bind Mount・Volume

#### 目的

Host と Container の間で、通信・ファイル共有・データ永続化をどのように扱うのかを理解する。

#### 主な学習内容

##### Port

- Container 内部の Port
- Host 側の Port
- Port Mapping
- Host → Container のアクセス
- localhost
- ブラウザから Container へアクセスする流れ

##### Bind Mount

- Bind Mount とは何か
- Host 上のファイル / ディレクトリを Container から利用する
- Host 側の変更が Container 側に反映されること
- Bind Mount が向いている用途

##### Volume

- Docker Volume
- Named Volume
- Volume の作成・確認・削除
- Container への Mount
- Container 削除後も Volume が残ること
- Bind Mount と Volume の違い
- Container 自体を永続データ保管場所にしない考え方

#### ハンズオンの大枠

Nginx と小さな HTML などを使い、Port Mapping と Bind Mount を確認する。  
Named Volume を使い、Container を削除・再作成してもデータを保持できることを確認する。

#### 到達状態

Port / Bind Mount / Volume がそれぞれ異なる目的の仕組みであることを説明し、基本的な設定ができる。

### Unit 04. Dockerfile と Image Build

#### 目的

既存 Image を利用するだけでなく、自分で Dockerfile を作成して Image を Build し、Container として実行できるようにする。

#### 主な学習内容

##### Dockerfile

- Dockerfile とは何か
- Base Image
- FROM
- WORKDIR
- COPY
- RUN
- CMD
- ENV
- EXPOSE

##### 補助的に扱う内容

- ENTRYPOINT
- ARG
- CMD と ENTRYPOINT の大まかな違い

##### Build

- Dockerfile から Image を Build する
- Build Context
- Image 名
- Image Tag
- Build した Image から Container を起動する
- Dockerfile を変更して再 Build する

##### Image の構造

- Image Layer
- Dockerfile の命令と Layer
- Build Cache
- Dockerfile の記述順と Cache

##### Build Context の整理

- .dockerignore
- 不要ファイルを Build Context に含めない
- 秘密情報を不用意に Image へ含めない

#### ハンズオンの大枠

HTML + Nginx などの極小サンプルを Dockerfile から Image 化し、Container として実行する。

#### 到達状態

Dockerfile → Build → Image → Container の一連の流れを自分で実行できる。

### Unit 05. React / Vite アプリの Docker 化

#### 目的

フロントエンドアプリケーションも Docker の共通的な考え方で Container 化できることを経験する。

#### 主な学習内容

- Node.js Base Image
- React / Vite ソースコードと Container
- npm による依存関係
- Container 内でのアプリ起動
- Port
- Dockerfile
- Image Build
- Container 起動
- ブラウザからの確認

#### サンプル方針

React 自体の学習は目的としない。  
文字列表示など、ごく小さな画面だけを持つアプリケーションを使用する。

#### ハンズオンの大枠

最小の React / Vite アプリに Dockerfile を用意し、Image を Build して Container としてブラウザからアクセスする。

#### 到達状態

React / Vite のようなフロントエンドアプリも Docker Image / Container として扱えることを理解する。

### Unit 06. Spring Boot アプリの Docker 化

#### 目的

Java / Spring Boot アプリケーションを Docker Image 化し、Container で実行する流れを経験する。

#### 主な学習内容

- Java 用 Base Image
- Spring Boot の Build 成果物
- JAR
- JAR を Container 内で実行する
- Dockerfile
- Port Mapping
- 環境変数
- Image Build
- Container 起動

#### サンプル方針

Spring Boot 自体の学習は目的としない。  
単純な Endpoint から JSON を返す程度の最小構成とする。

#### ハンズオンの大枠

Spring Boot アプリを Build して JAR を作成し、Dockerfile から Image 化して Container 内で実行する。

#### 到達状態

React と異なる技術スタックでも、Docker としては Image / Container という同じ基本構造で扱えることを理解する。

### Unit 07. PostgreSQL Container とデータ永続化

#### 目的

PostgreSQL 公式 Image を利用し、データベースを Container として動かす方法と、Volume による永続化を理解する。

#### 主な学習内容

- PostgreSQL 公式 Image
- Image Tag
- DB 初期設定用の環境変数
- PostgreSQL Container の起動
- PostgreSQL への接続
- Named Volume
- DB データ永続化
- Container の削除・再作成
- Host / Container / DB の関係

#### ハンズオンの大枠

PostgreSQL Container を起動し、データを作成する。  
Container を削除・再作成した後も Named Volume によってデータが保持されることを確認する。

#### 到達状態

既存の公式 Image を設定して利用する方法と、Volume の実践的な用途を理解する。

### Unit 08. Docker Network と Container 間通信

#### 目的

複数 Container が Docker Network を通して通信する仕組みを理解する。

#### 主な学習内容

- Docker Network
- Bridge Network
- Network の作成
- Container を Network へ接続する
- Container 間通信
- Container 名による名前解決
- Host → Container
- Container → Container
- Container 内部の localhost
- 別 Container へ接続するときの Host 名

#### ハンズオンの大枠

複数 Container を起動し、CLI で明示的に Network を作成・接続して Container 間通信を確認する。

#### 到達状態

Container A → Docker Network → Container B という通信を、Docker Compose に頼らず理解できる。

### Unit 09. Docker Compose の基本

#### 目的

これまで Docker CLI で個別に指定してきた Container / Port / Network / Volume / Environment などを、compose.yaml でまとめて宣言・管理できるようにする。

#### 主な学習内容

##### 基本概念

- Docker Compose
- compose.yaml
- Project
- Service
- Container
- Network
- Volume

##### Compose ファイル

- services
- image
- build
- ports
- environment
- volumes
- networks
- depends_on

##### Compose 操作

- 起動
- バックグラウンド起動
- 停止
- 削除
- Build
- 再 Build
- Service / Container 確認
- Logs
- Container 内部でのコマンド実行

##### 環境変数

- Container への環境変数
- Dockerfile の ENV
- Compose の environment
- .env
- DB 接続情報などを Container 外から渡す考え方
- 秘密情報を Git へ含めない

##### 概念としてのみ触れる内容

- Docker Secrets
- Build Secrets
- .env が Production における Secrets 管理の完成形ではないこと

#### ハンズオンの大枠

小さな複数 Service 構成を compose.yaml で定義し、Docker Compose から一括して起動・停止・確認する。

#### 到達状態

Docker Compose が「これまで CLI で個別指定していた Docker の設定をまとめて宣言する仕組み」であることを理解する。

### Unit 10. Spring Boot + PostgreSQL の 2 Container 構成

#### 目的

アプリケーション + データベースという一般的な 2 Container 構成を Docker Compose で構築し、Container 間通信とデータ永続化を実践する。

#### 主な学習内容

- Spring Boot Service
- PostgreSQL Service
- Compose による Image Build
- Docker Network
- Service 名による DB 接続
- DB 接続 URL
- 環境変数
- PostgreSQL Volume
- depends_on
- Compose による起動・停止
- Container 再作成
- DB データ永続化
- Logs
- 接続失敗時の確認

#### サンプル方針

Spring Boot から PostgreSQL の最小限のデータを取得し、JSON として返す程度の構成とする。  
CRUD や複雑な DB 設計は扱わない。

#### ハンズオンの大枠

Spring Boot と PostgreSQL をそれぞれ Service として定義し、Docker Compose で起動する。  
Spring Boot Container から PostgreSQL Container へ Docker Network 経由で接続し、データを取得する。

#### 到達状態

Spring Boot + PostgreSQL の 2 Container 構成を Docker Compose で自分で起動・確認できる。

### Unit 11. React + Spring Boot + PostgreSQL の 3 Container 構成

#### 目的

Frontend / Backend / Database からなる、実際の Web システムに近い 3 Container 構成を Docker Compose で構築する。

#### 主な学習内容

- Frontend / Backend / Database の 3 Service
- React Container
- Spring Boot Container
- PostgreSQL Container
- Port Mapping
- Docker Network
- Volume
- 環境変数
- Service 間の接続先
- Host から見た URL
- Docker Network 内部から見た Host 名
- Browser が Docker Network 内部に存在するわけではないこと

#### Docker 以外で必要最低限扱う内容

- React → Spring Boot の API 通信
- API URL
- CORS
- 環境変数による接続先設定

#### サンプル方針

以下の通信経路を確認するための最小構成とする。

Browser  
→ React  
→ Spring Boot  
→ PostgreSQL

認証・状態管理・複雑な CRUD・UI 作り込み・本格的 DB 設計などは扱わない。

#### ハンズオンの大枠

React / Spring Boot / PostgreSQL をそれぞれ Service として Docker Compose で起動する。  
Browser から React へアクセスし、React から Spring Boot API を呼び出し、Spring Boot が PostgreSQL のデータを取得して返す一連の流れを確認する。

#### 到達状態

Frontend + Backend + Database の 3 Container 構成を Docker 上で動かし、それぞれの通信経路と接続先の違いを説明できる。

### Unit 12. Dockerfile・Compose の改善と基本的な Best Practice

#### 目的

一度動く構成を作った後、Dockerfile / Compose をより適切にするための基本的な改善観点を学ぶ。

#### 主な学習内容

##### Multi-stage Build

- Build 環境と Runtime 環境の分離
- 最終 Image に不要な Build ツールを含めない
- React / Spring Boot などで実際に適用する

##### Build Cache

- Dockerfile の記述順
- 依存関係ファイルを先に COPY する考え方
- 再 Build 効率

##### .dockerignore

- 不要ファイルを Build Context へ含めない
- 実践構成で再確認する

##### Base Image / Image Tag

- Base Image 選択の基本
- latest のみに依存しない考え方
- Version Tag
- slim / Alpine などの存在

##### Healthcheck

- Container が起動していることと、アプリケーションが正常であることの違い
- Healthcheck の基本
- depends_on と準備完了の違い

##### Restart Policy

- Container 終了時の再起動設定があること
- 開発環境と Production 環境で必要性が異なること

##### non-root Container

- 必要以上に root で動かさない考え方
- Dockerfile の USER

##### Image サイズ

- .dockerignore
- Multi-stage Build
- Base Image
- 不要ファイルとの関係

##### Docker Hub への Image Push

- Docker Hub Account / Repository
- Login
- Tag
- Push
- Registry へ Image を置く意味

#### 扱わない内容

- CI/CD からの自動 Push
- GitHub Container Registry
- Private Registry
- Image 署名
- 本格的な Production 向け Security

#### ハンズオンの大枠

これまで作成した Dockerfile / Compose 構成を題材に、Multi-stage Build や Build Cache、Healthcheck などの改善を適用する。  
最後に、自分で Build した Image に Tag を付け、Docker Hub へ Push する基本的な流れを 1 回経験する。

#### 到達状態

「Container が動けば終わり」ではなく、Dockerfile / Compose をどのような観点で改善するのかを初学者レベルで判断できる。

## 7. 横断的に扱う学習内容

以下は独立した Unit にはせず、複数 Unit を通して繰り返し扱う。

### 7.1 トラブルシューティング

Container や複数 Service が期待どおり動かない場合に、以下を確認する。

- Container が起動しているか
- Container が終了していないか
- Logs
- Inspect
- Container 内部
- Port Mapping
- 環境変数
- Network
- Service 名 / Host 名
- Volume
- Build Error
- Image / Container の状態

特に後半では、以下のような基本的な切り分けを意識する。

問題発生  
→ どの Container / Service に問題があるか確認  
→ Logs  
→ Port / Network / Environment  
→ 接続先・設定  
→ 必要に応じて Container 内部を確認

### 7.2 Container / Image の状態確認

各 Unit を通して、Container や Image の状態を実際に確認しながら学習する。

### 7.3 Host と Container の境界

Port / File / Volume / Network / localhost などを扱うたびに、Host と Container のどちら側の話なのかを明確にする。

### 7.4 環境変数

React / Spring Boot / PostgreSQL / Docker Compose を通して、設定値を Container 外から渡す考え方を繰り返し確認する。

## 8. 一周完了時の到達状態

全 12 Unit 完了時には、Docker について以下の状態になっていることを目標とする。

### 基本概念

- Docker / Container / Image / Registry の役割を大まかに説明できる。
- Image と Container を区別できる。
- Host と Container の境界を意識できる。

### 基本操作

- Docker CLI で Image / Container を操作できる。
- Logs や Inspect を使って状態を確認できる。
- Container 内部でコマンドを実行できる。

### Dockerfile

- Dockerfile を読み書きできる。
- Dockerfile から Image を Build できる。
- Layer / Build Cache / .dockerignore の基本を理解している。

### データ・通信

- Port Mapping を設定できる。
- Bind Mount / Volume を使い分けられる。
- Docker Network を使って Container 同士を接続できる。
- Container 内部における localhost の意味を理解している。

### Docker Compose

- compose.yaml を読み書きできる。
- 複数 Service を一括で起動・停止できる。
- Network / Volume / Environment を Compose で定義できる。

### アプリケーション構成

- React / Vite を単体 Container 化できる。
- Spring Boot を単体 Container 化できる。
- PostgreSQL Container を Volume 付きで扱える。
- Spring Boot + PostgreSQL の 2 Container 構成を扱える。
- React + Spring Boot + PostgreSQL の 3 Container 構成を扱える。

### 基本的な改善

- Multi-stage Build の目的を理解している。
- Build Cache を意識して Dockerfile を確認できる。
- Base Image / Tag / Healthcheck / Restart Policy / non-root の基本的な考え方を知っている。
- Docker Hub へ Image を Push する基本的な流れを経験している。

本カリキュラムでは、これらを「Docker のすべてを理解した状態」とはせず、今後アプリケーション開発や追加学習の中で Docker を使いながら理解を深めていくための基礎ができた状態と位置付ける。
