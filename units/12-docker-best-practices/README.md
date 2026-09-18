# 12. Dockerfile・Compose の改善と基本的な Best Practice

## この項目の目的

この Unit では、Unit 11 までに「動く」状態へ到達した React + Spring Boot + PostgreSQL の 3 Container 構成を題材に、Dockerfile / Compose をより適切にするための基本的な改善観点を学ぶ。  
新しい Application 機能を増やすのではなく、Build、Runtime Image、起動状態、権限、再起動、Image 配布など、Container を継続して扱うときに必要になる観点へ学習を進める。

Unit 11 までの中心は、

```text
Dockerfile を書く
↓
Image を Build
↓
Container を起動
↓
Application が動く
```

だった。

Unit 12 では、その次に考える。

```text
Application が動く
↓
Build 環境と Runtime 環境を分離できないか
↓
不要な File / Tool を Image に含めていないか
↓
再 Build を効率化できないか
↓
Container が Running だけで正常と判断してよいか
↓
必要以上の権限で Process を動かしていないか
↓
Container 終了時の動作は適切か
↓
Image を他の Environment へ渡すにはどうするか
```

今回の主な改善は次のとおり。

```text
React
Development Server Image
↓
Multi-stage Build
↓
Build 済み静的 File + Nginx Runtime

Spring Boot
Host Maven Build
↓
Multi-stage Build
↓
Container 内 Maven Build + JRE Runtime

Compose
単純な起動順序
↓
Healthcheck + service_healthy

Runtime
Default User
↓
Spring Boot を non-root User で実行

Container 終了
↓
Restart Policy

Local Image
↓
Tag
↓
Docker Hub Push
```

最終的には、

> Container が動けば終わりではなく、Dockerfile / Compose を Build・Runtime・Health・権限・Image 配布などの観点から見直せる

状態を目指す。

## 学習内容

### Best Practice は「決まった正解の Dockerfile」を暗記することではない

Dockerfile / Compose の Best Practice は、すべての Application に同じ設定を機械的に追加することではない。

例えば、

```text
Image を小さくする
Healthcheck を追加する
non-root にする
Restart Policy を設定する
```

はいずれも重要な観点だが、Application の Runtime、利用する Base Image、Deployment Environment、運用方法によって適切な実装は変わる。

この Unit では、

```text
何のための改善か
↓
どの問題を減らすのか
↓
今回の構成ではどう適用するか
↓
どこから先は Environment ごとに判断するか
```

を意識する。

### Unit 11 から何を変えたか

Unit 12 の Application の役割自体は Unit 11 とほぼ同じである。

```text
Browser
↓
React
↓
Spring Boot
↓
PostgreSQL
```

今回変更する主な箇所は Container Build / Runtime と Compose の運用面である。

```text
Frontend Dockerfile
→ Multi-stage Build

Backend Dockerfile
→ Multi-stage Build
→ non-root

.dockerignore
→ Build Context の見直し

Compose
→ Healthcheck
→ depends_on: service_healthy
→ Restart Policy

Image
→ Size / 内容確認
→ Tag
→ Docker Hub Push
```

Application の実装を大きく変えないことで、Docker 設定の差に集中する。

### Multi-stage Build

Multi-stage Build は、1 つの Dockerfile 内に複数の Build Stage を定義し、前段で作った成果物だけを後段の Stage へ Copy する方法である。

基本形は次のようになる。

```dockerfile
FROM build-image AS build

# Build に必要な処理
RUN ...

FROM runtime-image AS runtime

COPY --from=build ... ...
```

重要なのは、

```text
Build に必要
≠
Runtime に必要
```

という分離である。

Compiler、Package Manager、Source Code、Build Cache などが Build 時に必要でも、Application 実行時に必要とは限らない。

### Frontend の Multi-stage Build

Unit 11 の Frontend は Node.js Image 上で Vite Development Server をそのまま動かしていた。

概念的には次の構成だった。

```text
Runtime Image
├─ Node.js
├─ npm
├─ node_modules
├─ React Source
├─ Vite
└─ Vite Development Server
```

Unit 12 では、

```text
Build Stage
Node.js
↓
npm install
↓
vite build
↓
dist/

Runtime Stage
Nginx
↓
dist/ のみ
```

へ変更する。

Dockerfile の前半は、

```dockerfile
FROM node:24.20.0-alpine3.24 AS build
```

で Build Stage を作る。

後半では、

```dockerfile
FROM nginx:1.30.4-alpine3.24 AS runtime
```

から別 Stage を開始し、

```dockerfile
COPY --from=build /app/dist/ /usr/share/nginx/html/
```

によって Build 成果物だけを Runtime Stage へ渡す。

最終 Image に Node.js / npm / React Source / `node_modules` を残す必要がなくなる。

### Development Server と静的配信を分ける

Unit 05 / 11 では Docker 学習を分かりやすくするため Vite Development Server を利用した。

しかし `vite build` 後の React Application は静的 File として配布できる。

```text
Development
React Source
↓
Vite Development Server

Build 後
HTML / CSS / JavaScript
↓
Static File Server
```

Unit 12 では Nginx を Runtime に利用し、Build 済み静的 File を配信する。

これは React 自体を Nginx 上で「実行」しているという意味ではない。

```text
Nginx
→ Build 済み File を Browser へ配信

Browser
→ JavaScript を実行
```

という関係は Unit 11 までと同じである。

### Vite の Environment Variable は Build 時に反映される

Unit 11 では Vite Development Server の Container Environment として `VITE_API_BASE_URL` を渡していた。

Unit 12 では Production Build を行うため、

```yaml
build:
  args:
    VITE_API_BASE_URL: '${VITE_API_BASE_URL:-http://localhost:8080}'
```

として Build Argument を渡す。

Dockerfile では、

```dockerfile
ARG VITE_API_BASE_URL=http://localhost:8080
RUN npm run build
```

として利用する。

Vite の `VITE_` Variable は Build 時に Client-side Code へ反映される。

そのため、

```text
Runtime Container Environment を変更
↓
既に Build 済みの JavaScript の API URL が自動変更
```

とはならない。

API URL を変える場合は Frontend Image を再 Build する必要がある。

また `VITE_` Variable は Browser へ公開され得るため、Password / Token などの Secret を渡してはいけない。

### Backend の Multi-stage Build

Unit 10 / 11 の Backend は、

```text
Host Maven
↓
JAR
↓
Docker Build
↓
JRE + JAR
```

という構成だった。

Runtime Image はすでに JRE + JAR に絞られていたが、Image Build 前に Host で、

```bash
mvn clean package
```

を実行する必要があった。

Unit 12 では、

```text
Maven + JDK Build Stage
↓
mvn package
↓
JAR

JRE Runtime Stage
↓
JAR のみ
```

とする。

Docker Build の中で Java Build まで完結するため、

```text
Docker Image を Build するために Host Maven が必要
```

という依存を減らせる。

### Backend では Image Size が大幅に減るとは限らない

Frontend では改善前 Runtime に Node.js / npm / `node_modules` などが含まれていたため、Multi-stage Build による差が分かりやすい。

一方、Unit 11 の Backend はすでに、

```text
JRE
+
JAR
```

だけを Runtime Image にしていた。

したがって Unit 12 の Backend Multi-stage Build は、

```text
改善前
Host Maven Build + JRE Runtime

改善後
Docker 内 Maven Build + JRE Runtime
```

であり、最終 Runtime Image の内容は近い。

そのため、

> Multi-stage Build を使えば必ず Image Size が大幅に小さくなる

とは考えない。

今回の Backend では、Build 環境を Image Build 内へ閉じ込め、Build Tool を最終 Image へ含めず、Host Environment への依存を減らすことも大きな改善点である。

### Build Cache

Docker Build では Dockerfile の Instruction ごとに Cache を再利用できる。

大まかには、

```text
ある Layer の入力が変わる
↓
その Layer の Cache が無効
↓
後続 Layer も再実行
```

となる。

そのため、頻繁に変わる File を早い段階で `COPY` すると、その後にある重い Dependency Install / Resolve まで再実行されやすい。

### Frontend の Build Cache

Frontend Dockerfile では、

```dockerfile
COPY package.json ./
RUN npm install
COPY . .
```

の順にしている。

```text
package.json
↓
npm install
↓
Application Source
```

と分けることで、React Source だけを変更した場合は `package.json` が変わらないため、Dependency Install Layer を再利用しやすくする。

反対に、

```dockerfile
COPY . .
RUN npm install
```

とすると Source Code の変更でも `COPY . .` が変化し、その後の `npm install` まで Cache が無効になりやすい。

### Frontend の lockfile について

今回の生成物は学習範囲を広げすぎないため `package-lock.json` を含めず、`npm install` を使用している。

ただし実際の Application Repository では通常、lockfile を Version Control に含め、依存 Version を固定したうえで `npm ci` を利用する構成も重要な選択肢になる。

```text
package.json
→ Dependency の要求

package-lock.json
→ 実際に解決された Dependency Tree の固定

npm ci
→ lockfile に基づく再現性の高い Install
```

今回は Docker Build Cache の基本に集中し、lockfile 運用自体は深掘りしない。

### Backend の Build Cache

Backend Dockerfile では、

```dockerfile
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package
```

としている。

考え方は Frontend と同じである。

```text
pom.xml
↓
Dependency 解決
↓
Java Source
↓
Application Build
```

Source Code だけ変更した場合に Maven Dependency の Download / Resolve Layer を再利用しやすくする。

`dependency:go-offline` がすべての Maven Build で必須という意味ではない。  
今回は「Dependency 定義と Source Code の変更頻度を分離する」という Build Cache の考え方を確認するために利用している。

### `.dockerignore`

Docker Build では Build Context 内の File が Builder から参照可能になる。

Build に不要な File まで Context に含めると、

```text
送信する Data が増える
不要な File が Build 対象へ入る
Cache の判定に不要な変更が影響しやすくなる
誤って不要な File を COPY する可能性が増える
```

といった問題につながる。

`.dockerignore` は不要な File / Directory を Build Context から除外する。

### Frontend の `.dockerignore`

Frontend では主に、

```text
node_modules/
dist/
coverage/
.env
.git/
Log / Temporary File
```

などを除外する。

特に Host の `node_modules` を Image へ持ち込まず、Container 内で必要な Dependency を Install することが重要である。

Build 済みの `dist/` も Docker Build 内で新しく生成するため、Host の古い成果物を Context へ含める必要はない。

### Backend の `.dockerignore`

Backend では、

```text
target/
.env
.git/
Editor / OS File
Log / Temporary File
Secret の可能性がある File
```

などを除外する。

Unit 11 までは Host Maven Build の `target/*.jar` を Dockerfile が必要としていた。

Unit 12 では Maven Build 自体を Build Stage 内で行うため、Host の `target/` は不要になる。

```text
Unit 11
Host target/
→ Docker Build に必要

Unit 12
Docker Build Stage で target/ を生成
→ Host target/ は不要
```

Multi-stage Build への変更が `.dockerignore` の内容にも影響している。

### Base Image の選択

Dockerfile の `FROM` は Application Runtime の土台になる。

Base Image を選ぶときは単に、

```text
一番小さい Image
```

を選べばよいわけではない。

少なくとも、

```text
必要な Runtime があるか
OS / Library Compatibility
Image の更新状況
Official / Trusted な提供元か
Debug / Operation に必要な Tool
Image Size
Security Update
```

などを考える。

### Alpine / slim などの違い

Base Image には Alpine Linux ベースや Debian slim 系など複数の Variant が存在することがある。

Alpine は小さな Image を作りやすい一方、使用 Library や Application によっては Compatibility を確認する必要がある。

slim 系も不要 Package を減らした選択肢である。

重要なのは、

```text
Alpine = 常に最適
```

ではなく、Application が問題なく動き、運用上必要な条件を満たす中で適切な Base Image を選ぶことである。

### `latest` のみに依存しない

この Repository では、

```text
node:24.20.0-alpine3.24
nginx:1.30.4-alpine3.24
maven:3.9.16-eclipse-temurin-21-alpine
eclipse-temurin:21.0.12_8-jre-alpine-3.24
postgres:18.6-alpine3.24
```

のように Version を含む Tag を利用している。  
`latest` だけを指定すると、別の時点で Build したときに異なる Version の Base Image が選ばれる可能性がある。

Version Tag を利用することで、

```text
どの Version 系を利用しているか
```

を Dockerfile から読み取りやすくする。

ただし Tag 自体も Registry 側で更新され得る。

さらに厳密な固定方法として Image Digest を利用する方法もあるが、この Unit では存在を知るところまでとし、Digest Pinning の運用は対象外とする。

### Image Size

Image Size は、

```text
Base Image
Build Tool
Dependency
Source Code
Build Artifact
不要 File
Layer
```

などの影響を受ける。

今回の主な改善要素は、

```text
Multi-stage Build
+
.dockerignore
+
Runtime 用 Base Image
```

である。

ただし Image Size は小さければ小さいほど無条件に優れているわけではない。

Debug / Compatibility / Security Update / Operation のしやすさとの Balance も必要になる。

### Healthcheck

Container が `Running` であることは、Application が正常に Service を提供できることと同じではない。

```text
Container Running
↓
Process は存在する

Application Healthy
↓
期待する Check が成功する
```

例えば PostgreSQL Container が Start していても、まだ Connection を受け付けられないことがある。

Healthcheck は Container 内で定期的に Command を実行し、その結果から、

```text
starting
healthy
unhealthy
```

という Health Status を持たせる仕組みである。

### PostgreSQL Healthcheck

PostgreSQL Service では、

```yaml
healthcheck:
  test: ['CMD-SHELL', 'pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}']
```

を利用する。

`pg_isready` は PostgreSQL Server が Connection を受け付けられる状態か確認するための Command である。

Compose の `${...}` 補間ではなく Container 内の Environment Variable を Shell へ渡したいため、

```text
$${POSTGRES_USER}
```

のように `$` を Escape している。

### Backend Healthcheck

Backend では、

```text
http://127.0.0.1:8080/api/message
```

へ Request する。

今回の API は PostgreSQL Access まで含むため、この Healthcheck が成功するには、

```text
Spring Boot
+
Database Connection
+
SELECT
+
HTTP Response
```

が成立する必要がある。

これは学習用に分かりやすい Check である。

実際の System では Liveness / Readiness の Check Endpoint を分ける設計などもあるが、この Unit では深掘りしない。

### Frontend Healthcheck

Frontend は Nginx の Root Page へ Request する。

```text
http://127.0.0.1/
```

に正常にアクセスできれば Frontend Service を `healthy` と判断する。

React の API Request まで成功することを確認する Healthcheck ではない。

つまり、

```text
Frontend Healthy
≠
System 全体が完全に Healthy
```

である。

Healthcheck は「何を Check するよう定義したか」によって意味が決まる。

### `interval` / `timeout` / `retries` / `start_period`

今回の Healthcheck では次の設定を利用する。

```text
interval
→ Check の実行間隔

timeout
→ 1 回の Check を待つ時間

retries
→ Failure を何回まで許容するか

start_period
→ 起動直後の猶予期間
```

Application の起動時間や負荷に応じて適切な値は変わる。

今回の値は Local 学習用 Sample であり、Production の推奨値として固定するものではない。

### `depends_on` と `service_healthy`

Unit 09〜11 では Short syntax の `depends_on` を使ってきた。

```yaml
depends_on:
  - postgres
```

これは依存 Service が Start される順序を表すが、Healthy になるまで待つものではなかった。

Unit 12 では Long syntax を使う。

```yaml
depends_on:
  postgres:
    condition: service_healthy
```

これにより Compose は、依存 Service の Healthcheck が `healthy` になるまで待ってから Dependent Service を Start する。

今回の流れは、

```text
postgres
↓ healthy
backend
↓ healthy
frontend
```

となる。

### Healthcheck は万能な Monitoring ではない

Healthcheck を追加しても、

```text
すべての Application Error を検知できる
Performance 問題を検知できる
外部 Service のすべての異常を判断できる
Monitoring / Alerting が不要になる
```

わけではない。

Healthcheck は Container Runtime / Compose が Service の基本状態を判断するための 1 つの Signal と考える。

### Restart Policy

Compose の Service-level `restart` は Container が終了したときに Docker がどのように再起動するかを指定する。

今回の構成では、

```yaml
restart: unless-stopped
```

を利用する。

概念的には、

```text
Process が予期せず終了
↓
Container 終了
↓
Restart Policy
↓
Container 再起動
```

となる。

### `unless-stopped`

`unless-stopped` は、Container が終了した場合や Docker Daemon が再起動した場合に再起動対象となる一方、User が明示的に Stop した Container を勝手に再開し続けないための Policy である。

Restart Policy には他にも、

```text
no
always
on-failure
unless-stopped
```

などがある。

どれを選ぶかは Application と運用 Environment によって決まる。

### Restart Policy は Local Development で必須ではない

Local 学習環境では、Container が異常終了したときにそのまま止まっている方が Error に気付きやすい場合もある。  
一方 Server 上で継続稼働させる Service では、Process の一時的な異常終了から自動復旧したい場合がある。

そのため今回 `unless-stopped` を設定しているのは Restart Policy の仕組みを確認するためであり、

```text
すべての Compose File に必ず unless-stopped を書く
```

というルールではない。

### `depends_on` の `restart` と混同しない

Compose の `depends_on` Long syntax には Dependency を Compose 操作で更新した場合などに関連 Service を再起動するための別の `restart` 設定も存在する。

今回使用している、

```yaml
restart: unless-stopped
```

は Service 自体の Container Restart Policy である。

同じ `restart` という語でも役割が異なるため、配置されている場所と目的を確認する。

### non-root Container

Linux Container 内の Process が root User で動く場合、その Process は Container 内で強い権限を持つ。  
Application が root 権限を必要としないなら、専用 User で実行することで不要な権限を減らせる。  
Dockerfile では `USER` Instruction を利用する。

### Backend の non-root 化

Backend Dockerfile では Runtime Stage で、

```dockerfile
RUN addgroup -S appgroup \
    && adduser -S appuser -G appgroup
```

として Application 用 User / Group を作る。

JAR を Copy するときには、

```dockerfile
COPY --from=build \
    --chown=appuser:appgroup \
    ... \
    app.jar
```

とし、

```dockerfile
USER appuser
```

以降の Application Process を `appuser` で実行する。

```text
root
↓
必要な File / User の準備

USER appuser
↓
java -jar app.jar
```

という分離になる。

### `USER` を機械的に追加すればよいわけではない

non-root 化する場合、Application が必要とする File / Directory の Permission や利用 Port を確認する必要がある。

今回 Spring Boot は Port `8080` を利用し、Runtime で特別な System Directory への書き込みも必要としないため、専用 User で実行しやすい。  
一方 Frontend は Nginx Official Image を Port `80` でそのまま利用している。

Nginx を完全な non-root 構成へ変更する場合は、Listen Port や Runtime Directory の Permission、専用 Variant など別の考慮が必要になる。

この Unit では Backend で `USER` の基本を実践し、すべての Official Image を独自に non-root 化するところまでは扱わない。

### Docker Hub / Registry

Local で Build した Docker Image は、その PC の Docker Environment 内に存在する。  
別の PC / Server などから同じ Image を利用するには、Image を受け渡す仕組みが必要になる。

Registry は Docker Image を保存・配布する場所である。

```text
Local Docker
↓ push
Registry
↓ pull
別 Environment
```

Docker Hub は Docker Registry Service の 1 つである。

### Image Name と Registry Namespace

Local Image は今回、

```text
unit12-backend:1.0.0
```

として Build する。

Docker Hub へ Push する場合は Docker Hub の Namespace を含む名前へ Tag を付ける。

```text
<docker-hub-username>/unit12-backend:1.0.0
```

分解すると、

```text
<docker-hub-username>
→ Docker Hub Namespace

unit12-backend
→ Repository

1.0.0
→ Image Tag
```

となる。

### `docker image tag`

Tag は Image Data 自体をもう 1 回 Build する操作ではない。

既存 Image に Registry / Repository / Version を表す別名を付ける。

```text
unit12-backend:1.0.0
↓ docker image tag
<docker-hub-username>/unit12-backend:1.0.0
```

同じ Image ID に複数の Repository / Tag が関連付くことがある。

### `docker login`

Docker Hub へ Push するには認証が必要である。

Docker Desktop を利用している現在の環境では、

```bash
docker login
```

から Docker Hub の Web-based Login / Device Code Flow を利用できる。

Credential を Shell Command へ直接書き込んだり、Repository 内へ保存したりしない。  
CLI で Username を明示して認証する場合は Personal Access Token などを利用できるが、Token 自体を Source Code / `.env` / Shell Script へ Commit しない。

### `docker image push`

Docker Hub 用 Tag を作成した後、

```bash
docker image push \
  <docker-hub-username>/unit12-backend:1.0.0
```

で Registry へ Layer を Upload する。

Push 後は Docker Hub Repository の Tag 一覧から `1.0.0` が存在することを確認できる。

### 今回扱わない内容

Unit 12 では Dockerfile / Compose の基礎的な改善に集中するため、次は扱わない。

```text
CI/CD からの自動 Build / Push
GitHub Container Registry
Private Registry の構築
Image Signing
SBOM / Vulnerability Management の本格運用
Production Hardening 全般
Kubernetes / Orchestration
Advanced Buildx / Multi-platform Build
```

## 使用するもの

### Frontend

```text
React 19.2.8
Vite 8.2.2
Node.js 24.20.0
Nginx 1.30.4
```

### Frontend Build Image

```text
node:24.20.0-alpine3.24
```

### Frontend Runtime Image

```text
nginx:1.30.4-alpine3.24
```

### Backend

```text
Spring Boot 4.1.1
Java 21
Maven 3.9.16
```

### Backend Build Image

```text
maven:3.9.16-eclipse-temurin-21-alpine
```

### Backend Runtime Image

```text
eclipse-temurin:21.0.12_8-jre-alpine-3.24
```

### PostgreSQL

```text
postgres:18.6-alpine3.24
```

### Compose Project

```text
unit12-docker-best-practices
```

### Local Images

```text
unit12-frontend:1.0.0
unit12-backend:1.0.0
```

### Services

```text
frontend
backend
postgres
```

### Docker Network

```text
unit12-network
```

### Named Volume

```text
unit12-postgres-data
```

### Browser URLs

```text
Frontend
http://localhost:5173

Backend
http://localhost:8080
```

## 事前準備

Unit 11 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/12-docker-best-practices
```

Git Bash で Repository 内の次の Unit Directory へ移動した状態から操作する。

```text
units/12-docker-best-practices
```

現在位置を確認する。

```bash
pwd
```

生成物を確認する。

```bash
find . -maxdepth 8 -type f | sort
```

主に次が存在する。

```text
.env.example
compose.yaml

frontend/
├─ Dockerfile
├─ Dockerfile.before
├─ .dockerignore
├─ package.json
└─ ...

backend/
├─ Dockerfile
├─ Dockerfile.before
├─ .dockerignore
├─ pom.xml
└─ ...

db/init/01-init.sql
```

Docker / Compose が利用できることを確認する。

```bash
docker version
docker compose version
```

Host Port `5173` / `8080` を別 Container が使用していないことも確認する。

```bash
docker container ls
```

Docker Hub Push の Step までは Docker Hub Account は不要である。  
最後に実際の Push を行う場合は、自分の Docker Hub Account と Push 先 Repository を用意する。

## ハンズオン

### 1. 改善後の Compose 構成を確認する

`compose.yaml` を確認する。

```bash
cat compose.yaml
```

Unit 11 と比べて主に次が追加されていることを確認する。

```text
frontend
├─ image
├─ build.args
├─ healthcheck
├─ depends_on.condition
└─ restart

backend
├─ image
├─ healthcheck
├─ depends_on.condition
└─ restart

postgres
├─ healthcheck
└─ restart
```

今回の改善が Application Function ではなく Container Build / Runtime / Lifecycle に集中していることを確認する。

### 2. `.env.example` から `.env` を作成する

```bash
cp .env.example .env
```

内容を確認する。

```bash
cat .env
```

特に、

```text
VITE_API_BASE_URL=http://localhost:8080
```

が Frontend の Production Build に利用されることを意識する。

この値は Browser へ公開され得るため Secret ではない。

### 3. Compose の最終解釈結果を確認する

```bash
docker compose config
```

次を確認する。

```text
frontend
→ build.args
→ VITE_API_BASE_URL

backend
→ DB_URL
→ CORS_ALLOWED_ORIGIN

depends_on
→ condition: service_healthy

healthcheck
restart
```

YAML をそのまま見るだけでなく、Compose が最終的にどの設定として解釈しているか確認する。

### 4. Frontend の改善前 Dockerfile を確認する

```bash
cat frontend/Dockerfile.before
```

構成は Unit 11 とほぼ同じである。

```text
Node.js
↓
npm install
↓
Source Copy
↓
Vite Development Server
```

Runtime Image に、

```text
Node.js
npm
node_modules
Source Code
Vite
```

が残る構成であることを確認する。

### 5. Frontend の改善後 Dockerfile を確認する

```bash
cat frontend/Dockerfile
```

2 つの `FROM` を確認する。

```text
Build Stage
node:24.20.0-alpine3.24

Runtime Stage
nginx:1.30.4-alpine3.24
```

また、

```dockerfile
COPY --from=build /app/dist/ /usr/share/nginx/html/
```

によって Build 成果物だけを Runtime Stage へ渡していることを確認する。

### 6. Frontend の改善前 Image を Build する

比較用 Dockerfile から Image を Build する。

```bash
docker image build \
  -f frontend/Dockerfile.before \
  -t unit12-frontend-before:1.0.0 \
  frontend
```

Build が完了したら確認する。

```bash
docker image ls | grep unit12-frontend
```

この Image は比較用であり、Compose の実行には使用しない。

### 7. Frontend の改善後 Image を Build する

Compose から Frontend だけ Build する。

```bash
docker compose build frontend
```

確認する。

```bash
docker image ls | grep unit12-frontend
```

次の 2 Image が存在することを確認する。

```text
unit12-frontend-before:1.0.0
unit12-frontend:1.0.0
```

### 8. Frontend Image Size を比較する

まず一覧で確認する。

```bash
docker image ls | grep unit12-frontend
```

より明示的に Size を確認する場合は次を実行する。

```bash
docker image inspect \
  --format '{{.RepoTags}} {{.Size}}' \
  unit12-frontend-before:1.0.0 \
  unit12-frontend:1.0.0
```

`.Size` は Byte 単位で表示される。

通常、改善後 Image は Node.js Development Environment 一式ではなく Nginx + Build 済み静的 File が中心になるため、改善前より Runtime Image を小さくしやすい。

ただし実際の Size は Base Image Version や Dependency によって変わるため、固定値を暗記しない。

### 9. Frontend Runtime に Node.js が含まれないことを確認する

改善後 Image を一時 Container として実行する。

```bash
docker container run --rm \
  unit12-frontend:1.0.0 \
  sh -c 'command -v node || echo "node is not included in runtime image"'
```

次のような結果になることを確認する。

```text
node is not included in runtime image
```

Build に Node.js を使っていても Runtime には必要ないという Multi-stage Build の効果を実際に確認する。

### 10. Frontend Runtime の Build 成果物を確認する

```bash
MSYS_NO_PATHCONV=1 docker container run --rm \
  unit12-frontend:1.0.0 \
  ls -la /usr/share/nginx/html
```

`index.html` や `assets/` など Vite Build 後の File が存在することを確認する。

React Source がそのまま実行されているのではなく、Build 済み静的 File が配信対象になっている。

### 11. Frontend の Build Cache を確認する

変更せずに再度 Build する。

```bash
docker compose build frontend
```

Build Output で既存 Layer が Cache から再利用されていることを確認する。

特に、

```text
COPY package.json
RUN npm install
```

周辺が再実行されていないことを見る。

### 12. Frontend Source だけ変更して Cache の違いを確認する

`frontend/src/App.jsx` の表示 Text など、Application 動作へ影響しない箇所を一時的に変更する。

例えば見出しの、

```text
Unit 12 - Docker Best Practices
```

を一時的に、

```text
Unit 12 - Docker Best Practices Cache Check
```

へ変更する。

再 Build する。

```bash
docker compose build frontend
```

Build Output を確認する。

期待する考え方は次のとおり。

```text
package.json
→ 変更なし
→ npm install Layer を再利用しやすい

src/App.jsx
→ 変更
→ Source COPY 以降は再実行
```

確認後は Text を元へ戻し、必要に応じてもう一度 Build する。

```bash
docker compose build frontend
```

### 13. Frontend `.dockerignore` を確認する

```bash
cat frontend/.dockerignore
```

特に、

```text
node_modules/
dist/
coverage/
.env
.git/
```

などが Build Context から除外されることを確認する。

Host に `node_modules` が存在しても、それを Docker Build へそのまま持ち込む構成ではない。

### 14. Backend の改善前 Dockerfile を確認する

```bash
cat backend/Dockerfile.before
```

Unit 11 相当では、

```text
Host Maven Build
↓
target/*.jar
↓
JRE Image へ COPY
```

という構成だったことを確認する。

今回の `backend/.dockerignore` では `target/` を除外しているため、`Dockerfile.before` は **比較用の読み取り対象**として扱う。  
Unit 12 の通常 Build ではこの Dockerfile を実行しない。

### 15. Backend の改善後 Dockerfile を確認する

```bash
cat backend/Dockerfile
```

Build Stage を確認する。

```dockerfile
FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
```

Runtime Stage を確認する。

```dockerfile
FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24 AS runtime
```

そして、

```dockerfile
COPY --from=build ...
```

で JAR だけを Runtime Stage へ渡していることを確認する。

### 16. Backend Build Cache の記述順を確認する

Backend Dockerfile の次の順番を見る。

```text
COPY pom.xml
↓
mvn dependency:go-offline
↓
COPY src
↓
mvn package
```

Dependency 定義と Source Code を分離する目的を整理する。

```text
pom.xml 変更なし
+
Source のみ変更
↓
Dependency 解決 Layer を再利用しやすい
```

という関係を確認する。

### 17. Backend `.dockerignore` を確認する

```bash
cat backend/.dockerignore
```

特に、

```text
target/
.env
.git/
Log
Temporary File
Secret の可能性がある File
```

が除外されることを確認する。

Unit 11 では Host で作った `target/*.jar` が必要だったが、Unit 12 では Docker Build Stage が JAR を生成するため Host `target/` は不要である。

### 18. Backend Image を Docker だけで Build する

Unit Root から次を実行する。

```bash
docker compose build backend
```

ここでは事前に、

```bash
mvn clean package
```

を実行しない。

Docker Build の中で、

```text
Maven Dependency 解決
↓
Java Compile / Package
↓
JAR
↓
Runtime Stage
```

まで進むことを確認する。

Host Maven Build への依存がなくなったことが重要である。

### 19. Backend Runtime に Maven / JDK が含まれないことを確認する

Maven を確認する。

```bash
docker container run --rm \
  unit12-backend:1.0.0 \
  sh -c 'command -v mvn || echo "maven is not included in runtime image"'
```

Java Compiler を確認する。

```bash
docker container run --rm \
  unit12-backend:1.0.0 \
  sh -c 'command -v javac || echo "javac is not included in runtime image"'
```

Java Runtime は存在する。

```bash
docker container run --rm \
  unit12-backend:1.0.0 \
  java -version
```

次の関係を確認する。

```text
Build Stage
→ Maven + JDK

Runtime Stage
→ JRE + JAR
```

### 20. Backend が non-root User で動く設定を確認する

Dockerfile の、

```dockerfile
USER appuser
```

を確認する。

一時 Container で User を確認する。

```bash
docker container run --rm \
  unit12-backend:1.0.0 \
  id
```

Output に、

```text
appuser
appgroup
```

が含まれ、`uid=0(root)` ではないことを確認する。

### 21. Base Image / Tag を確認する

Frontend / Backend Dockerfile と `compose.yaml` に記載されている Base Image を確認する。

```bash
grep -R '^FROM\|image:' \
  frontend/Dockerfile \
  backend/Dockerfile \
  compose.yaml
```

Version を含んだ Tag が利用されていることを確認する。

```text
node:24.20.0-alpine3.24
nginx:1.30.4-alpine3.24
maven:3.9.16-eclipse-temurin-21-alpine
eclipse-temurin:21.0.12_8-jre-alpine-3.24
postgres:18.6-alpine3.24
```

`latest` のみを利用する構成との違いを整理する。

### 22. 3 Service を Build する

Frontend / Backend の改善内容を確認したら、Compose Project 全体を Build する。

```bash
docker compose build
```

Build 後に確認する。

```bash
docker image ls | grep unit12
```

少なくとも、

```text
unit12-frontend:1.0.0
unit12-backend:1.0.0
```

を確認する。

### 23. 3 Service を起動する

```bash
docker compose up -d
```

状態を確認する。

```bash
docker compose ps
```

今回 `depends_on.condition: service_healthy` を設定しているため、起動時には依存 Service の Healthcheck が重要になる。

### 24. Health Status を確認する

まず Compose 一覧を見る。

```bash
docker compose ps
```

`STATUS` に `healthy` が含まれることを確認する。

PostgreSQL の Health Status を直接確認する。

```bash
docker container inspect \
  --format '{{.State.Health.Status}}' \
  "$(docker compose ps -q postgres)"
```

Backend も確認する。

```bash
docker container inspect \
  --format '{{.State.Health.Status}}' \
  "$(docker compose ps -q backend)"
```

Frontend も確認する。

```bash
docker container inspect \
  --format '{{.State.Health.Status}}' \
  "$(docker compose ps -q frontend)"
```

最終的に、

```text
postgres  healthy
backend   healthy
frontend  healthy
```

となることを確認する。

### 25. PostgreSQL Healthcheck を確認する

`compose.yaml` の `postgres.healthcheck` を確認する。

Container 内で同じ Check を実行する。

```bash
docker compose exec postgres \
  sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

正常時に PostgreSQL が Connection を受け付けられる状態であることを確認する。

### 26. `service_healthy` による依存関係を確認する

Compose 設定をもう一度確認する。

```text
postgres
↓ healthcheck
healthy
↓
backend
↓ healthcheck
healthy
↓
frontend
```

Unit 10 / 11 の Short syntax と比較する。

```text
以前
depends_on
→ Start Order

今回
depends_on + service_healthy
→ Dependency Healthcheck の成功を待つ
```

Healthcheck と `depends_on` が別々の機能であり、組み合わせて利用していることを確認する。

### 27. Application 全体の動作を確認する

Backend API を確認する。

```bash
curl http://localhost:8080/api/message
```

次の内容が返ることを確認する。

```json
{ "id": 1, "message": "Hello from the improved Unit 12 containers" }
```

Browser で Frontend を開く。

```text
http://localhost:5173
```

同じ PostgreSQL Data が画面に表示されることを確認する。

### 28. Frontend が Development Server ではなく Nginx で動いていることを確認する

Frontend Container の Process を確認する。

```bash
docker compose exec frontend \
  ps
```

Nginx Process が動いていることを確認する。

Node.js / Vite Development Server が Runtime Process ではない。

### 29. Backend の実行 User を Running Container でも確認する

```bash
docker compose exec backend \
  id
```

`appuser` で実行されていることを確認する。

さらに Process を確認する。

```bash
docker compose exec backend \
  ps
```

Java Process が non-root User の Container Environment で動いていることを確認する。

### 30. Restart Policy を確認する

Backend Container の Restart Policy を確認する。

```bash
docker container inspect \
  --format '{{.HostConfig.RestartPolicy.Name}}' \
  "$(docker compose ps -q backend)"
```

次が表示されることを確認する。

```text
unless-stopped
```

他 Service も同様に設定されている。

### 31. Process 終了時の自動 Restart を確認する

まず Restart Count を確認する。

```bash
docker container inspect \
  --format '{{.RestartCount}}' \
  "$(docker compose ps -q backend)"
```

Backend が `healthy` になって十分に起動した後、Container 内の Main Process を終了させる。

```bash
docker compose exec backend \
  sh -c 'kill -TERM 1'
```

数秒後に状態を確認する。

```bash
docker compose ps
```

Restart Count も再確認する。

```bash
docker container inspect \
  --format '{{.RestartCount}}' \
  "$(docker compose ps -q backend)"
```

Restart Policy によって Backend Container が再起動し、Restart Count が増えていることを確認する。

再起動後は Healthcheck が再び `healthy` になるまで待つ。

### 32. Manual Stop との違いを確認する

Backend を明示的に Stop する。

```bash
docker compose stop backend
```

確認する。

```bash
docker compose ps
```

User が明示的に Stop した場合は、`unless-stopped` で勝手に再起動し続けないことを確認する。

再開する。

```bash
docker compose start backend
```

Health Status が戻るまで確認する。

```bash
docker compose ps
```

Restart Policy は「User が Stop しても無条件に起動し続ける設定」ではない。

### 33. Image Size と Runtime 内容を関連付ける

改めて Local Image を確認する。

```bash
docker image ls | grep unit12
```

Size だけを見るのではなく、

```text
Frontend
Build Tool を Runtime から除外
↓
Nginx + dist/

Backend
Build Tool を Runtime から除外
↓
JRE + JAR
```

という Runtime 内容の違いと関連付けて見る。

必要に応じて History も確認する。

```bash
docker image history unit12-frontend:1.0.0
docker image history unit12-backend:1.0.0
```

### 34. Docker Hub Push 用の Image を確認する

この Unit では `unit12-backend:1.0.0` を 1 回 Docker Hub へ Push する。

Local Image が存在することを確認する。

```bash
docker image ls unit12-backend
```

ここから先は自分の Docker Hub Account を使用する。

### 35. Docker Hub Repository を用意する

Docker Hub に Sign in し、自分の Namespace 内に学習用 Repository を作成する。

Repository 名の例:

```text
unit12-backend
```

この README では Docker Hub User Name を、

```text
<docker-hub-username>
```

という Placeholder で表す。

以降の Command では必ず自分の User Name に置き換える。

### 36. Docker Hub へ Login する

Git Bash で実行する。

```bash
docker login
```

Docker Hub の Web-based Login / Device Code Flow に従って認証する。  
認証情報や Token を README / Shell History / Repository の File へ直接記録しない。  
Docker Desktop を利用している場合、認証情報は Docker が利用する Credential Store と連携して管理される。

### 37. Docker Hub 用 Tag を付ける

Local Image:

```text
unit12-backend:1.0.0
```

に Docker Hub 用の名前を付ける。

```bash
docker image tag \
  unit12-backend:1.0.0 \
  <docker-hub-username>/unit12-backend:1.0.0
```

確認する。

```bash
docker image ls | grep unit12-backend
```

同じ Image ID に、

```text
unit12-backend:1.0.0
<docker-hub-username>/unit12-backend:1.0.0
```

という複数 Tag が付いていることを確認する。

### 38. Docker Hub へ Push する

```bash
docker image push \
  <docker-hub-username>/unit12-backend:1.0.0
```

Layer が Registry へ Upload されることを確認する。

同じ Layer が Registry 側に存在する場合、再利用されることもある。

### 39. Docker Hub 上で Tag を確認する

Docker Hub の作成した Repository を開く。

次の Tag が存在することを確認する。

```text
1.0.0
```

これで、

```text
Local Image
↓
Tag
↓
Push
↓
Docker Hub Registry
```

という基本的な Image 配布の流れを 1 回経験できた。

### 40. Registry へ置く意味を整理する

Push の目的を次のように整理する。

```text
Local PC だけに Image がある
↓
別 Environment から直接利用できない

Registry に Image を置く
↓
認証 / 権限の範囲で
別 Environment が pull できる
```

Source Code を毎回受け渡して Build する方法とは別に、Build 済み Image を配布できる。

### 41. Local Resource を Cleanup する

Compose Resource を削除する。

```bash
docker compose down -v
```

確認する。

```bash
docker compose ps -a
docker network ls
docker volume ls
```

比較用 Frontend Image が不要なら削除する。

```bash
docker image rm \
  unit12-frontend-before:1.0.0
```

Docker Hub 用に追加した Local Tag も不要なら削除できる。

```bash
docker image rm \
  <docker-hub-username>/unit12-backend:1.0.0
```

これは Local Tag / Image Reference の削除であり、Docker Hub に Push 済みの Remote Repository / Tag を削除する操作ではない。

Unit 12 の改善後 Image を学習後も残す場合は、

```text
unit12-frontend:1.0.0
unit12-backend:1.0.0
```

を無理に削除する必要はない。

## 動作・確認ポイント

### Multi-stage Build

以下を確認する。

- Frontend で Node.js Build Stage と Nginx Runtime Stage が分離されている。
- Backend で Maven / JDK Build Stage と JRE Runtime Stage が分離されている。
- `COPY --from=build` によって必要な Build Artifact だけを Runtime Stage へ渡している。
- Frontend Runtime に Node.js がなく、Backend Runtime に Maven / `javac` がないことを確認できる。
- Multi-stage Build の効果が「必ず大幅な Image Size 削減」だけではないことを理解している。

### Build Cache / `.dockerignore`

以下を確認する。

- Dependency 定義 File を Source より先に `COPY` する理由を説明できる。
- Source だけ変更した Rebuild で Dependency Layer を再利用しやすいことを確認できる。
- `.dockerignore` が不要 File を Build Context から除外する役割を持つ。
- Unit 12 の Backend では Host `target/` が Build Context に不要になった理由を説明できる。

### Base Image / Image Tag / Size

以下を確認する。

- Build 用 Image と Runtime 用 Image を目的に応じて使い分けている。
- `latest` のみに依存せず Version を含む Tag を利用する理由を説明できる。
- Alpine / slim などの Variant は Size だけでなく Compatibility なども考えて選ぶ必要がある。
- Frontend の改善前後で Runtime Image の内容と Size の違いを確認できる。

### Healthcheck

以下を確認する。

- `Running` と `healthy` が異なる概念である。
- PostgreSQL / Backend / Frontend で異なる Check を実行している理由を説明できる。
- `interval` / `timeout` / `retries` / `start_period` の基本的な意味を説明できる。
- `depends_on.condition: service_healthy` により依存 Service の Healthcheck を起動条件に利用できる。

### Restart Policy

以下を確認する。

- `restart: unless-stopped` が Container 終了時の Restart Policy であることを説明できる。
- Process の予期しない終了後に Container が自動再起動することを確認できる。
- User が明示的に Stop した場合との違いを確認できる。
- Local Development と継続稼働 Environment では Restart Policy の必要性が異なることを理解している。

### non-root

以下を確認する。

- Backend Runtime で `USER appuser` を利用している。
- Running Container の User が root ではないことを `id` で確認できる。
- `USER` を追加する場合は File Permission / Port / Runtime Requirements も確認する必要があることを理解している。

### Docker Hub

以下を確認する。

- Registry が Build 済み Image を保存・配布する役割を持つ。
- Local Image に Docker Hub Namespace / Repository / Tag を付けられる。
- `docker login` / `docker image tag` / `docker image push` の基本的な流れを 1 回実行できる。
- Credential / Token を Source Code や Repository へ保存しない。

## 学習ポイント

### 「動く Image」と「扱いやすい Image」は同じではない

Unit 01〜11 では、まず Docker の仕組みを理解し、Application を Container 上で動かすところまで進めた。

しかし実際には、

```text
Build が毎回遅い
Runtime に不要 Tool が残る
Image が大きい
起動しただけで正常と判断している
root で動いている
異常終了後に復旧しない
別 Environment へ Image を渡せない
```

といった課題が残ることがある。

Unit 12 では、Container が動いた後に見るべき代表的な改善観点を整理した。

### Build と Runtime を分離する

Multi-stage Build の中心は、

```text
Build に必要なもの
と
実行に必要なもの
を分ける
```

ことである。

Frontend では、

```text
Node.js / npm / Vite
→ Build に必要

Nginx / dist
→ Runtime に必要
```

Backend では、

```text
Maven / JDK / Source
→ Build に必要

JRE / JAR
→ Runtime に必要
```

という違いがある。

Build Tool を最終 Runtime Image に残さないことで、Image の役割を明確にできる。

### Multi-stage Build の価値を Image Size だけで判断しない

Frontend のように改善前後で Runtime が大きく変わる場合、Image Size の差が目立ちやすい。  
一方 Backend では Unit 11 でも Runtime は JRE + JAR だったため、最終 Image Size の差が大きくない可能性がある。

それでも、

```text
Host Maven Build が必要
↓
Docker Build だけで完結
```

という改善がある。

Best Practice は 1 つの数値だけで評価するのではなく、

```text
再現性
Build の自己完結性
Runtime の責務
不要 Tool
Image Size
```

など複数の観点で見る。

### Build Cache は変更頻度を考えて Dockerfile を並べる

Build Cache を活用する基本は、

```text
変更頻度が低いもの
↓
先に処理

変更頻度が高いもの
↓
後から処理
```

である。

Frontend では、

```text
package.json
↓
npm install
↓
Source
```

Backend では、

```text
pom.xml
↓
Dependency Resolve
↓
Source
```

としている。

Dockerfile の Instruction 順序は単なる見た目ではなく Rebuild Cost に影響する。

### `.dockerignore` は Build Context を設計する File である

`.dockerignore` は単に Image Size を小さくする File ではない。

除外された File は Build Context から外れるため、

```text
Builder に送る必要がない
COPY 対象にならない
不要な変更が Build に影響しにくい
Secret を誤って取り込むリスクを減らす
```

といった意味を持つ。

ただし、本当に Dockerfile が必要とする File まで除外すると Build できなくなる。  
Dockerfile の `COPY` と `.dockerignore` はセットで確認する。

### Base Image は「小さい」以外の条件も見る

Image Size は重要な要素の 1 つだが、

```text
Application Compatibility
必要 Library
Security Update
Support
Debuggability
Runtime Requirements
```

も同時に考える。

Alpine や slim は有力な選択肢だが、

```text
Alpine を使えば必ず Best Practice
```

ではない。

Application と Environment に合う Base Image を選ぶことが目的である。

### Version Tag は Build の意図を残す

```text
latest
```

だけでは利用 Version が Dockerfile から読み取りにくく、時間経過によって異なる Image を取得する可能性もある。

Version を含む Tag を指定することで、

```text
この Application がどの Runtime Version を前提にしているか
```

を構成に残せる。

より厳密な再現性が必要な場合は Digest なども検討対象になるが、まず Version Tag を意識することが基礎になる。

### Healthcheck は Process と Service の状態を分ける

Container が Running であっても、

```text
Database がまだ初期化中
API が Error
Web Server が応答しない
```

といった状態はあり得る。

Healthcheck は、

```text
Container Process が存在する
```

から一歩進み、

```text
定義した Check が成功する
```

ことを確認する。

ただし Check の内容が狭ければ、確認できる範囲も狭い。

```text
Healthcheck が healthy
=
System のあらゆる機能が正常
```

ではない。

### `depends_on` と Healthcheck を組み合わせる意味

Unit 09〜11 では、

```text
先に Start した
```

ことと、

```text
利用可能になった
```

ことが別だと学んだ。

Unit 12 では、

```text
Healthcheck
↓
service_healthy
↓
Dependent Service Start
```

として、この違いを Compose 設定に反映した。

これは単に起動順序を指定するより Application Dependency を具体的に表現できる。

### Restart Policy と Healthcheck は別の役割を持つ

Healthcheck が `unhealthy` になったこと自体で、通常の Docker Container Restart Policy が必ず Container を再起動するわけではない。

```text
Healthcheck
→ Health Status を判定

Restart Policy
→ Container Process が終了したときの再起動方針
```

と役割を分けて考える。

今回 Process 終了を発生させて Restart Policy を確認したのは、この違いを理解するためでもある。

### Restart Policy は運用方針で選ぶ

`unless-stopped` は便利だが、すべての Local Development Container に必須ではない。

Development では、

```text
Error で止まる
↓
Developer が原因を見る
```

方が分かりやすい場合もある。

一方、継続稼働する Environment では、

```text
一時的な Process Failure
↓
自動 Restart
```

が有効な場合がある。

Best Practice は Environment ごとの目的に合わせて選ぶ。

### non-root は不要な権限を減らす考え方である

Container は Isolation されているが、

```text
Container 内だから root でも何も考えなくてよい
```

とは考えない。

Application が root 権限を必要としないなら、専用 User で Process を実行することで権限を絞れる。

ただし non-root 化では、

```text
File Ownership
Write Permission
Port
Runtime Directory
```

なども考える必要がある。

`USER` を 1 行追加すること自体ではなく、Application が必要な権限を整理することが重要である。

### Frontend の `VITE_` Variable は Build Artifact の一部になる

Unit 11 では Development Server を使ったため、Frontend Container Environment と React の設定が近く見えた。

Unit 12 の Production Build では、

```text
VITE_API_BASE_URL
↓ Build
JavaScript
↓ Image
Browser
```

となる。

つまり API URL を変更するときは、Runtime Container に Environment Variable を追加するだけではなく Frontend Image の再 Build が必要になる。

Frontend / Backend で Configuration の扱い方が異なる例として理解する。

### Image Size を「不要なものを持ち込んでいないか」の手掛かりにする

Image Size の確認は Score を競うためではない。

例えば Frontend の改善前後で差が大きければ、

```text
改善前に Runtime 不要な Development Tool / Dependency が多かった
```

ことを理解できる。

一方、サイズが大きい Image でも Application Requirements 上必要なら、それだけで誤りとは言えない。  
Size は Dockerfile を見直すための 1 つの Signal と考える。

### Registry に Push すると Image を Build Artifact として扱える

Docker Hub へ Push することで、

```text
Source Code
↓
Build
↓
Image
↓
Registry
```

という流れを経験した。

Registry に置かれた Image は、別 Environment が同じ Tag を Pull して利用できる。

これは、

```text
各 Environment が Source Code から独自 Build
```

する方法とは異なり、

```text
Build 済み Artifact を共有
```

する考え方につながる。

CI/CD ではこの流れを自動化することも多いが、Unit 12 では Manual Push までを基礎として扱った。

### Tag は Image Version を識別するための情報になる

Docker Hub へ Push するとき、

```text
<username>/unit12-backend:1.0.0
```

とした。

Tag によって Registry 内で複数 Version を区別できる。

実際の運用では、

```text
Application Version
Commit
Environment
Release
```

などと Tag Strategy を関連付ける場合がある。

この Unit では `1.0.0` という単純な Version Tag だけを使い、Tag Strategy の設計までは扱わない。

### Dockerfile / Compose は Application Architecture の一部になる

Unit 12 まで進むと Dockerfile / Compose は単なる起動用 File ではなく、

```text
どう Build するか
何を Runtime に含めるか
どの権限で動かすか
何を Healthy とするか
異常終了時にどうするか
どの Service を待つか
どの Image を配布するか
```

といった設計判断を表す。

Application Code と同様に、目的を理解して Review / 改善する対象になる。

### この学習リポジトリで到達した範囲

Unit 01 から Unit 12 までで、Docker の学習は次の流れを通った。

```text
Docker / Image / Container
↓
基本操作
↓
Port / Bind Mount / Volume
↓
Dockerfile / Image Build
↓
React / Spring Boot / PostgreSQL
↓
Docker Network
↓
Docker Compose
↓
2 Container
↓
3 Container
↓
Dockerfile / Compose 改善
```

ここまでで、

```text
自分で Web Application を Docker 化する
Compose で複数 Container をつなぐ
動いた構成を基本的な観点から改善する
```

ための基礎を一通り経験したことになる。

Unit 12 の目的は Production Docker のすべてを理解することではない。

今後必要になったときに、

```text
この Dockerfile は Build と Runtime が混ざっていないか
Build Cache を活用できるか
Build Context は適切か
Base Image / Tag は適切か
Healthcheck は必要か
Restart Policy はどうするか
root 権限が必要か
Image をどう Registry へ配布するか
```

という観点を持って調べ、改善できることが重要である。

## 完了条件

以下を満たしたら Unit 12 を完了とする。

- Multi-stage Build が Build 環境と Runtime 環境を分離し、必要な Artifact だけを最終 Image へ渡す仕組みであることを説明できる。
- React では Node.js Build Stage → Nginx Runtime Stage、Spring Boot では Maven / JDK Build Stage → JRE Runtime Stage という構成を実際に Build できる。
- Backend Multi-stage Build の価値が Image Size 削減だけではなく、Host Maven 依存の削減や Build の自己完結性にもあることを説明できる。
- Dependency 定義 File を Source より先に `COPY` する理由を Build Cache と関連付け、Source 変更後の Rebuild で Cache の再利用を確認できる。
- `.dockerignore` が不要 File を Build Context から除外する理由と、Dockerfile の `COPY` との関係を説明できる。
- Base Image を Size だけで選ばず Runtime / Compatibility / Update / Version などを考慮し、`latest` のみに依存しない理由を説明できる。
- Frontend の改善前後で Image Size / Runtime 内容を比較し、Multi-stage Build と不要 Tool の除外が Image にどう影響するか確認できる。
- Container の `Running` と Application の `healthy` の違いを説明し、PostgreSQL / Backend / Frontend の Health Status を確認できる。
- `depends_on.condition: service_healthy` が Short syntax の `depends_on` と何が異なるか説明できる。
- Service-level の `restart: unless-stopped` の役割を説明し、Process 終了時の自動 Restart と Manual Stop の違いを確認できる。
- Spring Boot Runtime を `USER appuser` で実行し、non-root Container の目的と Permission / Port などの考慮点を説明できる。
- `VITE_API_BASE_URL` が Production Build 時に Frontend JavaScript へ反映され、`VITE_` Variable に Secret を入れてはいけない理由を説明できる。
- Docker Registry / Docker Hub が Build 済み Image を保存・配布する役割を持つことを説明できる。
- Local Image に Docker Hub 用 Tag を付け、`docker login` → `docker image tag` → `docker image push` の流れを 1 回実行できる。
- Unit 01〜11 で作った「動く Docker 構成」を、Build / Runtime / Cache / Context / Health / Restart / User / Image 配布という観点から基本的に見直せる。

ここまで確認できれば、Unit 12 および `docker-foundations-study` の Unit 学習は完了となる。
