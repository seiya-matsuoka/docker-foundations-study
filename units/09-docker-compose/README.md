# 09. Docker Compose の基本

## この項目の目的

この Unit では、これまで Docker CLI の Option として個別に指定してきた Container、Port、Environment Variable、Volume、Network、Image Build などを `compose.yaml` にまとめて宣言し、Docker Compose から一括して管理する。  
Docker Compose を新しい Container 技術として覚えるのではなく、Unit 01〜08 で学んできた Docker Resource と設定を、複数 Service 単位で再現可能な File として表現する仕組みとして理解する。

これまでの CLI 操作は、例えば次のように分かれていた。

```text
docker image build ...
docker container run ...
  -p ...
  -e ...
  --mount ...
  --network ...

docker network create ...
docker volume create ...
```

Docker Compose では、これらの設定を `compose.yaml` に宣言する。

```text
compose.yaml
├─ services
│  ├─ web
│  │  ├─ build
│  │  ├─ ports
│  │  ├─ environment
│  │  └─ networks
│  └─ client
│     ├─ image
│     ├─ environment
│     ├─ volumes
│     ├─ networks
│     └─ depends_on
├─ networks
└─ volumes
```

そして、

```bash
docker compose up
```

のような Compose Command から、それらをまとめて作成・起動できる。

最終的には、Docker Compose が「これまで CLI で個別指定していた Docker の設定と複数 Container の関係を、宣言的にまとめて管理する仕組み」であることを説明できる状態を目指す。

## 学習内容

### Docker Compose

Docker Compose は、複数 Container で構成される Application を Compose File に定義し、まとめて作成・起動・停止・削除するための仕組みである。  
現在の Docker Desktop 環境では、Compose は次の形式で Docker CLI の Subcommand として利用する。

```bash
docker compose ...
```

この学習では旧形式の `docker-compose` ではなく、`docker compose` を使用する。

Compose を使っても、Container、Image、Network、Volume といった Docker Resource 自体が別のものへ変わるわけではない。

```text
Docker CLI
→ Resource を個別に作成・設定する

Docker Compose
→ Resource と関係を Compose File に宣言してまとめて管理する
```

### 宣言的に管理するということ

CLI だけで複数 Container を起動する場合、必要な Option を毎回 Command に記述する。

```text
Image
Container Name
Port
Environment Variable
Volume
Network
起動 Command
```

設定が増えるほど Command は長くなり、どの Container がどの設定で動いていたかを再現しにくくなる。  
Compose では設定を File に残す。

```text
compose.yaml
↓
Docker Compose が読み込む
↓
必要な Docker Resource を作成
↓
Service ごとの Container を起動
```

これにより、どの Service が存在し、どの Image / Port / Environment Variable / Volume / Network を利用するかを 1 つの構成として確認できる。

### `compose.yaml`

Docker Compose が構成を読み取る中心 File が `compose.yaml` である。

この Unit では次の構成を使用する。

```text
09-docker-compose/
├─ README.md
├─ compose.yaml
├─ .env.example
├─ Dockerfile
└─ site/
   └─ index.html
```

Compose を実行する際は、この Unit Directory を Compose Project の基準 Directory として扱う。

### Compose Project

Compose では、`compose.yaml` でまとめて管理する一連の Resource を Project として扱う。

この Unit では Top-level の `name` に、

```yaml
name: unit09-compose
```

を指定する。

概念的には次のようになる。

```text
Compose Project
unit09-compose
├─ Service: web
├─ Service: client
├─ Network
└─ Volume
```

Project は単一 Container ではなく、関連する Service / Container / Network / Volume などをまとめる管理単位である。  
Compose が作成する Resource 名には Project Name が反映されることがある。

細かな命名規則を暗記することよりも、

```text
Project
→ 複数の Compose Resource をまとめる単位
```

と理解することが重要である。

### Service と Container

Compose File の `services` には、Application を構成する Service を定義する。

今回の Service は次の 2 つである。

```text
web
→ Nginx Web Server

client
→ Alpine Linux の通信・Environment / Volume 確認用
```

Service は Container そのものと完全に同義ではない。

```text
Service
→ Compose 上の実行単位・定義

Container
→ Service 定義をもとに実際に作成される Container
```

この Unit では各 Service から 1 Container ずつ起動するため、見た目上は 1 Service = 1 Container になるが、概念としては分けて理解する。  
後に Scale などを利用すると 1 Service から複数 Container を起動することもできるが、今回は対象外とする。

### Service 名と Container 間通信

Compose Project 内で同じ Network に接続された Service は、Service 名を Host 名として通信できる。

今回、

```text
web
client
```

という Service を同じ Network に接続する。

Client から Web Server へは、

```text
http://web
```

で接続できる。

```text
client Service の Container
        ↓
Host 名: web
        ↓
Compose Network
        ↓
web Service の Container
        ↓
Nginx : 80
```

Unit 08 では Container 名 `unit08-web` を利用した。  
Compose では Service 名を接続先として利用できることが重要である。

### `services`

`services` は Compose Application を構成する Service 定義をまとめる Top-level Key である。

```yaml
services:
  web: ...
  client: ...
```

この下へ Service ごとの Image、Build、Port、Environment、Volume、Network などを記述する。  
CLI で別々に実行していた `docker container run` の設定を Service ごとの定義として整理するイメージである。

### `image`

`image` は Service が利用する Image を指定する。

今回 `client` Service では、

```yaml
client:
  image: alpine:3.24.1
```

とする。

これは CLI の、

```bash
docker container run alpine:3.24.1
```

における Image 指定に相当する。

既存 Image をそのまま利用する Service では `image` が中心になる。

### `build`

`build` は Dockerfile から Service 用 Image を Build する設定である。

今回 `web` Service では、

```yaml
build:
  context: .
  dockerfile: Dockerfile
```

を指定する。

CLI では Unit 04〜06 で、

```bash
docker image build \
  -t ... \
  .
```

と実行していた。

Compose では、

```text
build.context
→ Docker Build Context

build.dockerfile
→ 使用する Dockerfile
```

として Service 定義の中へ含める。

```text
web Service
↓
build
↓
Dockerfile
↓
Image
↓
Container
```

という流れになる。

### `image` と `build`

`image` と `build` はどちらも Service と Image の関係を定義するが、今回の使い方は異なる。

```text
client
image
→ 既存 Official Image を利用

web
build
→ 自分の Dockerfile から Image を Build
```

Compose では Service ごとに、既存 Image を使うのか、Dockerfile から Build するのかを構成に応じて選択できる。

### `ports`

`ports` は Host Port と Container Port の Mapping を宣言する。

```yaml
ports:
  - '${WEB_PORT:-8080}:80'
```

Unit 03 / 08 で使った CLI の、

```text
-p 8080:80
```

に相当する。

```text
Host
localhost:8080
↓
Compose ports
↓
web Container : 80
↓
Nginx
```

ここでも Host Port と Container Port の考え方自体は変わらない。

### `environment`

`environment` は Service の Container に Environment Variable を渡す。

```yaml
environment:
  APP_STAGE: '${APP_STAGE:-compose-learning}'
```

これは CLI の、

```text
-e APP_STAGE=...
```

に相当する。

```text
compose.yaml
environment
↓
Container Environment
↓
Application / Process
```

今回の `web` Container では `printenv` により値を確認する。

### Dockerfile `ENV` と Compose `environment`

今回の Dockerfile には次を定義している。

```dockerfile
ENV APP_STAGE=container-default
```

これは Image 側の Default 値である。

一方 `compose.yaml` では、

```yaml
environment:
  APP_STAGE: '${APP_STAGE:-compose-learning}'
```

を指定する。

Container 起動時には Compose から渡された Environment Variable が利用される。

```text
Dockerfile ENV
container-default
↓ Image の Default

Compose environment
compose-learning
↓ Container Runtime で上書き
```

Unit 06 で扱った Dockerfile `ENV` と `docker container run -e` の関係を Compose に置き換えている。

### `.env` と Compose の値補間

`.env` は特に混同しやすいため、役割を分けて理解する。

この Unit の `.env.example` は次のような値を持つ。

```text
WEB_PORT=8080
APP_STAGE=compose-learning
CLIENT_MESSAGE="Hello from Compose environment"
```

これを `.env` として用意すると、Compose は `compose.yaml` 内の `${...}` を置き換えるために利用できる。

```text
.env
↓ Compose が値を読み取る
compose.yaml の ${...}
↓
解決済み Compose 設定
```

重要なのは、`.env` に書いただけで、すべての変数が自動的に Container Environment へ入るわけではないという点である。

例えば `APP_STAGE` は、

```yaml
environment:
  APP_STAGE: '${APP_STAGE:-compose-learning}'
```

と `environment` から Container へ渡している。

```text
.env
APP_STAGE
↓ 値補間
compose.yaml environment
↓
Container APP_STAGE
```

一方 `WEB_PORT` は `ports` の値補間に使うだけであり、今回 `web` Container の Environment Variable としては渡していない。

```text
.env
WEB_PORT
↓
compose.yaml ports
↓
Host Port の設定

Container Environment
→ WEB_PORT は今回渡していない
```

`.env` と Container Environment を同一視しない。

### `.env.example` と `.env`

Repository には設定項目の Sample として `.env.example` を含める。

実際に Compose から利用する Local File は、

```text
.env
```

とする。

```text
.env.example
→ Git で共有する設定例

.env
→ Local で使用する実値
```

Root `.gitignore` では `.env` 系を除外し、`.env.example` は Commit できる方針になっている。

この Unit では学習用の公開可能な値だけを扱うが、Credential などを `.env` に書く場合は Git へ Commit しないことが重要である。

### `.env` は Secrets 管理の完成形ではない

`.env` を Git から除外することは、秘密情報を Repository へ直接 Commit しないための基本的な対策になる。

ただし、

```text
.env を Git に入れない
=
Production Secrets 管理が完成
```

ではない。

`.env` は Local File であり、その File 自体の配布・保存・Access Control を別途考える必要がある。  
Production Environment では利用する Platform / Infrastructure に応じて、専用の Secrets 管理機能を利用することがある。

この Unit では、

```text
秘密情報を Source / Compose File へ直接書かない
.env を Git へ Commit しない
.env だけを万能な Secrets 管理として考えない
```

という基本的な認識までを対象とする。

### Docker Secrets

Docker Secrets は秘密情報を Container へ安全に渡すための仕組みの 1 つである。

ただし Secrets の利用方法は実行 Environment や構成によって異なり、今回の Docker Compose 基礎の範囲を超える。

この Unit では、

```text
Password / Token など
↓
通常の Source Code や Dockerfile へ直接埋め込まない

必要に応じて
↓
Secrets 管理機能を利用する
```

という存在と目的だけ把握する。  
実際の Docker Secrets 運用は行わない。

### Build Secrets

Image Build 中だけ必要になる Credential などを、Dockerfile の `ARG` / `ENV` や Build Layer へ残さず扱うための仕組みとして Build Secrets がある。

例えば Private Package Repository への認証情報などが該当する。

```text
Runtime Secret
→ Container 実行時に必要

Build Secret
→ Image Build 中だけ必要
```

この Unit では Build Secrets の存在と用途だけ理解し、実践は対象外とする。

### `volumes`

Compose の `volumes` は Service への Mount を宣言する。

今回 `client` Service では、

```yaml
volumes:
  - unit09-data:/data
```

を指定する。

```text
Named Volume
unit09-data
↓
client Container
/data
```

Unit 03 / 07 では、

```text
docker volume create
--mount type=volume,...
```

を CLI から行った。

Compose では Top-level の `volumes` と Service の `volumes` を組み合わせて管理する。

### Top-level `volumes`

Compose File の末尾では Named Volume 自体を宣言する。

```yaml
volumes:
  unit09-data:
```

Service 側の、

```yaml
volumes:
  - unit09-data:/data
```

は「どの Service へどこに Mount するか」を表す。

Top-level の、

```yaml
volumes:
  unit09-data:
```

は「この Compose Project が管理する Volume Resource」を宣言する。

```text
Top-level volumes
↓
Volume Resource

Service volumes
↓
その Volume を Container のどこへ Mount するか
```

この 2 つを区別する。

### `networks`

Compose の `networks` は Service が参加する Docker Network を指定する。

```yaml
networks:
  - unit09-network
```

Top-level では次を宣言する。

```yaml
networks:
  unit09-network:
    driver: bridge
```

Unit 08 では、

```bash
docker network create unit08-network
docker container run --network unit08-network ...
```

と CLI から操作した。

Compose では、

```text
Top-level networks
→ Network Resource の定義

Service networks
→ Service をどの Network へ接続するか
```

として表現する。

### Compose と名前解決

`web` と `client` は同じ `unit09-network` に接続される。

そのため `client` から、

```text
web
```

を Host 名として Nginx へ接続できる。

```text
client
↓
http://web
↓
Compose が管理する Docker Network
↓
web
```

これは Unit 08 で学んだ User-defined Bridge Network と Docker の名前解決を Compose が利用している。

Compose がまったく別の通信方式を提供しているわけではない。

### `depends_on`

`depends_on` は Service 間の起動依存関係を表す。

```yaml
depends_on:
  - web
```

今回 `client` は `web` に依存する。

Simple な `depends_on` により、Compose は `web` を `client` より先に開始する。

ただし重要なのは、

```text
Container が開始した
≠
Application が利用可能な状態になった
```

という点である。

Simple `depends_on` だけでは、Web Server や Database が実際に Request を受け付けられる Readiness まで保証するものとして扱わない。

Healthcheck と連携したより詳細な起動制御もあるが、Healthcheck 自体は Unit 12 で扱う。

### Compose が作る Network / Volume

`docker compose up` を実行すると、必要な Network や Volume が存在しなければ Compose が作成する。

```text
compose.yaml
↓
docker compose up
↓
Project Network 作成
Named Volume 作成
Service Container 作成
Service Container 起動
```

Unit 07 / 08 では先に、

```bash
docker volume create ...
docker network create ...
```

を実行した。

Compose では File へ宣言しておけば、Compose が Project Resource として管理できる。

### `docker compose config`

`docker compose config` は、Compose が Compose File と Environment Variable の補間結果をどのように解釈したか確認するために利用できる。

```bash
docker compose config
```

例えば `.env` の、

```text
WEB_PORT=8080
```

が、

```yaml
ports:
  - '8080:80'
```

へ解決された最終構成を確認できる。

Application を起動する前に Compose File の解釈結果を確認する用途でも有効である。

### `docker compose build`

Build 設定を持つ Service の Image を Build する。

```bash
docker compose build
```

今回 `web` Service は Dockerfile を利用するため Build 対象になる。

特定 Service だけの場合は、

```bash
docker compose build web
```

と指定できる。

### `docker compose up`

Compose Project の Service を作成・起動する。

```bash
docker compose up
```

Default では Foreground で Logs が表示される。

Unit 02 以降で個別 Container に対して行っていた、

```text
Image Build
Network 準備
Volume 準備
Container Create
Container Start
```

などの複数操作を Compose が構成に基づいてまとめて行う。

### `docker compose up -d`

Background で起動する場合は、

```bash
docker compose up -d
```

を使用する。

CLI の `docker container run -d` と同じく、Terminal を占有せず Service を継続実行できる。

### `docker compose stop`

```bash
docker compose stop
```

は Service Container を停止する。

Container 自体は残るため、再度起動できる。

```text
stop
→ Container は残る

down
→ Compose Project の Container / Network などを削除
```

この違いを理解する。

### `docker compose down`

```bash
docker compose down
```

は Compose Project で作成した Container や Network などを停止・削除する。

ただし Named Volume は Default では削除しない。

```text
docker compose down
↓
Container 削除
Network 削除
Named Volume は残る
```

Volume まで削除する場合は、

```bash
docker compose down -v
```

を使用する。

Unit 07 で学んだ、

```text
Container と Volume は別ライフサイクル
```

という考え方は Compose でも同じである。

### `docker compose ps`

Compose Project の Service / Container 状態を確認する。

```bash
docker compose ps
```

個別 Docker CLI の、

```bash
docker container ls
```

に近い役割を、Compose Project 単位で確認できる。

### `docker compose logs`

Compose Service の Logs を確認する。

```bash
docker compose logs
```

特定 Service のみ確認する場合は、

```bash
docker compose logs web
```

とする。

複数 Service 構成では、「どの Service の Logs を見ているか」を意識する。

### `docker compose exec`

Running Service Container 内で Command を実行する。

```bash
docker compose exec web printenv APP_STAGE
```

個別 Docker CLI の、

```bash
docker container exec <container> ...
```

に相当する。

Compose では Container Name を直接指定する代わりに Service Name を指定して操作できる。

### Rebuild と Container への反映

Dockerfile や Build Context の File を変更した場合、新しい Image を Build する必要がある。

```bash
docker compose build web
```

または Build と起動をまとめて、

```bash
docker compose up -d --build
```

と実行できる。

```text
Source / Dockerfile 変更
↓
Compose Build
↓
新しい Image
↓
必要に応じて Container 再作成
```

という関係は Compose を使っても変わらない。

## 使用するもの

### Compose Project

```text
unit09-compose
```

### Compose File

```text
compose.yaml
```

### Web Service

```text
web
```

Dockerfile から Build する Nginx Service。

### Client Service

```text
client
```

`alpine:3.24.1` を利用する確認用 Service。

### Nginx Base Image

```text
nginx:1.30.4-alpine3.24
```

### Network

Compose File 上の Key:

```text
unit09-network
```

### Volume

Compose File 上の Key:

```text
unit09-data
```

### Default Host Port

```text
8080
```

### Container Port

```text
80
```

## 事前準備

Unit 08 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/09-docker-compose
```

Git Bash で Repository 内の次の Unit Directory へ移動した状態から操作する。

```text
units/09-docker-compose
```

Docker Compose が利用できることを確認する。

```bash
docker compose version
```

続いて File を確認する。

```bash
ls -la
```

次が存在することを確認する。

```text
README.md
compose.yaml
.env.example
Dockerfile
site/
```

この時点では `.env` はまだ作成しない。

## ハンズオン

### 1. `compose.yaml` の全体構造を確認する

```bash
cat compose.yaml
```

まず大きな構造を見る。

```text
name
services
├─ web
└─ client

networks
└─ unit09-network

volumes
└─ unit09-data
```

Compose File を細かな YAML Syntax の暗記として見るのではなく、Project / Service / Network / Volume という Docker Resource / 管理単位の宣言として確認する。

### 2. `web` Service を確認する

`web` Service では主に次を確認する。

```text
build
ports
environment
networks
```

CLI へ置き換えると、大まかには、

```text
Dockerfile から Image Build
Host 8080 → Container 80
Environment Variable を渡す
Network へ接続
```

という操作に対応する。

### 3. `client` Service を確認する

`client` Service では主に次を確認する。

```text
image
command
environment
volumes
networks
depends_on
```

CLI へ置き換えると、大まかには、

```text
Alpine Image を利用
sleep 1d で継続起動
Environment Variable を渡す
Named Volume を /data へ Mount
Network へ接続
web との起動依存関係を定義
```

という構成になる。

### 4. `.env.example` から `.env` を作成する

```bash
cp .env.example .env
```

確認する。

```bash
cat .env
```

この `.env` は Local 用 File であり、Repository へ Commit しない。  
`.env.example` は必要な設定項目を共有する Sample として Repository に残す。

### 5. Compose の値補間結果を確認する

Service を起動する前に実行する。

```bash
docker compose config
```

`.env` の値が `compose.yaml` の `${...}` へ反映された最終的な Compose Model を確認する。

特に次を見る。

```text
web ports
web environment
client environment
```

さらに、Compose が補間に利用する Environment Variable を確認する場合は次も利用できる。

```bash
docker compose config --environment
```

### 6. `.env` と Container Environment の違いを確認する

`.env` には、

```text
WEB_PORT
APP_STAGE
CLIENT_MESSAGE
```

が存在する。

ただし `web` の `environment` に指定しているのは `APP_STAGE` だけである。

この時点で、

```text
.env
→ Compose File の値補間

environment
→ Container Environment への設定
```

という違いを整理する。

実際の Container 内の値は起動後に確認する。

### 7. Compose から Image を Build する

```bash
docker compose build
```

`web` Service の Dockerfile Build が実行される。

`client` は、

```yaml
image: alpine:3.24.1
```

を直接利用するため、Custom Dockerfile の Build 対象ではない。

Build 後に Docker Image 一覧を確認する。

```bash
docker image ls
```

Compose Project の `web` Service 用に Build された Image が存在することを確認する。

### 8. Foreground で Service を起動する

```bash
docker compose up
```

Terminal 上に Service Logs が表示される。

`web` と `client` が起動し、Compose が必要な Network / Volume / Container を作成することを確認する。  
確認後は `Ctrl + C` で停止する。

Foreground の Compose Process を終了すると、今回の `docker compose up` で起動した Service も停止する。  
Resource 自体は `down` するまで残る。

### 9. Background で Service を起動する

```bash
docker compose up -d
```

Terminal が戻ってくることを確認する。  
Compose Project の状態を確認する。

```bash
docker compose ps
```

`web` と `client` の Service Container が Running であることを確認する。

### 10. Compose が作成した Docker Resource を確認する

通常の Docker CLI からも確認する。

```bash
docker container ls
docker network ls
docker volume ls
```

Compose を使用しても、実際に作られているものは Docker Container / Network / Volume であることを確認する。  
Project Name が Resource Name に反映されていることにも注目する。

### 11. Host から `web` Service へアクセスする

Git Bash から実行する。

```bash
curl http://localhost:8080
```

Browser で次を開いてもよい。

```text
http://localhost:8080
```

`site/index.html` の内容が表示されることを確認する。

通信経路は次のとおり。

```text
Windows Host
localhost:8080
↓
Compose ports
↓
web Container : 80
↓
Nginx
```

### 12. `client` から `web` Service へ通信する

```bash
docker compose exec client \
  wget -qO- http://web
```

Nginx の HTML が取得できればよい。

```text
client
↓
Host 名: web
↓
Compose Network
↓
web : 80
```

Unit 08 で学んだ Container 間通信が、Compose Service 名を利用する形で再現されている。  
この通信では Host Port `8080` を利用しない。

### 13. Service / Container の Logs を確認する

すべての Service Logs を確認する。

```bash
docker compose logs
```

`web` だけを確認する。

```bash
docker compose logs web
```

Step 11 / 12 の HTTP Request に対応する Nginx Access Log を確認する。

### 14. Dockerfile `ENV` と Compose `environment` を確認する

`web` Container の値を確認する。

```bash
docker compose exec web \
  printenv APP_STAGE
```

`.env` の Default Sample のままなら、

```text
compose-learning
```

が表示される。

Dockerfile では、

```text
container-default
```

を指定しているが、Compose の `environment` から Runtime に値を渡したため上書きされている。

### 15. `.env` の `WEB_PORT` が Container Environment へ自動注入されていないことを確認する

次を実行する。

```bash
docker compose exec web \
  sh -c 'printenv WEB_PORT || echo "WEB_PORT is not set in container"'
```

次が表示されればよい。

```text
WEB_PORT is not set in container
```

`.env` の `WEB_PORT` は `${WEB_PORT}:80` という Compose File の値補間に使用しただけである。  
`.env` に存在することと Container Environment に存在することは別である。

### 16. `client` の Environment Variable を確認する

```bash
docker compose exec client \
  printenv CLIENT_MESSAGE
```

次の値が確認できる。

```text
Hello from Compose environment
```

```text
.env
↓ 値補間
compose.yaml environment
↓
client Container
```

という経路を確認する。

### 17. Named Volume へ Data を保存する

`client` Service に Mount した `/data` へ File を作成する。

```bash
docker compose exec client \
  sh -c 'echo "$CLIENT_MESSAGE" > /data/message.txt && cat /data/message.txt'
```

確認する。

```bash
MSYS_NO_PATHCONV=1 docker compose exec client \
  cat /data/message.txt
```

Named Volume `unit09-data` に Data が保持される。

### 18. `docker compose stop` を確認する

```bash
docker compose stop
```

確認する。

```bash
docker compose ps -a
```

Container は存在するが停止していることを確認する。

再度起動する。

```bash
docker compose start
```

確認する。

```bash
docker compose ps
```

`stop` は Container を削除する操作ではない。

### 19. `docker compose down` を確認する

```bash
docker compose down
```

Compose が管理していた Service Container と Network が削除される。

確認する。

```bash
docker compose ps -a
docker network ls
docker volume ls
```

Named Volume は Default では残ることを確認する。

```text
Container
→ 削除

Compose Network
→ 削除

Named Volume
→ 残る
```

### 20. 再度 Compose Project を起動して Volume Data を確認する

```bash
docker compose up -d
```

新しく Service Container / Network が作成される。

`client` Container から確認する。

```bash
MSYS_NO_PATHCONV=1 docker compose exec client \
  cat /data/message.txt
```

以前保存した内容が残っていることを確認する。  
Unit 07 の Volume 永続化と同じ仕組みが、Compose で管理した Named Volume でも成立している。

### 21. `site/index.html` を変更して Rebuild する

`site/index.html` の本文を 1 箇所変更する。

例えば、

```text
compose.yaml から起動した Nginx Service です。
```

を、

```text
Rebuilt with Docker Compose.
```

へ変更して保存する。

現在の `web` Container は Build 済み Image から作られているため、Host File を保存しただけでは表示は変わらない。

### 22. Compose から再 Build・再作成する

Build と起動をまとめて実行する。

```bash
docker compose up -d --build
```

`web` Image が再 Build され、必要に応じて Container が再作成される。

確認する。

```bash
curl http://localhost:8080
```

変更後の HTML が表示されることを確認する。

```text
Source 変更
↓
Compose Build
↓
Image 更新
↓
Container 再作成
↓
表示更新
```

という流れを確認する。

### 23. `depends_on` の位置付けを確認する

`compose.yaml` の次を再確認する。

```yaml
depends_on:
  - web
```

`client` が `web` より後に開始される依存関係を Compose へ伝えている。

ただし、

```text
web Container Start
≠
Nginx が確実に Request を受け付けられる状態
```

である。

この Unit では Simple `depends_on` を「起動順序の依存関係」として理解し、Readiness 管理まで広げない。

### 24. `.env` の値を変更して Compose 設定を変更する

`.env` の、

```text
APP_STAGE=compose-learning
```

を例えば、

```text
APP_STAGE=compose-local
```

へ変更する。

解釈結果を確認する。

```bash
docker compose config
```

その後起動状態へ反映する。

```bash
docker compose up -d
```

確認する。

```bash
docker compose exec web \
  printenv APP_STAGE
```

次が表示されることを確認する。

```text
compose-local
```

Image の Dockerfile を変更・再 Build しなくても、Compose の Runtime Configuration を変更できる。

### 25. 最後に Compose Project を削除する

今回は Unit 09 用 Named Volume も不要になるため、Volume を含めて削除する。

```bash
docker compose down -v
```

確認する。

```bash
docker compose ps -a
docker network ls
docker volume ls
```

Unit 09 用の Service Container、Network、Volume が残っていないことを確認する。

Custom Image は `docker compose down` では自動削除されない。  
次で Docker Image 一覧を確認できる。

```bash
docker image ls
```

この Unit では Image の存在確認まででよく、後続 Unit の学習へ影響しない限り無理に削除する必要はない。

### 26. Local `.env` を確認する

最後に `.env` が Repository の Commit 対象になっていないことを確認する。

```bash
git status --short
```

`.env` が Root `.gitignore` によって Ignore され、`.env.example` は Repository に含められる状態であることを確認する。

## 動作・確認ポイント

### Compose Project / Service

以下を確認する。

- Compose Project が複数 Service / Network / Volume をまとめる管理単位である。
- Service 定義から実際の Container が作成されることを確認できる。
- Compose を使用しても Docker Container / Image / Network / Volume 自体の基本概念は変わらない。

### Compose File

以下を確認する。

- `services` に複数 Service を宣言できる。
- `image` と `build` の用途の違いを説明できる。
- `ports` / `environment` / `volumes` / `networks` が、これまでの Docker CLI Option と対応していることを理解している。
- `depends_on` の基本的な役割と、Simple な定義では Readiness まで保証しないことを理解している。

### Compose 操作

以下を確認する。

- `docker compose build` から Custom Image を Build できる。
- `docker compose up` / `up -d` から複数 Service をまとめて起動できる。
- `docker compose stop` と `docker compose down` の違いを確認できる。
- `docker compose ps` / `logs` / `exec` から Compose Project の状態を確認・操作できる。
- `docker compose up -d --build` で Build と起動をまとめて実行できる。

### Network / Volume

以下を確認する。

- Compose が宣言された Network / Volume を Project Resource として作成できる。
- 同じ Network 上で Service 名 `web` を Host 名として Container 間通信できる。
- `docker compose down` では Named Volume が Default で残ることを確認できる。
- `docker compose down -v` で Volume まで削除できる。

### Environment Variable

以下を確認する。

- Dockerfile `ENV` と Compose `environment` の役割を区別できる。
- `.env` が Compose File の `${...}` の値補間に利用されることを確認できる。
- `.env` に存在する値が自動的にすべて Container Environment へ渡るわけではないことを理解している。
- `.env.example` と Local `.env` の運用上の違いを説明できる。
- `.env` を Git から除外しても Production Secrets 管理が完成するわけではないことを理解している。

## 学習ポイント

### Compose は Docker の基本概念を置き換えない

Compose を使い始めても、

```text
Image
Container
Port
Environment Variable
Volume
Network
```

の意味は変わらない。

Compose は、それらを別の概念へ置き換えるものではなく、

```text
個別の CLI 指定
↓
Compose File の宣言
```

へまとめる仕組みである。

そのため Unit 01〜08 で CLI を使って各 Resource を個別に理解したことが、そのまま Compose の理解につながる。

### Compose File は再現可能な構成を表す

長い `docker container run` Command を人が毎回正確に再入力する代わりに、Compose File に必要な設定を記述する。

```text
compose.yaml
↓
同じ構成を読み取る
↓
Compose Project を作成
```

これにより、複数 Container の関係も含めて構成を共有・再現しやすくなる。

### Project / Service / Container を混同しない

```text
Project
→ Compose Application 全体の管理単位

Service
→ Compose File 上の実行単位・定義

Container
→ Service から実際に生成される実行環境
```

今回 1 Service = 1 Container であるため見た目上近いが、概念としては分けて理解する。

### Service 名は Container 間通信の重要な名前になる

Compose では同じ Network 上の Service 名を Host 名として利用できる。

```text
client
↓
web:80
↓
web Service
```

Unit 08 で学んだ Docker Network の名前解決を、Compose が Service 単位の構成として利用しやすくしている。

後の Unit 10 では、

```text
Spring Boot
↓
PostgreSQL
```

の接続先 Host に Database Service 名を利用する。

### Compose の Network / Volume は既習 Resource と同じ

Compose File に、

```yaml
networks:
volumes:
```

と書くと新しい種類の Resource が生まれるわけではない。

Unit 07 / 08 で CLI から扱った Docker Volume / Docker Network を、Compose が Project Resource として作成・管理する。

通常の、

```bash
docker network ls
docker volume ls
```

から確認できることが、その証拠になる。

### `.env` と Container Environment を分離して考える

Compose で最も混同しやすい点の 1 つである。

```text
.env
→ Compose File を組み立てるための値

environment
→ Container へ渡す値
```

もちろん `.env` の値を `environment` で参照すれば Container へ渡せる。

```text
.env
↓
${APP_STAGE}
↓
Compose environment
↓
Container
```

しかし `.env` 自体と Container Environment は同じものではない。

### Configuration と Secret を同一視しない

Environment Variable や `.env` は Application Configuration を外から渡すために便利である。

一方で、Password / Token などの秘密情報には、

```text
Git へ含めない
Access を制御する
Build Layer へ残さない
実行 Environment に応じた Secrets 管理を利用する
```

といった追加の考慮が必要になる。

Unit 09 ではまず「Compose で Environment Variable をどう扱うか」を理解し、Secrets については専用の考え方が存在することまでを把握する。

### `depends_on` と Readiness を分ける

Container の起動順序と Application の利用可能状態は同じではない。

```text
Container Start
↓
Application Process 起動
↓
Initialization
↓
Request を受け付けられる状態
```

Simple `depends_on` はこのすべてを保証するものとして扱わない。

後に複数 Application を接続したとき、

```text
depends_on があるのに Connection Error が出る
```

という状況を理解するための重要な前提になる。

### Compose の Lifecycle を Project 単位で考える

個別 Docker CLI では Container / Network / Volume をそれぞれ操作してきた。

Compose では、

```text
docker compose up
↓
Project を起動

docker compose stop
↓
Project の Service Container を停止

docker compose down
↓
Project の Container / Network を削除
```

というように、複数 Resource の Lifecycle をまとめて操作できる。

ただし Volume や Image には別のライフサイクルがあるため、`down` ですべてが消えると考えない。

### Unit 10 へのつながり

Unit 09 では Nginx + Alpine という小さな構成で Compose の仕組み自体を学んだ。

Unit 10 ではこれを、

```text
Spring Boot
+
PostgreSQL
+
Network
+
Volume
+
Environment Variable
```

という実際の 2 Container Web Application 構成へ置き換える。

そのとき利用する、

```text
services
build / image
environment
volumes
networks
depends_on
Service 名による通信
docker compose up / down / logs / exec
```

は Unit 09 で学んだものと同じである。

Unit 09 は Compose Syntax を暗記するためではなく、Unit 10 / 11 の複数 Container Application を理解して扱うための土台である。

## 完了条件

以下を満たしたら Unit 09 を完了とする。

- Docker Compose が、CLI で個別指定してきた Docker Resource / 設定を Compose File にまとめて宣言・管理する仕組みであることを説明できる。
- Compose Project / Service / Container の役割の違いを説明できる。
- `compose.yaml` の `services` / `image` / `build` / `ports` / `environment` / `volumes` / `networks` / `depends_on` の基本的な役割を説明できる。
- `docker compose build`、`up`、`up -d`、`stop`、`down`、`ps`、`logs`、`exec` を基本的な用途に応じて使える。
- Compose が Network / Volume を Project Resource として作成し、通常の Docker Resource として確認できることを理解している。
- 同じ Compose Network 上で Service 名を Host 名として Container 間通信できる。
- Dockerfile の `ENV`、Compose の `environment`、`.env` の役割を区別できる。
- `.env` に定義した値が自動的にすべて Container Environment へ入るわけではないことを実際に確認できる。
- `.env` が Production Secrets 管理の完成形ではなく、Docker Secrets / Build Secrets など別の仕組みが存在することを認識している。
- `depends_on` の基本的な起動依存関係と Application Readiness の違いを説明できる。
- `docker compose down` と `down -v` の違いを Volume のライフサイクルと関連付けて説明できる。
- Dockerfile / Source 変更後に Compose から Rebuild し、更新した Image / Container へ反映できる。
- Unit 08 までの Docker CLI 操作と Compose File の設定を対応付け、Unit 10 の Spring Boot + PostgreSQL 構成へつながる基本構造を説明できる。

ここまで確認できれば、Unit 10「Spring Boot + PostgreSQL（2 Container 構成）」へ進む。
