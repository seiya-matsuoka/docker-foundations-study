# 04. Dockerfile と Image Build

## この項目の目的

この Unit では、Docker Hub などから取得した既存 Image をそのまま利用するだけでなく、自分で Dockerfile を記述し、その Dockerfile から新しい Image を Build できるようにする。  
Unit 01〜03 では、主に既存 Image → Container という流れを学んだ。  
この Unit ではその手前に「Dockerfile から Image を作る」という工程を追加し、次の一連の流れを自分で実行する。

```text
Dockerfile
   ↓
docker image build
   ↓
自分で Build した Image
   ↓
docker container run
   ↓
Container
   ↓
Application
```

また、Dockerfile の命令を単なる暗記対象として扱わず、Build Context、Image Layer、Build Cache、`.dockerignore` と結び付けて理解する。  
最終的には、Dockerfile や Source File を変更したときに「どこから再 Build が必要になるのか」「どこは Cache を再利用できるのか」まで大まかに説明できる状態を目指す。

## 学習内容

### Dockerfile とは

Dockerfile は、Docker Image をどのように作るかを命令として記述する Text File である。  
Base Image、Image 内へ配置する File、Build 時に実行する Command、Container 起動時の Default Command などを上から順番に定義する。

今回の Dockerfile の大きな流れは次のとおり。

```text
Nginx Image を Base にする
        ↓
作業 Directory を設定する
        ↓
Nginx の Default HTML を削除する
        ↓
自分の HTML を Copy する
        ↓
Build / Runtime 用の値を設定する
        ↓
利用予定 Port を示す
        ↓
Container 起動時に Nginx を実行する
```

Dockerfile 自体は Image でも Container でもない。

```text
Dockerfile
= Image の作り方を定義する

Image
= Container を作る Template

Container
= Image を基に作られる実行単位
```

この 3 つの方向関係を最初に固定しておく。

```text
Dockerfile
  ↓ Build
Image
  ↓ Run
Container
```

### Base Image

Docker Image は、すべてをゼロから作る必要はない。  
多くの場合、目的に合った既存 Image を Base Image として利用し、その上へ Application 固有の File や設定を追加する。

今回使用する Base Image は次である。

```text
nginx:1.30.4-alpine3.24
```

Unit 03 ではこの Image をそのまま Container として実行した。  
Unit 04 では、この Image を土台にして自分の HTML を組み込んだ新しい Image を作る。

```text
nginx:1.30.4-alpine3.24
        ↓ Base
Dockerfile の変更を追加
        ↓
unit04-nginx:1.0.0
```

Base Image を利用することで、Nginx 自体の Installation や基本構成を毎回自分で作る必要がなくなる。

### `FROM`

`FROM` は Build の土台となる Base Image を指定する命令である。

```dockerfile
FROM nginx:1.30.4-alpine3.24
```

Dockerfile の通常の Build Stage は `FROM` から始まる。  
この Unit では 1 Stage だけを使用する。  
複数の `FROM` を使う Multi-stage Build は Unit 12 で扱う。

今回も `latest` だけに依存せず、Base Image の Version を含む Tag を明示している。

### `WORKDIR`

`WORKDIR` は、それ以降の `RUN`、`COPY`、`CMD` などで基準となる作業 Directory を設定する。

```dockerfile
WORKDIR /usr/share/nginx/html
```

今回の Nginx が静的 Content を配信する Directory を基準にしている。

そのため、

```dockerfile
RUN rm -rf ./*
COPY site/ ./
```

の `./` は `/usr/share/nginx/html` を基準として扱われる。

```text
WORKDIR
/usr/share/nginx/html
       ↓
RUN / COPY の相対 Path の基準
```

Dockerfile の途中でどの Directory を操作しているのかを明確にするため、`WORKDIR` は明示的に設定する。

### `RUN`

`RUN` は、**Image Build 時**に Command を実行する命令である。

今回の Dockerfile では次を使用する。

```dockerfile
RUN rm -rf ./*
```

`WORKDIR` が `/usr/share/nginx/html` なので、Base Image に含まれる Nginx Default Content を削除している。

重要なのは実行 Timing である。

```text
docker image build
        ↓
RUN を実行
        ↓
その結果を Image に反映
        ↓
Build 完了
```

Container を起動するたびに Dockerfile の `RUN` が再実行されるわけではない。  
Container は、すでに Build 済みの Image を利用して作られる。

### `COPY`

`COPY` は、Build Context にある File / Directory を Image 内へ Copy する命令である。

```dockerfile
COPY site/ ./
```

Build Context 内の、

```text
site/
└─ index.html
```

を、`WORKDIR` である、

```text
/usr/share/nginx/html
```

へ取り込む。

Build 後の Image は概念的に次の状態になる。

```text
Image
└─ /usr/share/nginx/html
   └─ index.html
```

#### Unit 03 の Bind Mount との違い

Unit 03 では Host File を Container から直接参照した。

```text
Host File
    ↓ Bind Mount
Container から参照
```

Unit 04 の `COPY` は異なる。

```text
Host の Build Context
    ↓
docker image build
    ↓ COPY
Image の一部になる
    ↓
Container
```

`COPY` 後に Host 側の `site/index.html` を変更しても、すでに Build 済みの Image は自動的には変わらない。  
変更を反映するには再 Build が必要である。

```text
Bind Mount
→ Host の現在の内容を Container から参照

COPY
→ Build 時点の内容を Image へ固定
```

この違いは Unit 03 と Unit 04 をつなぐ重要なポイントである。

### `ARG`

`ARG` は Build 時に利用する変数を定義する。

```dockerfile
ARG APP_VERSION=1.0.0
```

Default 値は `1.0.0` である。  
Build Command から値を上書きできる。

```bash
docker image build \
  --build-arg APP_VERSION=1.0.1 \
  -t unit04-nginx:1.0.1 \
  .
```

大まかには、

```text
ARG
→ Build 時の Parameter
```

として理解する。

通常、`ARG` 自体は Container 実行時の Environment Variable として残るものではない。  
今回の Dockerfile では学習のため、その値を後続の `ENV` へ渡している。

### `ENV`

`ENV` は Image に Environment Variable を設定する。

```dockerfile
ENV APP_ENV=learning \
    APP_VERSION=${APP_VERSION}
```

この値は Image Config に保持され、その Image から作成した Container でも確認できる。

```text
Dockerfile ENV
      ↓
Image Config
      ↓
Container Environment
```

今回の関係は次のとおり。

```text
ARG APP_VERSION
Build 時の値
      ↓
ENV APP_VERSION
Image に保持
      ↓
Container でも利用可能
```

`ARG` と `ENV` の用途を完全に同一視しない。

#### `ARG` / `ENV` に Secret を入れない

Password、API Token、Private Key などの秘密情報を Dockerfile の `ARG` / `ENV` に直接設定する使い方は避ける。  
Build 情報や Image Metadata に残る可能性があるため、安全な Secret 保管場所ではない。

この Unit では Secret Mount などの専用機構までは扱わない。  
現段階では次を基本方針とする。

```text
秘密情報
→ Dockerfile に直接書かない
→ ARG / ENV に安易に埋め込まない
```

### `EXPOSE`

`EXPOSE` は、その Image がどの Port を利用する想定かを Image Metadata として示す。

```dockerfile
EXPOSE 80
```

Nginx は Port `80` を利用するため、その意図を Image 側へ記録している。

重要なのは、

```text
EXPOSE 80
≠
-p 8080:80
```

という点である。

```text
EXPOSE
→ Image が利用する Port の意図を示す

-p / --publish
→ Host Port と Container Port を実際に接続する
```

`EXPOSE` だけでは Browser から Host 経由でアクセスできるようにはならない。  
実際の Port Mapping は Unit 03 で学んだ `-p` を Container 起動時に指定する。

### `CMD`

`CMD` は Container 起動時の Default Command を指定する。

```dockerfile
CMD ["nginx", "-g", "daemon off;"]
```

Nginx を Foreground で動かし、Nginx Process が Container の主 Process として継続するようにしている。

`RUN` と `CMD` はどちらも Command を書くため混同しやすいが、Timing が異なる。

```text
RUN
→ Image Build 時

CMD
→ Container 起動時
```

この違いを必ず意識する。

### `ENTRYPOINT`

`ENTRYPOINT` も Container 起動時の挙動に関係する命令である。  
この Unit では独自の `ENTRYPOINT` は書かない。

今回使用する Nginx Official Image は Base Image 側で `ENTRYPOINT` を持っているため、自分の Image もその設定を継承する。  
Build 後に `docker image inspect` を使い、Base Image 由来の `ENTRYPOINT` と今回指定した `CMD` の両方を確認する。

大まかな理解としては、

```text
ENTRYPOINT
→ Container の中心となる実行処理

CMD
→ Default Command / Default Argument
```

とする。

両者の細かな組み合わせはこの Unit では深追いしない。

### Build

Dockerfile から Image を作る操作を Build と呼ぶ。

今回の基本 Command は次である。

```bash
docker image build -t unit04-nginx:1.0.0 .
```

各部分は次の意味を持つ。

```text
docker image build
= Dockerfile から Image を Build

-t unit04-nginx:1.0.0
= Build 結果へ Image Name / Tag を付ける

.
= 現在 Directory を Build Context として指定
```

最後の `.` は非常に重要である。

### Build Context

Build Context は、Docker Build が材料として参照できる File / Directory の範囲である。

今回 Unit Directory で、

```bash
docker image build -t unit04-nginx:1.0.0 .
```

を実行すると、`."` により現在 Directory が Build Context になる。

```text
04-dockerfile-image-build/
├─ Dockerfile
├─ .dockerignore
├─ README.md
└─ site/
   └─ index.html
```

Dockerfile の、

```dockerfile
COPY site/ ./
```

は Build Context 内の `site/` を参照している。

Build Context は「Dockerfile が置いてある場所」と完全に同じ意味ではない。  
Dockerfile の場所を別指定することもできるため、

```text
Dockerfile
= Build 手順

Build Context
= Build に渡す材料の範囲
```

と区別して考える。

### Image 名と Tag

Build した Image の Name / Tag は Dockerfile に書くのではなく、Build Command の `-t` で付ける。

```text
unit04-nginx:1.0.0
```

```text
unit04-nginx
= Image Name

1.0.0
= Tag
```

同じ Dockerfile から別 Tag の Image を作ることもできる。

```text
unit04-nginx:1.0.0
unit04-nginx:1.0.1
```

### Image Layer

Docker Image は、Base Image の上へ変更を積み重ねた構造として考える。

```text
Final Image
├─ 自分の File / 設定
├─ Build 時の変更
└─ Base Image の Layers
```

今回の Dockerfileでは、特に `RUN` や `COPY` が File System の変更を加える。

```dockerfile
RUN rm -rf ./*
COPY site/ ./
```

大まかなイメージは次のとおり。

```text
unit04-nginx
├─ COPY site/ の変更
├─ RUN rm ... の変更
└─ nginx Base Image
```

ただし、すべての Dockerfile 命令を単純に「必ず File System Layer を 1 枚増やす」と理解しない。  
`ENV`、`EXPOSE`、`CMD` などは Image Config / Metadata に関係する命令である。

この Unit では、

```text
Base Image
+ Dockerfile による差分
= 新しい Image
```

という全体構造を理解する。

### Build Cache

Docker は以前の Build 結果を Cache として再利用できる。

同じ Dockerfile / Build Context で再び、

```bash
docker image build -t unit04-nginx:1.0.0 .
```

を実行すると、変更されていない Build Step は再実行せず Cache を利用できる場合がある。

BuildKit の Output では、

```text
CACHED
```

などの表示を確認できる。

Cache の目的は、

```text
変更していない処理
→ もう一度実行しない
```

ことで Build の無駄を減らすことにある。

### Cache が無効になる流れ

Dockerfile の命令や、その Step が参照する File が変更されると、その Step の Cache を再利用できなくなる。

今回、

```dockerfile
COPY site/ ./
```

があるため `site/index.html` を変更すると `COPY` Step は再 Build が必要になる。

```text
変更なし
→ Cache 再利用

site/index.html を変更
→ COPY の Cache が無効
→ それより後ろの Step も再評価
```

Dockerfile の上の方で Cache が無効になるほど、後続 Step への影響も大きくなりやすい。

### Dockerfile の記述順と Cache

実務的には、変わりにくい処理を先に置き、頻繁に変わる Source Code を後ろで Copy することで Cache を利用しやすくする。

概念的には、

```text
変わりにくい処理
↓
Dependency Installation
↓
頻繁に変わる Source Code
```

という順序を意識する。

今回の Nginx Sample には Dependency Installation はないが、

```dockerfile
RUN rm -rf ./*
COPY site/ ./
```

という順番にしているため、HTML だけを変更した場合 `COPY` より前の変更されていない処理は Cache を利用できることを確認する。

この考え方は Unit 05 の React / Vite Container 化でより実践的になる。

### `.dockerignore`

`.dockerignore` は Build Context から不要な File / Directory を除外するための File である。

今回の `.dockerignore` では例として次を除外している。

```text
README.md
.git/
.vscode/
*.log
.env
*.pem
*.key
```

学習用 README、Git Metadata、Editor 設定、Log、Local Environment File などは今回の Nginx Image の材料ではない。

概念的には次の流れになる。

```text
Unit Directory
       ↓
.dockerignore で除外
       ↓
必要な File を中心とした Build Context
       ↓
Docker Builder
```

Build Context を小さく保つことは、

- 不要 File の転送を減らす。
- 不要 File を Build から参照できる状態にしない。
- 無関係な File 変更による影響を抑えやすくする。

といった意味を持つ。

### `.gitignore` との違い

`.gitignore` と `.dockerignore` は似た Pattern を書くが目的が違う。

```text
.gitignore
→ Git の追跡対象から除外

.dockerignore
→ Docker Build Context から除外
```

Git に Commit しないことと、Docker Build に送らないことは別の問題である。

### 秘密情報を Image に含めない

`.env`、Private Key、Credential File などを誤って `COPY . .` すると、秘密情報が Image に入り込む危険がある。

`.dockerignore` は誤混入防止の 1 つとして有効だが、

```text
.dockerignore に書けば Secret 管理は完了
```

ではない。

基本方針は次である。

```text
秘密情報を Build Context に不用意に置かない
↓
不要 File は .dockerignore で除外
↓
ARG / ENV へ秘密情報を直接埋め込まない
↓
必要なら専用 Secret 機構を使う
```

この Unit では専用 Secret 機構の実践までは扱わない。

## 使用するもの

### Base Image

```text
nginx:1.30.4-alpine3.24
```

### Build する Image

最初の Version:

```text
unit04-nginx:1.0.0
```

変更後:

```text
unit04-nginx:1.0.1
```

### Unit の File

```text
04-dockerfile-image-build/
├─ README.md
├─ Dockerfile
├─ .dockerignore
└─ site/
   └─ index.html
```

## 事前準備

Unit 03 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/04-dockerfile-image-build
```

Git Bash で次の Unit Directory へ移動した状態から操作する。

```text
units/04-dockerfile-image-build
```

現在位置と File を確認する。

```bash
pwd
ls -la
```

次が存在することを確認する。

```text
Dockerfile
.dockerignore
README.md
site/
```

`site/` も確認する。

```bash
ls site
```

```text
index.html
```

## ハンズオン

### 1. Dockerfile を読む

まず Build せず、Dockerfile の内容を確認する。

```bash
cat Dockerfile
```

命令の並びを確認する。

```text
FROM
↓
WORKDIR
↓
RUN
↓
COPY
↓
ARG
↓
ENV
↓
EXPOSE
↓
CMD
```

この時点では Syntax の暗記ではなく、

```text
Base Image
↓
Image 内の状態を変更
↓
Application File を追加
↓
Image Config を設定
↓
Container 起動時の Default を定義
```

という流れを意識する。

### 2. `.dockerignore` を確認する

```bash
cat .dockerignore
```

今回の Image Build で必要なのは主に `Dockerfile` と `site/index.html` であり、README、Git Metadata、Editor 設定、Log などは Application Image の材料ではないことを確認する。

### 3. 最初の Image を Build する

次を実行する。

```bash
docker image build \
  -t unit04-nginx:1.0.0 \
  .
```

最後の `.` を見落とさない。

```text
.
= 現在 Directory を Build Context にする
```

Build Output では、環境によって細かな表示は異なるが、次のような処理を確認できる。

```text
load build definition from Dockerfile
load .dockerignore
load build context
FROM ...
WORKDIR ...
RUN ...
COPY ...
exporting to image
```

Base Image が Local に存在しない場合は、その取得も行われる。

### 4. Build した Image を確認する

```bash
docker image ls unit04-nginx
```

次の Image が表示されることを確認する。

```text
unit04-nginx   1.0.0
```

ここで、

```text
Dockerfile
↓ Build
unit04-nginx:1.0.0
```

という流れが成立した。

### 5. Image の Environment Variable を Inspect する

```bash
docker image inspect \
  --format '{{range .Config.Env}}{{println .}}{{end}}' \
  unit04-nginx:1.0.0
```

出力の中に次が含まれることを確認する。

```text
APP_ENV=learning
APP_VERSION=1.0.0
```

`ARG APP_VERSION=1.0.0` の値が `ENV APP_VERSION=${APP_VERSION}` へ渡され、Image Config に保持されている。

### 6. `EXPOSE` を Inspect する

```bash
docker image inspect \
  --format '{{json .Config.ExposedPorts}}' \
  unit04-nginx:1.0.0
```

Port `80/tcp` が Image Config に存在することを確認する。

ここではまだ Container を起動していないため、Host Port Mapping は存在しない。  
`EXPOSE` が `-p` の代わりではないことを意識する。

### 7. `ENTRYPOINT` と `CMD` を確認する

Base Image から継承した `ENTRYPOINT` を確認する。

```bash
docker image inspect \
  --format '{{json .Config.Entrypoint}}' \
  unit04-nginx:1.0.0
```

続いて `CMD` を確認する。

```bash
docker image inspect \
  --format '{{json .Config.Cmd}}' \
  unit04-nginx:1.0.0
```

大まかに、

```text
ENTRYPOINT
→ Base Image から継承した起動処理

CMD
→ nginx -g daemon off;
```

という組み合わせになっていることを確認する。

この Unit では `ENTRYPOINT` 自体を変更しない。  
Base Image の設定が自分の Image に継承されることを確認できればよい。

### 8. Build した Image から Container を起動する

```bash
docker container run \
  -d \
  --name unit04-nginx-v1 \
  -p 8080:80 \
  unit04-nginx:1.0.0
```

今回は Docker Hub の `nginx:...` を直接実行しているのではない。

```text
自分の Dockerfile
↓
unit04-nginx:1.0.0
↓
unit04-nginx-v1 Container
```

という流れである。

### 9. Browser から HTML を確認する

Browser で次を開く。

```text
http://localhost:8080
```

次の見出しが表示されることを確認する。

```text
Unit 04 - Dockerfile Build
```

Unit 03 では Host HTML を Bind Mount したが、今回は HTML が Image 内へ `COPY` されている。

```text
Unit 03
Host HTML
↓ Bind Mount
Container

Unit 04
Host HTML
↓ Build / COPY
Image
↓
Container
```

### 10. Container 内の Environment Variable を確認する

```bash
docker container exec unit04-nginx-v1 \
  sh -c 'echo "$APP_ENV / $APP_VERSION"'
```

次が表示される。

```text
learning / 1.0.0
```

Dockerfile の `ENV` が Image を経由して Container に引き継がれていることを確認する。

### 11. `EXPOSE` と Port Mapping を整理する

Browser からアクセスできているのは、Dockerfile の、

```dockerfile
EXPOSE 80
```

だけが理由ではない。

Container 起動時に、

```text
-p 8080:80
```

を指定したため、Host Port `8080` と Container Port `80` が Mapping されている。

```text
EXPOSE 80
→ Image Metadata

-p 8080:80
→ 実際の Port Mapping
```

Unit 03 で学んだ内容と結び付ける。

### 12. Image History を確認する

```bash
docker image history unit04-nginx:1.0.0
```

自分の Dockerfile に関係する命令と、Nginx Base Image 由来の履歴が存在することを確認する。

ここでは Layer ID の暗記は不要である。

```text
自分の変更
↓
Base Image の Layers / Config
```

という積み重なりを意識する。

### 13. 同じ内容でもう一度 Build する

Dockerfile と `site/index.html` を変更せず、同じ Build をもう一度実行する。

```bash
docker image build \
  -t unit04-nginx:1.0.0 \
  .
```

Build Output で、変更されていない Step に `CACHED` などの表示が出ることを確認する。

```text
変更なし
↓
以前の Build 結果を再利用
↓
不要な再実行を減らす
```

初回 Build と 2 回目の Build を比較して、Build Cache の存在を確認する。

### 14. Host 側 HTML を変更する

Editor で次を開く。

```text
site/index.html
```

次の行を探す。

```html
<p id="message">Dockerfile から Build した Image の HTML です。</p>
```

たとえば次のように変更して保存する。

```html
<p id="message">HTML を変更して Image を再 Build しました。</p>
```

この時点で、起動中の `unit04-nginx-v1` を Browser で Reload する。

表示は自動では変わらないことを確認する。

```text
Host site/index.html を変更
        ↓
既存 Image は変わらない
        ↓
既存 Container も変わらない
```

Unit 03 の Bind Mount と違い、現在の Container は Host File を直接参照していない。

### 15. 変更後 Image を別 Tag で Build する

変更を Image に反映するため再 Build する。  
今回は `ARG APP_VERSION` も上書きする。

```bash
docker image build \
  --build-arg APP_VERSION=1.0.1 \
  -t unit04-nginx:1.0.1 \
  .
```

Build Output を確認する。

`site/index.html` が変わったため、

```dockerfile
COPY site/ ./
```

の Step は以前の Cache をそのまま利用できない。

一方、`COPY` より前にあり変更されていない Step は Cache を利用できる場合がある。

```text
FROM
→ Cache 利用可能

WORKDIR
→ Cache 利用可能

RUN rm ...
→ Cache 利用可能

COPY site/ ./
→ File が変わったため再 Build

ARG / ENV ...
→ 新しい値を反映
```

BuildKit の具体的な表示は Docker Version や内部最適化によって異なるため、完全に同じ表示になる必要はない。

### 16. 2 つの Image を比較する

```bash
docker image ls unit04-nginx
```

次の 2 つが存在することを確認する。

```text
unit04-nginx   1.0.0
unit04-nginx   1.0.1
```

別 Tag で Build したことで、変更前 / 変更後の Image を別々に保持している。

### 17. 旧 Container を削除する

```bash
docker container rm -f unit04-nginx-v1
```

Container を削除しても Image は残る。

```bash
docker image ls unit04-nginx
```

Unit 02 で学んだ Image / Container のライフサイクルの違いを再確認する。

### 18. 新しい Image から Container を起動する

```bash
docker container run \
  -d \
  --name unit04-nginx-v2 \
  -p 8080:80 \
  unit04-nginx:1.0.1
```

Browser で再度開く。

```text
http://localhost:8080
```

変更後の文章が表示されることを確認する。

```text
site/index.html を変更
↓
再 Build
↓
unit04-nginx:1.0.1
↓
新しい Container
↓
変更後 HTML
```

### 19. `--build-arg` の反映を確認する

```bash
docker container exec unit04-nginx-v2 \
  sh -c 'echo "$APP_ENV / $APP_VERSION"'
```

次が表示される。

```text
learning / 1.0.1
```

Build 時の、

```text
--build-arg APP_VERSION=1.0.1
```

が、

```text
ARG APP_VERSION
↓
ENV APP_VERSION
↓
Image
↓
Container
```

という流れで反映されたことを確認する。

### 20. Build Context と `.dockerignore` を整理する

今回の Build Command は、

```bash
docker image build -t unit04-nginx:1.0.1 .
```

である。

最後の `.` で Unit Directory を Build Context とし、その中から `.dockerignore` に一致する不要 File を除外して Builder へ渡す。

```text
Unit Directory
       ↓
.dockerignore
       ↓
Build Context
       ↓
Docker Builder
```

Build Output に `load .dockerignore` や `load build context` が表示されることを確認する。

### 21. Unit 04 の Container を削除する

```bash
docker container rm -f unit04-nginx-v2
```

確認する。

```bash
docker container ls -a \
  --filter 'name=unit04-nginx'
```

Unit 04 の Container が残っていないことを確認する。

### 22. Unit 04 で Build した Image を削除する

```bash
docker image rm \
  unit04-nginx:1.0.0 \
  unit04-nginx:1.0.1
```

確認する。

```bash
docker image ls unit04-nginx
```

表示されなくなればよい。

Base Image の `nginx:1.30.4-alpine3.24` は、自分で Build した `unit04-nginx` とは別の Image である。  
後続 Unit でも利用できるため、この Unit では無理に削除する必要はない。

## 動作・確認ポイント

### Dockerfile

以下を確認する。

- Dockerfile が Image の Build 手順を定義する File である。
- Dockerfile / Image / Container を別々のものとして説明できる。
- `FROM` で Base Image を指定している。
- `WORKDIR` が後続命令の基準 Directory になる。
- `RUN` が Build 時に実行される。
- `COPY` が Build Context の File を Image へ取り込む。
- `ENV` が Image / Container の Environment Variable に反映される。
- `EXPOSE` が Port Publish そのものではない。
- `CMD` が Container 起動時の Default Command である。

### Build

以下を確認する。

- `docker image build` で Dockerfile から Image を作成できる。
- `-t` で Image Name / Tag を付けられる。
- 最後の `.` が Build Context である。
- Build 後に `docker image ls` で自作 Image を確認できる。
- 自作 Image から Container を起動できる。

### `ARG` / `ENV`

以下を確認する。

- `ARG` が Build 時の Parameter である。
- `--build-arg` で値を上書きできる。
- `ENV` は Image Config に保持され Container でも利用できる。
- 今回は `ARG APP_VERSION` を `ENV APP_VERSION` へ渡している。
- Secret を `ARG` / `ENV` へ安易に埋め込まない理由を理解している。

### `ENTRYPOINT` / `CMD`

以下を確認する。

- Base Image から `ENTRYPOINT` が継承されていることを Inspect で確認できる。
- Dockerfile の `CMD` を Inspect で確認できる。
- `ENTRYPOINT` / `CMD` が Container 起動時の挙動に関係する。
- `RUN` とは実行 Timing が異なる。

### Build Context

以下を確認する。

- Build Context が Docker Build に渡す File 群の範囲である。
- `COPY` は Build Context 内の File を参照する。
- Build Command 最後の `.` が現在 Directory を Context に指定している。
- Dockerfile と Build Context の概念を区別している。

### Image Layer / History

以下を確認する。

- 自作 Image が Base Image の上に変更を追加した構造である。
- `RUN` / `COPY` が File System の変更を Image に追加する。
- `docker image history` で Build 履歴を確認できる。
- すべての Dockerfile 命令を単純に「File System Layer 1 枚」と考えない。

### Build Cache

以下を確認する。

- 同じ内容で再 Build すると Cache が利用される。
- `site/index.html` を変更すると `COPY` Step が再評価される。
- 変更 Step より後ろにも影響することを理解している。
- Dockerfile の記述順が Cache 効率へ影響する理由を説明できる。

### `.dockerignore`

以下を確認する。

- `.dockerignore` が Build Context から不要 File を除外する。
- README / Git Metadata / Log / Local Environment File などを Build に含める必要がない。
- Build Context を小さくする意味を説明できる。
- `.dockerignore` だけを Secret 管理手段として考えない。

### Unit 03 の Bind Mount との違い

次を説明できる。

```text
Bind Mount
Host File を Container から直接参照
→ Host 変更がそのまま見える

Dockerfile COPY
Build 時点の File を Image へ取り込む
→ Host 変更後は再 Build が必要
```

## 学習ポイント

### Dockerfile は Image の作り方を Code として残す

Container 内へ手作業で入り File を変更するだけでは、「同じ Image をもう一度どう作るか」が明確に残らない。  
Dockerfile に Build 手順を記述することで、Image の作り方を Text として Repository に残せる。

```text
手作業だけで Container を変更
→ 再現手順が残りにくい

Dockerfile
→ Build 手順を Code として残せる
```

Dockerfile は単なる便利な設定 File ではなく、実行環境を再現するための重要な設計情報でもある。

### Dockerfile → Image → Container の方向を固定する

基本関係は次である。

```text
Dockerfile
  ↓ Build
Image
  ↓ Run
Container
```

次のような逆方向の理解はしない。

```text
Container を編集
→ Dockerfile が自動更新される
```

```text
Host File を変更
→ 既存 Image が自動更新される
```

どちらも行われない。  
Image に変更を反映するには Dockerfile / Build Context を材料として再 Build する。

### Base Image は既存環境を再利用するための土台

今回 Nginx 自体を一から Install していない。

```text
Nginx Official Image
↓
自分の HTML / 設定を追加
↓
自分の Image
```

Docker では、信頼できる Base Image を利用し、その上に Application 固有の差分を追加する構成が基本になる。

### `RUN` と `CMD` は実行 Timing で区別する

Dockerfile 初学時に特に混同しやすい違いである。

```text
RUN
Build 中に実行
↓
結果が Image に反映される
```

```text
CMD
Container 起動時に利用
↓
Container の Default Command になる
```

「どちらも Command を書く」という表面的な共通点ではなく、いつ実行されるのかで区別する。

### `COPY` と Bind Mount は異なる目的を持つ

Unit 03 とのつながりとして重要である。

```text
COPY
→ Build 時点の Application File を Image に組み込む
```

```text
Bind Mount
→ Host File / Directory を Container から直接利用する
```

Application を配布・実行する Image に File を含めたい場合は `COPY` が基本になる。  
一方、開発中に Host 側 Source の変更を即時に Container へ見せたい場合は Bind Mount が適することがある。

### Build Context は Docker Build の材料範囲

Dockerfile が Recipe だとすると、Build Context はその Recipe から利用できる材料の範囲である。

```text
Dockerfile
= 作り方

Build Context
= Build に渡す材料
```

`COPY` が参照できる File も Build Context によって制約される。  
最後の `.` を「お決まりの記号」として覚えず、現在 Directory を Context として指定していることを理解する。

### Image Layer は差分を積み重ねる考え方

Docker Image は Base Image を毎回丸ごと複製した 1 枚の File と考えるより、変更を積み重ねた構造として捉える。

```text
Base Image
↓
Build による変更
↓
Application File
↓
Final Image
```

この構造が Build Cache や Image 再利用の仕組みにつながる。

### Build Cache は変更していない処理を繰り返さない

Build Cache の本質は高速化そのものではなく、

```text
同じ Input から同じ Build Step を行うなら
以前の結果を再利用できる
```

ということである。

```text
変更なし
→ Cache

変更あり
→ 再 Build
```

さらに、ある Step の Cache が無効になると、その後ろの Step にも影響する。

### Dockerfile の順序は Build 効率に影響する

変わりにくく重い処理を先に、頻繁に変わる File を後に置くと Cache を再利用しやすくなる。

たとえば後の React / Vite Application では、

```text
package.json / lock File
↓
Dependency Installation
↓
Source Code
```

という順序が重要になる。

Source Code だけを変更した場合に、Dependency Installation を毎回やり直さないためである。  
Unit 04 の小さな Nginx Sample で Cache の考え方を先に理解しておく。

### `.dockerignore` は Build Context の境界を整理する

`.dockerignore` の目的は Image Size を直接小さくすることだけではない。  
Build に不要な File を最初から Context に含めないことで、Docker Builder に渡す材料を整理する。

```text
Repository 内に存在する
≠
Docker Build に必要
```

README、Git Metadata、Editor 設定、Local Log などは Repository に必要でも Application Image の材料ではない。

### `.gitignore` と `.dockerignore` は別の判断軸

```text
.gitignore
→ Version Control の対象か
```

```text
.dockerignore
→ Docker Build の材料か
```

同じ File が両方で除外される場合もあるが、目的は異なる。  
この違いを理解すると、Repository 管理と Container Image Build を別々に考えられる。

### Secret は Image に埋め込まない

Docker Image は Local だけでなく Registry へ Push して共有できる。  
そのため Image 内に Credential や Private Key を含めると、意図しない相手へ渡る可能性がある。

```text
Application Code / Runtime
→ Image に含める

Password / Token / Private Key
→ Image に埋め込まない
```

`.dockerignore` は誤混入を防ぐ補助であり、Secret を安全に渡す専用機構の代わりではない。

### `ARG` と `ENV` は Scope で考える

```text
ARG
→ Build を Parameterize する
```

```text
ENV
→ Image / Container の Environment を定義する
```

今回の `APP_VERSION` は学習のため `ARG` から `ENV` へ明示的に渡している。  
「ARG を指定したから自動的に Runtime に残る」と理解しない。

### `EXPOSE` と `-p` は設計情報と実際の接続

```text
EXPOSE 80
→ この Image は Port 80 を利用する想定
```

```text
-p 8080:80
→ Host 8080 と Container 80 を実際に接続
```

Dockerfile 内の Metadata と Container 起動時の Runtime 設定を区別する。

### 今後の Unit へのつながり

Unit 04 で学ぶ Dockerfile / Build Context / Cache は、この後の Application Container 化の基礎になる。

```text
Unit 04
HTML + Nginx
Dockerfile の基本
        ↓
Unit 05
React / Vite を Image 化
        ↓
Unit 06
Spring Boot を Image 化
        ↓
Unit 10〜11
複数 Application Container を組み合わせる
```

後続 Unit では Source Code、Dependency、Build Artifact などが増える。  
そのときも基本となる考え方は、

```text
何を Base にするか
何を Image に Copy するか
Build 時に何を行うか
Container 起動時に何を行うか
Build Context に何を含めるか
Cache をどう活かすか
```

である。

## 完了条件

以下を満たしたら Unit 04 を完了とする。

- Dockerfile / Image / Container の役割と、`Dockerfile → Build → Image → Container` の流れを説明できる。
- Base Image と `FROM`、`WORKDIR`、`RUN`、`COPY` の基本的な役割を説明できる。
- `RUN` と `CMD` の実行 Timing を区別し、`ENTRYPOINT` についても Container 起動時の挙動に関係することを大まかに理解している。
- `ARG` と `ENV` の基本的な違いを説明し、`--build-arg` を使って Build 時の値を変更できる。
- `EXPOSE` と `-p` / Port Publishing の役割の違いを説明できる。
- `docker image build` を使い、Image Name / Tag を付けて Dockerfile から Image を Build できる。
- Build Context の意味を説明し、Dockerfile と Build Context の役割を区別できる。
- 自分で Build した Image から Container を起動し、Image 内へ `COPY` した Content を確認できる。
- `COPY` と Bind Mount の違いを説明し、Host 側 File の変更を Image へ反映するには再 Build が必要であることを理解している。
- Dockerfile や Build Context の File を変更し、再 Build して変更後の Image を作成できる。
- Image Layer と `docker image history` の基本的な意味を理解している。
- Build Cache の役割を説明し、変更された Step と後続 Step の Cache への影響を大まかに説明できる。
- Dockerfile の記述順が Build Cache の効率に影響する理由を説明できる。
- `.dockerignore` の目的と `.gitignore` との違いを説明し、不要 File や秘密情報を Build Context / Image へ不用意に含めない考え方を理解している。

ここまで確認できれば、Unit 05「React / Vite Application の Docker 化」へ進む。
