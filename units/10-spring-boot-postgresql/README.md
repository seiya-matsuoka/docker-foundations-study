# 10. Spring Boot + PostgreSQL の 2 Container 構成

## この項目の目的

この Unit では、Spring Boot Application と PostgreSQL Database をそれぞれ独立した Container として動かし、Docker Compose で 2 Container 構成を構築する。  
これまで個別に学んだ Spring Boot の Docker 化、PostgreSQL Container、Docker Network、Named Volume、Docker Compose を組み合わせ、Application + Database という一般的な構成へ発展させる。

今回の通信経路は次のようになる。

```text
Windows Host
↓
localhost:8080
↓ Port Publishing
Spring Boot Service: app
↓
jdbc:postgresql://postgres:5432/unit10_db
↓ Docker Network
PostgreSQL Service: postgres
↓
PostgreSQL Data
↓
Named Volume
```

重要なのは、Spring Boot Container から PostgreSQL Container へ接続するときの Host が `localhost` ではなく、Compose の Service 名である `postgres` になることである。

```text
Spring Boot Container 内の localhost
→ Spring Boot Container 自身

PostgreSQL への接続先 Host
→ postgres
```

この Unit では CRUD や複雑な Database 設計を扱わない。  
PostgreSQL から 1 Record を取得し、Spring Boot API が JSON として返す最小構成を使い、Docker / Compose / Network / Volume / Environment Variable の理解に集中する。

最終的には、

```text
Spring Boot
+
PostgreSQL
+
Docker Network
+
Named Volume
+
Docker Compose
```

を自分で起動・確認し、Container 間通信と Database Data の永続化を説明できる状態を目指す。

## 学習内容

### Unit 06〜09 の内容を統合する

Unit 10 は新しい Docker 概念を大量に追加する Unit ではなく、これまで別々に学んだ内容を 1 つの Application 構成として組み合わせる Unit である。

```text
Unit 06
Spring Boot Application の Docker 化
        +
Unit 07
PostgreSQL Container / Named Volume
        +
Unit 08
Docker Network / Container 間通信
        +
Unit 09
Docker Compose
        ↓
Unit 10
Spring Boot + PostgreSQL
```

そのため、各設定を新しい暗記項目として見るのではなく、「以前 CLI で確認した仕組みが実際の 2 Container Application でどのように使われるか」を確認する。

### 今回の 2 Service 構成

`compose.yaml` には次の 2 Service を定義する。

```text
app
→ Spring Boot Application

postgres
→ PostgreSQL Database
```

Compose Project 全体は次のようになる。

```text
Compose Project
unit10-spring-postgresql
│
├─ app Service
│  └─ Spring Boot Container
│
├─ postgres Service
│  └─ PostgreSQL Container
│
├─ unit10-network
│
└─ unit10-postgres-data
```

`app` と `postgres` は同じ `unit10-network` に接続されるため、Service 名による Container 間通信が可能になる。

### Spring Boot Service

`app` Service は Dockerfile から Custom Image を Build する。

```yaml
app:
  build:
    context: .
    dockerfile: Dockerfile
```

Spring Boot Source 自体を Container 内で Build するのではなく、Unit 06 と同様に Host 側の Maven で JAR を生成してから、その JAR を Runtime Image へ `COPY` する。

```text
Java Source
↓
Host Maven Build
↓
JAR
↓
Docker Build
↓
Spring Boot Image
↓
app Container
```

Multi-stage Build は Unit 12 で扱うため、ここでは既習構成を維持する。

### PostgreSQL Service

`postgres` Service は Docker Official Image を直接利用する。

```yaml
postgres:
  image: postgres:18.6-alpine3.24
```

Application Service と異なり、PostgreSQL 用 Dockerfile は作成しない。

```text
Spring Boot
→ 自分の Application なので Custom Image を Build

PostgreSQL
→ Official Image に Runtime Configuration を渡して利用
```

Unit 07 で学んだ使い分けを、Compose の Service 定義へ移している。

### Docker Network と 2 Container 間通信

`app` と `postgres` は同じ `unit10-network` に接続する。

```yaml
networks:
  - unit10-network
```

Top-level では Network Resource を宣言する。

```yaml
networks:
  unit10-network:
    driver: bridge
```

通信経路は次のようになる。

```text
app Container
↓
Host 名: postgres
Port: 5432
↓
unit10-network
↓
postgres Container
↓
PostgreSQL Server
```

Host Port を経由して Database へ接続する構成ではない。

### Service 名 `postgres` が DB Host になる

Compose Network 上では Service 名を名前解決に利用できる。

今回の JDBC URL は次である。

```text
jdbc:postgresql://postgres:5432/unit10_db
```

分解すると、

```text
jdbc:postgresql://
→ PostgreSQL JDBC 接続

postgres
→ PostgreSQL Service 名 / DB Host

5432
→ PostgreSQL Container Port

unit10_db
→ Database 名
```

となる。

`postgres` は固定 IP Address ではない。  
Docker / Compose の名前解決によって、現在の PostgreSQL Container の Network 内 Address へ解決される。

Container を再作成すると IP Address は変わる可能性があるが、Service 名 `postgres` は接続先として維持できる。

### `localhost` を DB Host にしない理由

Spring Boot Container 内で、

```text
localhost
```

と指定すると Spring Boot Container 自身を指す。

```text
app Container
├─ Spring Boot
└─ localhost
   → app Container 自身
```

PostgreSQL は別 Container に存在するため、

```text
jdbc:postgresql://localhost:5432/unit10_db
```

ではなく、

```text
jdbc:postgresql://postgres:5432/unit10_db
```

を使用する。

これは Unit 08 の `localhost` と Container 名の学習が、実際の Database 接続設定へつながる重要な部分である。

### Host → Spring Boot と Spring Boot → PostgreSQL は別の通信

Browser / `curl` から Spring Boot API へアクセスする経路は、

```text
Windows Host
↓
localhost:8080
↓ Port Publishing
app Container:8080
```

である。

一方、Spring Boot から PostgreSQL への接続は、

```text
app Container
↓
postgres:5432
↓ Docker Network
postgres Container
```

である。

したがって、

```text
Host → app
→ Host Port を利用

app → postgres
→ Service 名 + Container Port を利用
```

と区別する。

今回 PostgreSQL Service には Host への `ports` を定義していない。  
これは Spring Boot から PostgreSQL へ接続するために Host Port Publishing が不要であることを確認する構成でもある。

### JDBC URL と Environment Variable

Spring Boot の `application.properties` は次の Environment Variable を参照する。

```text
DB_URL
DB_USER
DB_PASSWORD
```

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/unit10_db}
spring.datasource.username=${DB_USER:unit10_user}
spring.datasource.password=${DB_PASSWORD:unit10-password}
```

Compose では `app` Service に次の値を渡す。

```yaml
environment:
  DB_URL: 'jdbc:postgresql://postgres:5432/${POSTGRES_DB:-unit10_db}'
  DB_USER: '${POSTGRES_USER:-unit10_user}'
  DB_PASSWORD: '${POSTGRES_PASSWORD:-unit10-password}'
```

流れを整理すると、

```text
.env
↓ Compose の値補間
compose.yaml
↓ environment
app Container
↓
DB_URL / DB_USER / DB_PASSWORD
↓
application.properties
↓
Spring DataSource
↓
PostgreSQL
```

となる。

Unit 09 で学んだ `.env` / Compose `environment` / Container Environment の違いを実際の DB 接続設定へ適用している。

### PostgreSQL 初期設定用 Environment Variable

`postgres` Service では次を利用する。

```yaml
environment:
  POSTGRES_DB: '${POSTGRES_DB:-unit10_db}'
  POSTGRES_USER: '${POSTGRES_USER:-unit10_user}'
  POSTGRES_PASSWORD: '${POSTGRES_PASSWORD:-unit10-password}'
```

これらは PostgreSQL Official Image が空の Data Directory を初回初期化するときに利用する。

```text
空の Named Volume
↓
postgres Container 初回起動
↓
POSTGRES_DB / USER / PASSWORD
↓
PostgreSQL 初期化
```

Unit 07 と同様、初期化済み Volume が存在する場合に毎回 Database を作り直す設定ではない。

### Database 初期化 SQL

この Unit では、

```text
db/init/01-init.sql
```

を PostgreSQL Container の、

```text
/docker-entrypoint-initdb.d
```

へ Read-only で Mount する。

```yaml
- ./db/init:/docker-entrypoint-initdb.d:ro
```

PostgreSQL Official Image は、Data Directory が空の初回初期化時にこの Directory 内の SQL Script を実行する。

今回の SQL は最小限である。

```sql
CREATE TABLE IF NOT EXISTS study_message (
    id INTEGER PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

INSERT INTO study_message (id, message)
VALUES (1, 'Hello from PostgreSQL')
ON CONFLICT (id) DO NOTHING;
```

目的は SQL 学習ではなく、

```text
PostgreSQL 初期 Data
↓
Spring Boot が SELECT
↓
JSON Response
```

という一連の通信を確認することである。

### 初期化 SQL は毎回実行されるわけではない

`/docker-entrypoint-initdb.d` の Script は、PostgreSQL の Data Directory が空の初回初期化時に実行される。

```text
空 Volume
↓
PostgreSQL 初期化
↓
01-init.sql 実行
```

その後 Container を削除しても Named Volume が残っていれば、

```text
既存 Volume
↓
既存 PostgreSQL Data を利用
↓
初期化 Script は再実行しない
```

という動作になる。

この性質が、後で Database Data 永続化を確認するときに重要になる。

### Spring Boot での Database Access

Spring Boot 側では JPA や複雑な Repository Layer を導入せず、`JdbcTemplate` を使用する。

`MessageService` は次の SQL を実行する。

```sql
SELECT id, message
FROM study_message
WHERE id = 1
```

取得結果を `MessageResponse` に変換する。

```text
PostgreSQL
↓ SELECT
MessageService
↓
MessageResponse
↓
MessageController
↓
JSON
```

Application 実装は Docker / Compose の動作確認に必要な最小限にしている。

### API Endpoint

Endpoint は次の 1 つだけである。

```text
GET /api/message
```

正常時の Response は次の想定である。

```json
{
  "id": 1,
  "message": "Hello from PostgreSQL"
}
```

この JSON が返ることは、

```text
Host
↓
Spring Boot Container
↓
Docker Network
↓
PostgreSQL Container
↓
Database Data
↓
Spring Boot
↓
Host
```

という一連の経路が成立していることを意味する。

### PostgreSQL Named Volume

PostgreSQL Data は次の Named Volume に保存する。

```text
unit10-postgres-data
```

Compose File 上では、

```yaml
volumes:
  unit10-postgres-data:
```

と宣言し、PostgreSQL Service へ、

```yaml
volumes:
  - unit10-postgres-data:/var/lib/postgresql
```

として Mount する。

PostgreSQL 18 の Data は、この Volume 配下に保持される。

```text
postgres Container
↓
/var/lib/postgresql
↓
Named Volume
unit10-postgres-data
```

Container を削除・再作成しても、Volume を残せば Database Data を引き継げる。

### `depends_on`

`app` Service には次を定義している。

```yaml
depends_on:
  - postgres
```

これは `postgres` Service を `app` Service より先に Start する依存関係を表す。

ただし、

```text
postgres Container が Start
≠
PostgreSQL が Connection を受け付けられる状態
```

である。

Short syntax の `depends_on` は PostgreSQL の Readiness まで待つものではない。

### 起動順序と Readiness

PostgreSQL Container は Start 後に Database の初期化や Server 起動処理を行う。

```text
Container Start
↓
PostgreSQL Initialization
↓
Database / User 作成
↓
Initialization SQL
↓
Connection を受け付けられる状態
```

Spring Boot Container がその途中で起動する可能性がある。

今回の `application.properties` には、

```properties
spring.datasource.hikari.initialization-fail-timeout=-1
```

を設定している。

これは学習用 Sample で、Spring Boot 起動時に Database Connection をすぐ取得できなかった場合でも、Application 自体が即時に起動失敗しにくくするための設定である。

ただし、Database がまだ利用できない間に `/api/message` を呼べば DB Access は失敗する可能性がある。

この Unit では、

```text
depends_on
→ 起動順序

Database Readiness
→ 別の問題
```

と理解する。

Healthcheck を使った Readiness 管理は Unit 12 で扱う。

### Compose による起動・停止

今回も Unit 09 と同じ Compose Lifecycle を使う。

```text
docker compose up -d
→ Service / Network / Volume を構成して起動

docker compose ps
→ 状態確認

docker compose logs
→ Logs 確認

docker compose down
→ Container / Network を削除

docker compose down -v
→ Named Volume も削除
```

Unit 09 で学んだ Command を、実際の Application + Database 構成へ適用する。

### Container 再作成と Data 永続化

`docker compose down` では Named Volume は Default で削除されない。

```text
app Container
→ 削除

postgres Container
→ 削除

Network
→ 削除

Named Volume
→ 残る
```

その後、

```bash
docker compose up -d
```

を実行すると新しい Container が作成されるが、PostgreSQL は既存 Volume の Data を利用する。

```text
新しい postgres Container
↓
既存 Named Volume
↓
以前の Database Data
```

この Unit では Database の Record を一度変更してから Container を再作成し、変更後 Data が残ることを確認する。

### Logs の役割

2 Container 構成では、問題が発生したときに「どの Service で失敗しているか」を切り分ける必要がある。

```text
Browser / curl
↓
app
↓
postgres
```

例えば API が失敗した場合、

```text
app が起動していない
app から postgres の名前解決ができない
DB 接続情報が間違っている
postgres が起動していない
PostgreSQL がまだ Ready ではない
Table / Data が存在しない
```

など複数の原因が考えられる。

そのため、

```bash
docker compose ps
docker compose logs app
docker compose logs postgres
```

を利用して、Service ごとの状態を確認する。

### 接続失敗時の基本的な確認順

この Unit では、Database 接続に失敗した場合に次の順番で確認する。

```text
1. docker compose ps
   ↓
Container が Running か

2. docker compose logs app
   ↓
Spring Boot 側の Error

3. docker compose logs postgres
   ↓
PostgreSQL の起動 / 初期化状態

4. app Container の Environment Variable
   ↓
DB_URL / DB_USER / DB_PASSWORD

5. DB_URL の Host
   ↓
localhost ではなく postgres か

6. Docker Network
   ↓
app / postgres が同じ Network か

7. PostgreSQL 側
   ↓
Database / Table / Record が存在するか
```

闇雲に Container を作り直すのではなく、通信経路に沿って確認する。

### Unit 11 へのつながり

Unit 10 では、

```text
Browser
↓
Spring Boot
↓
PostgreSQL
```

という Backend + Database の 2 Container 構成を扱う。

Unit 11 ではさらに React Container を追加する。

```text
Browser
↓
React
↓
Spring Boot
↓
PostgreSQL
```

そのとき、

```text
Browser から見た Host
Container から見た Host
Docker Network 内の Service 名
Port Publishing
CORS
```

が新たに重要になる。

Unit 10 で Spring Boot → PostgreSQL の Container 間通信を確実に理解しておくことが、3 Container 構成の前提になる。

## 使用するもの

### Spring Boot

```text
4.1.1
```

### Java

```text
21
```

### Maven

Spring Boot JAR の Host Build に利用する。

### Spring Boot Runtime Image

```text
eclipse-temurin:21.0.12_8-jre-alpine-3.24
```

### PostgreSQL Image

```text
postgres:18.6-alpine3.24
```

### Compose Project

```text
unit10-spring-postgresql
```

### Spring Boot Service

```text
app
```

### PostgreSQL Service

```text
postgres
```

### Docker Network

Compose File 上の Key:

```text
unit10-network
```

### Named Volume

Compose File 上の Key:

```text
unit10-postgres-data
```

### Database

```text
unit10_db
```

### PostgreSQL User

```text
unit10_user
```

### Local 学習用 Password

```text
unit10-password
```

### Spring Boot Port

```text
Host      : 8080
Container : 8080
```

### PostgreSQL Container Port

```text
5432
```

PostgreSQL Service は Host へ Publish しない。

### API Endpoint

```text
GET /api/message
```

## 事前準備

Unit 09 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/10-spring-boot-postgresql
```

Git Bash で Repository 内の次の Unit Directory へ移動した状態から操作する。

```text
units/10-spring-boot-postgresql
```

現在位置を確認する。

```bash
pwd
```

生成物を確認する。

```bash
find . -maxdepth 6 -type f | sort
```

主に次が存在する。

```text
.dockerignore
.env.example
Dockerfile
compose.yaml
pom.xml
db/init/01-init.sql
src/main/java/com/example/unit10/...
src/main/resources/application.properties
```

Maven が利用できることを確認する。

```bash
mvn -version
```

Docker Compose も確認する。

```bash
docker compose version
```

Host Port `8080` を別 Application / Container が使用していないことも確認する。

```bash
docker container ls
```

## ハンズオン

### 1. 2 Service 構成を確認する

`compose.yaml` を確認する。

```bash
cat compose.yaml
```

大きな構造を整理する。

```text
services
├─ app
└─ postgres

networks
└─ unit10-network

volumes
└─ unit10-postgres-data
```

`app` が Spring Boot、`postgres` が PostgreSQL であることを確認する。

### 2. Spring Boot Service の設定を確認する

`app` Service では主に次を見る。

```text
build
ports
environment
depends_on
networks
```

特に `DB_URL` を確認する。

```text
jdbc:postgresql://postgres:5432/unit10_db
```

Host 部分が `localhost` ではなく `postgres` になっていることを確認する。

### 3. PostgreSQL Service の設定を確認する

`postgres` Service では主に次を見る。

```text
image
environment
volumes
networks
```

PostgreSQL Port `5432` を Host へ Publish する `ports` が存在しないことも確認する。  
Spring Boot との通信は Docker Network 内で行うためである。

### 4. `.env.example` から `.env` を作成する

```bash
cp .env.example .env
```

確認する。

```bash
cat .env
```

学習用の値は次である。

```text
APP_PORT=8080
POSTGRES_DB=unit10_db
POSTGRES_USER=unit10_user
POSTGRES_PASSWORD=unit10-password
```

`.env` は Local 用 File であり、Git へ Commit しない。

### 5. Compose の解釈結果を確認する

```bash
docker compose config
```

特に `app` Service の Environment Variable を確認する。

```text
DB_URL
DB_USER
DB_PASSWORD
```

`DB_URL` が最終的に次の形へ解決されていることを確認する。

```text
jdbc:postgresql://postgres:5432/unit10_db
```

`postgres` Service の初期化設定も確認する。

### 6. Spring Boot の DataSource 設定を確認する

```bash
cat src/main/resources/application.properties
```

Compose の Environment Variable と Spring Boot の DataSource が次のようにつながることを確認する。

```text
compose.yaml
DB_URL
↓
Container Environment
↓
application.properties
spring.datasource.url
↓
Spring DataSource
```

`localhost` は Default 値として残っているが、Compose 起動時は `DB_URL` によって `postgres` Service を指す値へ上書きされる。

### 7. Database Access の最小実装を確認する

`MessageService.java` を確認する。

```bash
cat src/main/java/com/example/unit10/MessageService.java
```

実行する SQL は次だけである。

```sql
SELECT id, message
FROM study_message
WHERE id = 1
```

JPA / Entity / CRUD へ広げず、Database Connection の確認に必要な最小構成であることを確認する。

### 8. PostgreSQL 初期化 SQL を確認する

```bash
cat db/init/01-init.sql
```

次の 2 点を確認する。

```text
study_message Table を作成
id = 1 の Record を Insert
```

初期値は次である。

```text
Hello from PostgreSQL
```

この Script は空の PostgreSQL Data Directory を初回初期化するときだけ利用される。

### 9. Spring Boot Application を Maven Build する

```bash
mvn clean package
```

Build が成功することを確認する。

生成物を確認する。

```bash
ls -lh target/
```

次の JAR が存在することを確認する。

```text
target/unit10-spring-postgresql-1.0.0.jar
```

### 10. Dockerfile と JAR の関係を確認する

```bash
cat Dockerfile
```

主な流れは次である。

```text
JRE Base Image
↓
WORKDIR /app
↓
JAR を app.jar として COPY
↓
java -jar app.jar
```

Unit 06 と同じく、Maven Build は Host、Application Runtime は Container という責務分離になっている。

### 11. Compose から Spring Boot Image を Build する

```bash
docker compose build app
```

Spring Boot JAR を含む Image が Build される。

Docker Image 一覧も確認する。

```bash
docker image ls
```

Compose Project 用の Application Image が作成されていることを確認する。

### 12. 2 Service を Background で起動する

```bash
docker compose up -d
```

Compose が、

```text
Network
Volume
PostgreSQL Container
Spring Boot Container
```

を必要に応じて作成・起動する。

起動直後は PostgreSQL の初期化が続いている可能性があるため、すぐに API の成否だけで判断しない。

### 13. Compose Service の状態を確認する

```bash
docker compose ps
```

`app` と `postgres` が起動していることを確認する。  
問題がある場合は、この段階で Status を確認する。

### 14. PostgreSQL Logs を確認する

```bash
docker compose logs postgres
```

初回起動では PostgreSQL の初期化処理と `01-init.sql` の実行を確認する。  
最終的に PostgreSQL が Connection を受け付けられる状態になっていることを Logs から確認する。

### 15. Spring Boot Logs を確認する

```bash
docker compose logs app
```

Spring Boot が起動していることを確認する。

この Unit では `depends_on` があっても PostgreSQL の Readiness まで保証されないため、起動タイミングによって Database Connection に関する Log が出る可能性がある。

重要なのは、

```text
Container Start
と
Database Ready
は同じではない
```

と理解することである。

### 16. Host から API へアクセスする

PostgreSQL が Ready になったことを確認してから実行する。

```bash
curl http://localhost:8080/api/message
```

次の内容が返ることを確認する。

```json
{ "id": 1, "message": "Hello from PostgreSQL" }
```

Browser では次を開いてもよい。

```text
http://localhost:8080/api/message
```

### 17. API Request の通信経路を整理する

成功した Request は次の経路を通っている。

```text
Windows Host
↓ localhost:8080
app Container
↓ JDBC
postgres:5432
↓ Docker Network
postgres Container
↓
unit10_db
↓
study_message
```

API が成功したことを単なる Spring Boot の確認で終わらせず、2 Container 間通信全体が成立している結果として理解する。

### 18. `app` Container の DB Environment Variable を確認する

```bash
docker compose exec app \
  printenv DB_URL
```

次が表示されることを確認する。

```text
jdbc:postgresql://postgres:5432/unit10_db
```

User も確認する。

```bash
docker compose exec app \
  printenv DB_USER
```

```text
unit10_user
```

Compose → Container → Spring Boot Configuration の流れを実際の値で確認する。

### 19. PostgreSQL Service 名が名前解決できることを確認する

`app` Container から `postgres` の名前解決を確認する。

```bash
docker compose exec app \
  getent hosts postgres
```

`postgres` が Docker Network 内 Address に解決されることを確認する。

固定 IP Address を Application 設定に書かず、Service 名を利用していることを確認する。

### 20. PostgreSQL Container から Data を直接確認する

```bash
docker compose exec postgres \
  psql -U unit10_user -d unit10_db \
  -c 'SELECT * FROM study_message;'
```

次の Record を確認する。

```text
1 | Hello from PostgreSQL
```

API Response と PostgreSQL 内の Data が対応していることを確認する。

### 21. PostgreSQL Volume を確認する

Compose が作成した Volume を確認する。

```bash
docker volume ls
```

続いて Container の Mount を確認する。

```bash
docker compose exec postgres \
  printenv PGDATA
```

PostgreSQL 18 の Data Directory が `/var/lib/postgresql` 配下に存在することを確認する。

必要に応じて通常の Docker CLI から PostgreSQL Container の Mount 情報も確認する。

```bash
docker container inspect \
  --format '{{json .Mounts}}' \
  "$(docker compose ps -q postgres)"
```

### 22. Database Data を変更する

永続化確認のため、Database の Message を変更する。

```bash
docker compose exec postgres \
  psql -U unit10_user -d unit10_db \
  -c "UPDATE study_message SET message = 'Persisted after recreation' WHERE id = 1;"
```

確認する。

```bash
curl http://localhost:8080/api/message
```

次の内容が返ることを確認する。

```json
{ "id": 1, "message": "Persisted after recreation" }
```

### 23. `docker compose down` で Container を削除する

```bash
docker compose down
```

確認する。

```bash
docker compose ps -a
```

Service Container は削除される。

Volume を確認する。

```bash
docker volume ls
```

Unit 10 の Named Volume は残っている。

### 24. Compose から Container を再作成する

```bash
docker compose up -d
```

新しい `app` / `postgres` Container が作成される。

状態と Logs を確認する。

```bash
docker compose ps
docker compose logs postgres
```

今回は既存 PostgreSQL Volume を利用するため、初回と同じ空 Data Directory の初期化として `01-init.sql` を実行し直す構成ではない。

### 25. Container 再作成後も Database Data が残ることを確認する

PostgreSQL が Ready になった後に実行する。

```bash
curl http://localhost:8080/api/message
```

次が返ることを確認する。

```json
{ "id": 1, "message": "Persisted after recreation" }
```

初期値の、

```text
Hello from PostgreSQL
```

へ戻っていないことが重要である。

```text
Container
→ 再作成

Named Volume
→ 継続

Database Data
→ 継続
```

という関係を確認する。

### 26. 接続失敗時の確認方法を整理する

現在の正常状態を基準に、問題が起きたときの確認順を実際の Command と対応付ける。

まず状態を見る。

```bash
docker compose ps
```

Spring Boot 側の Logs を見る。

```bash
docker compose logs app
```

PostgreSQL 側の Logs を見る。

```bash
docker compose logs postgres
```

Spring Boot Container の接続情報を見る。

```bash
docker compose exec app \
  printenv DB_URL

docker compose exec app \
  printenv DB_USER
```

Network の接続状態を見る。

```bash
docker network inspect \
  unit10-spring-postgresql_unit10-network
```

環境によって実際の Resource 名を確認したい場合は、先に次を利用する。

```bash
docker network ls
```

最後に PostgreSQL 自体の Data を確認する。

```bash
docker compose exec postgres \
  psql -U unit10_user -d unit10_db \
  -c 'SELECT * FROM study_message;'
```

### 27. PostgreSQL を停止した場合の影響を確認する

Database Service だけを停止する。

```bash
docker compose stop postgres
```

状態を確認する。

```bash
docker compose ps
```

`app` は Running でも `postgres` は停止している。

この状態で API へアクセスする。

```bash
curl http://localhost:8080/api/message
```

Database Connection が利用できないため、正常な JSON Response は取得できない。

Spring Boot Logs を確認する。

```bash
docker compose logs app
```

ここで、

```text
app Container が Running
≠
Database を利用できる
```

ことを確認する。

### 28. PostgreSQL を再開して通信を復旧する

```bash
docker compose start postgres
```

PostgreSQL が Ready になるまで Logs を確認する。

```bash
docker compose logs postgres
```

その後、再度 API へアクセスする。

```bash
curl http://localhost:8080/api/message
```

再び、

```json
{ "id": 1, "message": "Persisted after recreation" }
```

が取得できることを確認する。

### 29. Unit 10 の Resource を Cleanup する

最後は PostgreSQL Data も不要になるため、Named Volume を含めて削除する。

```bash
docker compose down -v
```

確認する。

```bash
docker compose ps -a
docker network ls
docker volume ls
```

Unit 10 用の Container / Network / Named Volume が残っていないことを確認する。

Spring Boot の Build Image や Maven の `target/` は別 Resource / Build Artifact であり、`down -v` で削除されるものではない。

## 動作・確認ポイント

### Spring Boot + PostgreSQL

以下を確認する。

- `app` と `postgres` を別 Service / Container として起動できる。
- Host から Spring Boot API へアクセスし、PostgreSQL の Data を JSON として取得できる。
- Docker / Compose の通信経路を確認できる。

### Docker Network / Service 名

以下を確認する。

- `app` と `postgres` が同じ Docker Network に接続される。
- Spring Boot の DB Host に `localhost` ではなく Service 名 `postgres` を利用する。
- JDBC URL `jdbc:postgresql://postgres:5432/unit10_db` の各要素を説明できる。
- PostgreSQL Port を Host へ Publish しなくても Container 間通信できる。

### Environment Variable

以下を確認する。

- `.env` → Compose の値補間 → `environment` → Container → Spring Boot DataSource という設定の流れを確認できる。
- `DB_URL` / `DB_USER` / `DB_PASSWORD` と `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` の役割を区別できる。
- Connection 情報を Source Code に固定せず Container 外から渡す考え方を理解している。

### PostgreSQL / Volume

以下を確認する。

- PostgreSQL 初回起動時に `01-init.sql` から最小 Data が作成される。
- Named Volume に PostgreSQL Data が保存される。
- `docker compose down` 後に Container を再作成しても Database Data が残る。
- 初期化済み Volume では Initialization Script が毎回初期状態へ戻すものではないことを理解している。

### `depends_on` / Readiness

以下を確認する。

- Short syntax の `depends_on` が Service の起動順序を表すことを理解している。
- Container Start と PostgreSQL Ready を同一視しない。
- Database が Ready ではない場合、Application Container が存在していても DB Access が失敗し得ることを理解している。

### Troubleshooting

以下を確認する。

- `docker compose ps` で Service の状態を確認できる。
- `docker compose logs app` / `logs postgres` を Service ごとに使い分けられる。
- DB 接続情報、Service 名、Network、PostgreSQL Data の順に問題を切り分けられる。
- PostgreSQL Service 停止時に API が失敗し、再開後に復旧することを確認できる。

## 学習ポイント

### 2 Container 構成では Container ごとの役割を分ける

今回、

```text
app
→ Application Logic / HTTP API

postgres
→ Database Server / Persistent Data
```

という役割分離を行っている。

Spring Boot Image の中へ PostgreSQL Server を入れて 1 Container にまとめているわけではない。

```text
1 Container
→ 基本的に 1 つの主要な責務 / Process

複数の役割
→ Container を分けて Network で接続
```

という Docker の基本的な構成につながる。

### `localhost` は Container 境界を越えない

Spring Boot + PostgreSQL 構成で特に重要なのが `localhost` である。

```text
Spring Boot Container の localhost
→ Spring Boot Container

PostgreSQL Container の localhost
→ PostgreSQL Container
```

別 Container にある PostgreSQL へ接続するには、Docker Network 上の名前を使用する。

```text
postgres
↓
Docker の名前解決
↓
PostgreSQL Container
```

この理解がないと、Local Environment では動く JDBC URL をそのまま Container へ持ち込んで接続できない原因になる。

### Service 名は固定 IP Address の代わりになる接続点

Container の Network 内 IP Address は再作成によって変わり得る。  
しかし Compose Service 名は設定上の接続先として維持できる。

```text
Spring Boot
↓
postgres
↓
現在の PostgreSQL Container Address
```

Application は Container の具体的な IP Address ではなく、論理的な Service 名へ依存する。

### Host Port と Container Port の役割を分ける

今回 Host から必要なのは Spring Boot API への入口である。

```text
Host
↓ 8080
app
```

PostgreSQL は Spring Boot Container だけが Docker Network 内から利用するため、Host へ `5432` を Publish していない。

```text
app
↓ postgres:5432
postgres
```

すべての Container Port を Host へ公開する必要はない。

### Environment Variable で接続設定を Image から分離する

同じ Spring Boot Image でも、Database 接続先は実行 Environment によって変わり得る。

```text
Image
→ Application Program

Environment Variable
→ Database Host / Database / User / Password
```

DB Connection 情報を Image Build 時に固定せず、Runtime Configuration として扱う。

これは Unit 06 / 09 で学んだ「Image と設定の分離」が、実際の Database Connection に適用された形である。

### Database Data は PostgreSQL Container の寿命から分離する

PostgreSQL Container は作り直せるが、Database Data は残したい。

```text
postgres Container
→ 実行環境

unit10-postgres-data
→ Persistent Data
```

Unit 07 の Volume 学習が、Application + Database 構成でも同じ意味を持つ。

### Initialization Script と Persistent Data の役割を分ける

`01-init.sql` は初期 Data を作るための仕組みであり、毎回 Application 起動時に Database を初期状態へ戻す仕組みではない。

```text
空 Volume
→ Initialization Script

既存 Volume
→ 既存 Data
```

この違いによって Container を再作成しても更新後の Database Data を保持できる。

### `depends_on` は Database Connection 成功の保証ではない

`depends_on` があると、

```text
postgres
↓
app
```

の順で Service を Start できる。

しかし Database Server の Initialization が完了する時刻までは保証しない。

```text
Start Order
≠
Readiness
```

この違いは、実際の複数 Container Application で発生しやすい Connection Error を理解するうえで重要である。

### Logs は複数 Container 構成の重要な観測手段になる

単一 Container の場合より、複数 Container では問題の発生箇所が増える。

```text
HTTP
Network
Spring Boot
JDBC
Docker DNS
PostgreSQL
Database Data
```

そのため、まず Service ごとの状態と Logs を分けて確認する。

```text
ps
↓
app logs / postgres logs
↓
Environment
↓
Network
↓
Database
```

という順番を持っておくことで、原因を切り分けやすくなる。

### Compose は複数 Resource の関係を 1 つの構成として残す

CLI だけで同じ構成を作る場合、

```text
Network 作成
Volume 作成
PostgreSQL 起動
Spring Boot 起動
Port
Environment Variable
Mount
Network 接続
```

を個別に指定する必要がある。

Compose File ではそれらの関係を 1 つにまとめられる。

```text
compose.yaml
↓
Application + Database の構成
```

Unit 09 で学んだ Compose の価値が、Unit 10 ではより実際の Application 構成として確認できる。

### Unit 11 では Browser / Frontend の視点が追加される

Unit 10 では Container 間通信が、

```text
Spring Boot
↓
PostgreSQL
```

だけだった。

Unit 11 では、

```text
Browser
↓
React
↓
Spring Boot
↓
PostgreSQL
```

へ増える。

特に Browser 上の JavaScript は Docker Network の中で動いているわけではないため、

```text
Browser から見た API Host
Container から見た Service Host
```

を区別する必要がある。

Unit 10 の `localhost` / Service 名 / Port の理解が、そのまま Unit 11 の土台になる。

## 完了条件

以下を満たしたら Unit 10 を完了とする。

- Spring Boot と PostgreSQL を別 Service / Container とする 2 Container 構成を Docker Compose から起動できる。
- Unit 06〜09 で個別に学んだ Spring Boot Image、PostgreSQL、Network、Volume、Compose が今回どのように統合されているか説明できる。
- Spring Boot Container から PostgreSQL へ接続するとき、`localhost` ではなく Service 名 `postgres` を DB Host として利用する理由を説明できる。
- `jdbc:postgresql://postgres:5432/unit10_db` の Host / Port / Database 名を Container 間通信と関連付けて説明できる。
- `.env` / Compose `environment` / Spring Boot DataSource / PostgreSQL 初期設定 Environment Variable の流れと役割を区別できる。
- Host から `/api/message` へアクセスし、PostgreSQL の Record を JSON として取得できる。
- PostgreSQL Service を Host へ Port Publish しなくても、同じ Docker Network 上の Spring Boot から接続できることを理解している。
- PostgreSQL の Initialization Script が空の Data Directory の初回初期化時に利用されることを説明できる。
- Named Volume に Database Data を保存し、Container 削除・再作成後も更新した Data が保持されることを確認できる。
- Short syntax の `depends_on` による起動順序と PostgreSQL Readiness の違いを説明できる。
- `docker compose ps` / `logs` / `exec` を利用して、Spring Boot と PostgreSQL の状態・設定・Data を確認できる。
- DB 接続失敗時に、Container 状態 → Service Logs → Environment Variable → Service 名 / Network → PostgreSQL Data の順で基本的な切り分けができる。
- PostgreSQL Service を停止すると Application の DB Access が失敗し、再開後に復旧することを確認できる。
- Spring Boot + PostgreSQL の 2 Container 構成を、Unit 11 の React + Spring Boot + PostgreSQL 3 Container 構成へつながる基礎として説明できる。

ここまで確認できれば、Unit 11「React + Spring Boot + PostgreSQL（3 Container 構成）」へ進む。
