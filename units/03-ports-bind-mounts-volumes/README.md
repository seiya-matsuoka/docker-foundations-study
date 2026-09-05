# 03. Port・Bind Mount・Volume

## この項目の目的

この Unit では、Host と Container の境界をまたぐ代表的な 3 つの仕組みとして、Port / Bind Mount / Volume を学ぶ。  
Port は「通信」、Bind Mount は「Host 上の File / Directory の共有」、Volume は「Docker が管理する永続データ」という、それぞれ異なる問題を解決するための仕組みである。  
Nginx と小さな HTML、Alpine Linux を使って実際に操作し、単にコマンドを実行できるだけではなく、「なぜその仕組みが必要なのか」「何と何を接続しているのか」「他の仕組みとどう違うのか」まで説明できる状態を目指す。

Unit 01〜02 では、Image / Container の関係や Container のライフサイクルを確認した。  
この Unit では、その Container が Host から分離されたままでは、通信・File・Data をどのように扱うのか、という次の段階へ進む。

```text
Unit 01〜02
Image / Container
      ↓
Container を作成・起動できる
      ↓
Unit 03
Host と Container の間をどうつなぐか
```

## 学習内容

### Host と Container の境界

Container は Host から分離された実行環境である。  
Unit 02 までの操作では、Container を作成・起動し、Container 内の Process や File System を確認した。  
一方、実際の Application では Container 内だけで完結せず、Host や他の環境との間で通信したり、File を共有したり、データを長期間保持したりする必要がある。

この Unit で扱う 3 つは、それぞれ次の境界を扱う。

```text
通信
Host
  ↕
Container
→ Port

File / Directory
Host
  ↕
Container
→ Bind Mount

永続データ
Container
  ↕
Docker 管理 Storage
→ Volume
```

Port / Bind Mount / Volume はいずれも Host と Container の境界に関係するため似て見えるが、解決する問題は異なる。  
この違いを最初から意識して学習する。

### Port

#### Container 内部の Port

Web Server や API Server などの Network Application は、OS 上の特定 Port で通信を待ち受ける。  
たとえば今回使用する Nginx は、Container 内部で HTTP Request を Port `80` で受け付ける。

```text
Nginx Container
└─ Nginx Process
   └─ Port 80 で待ち受け
```

ここで重要なのは、この `80` が **Container 側の Network 空間にある Port** だということである。  
Windows Host の Port `80` と同じものではない。

Container は Network 的にも分離されているため、Container 内で Nginx が Port `80` を Listen していることと、Windows Host の Browser からその Port へアクセスできることは別問題である。

#### Host 側の Port

Host 側にも独立した Port が存在する。  
今回の Host は Windows なので、Host Port とは Windows 側で通信を受け付ける入口である。

たとえば Host Port `8080` を使う場合、

```text
Windows Host
└─ Port 8080
```

という入口を作ることになる。

Host Port と Container Port は同じ番号である必要はない。

```text
Host : 8080
Container : 80
```

のように異なる番号同士を対応付けられる。

#### Port Mapping / Port Publishing

Container 内の Service へ Host 側からアクセスさせるため、Docker では Container Port を Host 側へ Publish できる。  
Docker CLI では `-p` または `--publish` を使用する。

基本形式は次のとおり。

```text
HOST_PORT:CONTAINER_PORT
```

今回使用する、

```text
8080:80
```

であれば、

```text
Windows Host : 8080
        ↓
Docker による Port Mapping
        ↓
Nginx Container : 80
```

という対応になる。

コマンドでは次のように指定する。

```bash
docker container run -p 8080:80 ...
```

```text
-p 8080:80
   │    │
   │    └─ Container Port
   └────── Host Port
```

Port Mapping は、Container 内の Nginx が Listen している Port `80` を `8080` に変更する操作ではない。  
Nginx は Container 内で引き続き Port `80` を使用し、Docker が Host Port `8080` に届いた通信を Container Port `80` へ転送する。

#### Port を Publish していない状態

Container 内で Process が Port を Listen していても、Host 側へ Publish していなければ、Host の任意の Port とその Container Port の対応関係は作られていない。

```text
Windows Host
    ×
    │ Publish 設定なし
    ×
Container
└─ Nginx : 80
```

これは「Nginx が動いていない」という意味ではない。  
Container 内では Nginx が正常に動いていても、Host から利用するための Port Mapping がない状態である。

この違いを確認するため、ハンズオンでは最初に Port を Publish せず Nginx を起動し、その後 `-p 8080:80` を付けて起動する。

#### Host → Container のアクセス

Port を Publish すると、Host 上の Application から Container 内の Service へアクセスできる。

今回の Browser Access は次の経路を通る。

```text
Browser
  ↓
http://localhost:8080
  ↓
Windows Host
  ↓ Host Port 8080
Docker Port Mapping
  ↓ Container Port 80
Nginx Container
  ↓
Nginx Process
```

Browser が直接 Container の Port `80` を開いているのではない。  
Browser は Host Port `8080` へ接続し、Docker がその通信を Container Port `80` へ転送している。

#### `localhost`

`localhost` は「常に Windows PC を意味する特別な名前」ではなく、**その Network Application が実行されている環境自身**を指す。

今回 Browser は Windows Host 上で動いているため、

```text
Browser から見た localhost
= Windows Host
```

となる。

したがって、

```text
http://localhost:8080
```

は、

```text
Windows Host の Port 8080
```

へアクセスしている。

一方、後の Unit で Container 内から `localhost` を使った場合、

```text
Container 内 Process から見た localhost
= その Container 自身
```

となる。

この違いは Docker Network を扱う Unit 08 で特に重要になる。  
Unit 03 の段階では、「`localhost` はどこから見ているかで対象が変わる」と理解しておく。

### Mount とは

Bind Mount と Volume を理解する前に、Mount という考え方を整理する。

Mount は、ある Storage や File / Directory を、別の File System 上の特定 Path から利用できるようにする仕組みである。  
Docker では、Host 上の Directory や Docker が管理する Volume を Container 内の Path へ Mount できる。

概念的には次のようになる。

```text
Mount 元
   ↓
Container 内の Mount 先 Path
```

Container 内の Application から見ると、その Mount 先は通常の File / Directory のように扱える。

```text
Container
└─ /data
   └─ message.txt
```

しかし `/data` の実体がどこにあるかは Mount の種類によって異なる。

```text
Bind Mount
Host Directory
    ↓
Container /data

Volume
Docker Volume
    ↓
Container /data
```

Container 内から見ると同じ `/data` でも、外側で誰が管理しているかが異なる。

### Bind Mount

#### Bind Mount とは

Bind Mount は、Host 上にすでに存在する File / Directory を Container 内の Path へ直接 Mount する仕組みである。

今回使用する構成は次のとおり。

```text
Host
└─ bind-mount-site/
   └─ index.html
        │
        │ Bind Mount
        ▼
Container
└─ /usr/share/nginx/html/
   └─ index.html
```

Mount 元は Repository 内の Host Directory である。

```text
units/03-ports-bind-mounts-volumes/bind-mount-site
```

Mount 先は Nginx Container 内の次の Directory である。

```text
/usr/share/nginx/html
```

Nginx はこの Directory にある HTML を配信するため、Host 側 HTML を Mount すると、その HTML を Nginx から配信できる。

#### Host 上の File / Directory を Container から利用する

Bind Mount の特徴は、Host と Container がそれぞれ別 Copy を持つのではなく、Host 側 Path を Container から参照することである。

```text
Host の index.html
        ↑↓
    Bind Mount
        ↑↓
Container から見える index.html
```

そのため、Host 側で File を編集すると、Container 側から見える内容も変わる。  
通常、この変更のために Image を Rebuild したり Container を作り直したりする必要はない。

この性質は、開発時に Source Code を Host の Editor で編集し、Container 内の Application からその変更を参照するような使い方と相性がよい。

#### Host 側の変更が反映される理由

Bind Mount では、

```text
Host File
→ Copy
→ Container File
```

という一度きりの Copy をしているわけではない。

Host Directory を Container File System の特定 Path へ Mount しているため、Container からは Host 側の現在の内容が見える。

```text
Host で編集
   ↓
Host File が変更
   ↓
同じ Mount を Container が参照
   ↓
Container 側からも変更後内容が見える
```

ハンズオンでは `index.html` を Host 側で変更し、Container 再作成なしで Browser 表示が変わることを確認する。

#### Mount 先に元々あった File

Mount 先の Container Directory に元々 File が存在する場合、その Path へ Bind Mount すると、Mount している間は元の File が Mount された内容によって隠れて見えなくなる。

今回の Nginx Image には元々 Default Page 用 File が存在する。  
しかし、

```text
/usr/share/nginx/html
```

へ Host の `bind-mount-site` を Mount すると、Nginx からは Host 側 Directory の内容が見える。

```text
Mount 前
Container /usr/share/nginx/html
└─ Nginx Default Content

Mount 後
Container /usr/share/nginx/html
└─ Host の bind-mount-site の内容が見える
```

元の Default Content が Image から削除されたわけではない。  
Bind Mount によってその Path 上で見えなくなっているだけである。  
Container を削除し、Bind Mount なしで新しい Nginx Container を起動すれば、再び Image に含まれる Default Content が見える。

#### Bind Mount が向いている用途

Bind Mount は Host 側の File を直接扱いたい場合に向いている。

代表的な用途は次のとおり。

- Host の Editor で編集する Source Code を Container から利用する。
- Host で管理している設定 File を Container に渡す。
- Container が生成した File を Host 側でも直接扱う。
- Build や Test 用に Host の Source を一時的に Container へ渡す。

一方、Host Directory 構成に依存するため、特定 Host Path に依存させたくない Application Data の永続化では Volume の方が適している場合が多い。

### `--mount`

Docker では Bind Mount / Volume の指定に `--mount` と `-v` / `--volume` を利用できる。  
この学習では、Mount の Type / Source / Destination を読み取りやすくするため `--mount` を使用する。

Bind Mount の基本形は次のとおり。

```text
--mount type=bind,src=HOST_PATH,dst=CONTAINER_PATH
```

今回の場合、

```text
type=bind
src=$(pwd)/bind-mount-site
dst=/usr/share/nginx/html
```

という関係になる。

また Nginx は HTML を読むだけでよいため、`readonly` を付ける。

```text
--mount type=bind,...,readonly
```

これにより Container 内の Process から Mount 元へ書き込ませない。

### Container の Writable Layer

Volume の必要性を理解するためには、Container 自体が持つ Writable Layer との違いを知る必要がある。

Image は基本的に Read-only の Layer から構成される。  
Container を作成すると、その Image の上に Container 固有の書き込み可能な Layer が追加される。

```text
Container
├─ Writable Layer
│  └─ Container 実行中の変更
└─ Image Layers
   └─ 元の File
```

Container 内で通常の Path に File を作成すると、その変更はその Container の Writable Layer に保存される。

重要なのは、この Writable Layer が **その Container 固有**だということである。

```text
Container A
└─ /tmp/data.txt

Container B
└─ Container A の /tmp/data.txt は存在しない
```

同じ Image から作った Container でも Writable Layer は共有されない。

#### Container を削除した場合

Container を削除すると、その Container 固有の Writable Layer も失われる。

```text
Image
  ↓
Container A
└─ Writable Layer
   └─ data.txt

Container A を削除
       ↓
Writable Layer も削除
       ↓
data.txt も失われる
```

そのため、Database Data や User Upload など「Container を作り直しても残す必要があるデータ」を Container 固有の Writable Layer だけに置くのは適切ではない。

Container は作り直せる実行単位として扱い、永続データは Container のライフサイクルから分離する。

### Volume

#### Docker Volume とは

Volume は Docker が作成・管理する Storage である。  
Container の Writable Layer とは別に存在し、Container へ Mount して利用する。

```text
Docker Volume
    │
    │ Mount
    ▼
Container
└─ /data
```

Volume に書き込んだ File は Container の Writable Layer ではなく Volume 側に保存される。

```text
Container
└─ /data/message.txt
        │
        ↓
Named Volume
└─ message.txt
```

Container 内の Application から見ると通常の Directory / File だが、その実体は Container のライフサイクルから分離されている。

#### 「Docker が管理する」とは

Bind Mount ではユーザー自身が Host Path を指定する。

```text
C Drive 上の Repository Directory
        ↓
Bind Mount
```

Volume ではユーザーは Volume 名を扱い、実際の保存場所の管理を Docker に任せる。

```text
unit03-data
    ↓
Docker が Storage Location を管理
    ↓
Container へ Mount
```

`docker volume inspect` では Docker が管理する `Mountpoint` を確認できるが、通常はその Host Path を直接編集するのではなく、Volume を Container へ Mount してデータを扱う。

#### Named Volume

明示的な名前を付けた Volume を Named Volume と呼ぶ。

この Unit では、

```text
unit03-data
```

を使用する。

基本操作は次のとおり。

```text
docker volume create
docker volume ls
docker volume inspect
docker volume rm
```

Container へ Mount する場合は、

```text
--mount type=volume,src=VOLUME_NAME,dst=CONTAINER_PATH
```

と指定する。

#### Volume のライフサイクル

Volume の大きな特徴は、Container とは独立したライフサイクルを持つことである。

```text
Volume 作成
   ↓
Container A へ Mount
   ↓
Data を保存
   ↓
Container A 削除
   ↓
Volume は残る
   ↓
Container B へ同じ Volume を Mount
   ↓
Data を再利用
   ↓
不要になったら Volume 自体を削除
```

Container 削除と Volume 削除は別操作である。

```text
docker container rm
≠
docker volume rm
```

この違いによって、Application Container を新しく作り直しても、同じ Volume を Mount すれば以前のデータを引き継げる。

#### 別 Container から同じ Volume を利用する意味

Volume は特定 Container の一部ではないため、別 Container に同じ Volume を Mount できる。

```text
Container A
   ↓ write
unit03-data
   ↓ read
Container B
```

ハンズオンでは、

1. Writer Container が Volume に File を書く。
2. Writer Container を削除する。
3. Reader Container に同じ Volume を Mount する。
4. 同じ File を読み出す。

という順番で確認する。

これにより「Container を残しているから Data が残った」のではなく、「Data が Container の外にある Volume に保存されているから残った」ことを確認する。

### Bind Mount と Volume の違い

Bind Mount と Volume は、どちらも Container の Writable Layer 外へデータを置くために使える。  
ただし、管理主体と用途が異なる。

| 観点                        | Bind Mount                | Volume                        |
| --------------------------- | ------------------------- | ----------------------------- |
| 管理主体                    | Host / ユーザー           | Docker                        |
| Mount 元                    | Host の具体的な Path      | Docker Volume                 |
| Host から直接編集           | しやすい                  | 基本的に直接操作しない        |
| Container 削除後            | Host File は残る          | Volume は残る                 |
| Host Directory 構成への依存 | ある                      | 比較的小さい                  |
| 主な用途                    | Source / 設定 File の共有 | Application / DB の永続データ |

大まかな判断軸は次のとおり。

```text
Host 側で直接 File を見たい・編集したい
→ Bind Mount

Container が使う Data を Docker に管理させたい
→ Volume
```

これは絶対的なルールではないが、基礎段階での使い分けとして重要である。

### Port / Bind Mount / Volume の全体整理

この Unit の 3 つを並べると次のようになる。

```text
Port
目的: Host と Container の通信をつなぐ

Bind Mount
目的: Host の File / Directory を Container から利用する

Volume
目的: Container から独立した永続データを保持する
```

同じ Nginx Container で Port と Bind Mount を同時に使用できる。

```text
Browser
  ↓
Port Mapping
  ↓
Nginx Container
  ↓
Bind Mount
  ↓
Host HTML
```

つまり、1 つの Container に対して複数の異なる仕組みが、それぞれ別の役割を担当する。

## 使用するもの

### Nginx

```text
nginx:1.30.4-alpine3.24
```

Port Mapping と Bind Mount の確認に使用する。

### Alpine Linux

```text
alpine:3.24.1
```

Container Writable Layer と Named Volume の違いを確認するために使用する。

### Bind Mount 用 HTML

```text
bind-mount-site/
└─ index.html
```

Host 側にあるこの Directory を Nginx Container の `/usr/share/nginx/html` へ Mount する。

## 事前準備

Unit 02 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/03-ports-bind-mounts-volumes
```

Git Bash で次の Unit Directory へ移動した状態から操作する。

```text
units/03-ports-bind-mounts-volumes
```

現在位置と File を確認する。

```bash
pwd
ls bind-mount-site
```

`index.html` が表示されればよい。  
Host Port `8080` が使用中の場合は、使用中の Process / Container を停止する。停止できない場合は `8081` など別 Port に読み替える。

## ハンズオン

### 1. Nginx Image を取得する

Unit 02 の最後に Nginx Image を削除している場合は、再度取得する。

```bash
docker image pull nginx:1.30.4-alpine3.24
```

確認する。

```bash
docker image ls nginx:1.30.4-alpine3.24
```

この後の Port / Bind Mount の確認では同じ Nginx Image から複数 Container を作る。

### 2. Port を Publish せず Nginx Container を起動する

Port Mapping が何を追加しているのか理解するため、まず Publish していない状態を確認する。

```bash
docker container run \
  -d \
  --name unit03-nginx-no-port \
  nginx:1.30.4-alpine3.24
```

Running 状態を確認する。

```bash
docker container ls
```

Nginx は Container 内部で正常に動いている。  
しかし Host Port との Mapping はまだ存在しない。

確認する。

```bash
docker container port unit03-nginx-no-port
```

Publish された Port がないため、Host Port との対応は表示されない。

この時点の状態は次のとおり。

```text
Windows Host
    │
    │ Host Port との Mapping なし
    ×
Nginx Container
└─ Port 80 で Nginx は動作中
```

「Port を Publish していない」と「Container 内で Port を Listen していない」は別であることを意識する。

確認後に削除する。

```bash
docker container rm -f unit03-nginx-no-port
```

### 3. Host Port `8080` を Container Port `80` へ Publish する

同じ Nginx Image から、今度は Port Mapping を指定して Container を作る。

```bash
docker container run \
  -d \
  --name unit03-nginx-port \
  -p 8080:80 \
  nginx:1.30.4-alpine3.24
```

今回追加した設定は次である。

```text
-p 8080:80
```

```text
Host : 8080
     ↓
Docker
     ↓
Container : 80
```

確認する。

```bash
docker container ls
docker container port unit03-nginx-port
```

`docker container ls` の `PORTS` や `docker container port` の出力から、Container Port `80` が Host 側へ Publish されていることを確認する。

### 4. Browser から Container の Nginx へアクセスする

Windows の Browser で次を開く。

```text
http://localhost:8080
```

Nginx の Default Page が表示されることを確認する。

この通信を、単に「Docker にアクセスした」と考えず、経路に分解する。

```text
Browser
  ↓
localhost
  ↓
Windows Host
  ↓ Port 8080
Docker Port Mapping
  ↓ Port 80
Nginx Container
  ↓
Nginx Process
```

ここで Browser から見た `localhost` は Windows Host である。

### 5. Port Mapping を Inspect する

次を実行する。

```bash
docker container inspect unit03-nginx-port
```

すべての JSON を理解する必要はない。  
Port Mapping に関する部分だけ確認する場合は次を使う。

```bash
docker container inspect \
  --format '{{json .NetworkSettings.Ports}}' \
  unit03-nginx-port
```

Port Mapping が単なる Browser 側の設定ではなく、Container 作成時に Docker が保持している Network 設定であることを確認する。

確認後、Container を削除する。

```bash
docker container rm -f unit03-nginx-port
```

### 6. Bind Mount 用 HTML を Host 側で確認する

次を実行する。

```bash
cat bind-mount-site/index.html
```

この File は Container Image 内ではなく Repository 内、つまり Host 側に存在する。

```text
Windows Host
└─ docker-foundations-study
   └─ units
      └─ 03-ports-bind-mounts-volumes
         └─ bind-mount-site
            └─ index.html
```

次の操作では、この Host Directory を Nginx Container 内へ Mount する。

### 7. Host Directory を Nginx Container へ Bind Mount する

```bash
docker container run \
  -d \
  --name unit03-nginx-bind \
  -p 8080:80 \
  --mount type=bind,src="$(pwd)/bind-mount-site",dst=/usr/share/nginx/html,readonly \
  nginx:1.30.4-alpine3.24
```

`--mount` を分解する。

```text
type=bind
= Bind Mount

src="$(pwd)/bind-mount-site"
= Host 側 Mount 元

dst=/usr/share/nginx/html
= Container 側 Mount 先

readonly
= Container 側から Mount 元へ書き込ませない
```

この Container では同時に 2 種類の接続を作っている。

```text
Network
Host 8080
  ↓
Container 80

File
Host bind-mount-site
  ↓
Container /usr/share/nginx/html
```

### 8. Browser で Bind Mount された HTML を確認する

Browser で再度開く。

```text
http://localhost:8080
```

Nginx Default Page ではなく、次の見出しが表示されることを確認する。

```text
Unit 03 - Bind Mount
```

これは Image 内の Default HTML を編集した結果ではない。  
Host の `bind-mount-site` が Nginx の Document Root に Mount され、その内容が見えている。

### 9. Host 側 HTML を変更する

Editor で次の File を開く。

```text
bind-mount-site/index.html
```

次の行を変更する。

```html
<p id="message">Host 側の HTML を Container から参照しています。</p>
```

たとえば次のようにする。

```html
<p id="message">Host 側の変更が Bind Mount 経由で反映されました。</p>
```

保存後、Container の Build や再作成は行わず Browser を Reload する。  
変更後の文章が表示されることを確認する。

ここで起きていることは次である。

```text
Host File を編集
   ↓
Host File 自体が更新
   ↓
Bind Mount 先から同じ内容が見える
   ↓
Nginx が更新後 HTML を配信
```

### 10. Container 内から Bind Mount 先を確認する

```bash
docker container exec unit03-nginx-bind \
  cat usr/share/nginx/html/index.html
```

Host 側で編集した内容と同じものが表示されることを確認する。

さらに Mount 情報を確認する。

```bash
docker container inspect \
  --format '{{json .Mounts}}' \
  unit03-nginx-bind
```

少なくとも次を意識する。

```text
Type
Source
Destination
RW
```

概念的には次の対応である。

```text
Type
= bind

Source
= Host 側 bind-mount-site

Destination
= /usr/share/nginx/html

RW
= false
```

### 11. Bind Mount Container を削除する

```bash
docker container rm -f unit03-nginx-bind
```

Container を削除しても Host 側 File は残る。

```bash
ls bind-mount-site
```

`index.html` が存在することを確認する。

Bind Mount では Container が Host File の所有者になっているわけではない。  
Container から Host Path を利用していたため、Container の削除と Host File の削除は別である。

### 12. Container Writable Layer にだけ File を作る

Volume と比較するため、まず Container 固有領域へ File を保存する。

```bash
docker container run \
  -d \
  --name unit03-container-data \
  alpine:3.24.1 \
  sleep 300
```

Container 内へ File を作る。

```bash
docker container exec unit03-container-data \
  sh -c 'echo "container only data" > /tmp/container-only.txt'
```

確認する。

```bash
docker container exec unit03-container-data \
  cat tmp/container-only.txt
```

```text
container only data
```

この File は Bind Mount や Volume に保存していないため、`unit03-container-data` の Writable Layer に存在する。

Container を削除する。

```bash
docker container rm -f unit03-container-data
```

同じ Alpine Image から別 Container を作り、File を確認する。

```bash
docker container run \
  --rm \
  alpine:3.24.1 \
  sh -c 'if [ -f /tmp/container-only.txt ]; then cat /tmp/container-only.txt; else echo "file not found"; fi'
```

次が表示される。

```text
file not found
```

同じ Image から作ったからといって、削除済み Container の Writable Layer が新しい Container へ引き継がれるわけではない。

### 13. Named Volume を作成する

次に Container のライフサイクルから独立した Storage を作る。

```bash
docker volume create unit03-data
```

確認する。

```bash
docker volume ls
```

`unit03-data` が存在することを確認する。

### 14. Volume の詳細情報を確認する

```bash
docker volume inspect unit03-data
```

主に次の情報を確認する。

- `Name`
- `Driver`
- `Mountpoint`

`Mountpoint` は Docker が Volume Data を管理している場所を示す。  
今回の学習では、その Path を Host 側から直接編集せず、Container へ Volume を Mount して利用する。

### 15. Named Volume を Container へ Mountして Data を書く

```bash
MSYS_NO_PATHCONV=1 docker container run \
  --name unit03-volume-writer \
  --mount type=volume,src=unit03-data,dst=/data \
  alpine:3.24.1 \
  sh -c 'echo "Unit 03 persistent data" > /data/message.txt && cat /data/message.txt'
```

次が表示される。

```text
Unit 03 persistent data
```

Mount の意味は次のとおり。

```text
type=volume
= Volume Mount

src=unit03-data
= Docker が管理する Named Volume

dst=/data
= Container 内の Mount 先
```

Container から見ると `/data/message.txt` だが、Data は Container Writable Layer ではなく `unit03-data` に保存される。

### 16. Writer Container を削除する

Container を確認する。

```bash
docker container ls -a \
  --filter 'name=unit03-volume-writer'
```

削除する。

```bash
docker container rm unit03-volume-writer
```

ここで消したものは Container だけである。

Volume が残っていることを確認する。

```bash
docker volume ls
```

```text
unit03-data
```

この状態が、

```text
Container のライフサイクル
≠
Volume のライフサイクル
```

という意味である。

### 17. 別 Container から同じ Volume の Data を読む

新しい Container へ同じ Volume を Mount する。

```bash
MSYS_NO_PATHCONV=1 docker container run \
  --name unit03-volume-reader \
  --mount type=volume,src=unit03-data,dst=/data \
  alpine:3.24.1 \
  cat /data/message.txt
```

次が表示される。

```text
Unit 03 persistent data
```

Writer Container はすでに存在しない。  
それでも Data が読めるのは、Data の保存先が Writer Container ではなく Volume だからである。

```text
Writer Container
   ↓ write
Named Volume
unit03-data
   ↓ Writer Container 削除後も保持
Reader Container
   ↓ read
```

### 18. Reader Container を削除する

```bash
docker container rm unit03-volume-reader
```

再度 Volume を確認する。

```bash
docker volume ls
```

Container が 1 つも残っていなくても `unit03-data` が存在することを確認する。

### 19. Named Volume 自体を削除する

この Unit では学習用 Data なので Volume 自体も削除する。

```bash
docker volume rm unit03-data
```

確認する。

```bash
docker volume ls
```

`unit03-data` が表示されなくなればよい。

ここで初めて Volume のライフサイクルも終了した。

```text
Container A 作成
↓
Volume 使用
↓
Container A 削除

Container B 作成
↓
同じ Volume 使用
↓
Container B 削除

Volume はまだ存在
↓
docker volume rm
↓
Volume 削除
```

## 動作・確認ポイント

### Port

以下を確認する。

- Nginx は Container 内部の Port `80` で動作する。
- Container Port と Host Port は別々の Network 空間にある。
- Port を Publish していなくても Container 内の Nginx は Running である。
- `-p 8080:80` では `8080` が Host Port、`80` が Container Port である。
- `http://localhost:8080` で Host から Nginx Container へアクセスできる。
- `docker container port` で Publish 状態を確認できる。
- Port Mapping は Nginx 自体の Listen Port を変更する操作ではない。

### `localhost`

以下を確認する。

- Browser から見た `localhost` は Windows Host である。
- Browser → Host Port → Container Port → Nginx という経路を説明できる。
- `localhost` は「どこから見ているか」によって指す環境が変わることを理解している。

### Bind Mount

以下を確認する。

- Mount が「Mount 元を Container 内の特定 Path から利用できるようにする」仕組みであることを理解している。
- Host 側 `bind-mount-site` を Container の `/usr/share/nginx/html` へ Mount できる。
- Host 側 HTML が Nginx から配信される。
- Host 側 File の変更が Container 再作成なしで反映される。
- Container 内から Mount された File を確認できる。
- Container を削除しても Host File は残る。
- Bind Mount 先では元々 Container 側にあった Content が Mount 中は隠れて見えることを理解している。
- `readonly` によって Container 側から書き込ませない設定にできる。

### Container Writable Layer

以下を確認する。

- Container 内へ通常作成した File は Container 固有の Writable Layer に置かれる。
- 同じ Image から別 Container を作っても Writable Layer は共有されない。
- Container を削除すると Writable Layer の Data も失われる。
- 永続 Data を Container 固有領域だけに置かない理由を説明できる。

### Volume

以下を確認する。

- Volume が Docker 管理の Storage であることを理解している。
- `docker volume create` / `ls` / `inspect` / `rm` を使用できる。
- Named Volume を Container の `/data` へ Mount できる。
- Volume へ保存した Data が Container Writable Layer とは別に存在することを理解している。
- Container を削除しても Volume は残る。
- 別 Container へ同じ Volume を Mountすると保存済み Data を読める。
- Container と Volume のライフサイクルを別々に説明できる。

## 学習ポイント

### Port は Container の Network Isolation を越える入口を作る

Container 内で Service が動いていることと、Host からその Service を利用できることは同じではない。

```text
Container 内で Nginx Running
≠
Host からアクセス可能
```

Host から利用するためには、必要な Port を Publish する。

```text
Host Port
  ↓
Port Mapping
  ↓
Container Port
```

この考え方は今後 Spring Boot や React を Container 化した際にも同じである。

### `localhost` は視点を持って考える

Docker では「どこから通信しているか」が重要である。

```text
Windows Browser の localhost
→ Windows Host

Container 内 Process の localhost
→ その Container
```

Unit 03 では Host → Container の通信だけを扱うが、Unit 08 では Container → Container の通信を扱う。  
そこで今回の `localhost` の考え方が再び重要になる。

### Mount は Container File System に外部 Storage を接続する考え方

Bind Mount と Volume の両方に共通するのは、外側にある Storage を Container 内 Path から使えるようにする点である。

```text
外部 Storage
  ↓ Mount
Container Path
```

違いは Mount 元である。

```text
Bind Mount
→ Host Path

Volume
→ Docker Volume
```

この共通点と違いをセットで理解する。

### Bind Mount は Host との File 共有

Bind Mount では Host Path が中心になる。

```text
Host で編集
  ↓
同じ File を Container から参照
```

そのため開発中の Source Code や設定 File など、Host 側でも積極的に扱いたいものに向いている。

一方、特定 Host Path に依存することも Bind Mount の特徴である。

### Volume は Container と Data の寿命を分離する

Volume の本質は単に「Data を保存できる」ことではない。

```text
Container は削除できる
Data は残したい
```

という異なるライフサイクルを分離することにある。

```text
Application 実行環境
→ Container

Application の永続 Data
→ Volume
```

Container を作り直せる構成にするために重要な考え方である。

### Container Writable Layer と Volume を区別する

Container 内で File を作れたからといって、その File が永続化されているとは限らない。

```text
Container Writable Layer
→ Container 固有
→ Container 削除で失われる

Volume
→ Container 外部
→ Container 削除後も残る
```

Database Data を扱う Unit 07 では、この違いが実際の Database の永続化につながる。

### Bind Mount と Volume は「どちらが上位」ではない

Bind Mount と Volume は競合する 2 つの技術ではなく、用途が異なる。

```text
Host 側で直接編集・管理する必要がある
→ Bind Mount

Docker Application の Data として保持したい
→ Volume
```

状況によって適切な方を選ぶ。

### Port / Bind Mount / Volume の役割を混同しない

今回の 3 つを一文で整理すると次のようになる。

```text
Port
→ Network 通信の入口を作る

Bind Mount
→ Host File / Directory を Container から利用する

Volume
→ Container から独立して Data を保持する
```

1 つの Application で 3 つすべてを同時に使うこともある。  
それぞれ別の問題を解決しているため、「Docker の外部接続機能」のようにまとめて覚えず、目的で区別する。

## 完了条件

以下をすべて満たしたら Unit 03 を完了とする。

- Container 内部 Port と Host Port が別のものであることを説明できる。
- Host Port と Container Port は同じ番号である必要がないことを理解している。
- Port を Publish していない Container でも、Container 内の Service 自体は動作できることを理解している。
- `-p HOST_PORT:CONTAINER_PORT` の左右を説明できる。
- `-p 8080:80` で Host から Nginx Container へアクセスできる。
- Browser → `localhost` → Host Port → Container Port → Nginx の通信経路を説明できる。
- Browser から見た `localhost` が Windows Host であることを説明できる。
- `localhost` は実行環境によって指す対象が変わることを理解している。
- `docker container port` で Port Mapping を確認できる。
- Mount の基本的な意味を説明できる。
- Bind Mount が Host の File / Directory を Container 内 Path から利用する仕組みであることを説明できる。
- `--mount type=bind` の `src` / `dst` を区別できる。
- Host 側 HTML を Nginx Container から配信できる。
- Host 側 File の変更が Container 再作成なしで反映される理由を大まかに説明できる。
- Bind Mount 先に元々あった Container 側 Content が Mount 中は隠れて見えることを理解している。
- Container を削除しても Bind Mount 元の Host File が残ることを理解している。
- Container Writable Layer が Container 固有であることを説明できる。
- Container Writable Layer の Data が Container 削除後に引き継がれないことを確認できる。
- 永続 Data を Container 自体だけに保存しない理由を説明できる。
- Volume が Docker によって管理される Storage であることを説明できる。
- `docker volume create` / `ls` / `inspect` / `rm` を使用できる。
- Named Volume の名前と Docker が管理する実際の保存場所を区別して考えられる。
- `--mount type=volume` の `src` / `dst` を区別できる。
- Named Volume へ File を保存できる。
- Writer Container を削除しても Volume が残ることを確認できる。
- 別 Container から同じ Volume の Data を読み出せる。
- Container と Volume が独立したライフサイクルを持つことを説明できる。
- Bind Mount と Volume の共通点として「Container 内 Path へ Mount する」ことを説明できる。
- Bind Mount と Volume の違いを、管理主体・Mount 元・用途から説明できる。
- 次の 3 つを用途で区別できる。

```text
通信
→ Port

Host File / Directory の共有
→ Bind Mount

Container から独立した Data 永続化
→ Volume
```

ここまで確認できれば、Unit 04「Dockerfile と Image Build」へ進む。
