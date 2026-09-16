# 11. React + Spring Boot + PostgreSQL の 3 Container 構成

## この項目の目的

この Unit では、React / Vite の Frontend、Spring Boot の Backend、PostgreSQL の Database をそれぞれ独立した Service / Container として動かし、Docker Compose で 3 Container 構成を構築する。  
Unit 10 の Spring Boot + PostgreSQL に Frontend を加え、実際の Web Application に近い通信経路を確認する。

今回の全体像は次のとおり。

```text
Host Browser
↓
http://localhost:5173
↓
React / Vite

Browser 上の React JavaScript
↓
http://localhost:8080/api/message
↓
Spring Boot

Spring Boot Container
↓
jdbc:postgresql://postgres:5432/unit11_db
↓ Docker Network
PostgreSQL Container
↓
Named Volume
```

一見すると、

```text
Browser
→ React
→ Spring Boot
→ PostgreSQL
```

という一直線の構成に見える。

しかし、実際には React の JavaScript は Frontend Container の中で API Request を送るのではなく、Host Browser 上で実行される。

そのため、この Unit では次の 2 種類の接続先を明確に区別する。

```text
Browser 上の React
→ Spring Boot
→ http://localhost:8080

Spring Boot Container
→ PostgreSQL Container
→ postgres:5432
```

Unit 11 の最重要ポイントは、

> Browser から見た接続先と、Docker Network 内の Container から見た接続先は同じではない

ということを理解することである。

認証、状態管理、Routing、複雑な CRUD、UI 作り込み、本格的な Database 設計などは扱わない。  
React から Spring Boot API を呼び出し、Spring Boot が PostgreSQL の 1 Record を取得して JSON として返し、その内容を React 画面へ表示する最小構成に限定する。

最終的には、

```text
Frontend
+
Backend
+
Database
```

の 3 Container 構成を Docker Compose で起動し、それぞれの通信が「どこから」「どこへ」「どの名前 / Port を使って」行われているか説明できる状態を目指す。

## 学習内容

### Unit 05〜10 の内容を統合する

Unit 11 は、これまで学んできた主要な Docker 学習内容を統合する Unit である。

```text
Unit 05
React / Vite Container
        +
Unit 06
Spring Boot Container
        +
Unit 07
PostgreSQL / Volume
        +
Unit 08
Docker Network
        +
Unit 09
Docker Compose
        +
Unit 10
Spring Boot → PostgreSQL
        ↓
Unit 11
React + Spring Boot + PostgreSQL
```

新しい Docker Command を大量に覚えることが目的ではない。

これまで個別に確認した、

```text
Port Mapping
Environment Variable
Docker Network
Service 名
Named Volume
Compose
```

が 3 Service 構成でどのように組み合わさるかを確認する。

### 今回の 3 Service 構成

`compose.yaml` には次の 3 Service を定義する。

```text
frontend
→ React / Vite

backend
→ Spring Boot

postgres
→ PostgreSQL
```

Compose Project 全体は次のようになる。

```text
Compose Project
unit11-react-spring-postgresql
│
├─ frontend Service
│  └─ React / Vite Container
│
├─ backend Service
│  └─ Spring Boot Container
│
├─ postgres Service
│  └─ PostgreSQL Container
│
├─ unit11-network
│
└─ unit11-postgres-data
```

3 Service は同じ Docker Network に接続される。

ただし、

> 同じ Docker Network に接続されているから、Browser 上の React も Docker Network 内部から通信する

という意味ではない。

この違いを後続で詳しく確認する。

### Frontend Service

`frontend` Service は React / Vite Application を Dockerfile から Build する。

```yaml
frontend:
  build:
    context: ./frontend
    dockerfile: Dockerfile
```

Unit 05 と同じく Vite Development Server を Container 内で動かす。

```text
React Source
↓
Docker Build
↓
Node.js Image
↓
frontend Container
↓
Vite Development Server
```

Vite は Container 内の Port `5173` で待ち受ける。

Host Browser からアクセスできるように、

```yaml
ports:
  - '${FRONTEND_PORT:-5173}:5173'
```

を設定する。

### Frontend Container と Browser は別の実行場所

Frontend Container は Vite Development Server を動かしている。

一方、React Application の JavaScript は Browser へ配信された後、Browser 上で実行される。

```text
frontend Container
↓
HTML / JavaScript を配信
↓
Host Browser
↓
React JavaScript を実行
```

したがって、

```javascript
fetch(...)
```

を実行する主体は Frontend Container ではなく Browser である。

ここを理解することが Unit 11 では非常に重要である。

### React の API Request は Browser から送られる

`App.jsx` では次の API Base URL を利用する。

```javascript
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
```

そして、

```javascript
fetch(`${apiBaseUrl}/api/message`);
```

を実行する。

この `fetch` は Browser 上で実行されるため、接続先は Browser から到達できる URL でなければならない。

今回の Browser から Spring Boot への接続先は、

```text
http://localhost:8080
```

である。

### なぜ `http://backend:8080` ではないのか

Compose では `backend` という Service 名が存在する。

しかし、

```text
backend
```

という名前は Docker Network 内で Service 間通信に利用できる名前である。

Host Browser は Docker Network の内側に存在しない。

```text
Docker Network
├─ frontend Container
├─ backend Container
└─ postgres Container

Host Browser
→ Docker Network の外側
```

そのため Browser 上の JavaScript から、

```text
http://backend:8080
```

へ接続しようとしても、通常は Host OS の DNS / Name Resolution から `backend` を解決できない。

今回の構成では、

```text
Browser
↓
localhost:8080
↓ Host Port Mapping
backend Container:8080
```

とアクセスする。

### `VITE_API_BASE_URL`

`.env.example` では次を定義する。

```text
VITE_API_BASE_URL=http://localhost:8080
```

Compose はこの値を `frontend` Service の Environment Variable として渡す。

```yaml
environment:
  VITE_API_BASE_URL: '${VITE_API_BASE_URL:-http://localhost:8080}'
```

Vite は `VITE_` Prefix の Variable を Frontend Code から `import.meta.env` 経由で参照できる。

流れは次のとおり。

```text
.env
↓ Compose の値補間
compose.yaml
↓
frontend Container Environment
↓
Vite
↓
Frontend JavaScript
↓
Host Browser
```

ただし、`VITE_` Prefix の値は Browser から参照できる Frontend Code に含まれ得る。  
そのため Password / Token などの秘密情報を `VITE_` Variable に入れてはいけない。  
今回設定するのは公開されても問題のない API URL のみである。

### Backend Service

`backend` Service は Unit 10 と同様に Spring Boot Application を動かす。

```yaml
backend:
  build:
    context: ./backend
    dockerfile: Dockerfile
```

Host 側で Maven Build した JAR を Dockerfile から Runtime Image へ取り込む。

```text
Java Source
↓
Maven Build
↓
JAR
↓
Docker Build
↓
backend Container
```

Spring Boot は Container 内 Port `8080` を利用する。

### Backend を Host へ Publish する理由

`backend` Service には次を定義している。

```yaml
ports:
  - '${BACKEND_PORT:-8080}:8080'
```

Unit 10 では Host の `curl` / Browser から API を確認するために Spring Boot を Publish した。

Unit 11 ではさらに、Browser 上の React JavaScript が Spring Boot API を直接呼び出すために必要になる。

```text
Host Browser
↓
localhost:8080
↓
Host Port
↓
backend Container:8080
```

Frontend Container から Backend Container への Server-side 通信ではないことが重要である。

### PostgreSQL Service

`postgres` Service は Unit 10 と同じ考え方を利用する。

```yaml
postgres:
  image: postgres:18.6-alpine3.24
```

PostgreSQL は Host へ Port Publish しない。

```text
backend Container
↓
postgres:5432
↓ Docker Network
postgres Container
```

という内部通信だけで利用するためである。

### Spring Boot → PostgreSQL は Docker Network 内通信

Backend の JDBC URL は次である。

```text
jdbc:postgresql://postgres:5432/unit11_db
```

ここでは Browser は関係しない。

Request を送る主体は Spring Boot Container であり、接続先は同じ Docker Network に存在する PostgreSQL Service である。

```text
backend Container
↓
Host: postgres
Port: 5432
↓
unit11-network
↓
postgres Container
```

したがって、ここでは `localhost` でも Host 側の公開 Port でもなく Service 名を利用する。

### Browser → Backend と Backend → PostgreSQL の違い

Unit 11 の通信先を並べると次のようになる。

```text
Browser → Frontend
http://localhost:5173

Browser 上の React → Backend
http://localhost:8080

Backend Container → PostgreSQL
postgres:5432
```

同じ Application 全体の通信であっても、「Request を送る主体」が異なるため接続先の名前も異なる。

```text
Browser
→ Host から解決できる URL

Container
→ Docker Network 内で解決できる Service 名
```

という基準で考える。

### Port Mapping

今回 Host へ Publish する Port は 2 つである。

```text
Frontend
Host 5173
→ Container 5173

Backend
Host 8080
→ Container 8080
```

PostgreSQL は Publish しない。

```text
Frontend
→ Browser がアクセスするため Publish

Backend
→ Browser 上の React がアクセスするため Publish

PostgreSQL
→ Backend Container だけが内部 Network から利用するため Publish しない
```

「Container が存在するからすべて Host へ Publish する」のではなく、誰がどこから接続するかで判断する。

### Docker Network

3 Service は同じ `unit11-network` に接続する。

```yaml
networks:
  unit11-network:
    driver: bridge
```

Compose 内では次のような関係になる。

```text
unit11-network
├─ frontend
├─ backend
└─ postgres
```

この Network により Service 間通信が可能になる。

ただし今回 Browser 上の React は `backend` Service 名を使わない。

Frontend Container が Docker Network に接続されている事実と、Browser 上の JavaScript の Network Environment は別である。

### CORS が必要になる理由

React Page は、

```text
http://localhost:5173
```

から表示される。

API は、

```text
http://localhost:8080
```

に存在する。

Browser から見ると、Port が異なるため Origin が異なる。

```text
Frontend Origin
http://localhost:5173

Backend Origin
http://localhost:8080
```

Origin は概念的に、

```text
scheme
+
host
+
port
```

の組み合わせで判断される。

したがって今回の React → Spring Boot API Request は Cross-Origin Request になる。

### CORS とは

CORS は Cross-Origin Resource Sharing の略である。

Browser は Security のため、異なる Origin への JavaScript Request を無条件には許可しない。

Backend 側が、

```text
この Origin からの Request を許可する
```

という情報を Response Header で返すことで、Browser が Frontend JavaScript から Response を利用できるようにする。

重要なのは、

> CORS は Docker Network の接続許可設定ではなく、Browser の Same-Origin Policy に関係する仕組み

という点である。

Docker Network 内で `backend` と `postgres` が通信するときには Browser が介在しないため、今回の CORS は関係しない。

### Spring Boot の CORS 設定

`CorsConfig.java` では、

```text
/api/**
```

に対して、

```text
GET
```

だけを許可する。

許可 Origin は Environment Variable から受け取る。

```text
CORS_ALLOWED_ORIGIN=http://localhost:5173
```

構成は次のようになる。

```text
.env
↓
compose.yaml
↓
backend Container
↓
CORS_ALLOWED_ORIGIN
↓
application.properties
↓
CorsConfig
```

今回必要な最小範囲だけを許可している。

### `*` を使わない理由

学習用だからといって、

```text
すべての Origin を無条件に許可
```

する構成にはしていない。

今回実際に利用する Frontend Origin は、

```text
http://localhost:5173
```

と分かっているため、その Origin だけを許可する。

CORS の詳細な Security 設計は今回の対象外だが、

```text
必要な Origin
必要な Path
必要な HTTP Method
```

に限定する考え方だけは押さえる。

### React から API Response を表示する流れ

React では Page 表示後に `useEffect` から API Request を送る。

```text
React Render
↓
useEffect
↓
fetch
↓
Spring Boot API
↓
JSON
↓
React State
↓
画面表示
```

取得する Data は次の形である。

```json
{
  "id": 1,
  "message": "Hello from PostgreSQL through Spring Boot"
}
```

React の高度な State Management は扱わず、API 通信結果を表示する最小実装にしている。

### Spring Boot の API

Backend の Endpoint は Unit 10 と同じ考え方で次の 1 つだけである。

```text
GET /api/message
```

Spring Boot は `JdbcTemplate` を使って PostgreSQL から 1 Record を取得する。

```text
Browser
↓
GET /api/message
↓
MessageController
↓
MessageService
↓
JdbcTemplate
↓
PostgreSQL
```

Application 実装の複雑さではなく、Container 間と Browser 間の通信に集中する。

### PostgreSQL の初期 Data

`db/init/01-init.sql` では最小 Table と Data を作成する。

```sql
CREATE TABLE IF NOT EXISTS study_message (
    id INTEGER PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);
```

初期 Record は次である。

```text
Hello from PostgreSQL through Spring Boot
```

この Data が、

```text
PostgreSQL
↓
Spring Boot
↓
JSON
↓
React
↓
Browser
```

と最終画面まで到達することを確認する。

### PostgreSQL Named Volume

PostgreSQL Data は、

```text
unit11-postgres-data
```

へ保存する。

```yaml
volumes:
  - unit11-postgres-data:/var/lib/postgresql
```

Container の再作成と Data の寿命を分離する考え方は Unit 07 / 10 と同じである。

```text
postgres Container
→ 再作成可能

Named Volume
→ Data を保持
```

### Environment Variable の役割を分ける

Unit 11 では複数種類の Environment Variable が登場する。

Frontend:

```text
VITE_API_BASE_URL
```

Backend:

```text
DB_URL
DB_USER
DB_PASSWORD
CORS_ALLOWED_ORIGIN
```

PostgreSQL:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
```

それぞれの役割は異なる。

```text
VITE_API_BASE_URL
→ Browser 上の React が呼ぶ API URL

CORS_ALLOWED_ORIGIN
→ Spring Boot が許可する Browser Origin

DB_URL / DB_USER / DB_PASSWORD
→ Spring Boot が PostgreSQL へ接続する設定

POSTGRES_*
→ PostgreSQL Official Image の初期化設定
```

同じ `.env` を元にしていても、利用する Process / Container が異なることを理解する。

### `depends_on`

構成は次の順序になっている。

```text
postgres
↓
backend
↓
frontend
```

Compose File では、

```text
frontend depends_on backend
backend depends_on postgres
```

としている。

ただし Unit 09 / 10 と同じく、Short syntax の `depends_on` は Application の Readiness まで保証するものではない。

```text
Service Start
≠
Application Ready
```

Frontend が起動していても Backend がまだ API Request を受け付けられない場合がある。  
Backend が起動していても PostgreSQL がまだ Ready ではない場合がある。

### 3 Container 構成での Troubleshooting

3 Service になると、問題が起きたときの確認対象も増える。

React 画面に Data が表示されない場合、例えば次の原因がある。

```text
frontend が起動していない
Browser が frontend に到達できない
VITE_API_BASE_URL が誤っている
backend が起動していない
CORS が許可されていない
backend が postgres に接続できない
postgres が起動していない
Database Data が存在しない
```

そのため通信経路の順番に切り分ける。

```text
Browser
↓
Frontend
↓
Backend
↓
PostgreSQL
```

### 接続失敗時の確認順

今回の基本的な確認順は次のとおり。

```text
1. docker compose ps
   ↓
3 Service が Running か

2. Browser
   ↓
http://localhost:5173 が開くか

3. Frontend の API URL
   ↓
http://localhost:8080 か

4. Backend 単体 API
   ↓
curl http://localhost:8080/api/message

5. backend logs
   ↓
Spring Boot / CORS / DB Error

6. postgres logs
   ↓
PostgreSQL 起動状態

7. Backend Environment
   ↓
DB_URL / CORS_ALLOWED_ORIGIN

8. Docker Network
   ↓
backend / postgres が同じ Network か

9. Database
   ↓
Table / Record が存在するか
```

React 画面だけを見て原因を推測せず、通信経路を分解して確認する。

### Unit 12 へのつながり

Unit 11 では 3 Container 構成を動かすことを優先している。  
Frontend は Vite Development Server、Backend は Host Maven Build 後の JAR を Runtime Image へ Copy する構成である。  
これは学習には分かりやすいが、Production 用 Container Image として完成形ではない。

Unit 12 では、

```text
Multi-stage Build
Build Cache
.dockerignore
Version 固定
Healthcheck
Restart Policy
USER
Image Size
Docker Hub
```

などを扱い、これまで作ってきた Dockerfile / Compose の改善点を整理する。

Unit 11 では、まず 3 Container Application の通信構造を確実に理解することを優先する。

## 使用するもの

### Frontend

```text
React 19.2.8
Vite 8.2.2
Node.js 24.20.0
```

### Frontend Base Image

```text
node:24.20.0-alpine3.24
```

### Backend

```text
Spring Boot 4.1.1
Java 21
```

### Backend Runtime Image

```text
eclipse-temurin:21.0.12_8-jre-alpine-3.24
```

### PostgreSQL Image

```text
postgres:18.6-alpine3.24
```

### Compose Project

```text
unit11-react-spring-postgresql
```

### Services

```text
frontend
backend
postgres
```

### Docker Network

```text
unit11-network
```

### Named Volume

```text
unit11-postgres-data
```

### Browser から Frontend

```text
http://localhost:5173
```

### Browser / React から Backend

```text
http://localhost:8080
```

### Backend から PostgreSQL

```text
jdbc:postgresql://postgres:5432/unit11_db
```

### API Endpoint

```text
GET /api/message
```

### Frontend Origin

```text
http://localhost:5173
```

## 事前準備

Unit 10 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/11-react-spring-boot-postgresql
```

Git Bash で Repository 内の次の Unit Directory へ移動した状態から操作する。

```text
units/11-react-spring-boot-postgresql
```

現在位置を確認する。

```bash
pwd
```

生成物を確認する。

```bash
find . -maxdepth 8 -type f | sort
```

主に次が存在することを確認する。

```text
.env.example
compose.yaml
frontend/
backend/
db/init/01-init.sql
```

Maven を確認する。

```bash
mvn -version
```

Docker Compose も確認する。

```bash
docker compose version
```

Host Port `5173` / `8080` を別 Container が利用していないことも確認する。

```bash
docker container ls
```

## ハンズオン

### 1. 3 Service 構成を確認する

`compose.yaml` を確認する。

```bash
cat compose.yaml
```

大きな構造は次のとおり。

```text
services
├─ frontend
├─ backend
└─ postgres

networks
└─ unit11-network

volumes
└─ unit11-postgres-data
```

3 Service が同じ Network に参加する一方、それぞれ役割が異なることを確認する。

### 2. Frontend Service を確認する

`frontend` Service の主な設定を見る。

```text
build
ports
environment
depends_on
networks
```

特に次を確認する。

```text
VITE_API_BASE_URL=http://localhost:8080
```

ここで `backend:8080` ではない理由を、Browser が Docker Network の外側にいることと関連付けて確認する。

### 3. Backend Service を確認する

`backend` Service の主な設定を見る。

```text
build
ports
environment
depends_on
networks
```

DB 接続先は次である。

```text
jdbc:postgresql://postgres:5432/unit11_db
```

Frontend の API URL と違い、ここでは Docker Network 内の Service 名 `postgres` を利用する。

### 4. PostgreSQL Service を確認する

`postgres` Service の主な設定を見る。

```text
image
environment
volumes
networks
```

Host へ Port `5432` を Publish する `ports` が存在しないことを確認する。  
Backend から Docker Network 内部で利用するためである。

### 5. `.env.example` から `.env` を作成する

```bash
cp .env.example .env
```

確認する。

```bash
cat .env
```

主な値は次のとおり。

```text
FRONTEND_PORT=5173
BACKEND_PORT=8080
VITE_API_BASE_URL=http://localhost:8080
CORS_ALLOWED_ORIGIN=http://localhost:5173
POSTGRES_DB=unit11_db
POSTGRES_USER=unit11_user
POSTGRES_PASSWORD=unit11-password
```

`.env` は Local 用 File なので Git へ Commit しない。

### 6. Compose の解釈結果を確認する

```bash
docker compose config
```

Frontend:

```text
VITE_API_BASE_URL
```

Backend:

```text
DB_URL
DB_USER
DB_PASSWORD
CORS_ALLOWED_ORIGIN
```

PostgreSQL:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
```

がどのように解決されているか確認する。

### 7. React の API URL 実装を確認する

```bash
cat frontend/src/App.jsx
```

次を確認する。

```javascript
import.meta.env.VITE_API_BASE_URL;
```

および、

```javascript
fetch(`${apiBaseUrl}/api/message`);
```

API Request を送る JavaScript が Browser 上で実行されるため、

```text
http://localhost:8080
```

を利用することを確認する。

### 8. Frontend Dockerfile を確認する

```bash
cat frontend/Dockerfile
```

Unit 05 と同じく、

```text
Node.js Image
↓
npm install
↓
Source Copy
↓
Vite Development Server
```

という構成である。

起動 Command の、

```text
--host 0.0.0.0
```

により、Container 外の Host Browser から Vite へアクセス可能にしている。

### 9. Backend の CORS 設定を確認する

```bash
cat backend/src/main/java/com/example/unit11/CorsConfig.java
```

次を確認する。

```text
Path
/api/**

Origin
http://localhost:5173

Method
GET
```

CORS が Docker Network 用の設定ではなく、Browser の Cross-Origin Request に対する設定であることを確認する。

### 10. Backend の DB 接続設定を確認する

```bash
cat backend/src/main/resources/application.properties
```

Compose から渡された、

```text
DB_URL
DB_USER
DB_PASSWORD
```

が Spring DataSource に使用される。

Compose 起動時の接続 Host は `postgres` である。

### 11. PostgreSQL 初期化 SQL を確認する

```bash
cat db/init/01-init.sql
```

初期 Record を確認する。

```text
Hello from PostgreSQL through Spring Boot
```

この Data が最終的に React 画面まで届くことを後で確認する。

### 12. Backend を Maven Build する

Backend Directory へ移動する。

```bash
cd backend
```

Maven Build を実行する。

```bash
mvn clean package
```

次の JAR が生成されることを確認する。

```bash
ls -lh target/
```

```text
unit11-spring-backend-1.0.0.jar
```

Unit Root へ戻る。

```bash
cd ..
```

### 13. Compose から Frontend / Backend Image を Build する

```bash
docker compose build
```

`frontend` と `backend` が Build 対象になる。

`postgres` は Official Image を利用する。

Image 一覧も確認する。

```bash
docker image ls
```

### 14. 3 Service を起動する

```bash
docker compose up -d
```

状態を確認する。

```bash
docker compose ps
```

次の 3 Service が確認できる。

```text
frontend
backend
postgres
```

起動直後は Backend / PostgreSQL の初期化が続いている可能性があるため、Status だけで Application Ready と判断しない。

### 15. PostgreSQL Logs を確認する

```bash
docker compose logs postgres
```

初回は Database Initialization と `01-init.sql` の実行を確認する。

PostgreSQL が Connection を受け付けられる状態になるまで確認する。

### 16. Backend Logs を確認する

```bash
docker compose logs backend
```

Spring Boot が起動していることを確認する。  
Database 起動タイミングによって一時的な DB Connection 関連 Log が発生する可能性があることも Unit 10 と同様に理解する。

### 17. Frontend Logs を確認する

```bash
docker compose logs frontend
```

Vite Development Server が起動し、Container Port `5173` で待ち受けていることを確認する。

### 18. Backend API を単体で確認する

まず React を介さず Backend API 自体を Host から確認する。

```bash
curl http://localhost:8080/api/message
```

次の JSON が返ることを確認する。

```json
{ "id": 1, "message": "Hello from PostgreSQL through Spring Boot" }
```

これにより、

```text
Host
↓
Backend
↓
PostgreSQL
```

までが正常であることを先に確認する。

### 19. Browser で React へアクセスする

Browser で次を開く。

```text
http://localhost:5173
```

画面に、

```text
API 接続先
http://localhost:8080
```

が表示されることを確認する。

さらに PostgreSQL 由来の、

```text
ID: 1 / Message: Hello from PostgreSQL through Spring Boot
```

が表示されることを確認する。

### 20. 一連の通信経路を整理する

画面表示が成功したときの経路は次のとおり。

```text
Host Browser
↓
localhost:5173
↓
frontend Container
↓
React JavaScript を Browser へ配信
↓
Host Browser 上で React 実行
↓
localhost:8080/api/message
↓
backend Container
↓
postgres:5432
↓
postgres Container
↓
Database
```

「Frontend Container → Backend Container」と単純化しすぎず、Browser が途中に存在することを確認する。

### 21. Browser の API URL と Docker Service 名の違いを確認する

Frontend で利用している API URL を確認する。

```bash
docker compose exec frontend \
  printenv VITE_API_BASE_URL
```

次を確認する。

```text
http://localhost:8080
```

Backend の DB URL を確認する。

```bash
docker compose exec backend \
  printenv DB_URL
```

次を確認する。

```text
jdbc:postgresql://postgres:5432/unit11_db
```

並べて違いを整理する。

```text
Browser → Backend
localhost

Backend → PostgreSQL
postgres
```

### 22. Backend から PostgreSQL の名前解決を確認する

```bash
docker compose exec backend \
  getent hosts postgres
```

`postgres` が Docker Network 内 Address に解決されることを確認する。  
この名前解決は Container 内から利用するものであり、Browser が `postgres` を解決するわけではない。

### 23. Frontend Container から Backend Service 名を確認する

Frontend Container 自体も同じ Docker Network に存在するため、Container 内からは `backend` を名前解決できる。

```bash
docker compose exec frontend \
  ping -c 1 backend
```

成功した場合でも、

```text
frontend Container から backend を解決できる
```

ことと、

```text
Browser 上の React が backend を解決できる
```

ことは別である。

この違いを確認するための重要な Step である。

### 24. CORS を Browser の通信として確認する

現在の Backend は、

```text
http://localhost:5173
```

を許可している。

Browser から React Page を開き、API Data が表示されることを確認する。  
Backend Logs も確認する。

```bash
docker compose logs backend
```

CORS が成功している場合でも、Docker Network の設定が変更されたわけではないことを理解する。

### 25. PostgreSQL Data を直接確認する

```bash
docker compose exec postgres \
  psql -U unit11_user -d unit11_db \
  -c 'SELECT * FROM study_message;'
```

API / React 画面の Data と同じ Record が存在することを確認する。

### 26. PostgreSQL Data を変更する

永続化と画面反映を確認するため Data を変更する。

```bash
docker compose exec postgres \
  psql -U unit11_user -d unit11_db \
  -c "UPDATE study_message SET message = 'Updated in Unit 11' WHERE id = 1;"
```

Browser を Reload する。

次が表示されることを確認する。

```text
ID: 1 / Message: Updated in Unit 11
```

```text
PostgreSQL
↓
Spring Boot
↓
React
↓
Browser
```

という経路で変更後 Data が反映される。

### 27. Named Volume を確認する

```bash
docker volume ls
```

PostgreSQL Container の Mount も確認する。

```bash
docker container inspect \
  --format '{{json .Mounts}}' \
  "$(docker compose ps -q postgres)"
```

Database Data が Container Writable Layer ではなく Named Volume に保存されていることを確認する。

### 28. Container を削除する

```bash
docker compose down
```

Service Container と Network が削除される。

```bash
docker compose ps -a
```

Volume は残っている。

```bash
docker volume ls
```

### 29. 3 Container を再作成する

```bash
docker compose up -d
```

状態と Logs を確認する。

```bash
docker compose ps
docker compose logs postgres
docker compose logs backend
```

PostgreSQL は既存 Volume を利用するため、初回の空 Data Directory として初期化し直すものではない。

### 30. 再作成後も Data が残ることを確認する

Browser で再度、

```text
http://localhost:5173
```

を開く。

次が残っていることを確認する。

```text
Updated in Unit 11
```

Container を再作成しても Database Data が Named Volume によって保持されている。

### 31. Backend を停止して Frontend への影響を確認する

Backend Service だけを停止する。

```bash
docker compose stop backend
```

状態を確認する。

```bash
docker compose ps
```

Browser を Reload する。

React 自体は Frontend Service から配信されるため Page を表示できても、API Request は失敗する。

画面に Error が表示されることを確認する。

```text
Frontend が動いている
≠
Backend API が利用できる
```

という Service 分離を確認する。

### 32. Backend を再開する

```bash
docker compose start backend
```

Logs を確認する。

```bash
docker compose logs backend
```

Backend が利用可能になった後、Browser を Reload し、再び PostgreSQL Data が表示されることを確認する。

### 33. Troubleshooting の確認順を整理する

React 画面に Data が出ない場合、次の順で確認する。

Compose Service:

```bash
docker compose ps
```

Frontend:

```bash
docker compose logs frontend
```

Backend API:

```bash
curl http://localhost:8080/api/message
```

Backend Logs:

```bash
docker compose logs backend
```

Frontend API URL:

```bash
docker compose exec frontend \
  printenv VITE_API_BASE_URL
```

Backend DB URL:

```bash
docker compose exec backend \
  printenv DB_URL
```

PostgreSQL:

```bash
docker compose logs postgres

docker compose exec postgres \
  psql -U unit11_user -d unit11_db \
  -c 'SELECT * FROM study_message;'
```

問題を、

```text
Browser
Frontend
Backend
Database
```

の順に分解して確認する。

### 34. Unit 11 の Resource を Cleanup する

最後は PostgreSQL Data も不要になるため Volume を含めて削除する。

```bash
docker compose down -v
```

確認する。

```bash
docker compose ps -a
docker network ls
docker volume ls
```

Unit 11 用 Container / Network / Named Volume が残っていないことを確認する。  
Frontend / Backend の Build Image は `down -v` だけでは削除されない。

## 動作・確認ポイント

### 3 Service 構成

以下を確認する。

- `frontend` / `backend` / `postgres` の 3 Service を Docker Compose から起動できる。
- 各 Service が Frontend / Backend / Database という異なる役割を持つ。
- 3 Container が同じ Docker Network に接続される。

### Browser / Frontend

以下を確認する。

- Browser から `http://localhost:5173` で React Page を表示できる。
- React の JavaScript が Browser 上で実行されることを理解している。
- React の API Request が Frontend Container ではなく Browser から送られることを説明できる。
- `VITE_API_BASE_URL` に Browser から到達できる `http://localhost:8080` を設定する理由を理解している。

### Backend / PostgreSQL

以下を確認する。

- Browser 上の React から `http://localhost:8080/api/message` へアクセスできる。
- Backend Container から PostgreSQL へ `postgres:5432` で接続できる。
- `localhost` と Docker Network 内 Service 名の使い分けを説明できる。
- PostgreSQL Port を Host へ Publish しなくても Backend から通信できる。

### CORS

以下を確認する。

- `http://localhost:5173` と `http://localhost:8080` が別 Origin である理由を理解している。
- CORS が Docker Network ではなく Browser の Same-Origin Policy に関係することを説明できる。
- Spring Boot 側で Frontend Origin と GET `/api/**` に限定した CORS 設定を利用できる。

### Data / Volume

以下を確認する。

- PostgreSQL の Data が Spring Boot を経由して React 画面に表示される。
- Database Data を変更すると Browser の表示にも反映される。
- Named Volume によって Container 再作成後も Data が保持される。

### Troubleshooting

以下を確認する。

- Frontend が Running でも Backend 停止時には API Data を取得できないことを確認できる。
- `ps` / Service Logs / API 単体確認 / Environment Variable / Database の順で基本的な切り分けができる。
- Browser から見た URL と Container から見た Host 名を切り分けて調査できる。

## 学習ポイント

### 「3 Container」と「3 段階の通信」は同じ意味ではない

構成上は、

```text
frontend
backend
postgres
```

の 3 Container が存在する。

しかし実際の Request の流れは、

```text
Browser
↓ frontend から JavaScript を取得
Browser
↓ backend API を呼ぶ
backend
↓ postgres へ接続
```

となる。

React が Browser Application であるため、Frontend Container が Backend への HTTP Client になるわけではない。

### Browser と Container では名前解決の世界が違う

Docker Network 内では、

```text
backend
postgres
```

といった Service 名を利用できる。

一方、Host Browser は Docker Network の DNS を利用しない。

```text
Browser
→ localhost:8080

Backend Container
→ postgres:5432
```

という違いは、Frontend / Backend / Database を Container 化するときに非常に重要である。

### Frontend Container の存在だけで通信元を判断しない

Frontend を Container 化すると、

> React から API を呼ぶので `backend` Service 名を使えばよい

と考えやすい。

しかし Frontend Container は Vite Server を動かし、JavaScript を Browser へ配信している。

```text
Container
→ JavaScript を配信

Browser
→ JavaScript を実行
```

どの Process / Runtime が実際に通信するのかを考えて接続先を決める。

### Port Publishing は Host / Browser からの入口になる

Frontend と Backend は Browser から利用するため Host Port を Publish する。

```text
5173
→ React / Vite

8080
→ Spring Boot API
```

PostgreSQL は Browser から直接利用しないため Publish しない。  
「外部から必要な Service だけ入口を作る」という考え方を確認する。

### CORS と Docker Network を混同しない

Docker Network が接続可能でも Browser の CORS によって JavaScript が Response を利用できない場合がある。  
反対に CORS を許可しても、Docker Network や Port Mapping が壊れていれば通信は成立しない。

```text
Docker Network / Port
→ Network 到達性

CORS
→ Browser が Cross-Origin Response を利用できるか
```

別の Layer の問題として考える。

### Environment Variable は利用する場所によって意味が変わる

Unit 11 では Environment Variable が増えるが、名前ではなく「誰が使うか」で整理する。

```text
VITE_API_BASE_URL
→ Frontend / Browser 側の API 接続先

CORS_ALLOWED_ORIGIN
→ Backend が許可する Browser Origin

DB_URL
→ Backend から Database への接続先

POSTGRES_*
→ PostgreSQL 初期化
```

設定値の流れを Process / Container 単位で考える。

### `VITE_` Variable は Secret 用ではない

Frontend Code は Browser へ配信される。  
Vite から Frontend Code へ公開した Environment Variable も Browser 側から確認可能になる。

そのため、

```text
API URL
→ 公開してよい

Password / Token
→ VITE_ Variable へ入れない
```

と区別する。

### Frontend / Backend / Database は独立して故障し得る

3 Service は別 Process / Container である。

```text
Frontend 正常
Backend 異常
Database 正常
```

のような状態もあり得る。

そのため、

```text
Page が表示された
=
System 全体が正常
```

ではない。  
各 Service と通信区間を分けて確認する。

### Unit 11 は実際の Web System の通信境界を理解する Unit である

この Unit の本質は 3 Container を起動することだけではない。

```text
Browser 境界
Host 境界
Container 境界
Docker Network 境界
Database 境界
```

を意識し、

```text
誰が通信するか
どこから通信するか
どの名前を解決できるか
どの Port を利用するか
```

を判断できるようになることが重要である。

### Unit 12 では構成を改善する

Unit 11 までで、

```text
React
Spring Boot
PostgreSQL
Docker Compose
Network
Volume
Environment Variable
```

を組み合わせた Application を動かせる状態になる。

Unit 12 では、ここまで使ってきた Dockerfile / Compose を題材に、

```text
Build
Cache
Image
Health
Restart
User
Size
```

といった観点から Best Practice を整理する。

まず Unit 11 では、3 Container の通信構造を確実に理解する。

## 完了条件

以下を満たしたら Unit 11 を完了とする。

- React / Spring Boot / PostgreSQL をそれぞれ独立した Service / Container とする 3 Container 構成を Docker Compose から起動できる。
- Frontend Container が Vite Server を動かし、React JavaScript 自体は Host Browser 上で実行されることを説明できる。
- Browser 上の React → Spring Boot では `http://localhost:8080`、Spring Boot Container → PostgreSQL では `postgres:5432` を利用する理由を説明できる。
- Host / Browser から見た URL と Docker Network 内の Service 名の違いを説明できる。
- Frontend / Backend の Port Mapping が必要で、PostgreSQL の Host Port Publishing が今回不要である理由を説明できる。
- `VITE_API_BASE_URL` / `CORS_ALLOWED_ORIGIN` / `DB_URL` / `POSTGRES_*` の役割を、それぞれ利用する Process / Service と関連付けて説明できる。
- `http://localhost:5173` と `http://localhost:8080` が別 Origin となり、React → Spring Boot の Browser 通信で CORS が必要になる理由を説明できる。
- CORS と Docker Network / Port Mapping が異なる Layer の仕組みであることを説明できる。
- Browser から React を開き、Spring Boot API を経由して PostgreSQL の Data を画面に表示できる。
- PostgreSQL Data の変更が Spring Boot → React を経由して Browser 表示へ反映されることを確認できる。
- Named Volume によって 3 Container を再作成しても PostgreSQL Data が保持されることを確認できる。
- Frontend が Running でも Backend が停止していれば API Request が失敗することを確認し、Service ごとの独立性を理解している。
- `docker compose ps` / Service Logs / API 単体確認 / Environment Variable / Database の順で 3 Service 構成の基本的な Troubleshooting ができる。
- Unit 05〜10 で学んだ React Container、Spring Boot Container、PostgreSQL、Network、Volume、Compose が Unit 11 でどのように統合されているか説明できる。

ここまで確認できれば、Unit 12「Dockerfile / Compose の Best Practice」へ進む。
