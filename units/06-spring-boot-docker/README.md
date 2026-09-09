# 06. Spring Boot アプリの Docker 化

## この項目の目的

この Unit では、Java / Spring Boot で作られた最小 Application を Maven で Build して実行可能 JAR を生成し、その JAR を Docker Image に組み込み、Container 内で実行する。  
Spring Boot 自体の学習が目的ではなく、Unit 05 の React / Vite Application とは Runtime や Build 成果物が異なっていても、Docker では同じ Image / Container の基本構造で扱えることを確認する。

Unit 05 では Node.js Base Image に npm Dependency と Source Code を組み込み、Container 内で Vite Development Server を起動した。  
Unit 06 では先に Host 側で Maven Build を行い、生成された JAR を Java Runtime Image へ `COPY` して `java -jar` で起動する。

```text
Spring Boot Source
        ↓
Maven Build
        ↓
実行可能 JAR
        ↓
Dockerfile
        ↓
Docker Image
        ↓
Container
        ↓
java -jar
        ↓
Spring Boot Application
```

最終的には、Application Technology ごとの違いと Docker の共通部分を切り分けて説明できる状態を目指す。

## 学習内容

### Spring Boot Application と Docker

Docker から見ると Spring Boot Application も Container 内で実行する Application の 1 つである。  
Docker が Spring MVC や Dependency Injection の仕組みを理解して Container を作るわけではない。

今回 Docker の観点で必要になる要素は次のとおり。

```text
Application Source
→ Java / Spring Boot Source Code

Build Tool
→ Maven

Build 成果物
→ 実行可能 JAR

Runtime
→ Java Runtime Environment

Application 起動
→ java -jar app.jar

Application Port
→ 8080
```

React / Vite と比較すると Application 固有の Build 方法は異なるが、

```text
Application を実行できる形にする
↓
必要な Runtime と成果物を Image に含める
↓
Image から Container を作る
↓
Container 内で Application を起動する
```

という Docker の流れは共通している。

### 今回の Spring Boot Sample

今回の Application は `/api/info` から小さな JSON を返すだけの最小構成とする。

```text
06-spring-boot-docker/
├─ README.md
├─ Dockerfile
├─ .dockerignore
├─ pom.xml
└─ src/
   └─ main/
      ├─ java/
      │  └─ com/example/unit06/
      │     ├─ Unit06Application.java
      │     └─ InfoController.java
      └─ resources/
         └─ application.properties
```

Database、Service Layer、Repository、Validation、Security などは扱わない。  
Spring Boot の Application 設計を学ぶ Unit ではないため、Docker で実行する対象として必要な最小構成に限定する。

### Java 用 Base Image

Spring Boot JAR を実行するには Java Runtime が必要である。  
今回は Eclipse Temurin の Docker Official Image を使用する。

```text
eclipse-temurin:21.0.12_8-jre-alpine-3.24
```

この Image には Java 21 の JRE が含まれている。

```text
Java Runtime Base Image
├─ Linux Environment
└─ Java Runtime
```

その上へ Maven Build 済みの JAR を追加する。

```text
Eclipse Temurin JRE Image
        ↓
Spring Boot JAR
        ↓
unit06-spring-boot Image
```

### Java 21 と Spring Boot 4.1.1

この Unit では Java 21 と Spring Boot 4.1.1 を使用する。  
Spring Boot 4.1.1 は Java 17 以上を必要とし、Java 21 で実行できる。

```text
Spring Boot
4.1.1

Java
21

Runtime Image
eclipse-temurin:21.0.12_8-jre-alpine-3.24
```

### JDK と JRE

Java Application では、Source Code を Compile / Build するときと、Build 済み Application を実行するときで必要な機能が異なる。

大まかには次のように整理できる。

```text
JDK
→ Java Source の Compile / Build など開発に必要

JRE
→ Build 済み Java Application の実行に必要
```

今回 Docker Image に必要なのは、すでに Maven で Build 済みの JAR を実行する Runtime である。  
そのため Dockerfile では JDK Image ではなく `jre` Image を Base にする。

```text
Host
JDK + Maven
↓
JAR を Build

Docker Image
JRE
↓
Build 済み JAR を実行
```

この分離により、Runtime Image に Java Compiler や Maven 自体を含める必要がない。

### Maven と `pom.xml`

Maven は Java Project の Build Tool である。  
今回の `pom.xml` では Spring Boot Web Application を Build するために必要な最小設定だけを定義する。

主な要素は次のとおり。

```text
Spring Boot Version
→ 4.1.1

Java Version
→ 21

Dependency
→ spring-boot-starter-web

Build Plugin
→ spring-boot-maven-plugin
```

`spring-boot-maven-plugin` により、Maven Build 後に `java -jar` で実行できる Spring Boot JAR を生成する。

### Spring Boot の Build 成果物

次を実行すると Maven が Project を Build する。

```bash
mvn clean package
```

Build が成功すると `target/` Directory に JAR が生成される。

今回の JAR 名は次である。

```text
target/unit06-spring-boot-1.0.0.jar
```

ここで重要なのは、Dockerfile が Java Source Code を直接実行するのではなく、**Maven によって作られた Build 成果物である JAR を Runtime Image へ取り込む**ことである。

```text
src/
↓ Maven Build
target/unit06-spring-boot-1.0.0.jar
↓ Docker Build
Image /app/app.jar
```

### JAR とは

JAR は Java の Class File や Resource などをまとめた Archive Format である。  
Spring Boot の実行可能 JAR には、Application の Class や必要な Dependency、Spring Boot の起動に必要な構成がまとめられる。

今回 Docker で重要なのは JAR の内部構造を詳しく理解することではなく、

```text
Java Source
↓ Build
実行可能 JAR
↓
java -jar
Application 起動
```

という関係である。

### Unit 05 との Build の違い

Unit 05 では Dockerfile 内で `npm install` を行い、Source Code も Image に `COPY` した。

```text
Unit 05
Docker Build
├─ npm Dependency Install
└─ Source Code COPY
```

Unit 06 では Maven Build を先に Host 側で行う。

```text
Unit 06
Host
└─ mvn clean package
      ↓
    JAR

Docker Build
└─ JAR を COPY
```

つまり、この Unit の Docker Image は Java Source Code を Compile する役割を持たない。

後の Unit 12 では Multi-stage Build を扱い、Build 用 Container と Runtime 用 Container Image を分ける考え方にも触れる。  
Unit 06 ではまず「Build 成果物を Runtime Image へ入れる」という単純な構造を理解する。

### Dockerfile の全体構造

今回の Dockerfile は次の流れで構成する。

```text
FROM
Java JRE Base Image
   ↓
WORKDIR
Application Directory
   ↓
COPY
Maven Build 済み JAR
   ↓
ENV
Default Environment Variable
   ↓
EXPOSE
Spring Boot Port
   ↓
CMD
java -jar で Application 起動
```

Unit 04 で学んだ Dockerfile の基本命令を、Java Application の Runtime Image 作成に適用している。

### `WORKDIR /app`

Dockerfile では次を指定する。

```dockerfile
WORKDIR /app
```

Application JAR を `/app` に配置し、Container 起動時の基準 Directory とする。

```text
Container
└─ /app
   └─ app.jar
```

### JAR の `COPY`

Dockerfile では次を使用する。

```dockerfile
COPY target/unit06-spring-boot-1.0.0.jar app.jar
```

Host 側の Maven Build 成果物を Image の `/app/app.jar` として取り込む。

```text
Host
target/unit06-spring-boot-1.0.0.jar
        ↓ COPY
Image
/app/app.jar
```

Unit 05 では Source Code を `COPY` したが、今回は Build 済み Artifact だけを `COPY` する点が違う。

### Build Context と `.dockerignore`

Docker Build は Unit Directory を Build Context として使用する。

```bash
docker image build \
  -t unit06-spring-boot:1.0.0 \
  .
```

ただし Runtime Image に必要なのは Maven Build 済みの JAR であり、Java Source Code や `pom.xml` は Docker Build 自体には不要である。

そのため `.dockerignore` では、

```text
src/
pom.xml
README.md
```

などを Build Context から除外する。

一方、次の JAR は除外しない。

```text
target/unit06-spring-boot-1.0.0.jar
```

```text
Project Directory
      ↓
.dockerignore
      ↓
Runtime Image に必要な Build Context
      ↓
Dockerfile COPY JAR
```

「Repository に必要な File」と「Runtime Image の Build に必要な File」は同じではないことを再確認する。

### `ENV` と Spring Boot

Dockerfile では Default の Environment Variable を設定する。

```dockerfile
ENV APP_MESSAGE="Hello from Docker image"
```

Spring Boot 側の `application.properties` では次のように参照する。

```properties
app.message=${APP_MESSAGE:Hello from Spring Boot}
```

この関係は次のとおり。

```text
Container Environment
APP_MESSAGE
      ↓
Spring Boot Configuration
app.message
      ↓
/api/info の JSON
```

Environment Variable を利用することで、同じ Image を作り直さず Container 起動時に設定値を変更できる。

### Image と Runtime Configuration を分ける

Dockerfile に Default 値はあるが、Container 起動時に `-e` で Environment Variable を上書きできる。

```bash
docker container run \
  -e APP_MESSAGE="Overridden at runtime" \
  ...
```

```text
同じ Image
   ↓
Container A
APP_MESSAGE=Default

同じ Image
   ↓
Container B
APP_MESSAGE=別の値
```

Application Binary と実行時設定を分離する基本的な考え方である。

Password や Token などの秘密情報を Dockerfile の `ENV` に埋め込むことを推奨するものではない。  
Unit 06 では公開して問題のない学習用文字列だけを Environment Variable として扱う。

### Spring Boot の Port

Spring Boot の Embedded Web Server は Default で Port `8080` を使用する。  
Dockerfile では、

```dockerfile
EXPOSE 8080
```

として利用予定 Port を Metadata に記録する。

Unit 03〜05 と同様に、

```text
EXPOSE 8080
≠
Host からアクセス可能
```

である。

Container 起動時に、

```text
-p 8080:8080
```

を指定して Host Port と Container Port を Mapping する。

```text
Browser / curl
      ↓
Windows Host : 8080
      ↓
Docker Port Mapping
      ↓
Container : 8080
      ↓
Spring Boot
```

### `CMD` と `java -jar`

Container 起動時には次を実行する。

```dockerfile
CMD ["java", "-jar", "app.jar"]
```

意味は次のとおり。

```text
java
→ Java Runtime を起動

-jar
→ JAR File を実行

app.jar
→ Dockerfile で COPY した Spring Boot JAR
```

Unit 05 では、

```text
npm run dev
```

を起動したが、Unit 06 では、

```text
java -jar
```

を起動する。

Application Technology は違っても、Dockerfile の `CMD` が Container の Default 起動処理を定義する点は同じである。

### Source 変更から Container 反映まで

Java Source を変更した場合、Docker Image を再 Buildするだけでは Source が Compile されない。  
今回の構成では先に Maven Build をやり直す必要がある。

```text
Java Source 変更
↓
mvn clean package
↓
新しい JAR
↓
docker image build
↓
新しい Image
↓
Container 再作成
```

Unit 05 では Docker Build が直接 Source Code を取り込んだが、Unit 06 では Maven Build が追加されている点を意識する。

## 使用するもの

### Spring Boot

```text
4.1.1
```

### Java

```text
21
```

### Runtime Base Image

```text
eclipse-temurin:21.0.12_8-jre-alpine-3.24
```

### Build Tool

```text
Maven
```

### Build する Image

```text
unit06-spring-boot:1.0.0
```

### Container

```text
unit06-spring-boot
```

### Port

```text
Host      : 8080
Container : 8080
```

### Endpoint

```text
GET /api/info
```

## 事前準備

Unit 05 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/06-spring-boot-docker
```

Git Bash で次の Unit Directory へ移動した状態から操作する。

```text
units/06-spring-boot-docker
```

まず Java / Maven の成立を確認する。

```bash
java -version
mvn --version
```

Java 21 で Maven Build できる Environment であることを確認する。  
続いて Unit の File を確認する。

```bash
pwd
ls -la
```

次が存在することを確認する。

```text
Dockerfile
.dockerignore
pom.xml
src/
```

## ハンズオン

### 1. Spring Boot Sample の構成を確認する

まず `pom.xml` を確認する。

```bash
cat pom.xml
```

Spring Boot 4.1.1、Java 21、`spring-boot-starter-web`、`spring-boot-maven-plugin` が定義されていることを確認する。  
続いて Endpoint を確認する。

```bash
cat src/main/java/com/example/unit06/InfoController.java
```

`/api/info` から JSON を返すだけの小さな Application であることを確認する。  
Spring Boot の Controller 実装そのものを深く学ぶ必要はない。

### 2. Environment Variable の設定箇所を確認する

```bash
cat src/main/resources/application.properties
```

次の設定を確認する。

```properties
app.message=${APP_MESSAGE:Hello from Spring Boot}
```

Container の `APP_MESSAGE` が Spring Boot の `app.message` として利用される構成である。

### 3. Maven で Spring Boot Application を Build する

次を実行する。

```bash
mvn clean package
```

Maven が Dependency を取得し、Compile / Package を行う。  
Build 成功後に `target/` を確認する。

```bash
ls -lh target
```

次の JAR が存在することを確認する。

```text
unit06-spring-boot-1.0.0.jar
```

ここで Docker Build より先に Java Application の Build 成果物が作られている。

### 4. JAR が Java で実行できる形式であることを確認する

JAR 自体を Host で長時間起動する必要はないため、Maven Build が成功し JAR が生成されたことをまず成功条件とする。  
Dockerfile ではこの JAR を Container 内で `java -jar` により実行する。

```text
Source
↓
Maven
↓
JAR
```

という Build Tool 側の責務を整理する。

### 5. Dockerfile を確認する

```bash
cat Dockerfile
```

大きな流れを確認する。

```text
FROM JRE
↓
WORKDIR
↓
COPY JAR
↓
ENV
↓
EXPOSE
↓
CMD java -jar
```

Source Code や Maven を Docker Image に含めていない点に注目する。

### 6. `.dockerignore` を確認する

```bash
cat .dockerignore
```

`src/`、`pom.xml`、README、Maven の補助 Build Artifact などが除外されている一方、実行対象 JAR は Build Context に残る構成であることを確認する。

### 7. Spring Boot Image を Build する

```bash
docker image build \
  -t unit06-spring-boot:1.0.0 \
  .
```

Build Output では、Runtime Base Image の取得と JAR の `COPY` が中心になる。  
Unit 05 のように Docker Build 内で `npm install` を行わないことを比較する。

### 8. Build した Image を確認する

```bash
docker image ls unit06-spring-boot
```

次が表示されることを確認する。

```text
unit06-spring-boot   1.0.0
```

### 9. Image 内の Java Runtime を確認する

Image から一時 Container を起動して Java Version を確認する。

```bash
docker container run \
  --rm \
  unit06-spring-boot:1.0.0 \
  java -version
```

Java 21 系が表示されることを確認する。  
この Java は Host の Java ではなく、Eclipse Temurin Base Image に含まれる Container 側 Runtime である。

### 10. Image 内の JAR を確認する

```bash
MSYS_NO_PATHCONV=1 docker container run \
  --rm \
  unit06-spring-boot:1.0.0 \
  ls -lh /app
```

`app.jar` が存在することを確認する。

```text
Host target/...jar
↓ Docker Build
Image /app/app.jar
```

という対応を確認する。

### 11. Spring Boot Container を起動する

```bash
docker container run \
  -d \
  --name unit06-spring-boot \
  -p 8080:8080 \
  unit06-spring-boot:1.0.0
```

Container を確認する。

```bash
docker container ls
```

`unit06-spring-boot` が Running であることと、Host `8080` → Container `8080` の Port Mapping を確認する。

### 12. Spring Boot の Logs を確認する

```bash
docker container logs unit06-spring-boot
```

Spring Boot が起動し、Web Server が Port `8080` で動作していることを確認する。  
Application が Browser / curl から確認できない場合も、まず Container Status と Logs を確認する。

### 13. Endpoint へアクセスする

Git Bash から次を実行する。

```bash
curl http://localhost:8080/api/info
```

Browser で次を開いてもよい。

```text
http://localhost:8080/api/info
```

JSON が返ることを確認する。

```json
{
  "application": "unit06-spring-boot",
  "message": "Hello from Docker image"
}
```

通信経路は次のとおり。

```text
Browser / curl
      ↓
localhost:8080
      ↓
Windows Host : 8080
      ↓
Docker Port Mapping
      ↓
Container : 8080
      ↓
Spring Boot Application
```

### 14. Environment Variable を Runtime に上書きする

同じ Image を利用したまま設定だけを変えるため、現在の Container を削除する。

```bash
docker container rm -f unit06-spring-boot
```

次に `APP_MESSAGE` を指定して Container を起動する。

```bash
docker container run \
  -d \
  --name unit06-spring-boot \
  -p 8080:8080 \
  -e APP_MESSAGE="Overridden at runtime" \
  unit06-spring-boot:1.0.0
```

再度 Endpoint を確認する。

```bash
curl http://localhost:8080/api/info
```

今度は `message` が次の値になることを確認する。

```text
Overridden at runtime
```

Image は再 Build していない。

```text
同じ Image
+
異なる Runtime Environment Variable
↓
異なる Application 設定
```

という関係を確認する。

### 15. Container 内の Environment Variable を確認する

```bash
docker container exec unit06-spring-boot \
  printenv APP_MESSAGE
```

次が表示される。

```text
Overridden at runtime
```

Spring Boot の設定変更が Source Code や Image の変更ではなく、Container Runtime の Environment Variable によるものであることを確認する。

### 16. Source 変更時の流れを整理する

この Unit では実際に Java Source を変更する必要はない。  
変更するとしたら次の順序が必要になる。

```text
Java Source
↓
mvn clean package
↓
新しい JAR
↓
docker image build
↓
新しい Image
↓
Container 再作成
```

Unit 05 の、

```text
Source
↓
docker image build
↓
Image
```

よりも Maven Build が 1 段階追加されることを説明できればよい。

### 17. Unit 06 の Container を削除する

```bash
docker container rm -f unit06-spring-boot
```

確認する。

```bash
docker container ls -a \
  --filter 'name=unit06-spring-boot'
```

Container が残っていないことを確認する。

### 18. Unit 06 の Image を削除する

```bash
docker image rm unit06-spring-boot:1.0.0
```

確認する。

```bash
docker image ls unit06-spring-boot
```

表示されなくなればよい。

Runtime Base Image の `eclipse-temurin:21.0.12_8-jre-alpine-3.24` は、自作 Image とは別の Image である。  
後続 Unit でも Java Runtime Image を利用するため、この Unit では無理に削除する必要はない。

## 動作・確認ポイント

### Maven Build / JAR

以下を確認する。

- Java Source から Maven Build によって実行可能 JAR を生成できる。
- `target/unit06-spring-boot-1.0.0.jar` が Docker Image に取り込む Build 成果物である。
- Java Source と JAR、Docker Image を別々の段階として考えられる。

### Java Runtime Image

以下を確認する。

- JRE Base Image が Build 済み Java Application を実行する Runtime を提供する。
- Maven / Java Compiler を Runtime Image に含めなくても JAR を実行できる。
- Host Java と Container 内 Java Runtime を区別できる。

### Dockerfile / Image

以下を確認する。

- Dockerfile が JAR を `/app/app.jar` へ `COPY` する。
- `CMD ["java", "-jar", "app.jar"]` で Container 起動時に Spring Boot Application を実行する。
- Source Code ではなく Build 成果物を Runtime Image に組み込む構成を説明できる。

### Port

以下を確認する。

- Spring Boot Application が Container Port `8080` で動作する。
- `-p 8080:8080` で Host と Container の Port を Mapping できる。
- Browser / curl から `/api/info` へアクセスできる。

### Environment Variable

以下を確認する。

- Dockerfile の `ENV APP_MESSAGE` が Default 値として利用される。
- `docker container run -e` により Image を再 Build せず Runtime に値を上書きできる。
- Container Environment Variable が Spring Boot Configuration を通して Application の Response に反映される。

## 学習ポイント

### Application Technology が変わっても Docker の基本は同じ

Unit 05 と Unit 06 では Application の作り方が大きく異なる。

```text
React / Vite
→ Node.js / npm

Spring Boot
→ Java / Maven / JAR
```

しかし Docker の基本構造は変わらない。

```text
必要な Runtime
+
Application を実行できる材料
+
起動 Command
↓
Image
↓
Container
```

Docker を学ぶ際は、Framework 固有知識と Container 共通知識を分けて考える。

### Java では Build 成果物である JAR が境界になる

今回の構成では Docker が Java Source を Build していない。

```text
Java / Maven の責務
Source
↓
JAR

Docker の責務
JAR
↓
Image
↓
Container
```

JAR が Application Build と Container Image Build の境界になっている。

### JDK と JRE の役割を分ける

```text
JDK
→ Build / Compile

JRE
→ Runtime
```

今回 Host で JAR を Build し、Docker Image では JRE だけを使うことで、Build Environment と Runtime Environment の役割を分離している。

### Runtime Image に不要なものを含めない

Container 内で必要なのは Build 済み JAR と Java Runtime である。

```text
必要
JRE
JAR

今回の Runtime Image では不要
Java Source
Maven
pom.xml
```

何を Image に含めるべきかを Application の Runtime から逆算して考える。

### Environment Variable で Image と設定を分離する

同じ Image に対して Runtime の Environment Variable を変えられる。

```text
Image
= Application Binary / Default 構成

Environment Variable
= 実行時に変える設定
```

Environment ごとに Source や Image を書き換えるのではなく、Runtime Configuration を外から渡せることが重要である。

### `ENV` と `-e` の優先関係を理解する

Dockerfile の `ENV` は Image 側の Default 値として利用できる。  
Container 起動時の `-e` で同じ Environment Variable を指定すると Runtime 側の値で上書きできる。

```text
Dockerfile ENV
→ Default

docker container run -e
→ Container ごとの Runtime 値
```

### Port の考え方は Frontend / Backend で共通

Unit 05 は Vite の Port `5173`、Unit 06 は Spring Boot の Port `8080` を使う。

```text
Application が Container 内で Listen
+
Docker が Port Publish
↓
Host からアクセス
```

Port 番号は違っても Unit 03 で学んだ仕組みそのものは同じである。

### Source 変更後に必要な Build Step は Technology ごとに異なる

React / Vite と Spring Boot では Build Pipeline が異なる。

```text
Unit 05
Source
↓
Docker Build

Unit 06
Source
↓
Maven Build
↓
JAR
↓
Docker Build
```

Docker の前に何を準備する必要があるかは Application Technology によって変わる。

### Multi-stage Build へのつながり

今回 Maven Build は Host で実行した。

```text
Host Maven
↓
JAR
↓
Runtime Image
```

将来的には Build 用 Image の中で Maven Build を行い、生成した JAR だけを Runtime Image へ渡す Multi-stage Build も利用できる。

```text
Build Stage
JDK + Maven
↓ JAR
Runtime Stage
JRE + JAR
```

この Unit では Multi-stage Build 自体を実践せず、Unit 12 で扱うための前提として位置付ける。

### Unit 07 へのつながり

次の Unit 07 では PostgreSQL Container を扱う。

```text
Unit 06
Spring Boot
→ Application Container

Unit 07
PostgreSQL
→ Database Container
```

その後、Docker Network / Compose を学び、Unit 10 で Spring Boot Container と PostgreSQL Container を接続する。

Unit 06 ではまず Spring Boot Application 単体を安定して Image / Container として扱える状態にする。

## 完了条件

以下を満たしたら Unit 06 を完了とする。

- Java / Spring Boot Application も Docker Image / Container として扱えることを説明できる。
- Java Source → Maven Build → JAR → Docker Image → Container の流れを説明・実行できる。
- JDK と JRE の基本的な役割の違いと、今回 Runtime Image に JRE を使用する理由を説明できる。
- Maven Build で生成された JAR が Docker Image に取り込む Build 成果物であることを理解している。
- Dockerfile から JAR を含む Spring Boot Image を Build し、その Image から Container を起動できる。
- `java -jar` が Container 内で Spring Boot JAR を実行する起動 Command であることを説明できる。
- Host Port `8080` と Container Port `8080` を Mapping し、Browser / curl から Endpoint を確認できる。
- Dockerfile の `ENV` と `docker container run -e` の関係を説明し、Runtime に Environment Variable を上書きできる。
- Environment Variable を変更しても同じ Image を再利用できることを理解している。
- Runtime Image に Java Source や Maven を含めず、実行に必要な JRE と JAR を中心に構成する理由を説明できる。
- Unit 05 と Unit 06 の Application Build 方法の違いと、Docker の共通的な Image / Container の流れを比較して説明できる。
- Java Source を変更した場合、Maven Build → Docker Build → Container 再作成が必要になることを説明できる。

ここまで確認できれば、Unit 07「PostgreSQL Container」へ進む。
