# 03. Port・Bind Mount・Volume

## この項目の目的

この Unit では、Host と Container の境界をまたぐ代表的な 3 つの仕組みとして、Port / Bind Mount / Volume を学ぶ。  
Port は「通信」、Bind Mount は「Host 上の File / Directory の共有」、Volume は「Docker が管理する永続データ」という、それぞれ異なる目的を持つ。  
Nginx と小さな HTML、Alpine Linux を使って実際に操作し、3 つを混同せず説明・設定できる状態を目指す。

## 学習内容

### Port

Container は Host から分離されているため、Container 内で Web Server が起動していても、それだけでは Host の Browser から直接アクセスできるとは限らない。  
Host から Container の Service へアクセスさせるために、Docker では Container の Port を Host 側へ Publish できる。

基本形式は次のとおり。

```text
HOST_PORT:CONTAINER_PORT
```

今回使用する Nginx は Container 内部の Port `80` で HTTP Request を受け付ける。  
Host 側の `8080` を Container 側の `80` へ接続すると、次の経路になる。

```text
Browser
  ↓
http://localhost:8080
  ↓
Windows Host : 8080
  ↓ Port Mapping
Nginx Container : 80
  ↓
Nginx
```

`localhost` を Browser で指定した場合、この Unit では Windows Host 自身を指す。  
Browser は Container の中で動いているわけではないため、「どの環境から見た localhost か」を意識する。

Docker CLI では `-p` または `--publish` を使用する。

```bash
docker container run -p 8080:80 ...
```

```text
-p 8080:80
   │    │
   │    └─ Container Port
   └────── Host Port
```

Port Mapping は Container 内の Nginx の Listen Port 自体を変更するものではない。  
Host 側に入口を作り、その通信を Container 側の Port へ転送する設定である。

### Bind Mount

Bind Mount は、Host 上に存在する File / Directory を Container 内の Path へ直接 Mount する仕組みである。

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

Host 側の File を変更すると、同じ File / Directory を参照している Container 側にも変更が反映される。  
Source Code や設定 File など、Host 側で直接編集・管理したいものを Container と共有する用途に向いている。

この学習では、意味を読み取りやすい `--mount` 形式を使用する。

```text
--mount type=bind,src=HOST_PATH,dst=CONTAINER_PATH
```

今回は Nginx が読むだけでよいため `readonly` も指定する。

### Volume

Volume は、Docker が作成・管理する永続データ領域である。  
Bind Mount のように Host の具体的な Directory Path を指定するのではなく、Docker に Storage の管理を任せる。

```text
Docker
└─ Named Volume: unit03-data
       │
       ├─ Container A から Mount
       └─ Container B から Mount
```

Container を削除しても、Volume 自体を削除しない限りデータは残る。

```text
Container
作成 → 実行 → 削除

Volume
作成 ───────────────→ 保持
```

Database のデータなど、Container を作り直しても残す必要があるデータで特に重要になる。  
PostgreSQL での実践的な利用は Unit 07 で扱う。

Named Volume の基本操作は次のとおり。

```text
docker volume create
docker volume ls
docker volume inspect
docker volume rm
```

Container へ Mount する場合は次の形式を使う。

```text
--mount type=volume,src=VOLUME_NAME,dst=CONTAINER_PATH
```

### Bind Mount と Volume の違い

| 観点                        | Bind Mount                    | Volume                        |
| --------------------------- | ----------------------------- | ----------------------------- |
| データの管理                | Host 側で直接管理             | Docker が管理                 |
| Mount 元                    | Host の File / Directory Path | Docker Volume                 |
| Host から直接編集           | しやすい                      | 基本的に直接操作しない        |
| Container 削除後            | Host File が残る              | Volume が残る                 |
| 主な用途                    | Source / 設定 File の共有     | Application / DB の永続データ |
| Host Directory 構成への依存 | ある                          | 比較的小さい                  |

### Container 自体を永続データ保管場所にしない

Container の Writable Layer に File を作ることはできる。  
しかし、その Container を削除すると、その Container 固有のデータも失われる。

```text
Container
└─ Writable Layer
   └─ data.txt

Container を削除
        ↓
data.txt も失われる
```

残す必要があるデータは、Bind Mount や Volume など Container のライフサイクルから分離された場所へ置く。

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

Container 固有データと Named Volume の永続性確認に使用する。

### Bind Mount 用 HTML

```text
bind-mount-site/
└─ index.html
```

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

```bash
docker image pull nginx:1.30.4-alpine3.24
```

確認する。

```bash
docker image ls nginx:1.30.4-alpine3.24
```

### 2. Port を Publish せず Nginx を起動する

```bash
docker container run   -d   --name unit03-nginx-no-port   nginx:1.30.4-alpine3.24
```

確認する。

```bash
docker container ls
docker container port unit03-nginx-no-port
```

Nginx は Container 内で動いているが、Host 側へ Publish された Port はない。  
確認後に削除する。

```bash
docker container rm -f unit03-nginx-no-port
```

### 3. Host Port `8080` を Container Port `80` へ Publish する

```bash
docker container run   -d   --name unit03-nginx-port   -p 8080:80   nginx:1.30.4-alpine3.24
```

確認する。

```bash
docker container ls
docker container port unit03-nginx-port
```

`PORTS` や `docker container port` の出力から、Host と Container の Port の対応を確認する。

### 4. Browser から Nginx へアクセスする

Browser で次を開く。

```text
http://localhost:8080
```

Nginx の Default Page が表示されることを確認する。

```text
Browser
  ↓
localhost:8080
  ↓
Windows Host
  ↓
8080 → 80
  ↓
Nginx Container
```

確認後に Container を削除する。

```bash
docker container rm -f unit03-nginx-port
```

### 5. Bind Mount 用 HTML を確認する

```bash
cat bind-mount-site/index.html
```

この File は Repository 内、つまり Host 側に存在する。

### 6. Host Directory を Nginx Container へ Bind Mount する

```bash
docker container run   -d   --name unit03-nginx-bind   -p 8080:80   --mount type=bind,src="$(pwd)/bind-mount-site",dst=/usr/share/nginx/html,readonly   nginx:1.30.4-alpine3.24
```

`--mount` の意味は次のとおり。

```text
type=bind
= Bind Mount

src="$(pwd)/bind-mount-site"
= Host 側 Mount 元

dst=/usr/share/nginx/html
= Container 側 Mount 先

readonly
= Container 側から書き込ませない
```

### 7. Browser で Host 側 HTML を確認する

```text
http://localhost:8080
```

次の見出しを持つ Page が表示されることを確認する。

```text
Unit 03 - Bind Mount
```

ここでは Port と Bind Mount が同時に使われている。

```text
通信
Browser → Host : 8080 → Container : 80

File
Host bind-mount-site/ → Container /usr/share/nginx/html/
```

### 8. Host 側 HTML を変更する

Editor で `bind-mount-site/index.html` を開き、次の行を変更する。

```html
<p id="message">Host 側の HTML を Container から参照しています。</p>
```

たとえば次のようにする。

```html
<p id="message">Host 側の変更が Bind Mount 経由で反映されました。</p>
```

保存後、Container を再作成せず Browser を Reload する。  
変更後の文章が表示されることを確認する。

### 9. Container 内から Bind Mount 先を確認する

```bash
docker container exec unit03-nginx-bind   cat /usr/share/nginx/html/index.html
```

Host 側 File と同じ内容が表示されることを確認する。

Mount 情報も確認する。

```bash
docker container inspect   --format '{{json .Mounts}}'   unit03-nginx-bind
```

`Type`、`Source`、`Destination`、`RW` を確認する。  
`readonly` のため Write 可能な Mount ではない。

確認後に Container を削除する。

```bash
docker container rm -f unit03-nginx-bind
```

Host File は残っている。

```bash
ls bind-mount-site
```

### 10. Container 固有領域だけに File を作成する

```bash
docker container run   -d   --name unit03-container-data   alpine:3.24.1   sleep 300
```

Container 内に File を作成する。

```bash
docker container exec unit03-container-data   sh -c 'echo "container only data" > /tmp/container-only.txt'
```

確認する。

```bash
docker container exec unit03-container-data   cat /tmp/container-only.txt
```

Container を削除する。

```bash
docker container rm -f unit03-container-data
```

同じ Image から別 Container を作り、File が引き継がれていないことを確認する。

```bash
docker container run   --rm   alpine:3.24.1   sh -c 'if [ -f /tmp/container-only.txt ]; then cat /tmp/container-only.txt; else echo "file not found"; fi'
```

次が表示されればよい。

```text
file not found
```

### 11. Named Volume を作成する

```bash
docker volume create unit03-data
```

確認する。

```bash
docker volume ls
docker volume inspect unit03-data
```

`Name`、`Driver`、`Mountpoint` などを確認する。  
`Mountpoint` を直接編集するのではなく、Volume を Container へ Mount して利用する。

### 12. Named Volume へデータを書き込む

```bash
docker container run   --name unit03-volume-writer   --mount type=volume,src=unit03-data,dst=/data   alpine:3.24.1   sh -c 'echo "Unit 03 persistent data" > /data/message.txt && cat /data/message.txt'
```

次が表示される。

```text
Unit 03 persistent data
```

Container を削除する。

```bash
docker container rm unit03-volume-writer
```

Volume は残っていることを確認する。

```bash
docker volume ls
```

### 13. 別 Container から同じ Volume のデータを読む

```bash
docker container run   --name unit03-volume-reader   --mount type=volume,src=unit03-data,dst=/data   alpine:3.24.1   cat /data/message.txt
```

次が再び表示されることを確認する。

```text
Unit 03 persistent data
```

Writer Container は削除済みである。  
それでも読めるのは、File が Container ではなく Volume に保存されているためである。

```text
Container A
   ↓ write
Named Volume
   ↓ Container A 削除後も保持
Container B
   ↓ read
```

Reader Container を削除する。

```bash
docker container rm unit03-volume-reader
```

Volume がまだ残っていることを確認する。

```bash
docker volume ls
```

### 14. Named Volume を削除する

```bash
docker volume rm unit03-data
```

確認する。

```bash
docker volume ls
```

`unit03-data` が表示されなくなればよい。  
Volume を参照する Container が残っている場合は削除できないため、その Container を先に確認・削除する。

## 動作・確認ポイント

### Port

- Nginx は Container 内部の Port `80` で動作する。
- `-p 8080:80` では `8080` が Host Port、`80` が Container Port である。
- `http://localhost:8080` で Browser から Container の Nginx へアクセスできる。
- Port を Publish していない状態との違いを確認できる。
- `docker container port` で Publish 状態を確認できる。

### Bind Mount

- Host 側 `bind-mount-site` を `/usr/share/nginx/html` へ Mount できる。
- Host 側 HTML が Nginx から配信される。
- Host 側の変更が Container 再作成なしで反映される。
- Container 内から Mount された File を確認できる。
- Container を削除しても Host 側 File は残る。

### Container 固有データ

- Container の Writable Layer に File を作成できる。
- Container を削除して別 Container を作ると、その File は引き継がれない。
- Container 自体を永続データ保管場所として扱わない理由を理解できる。

### Volume

- `docker volume create` / `ls` / `inspect` / `rm` を使用できる。
- Named Volume を Container の `/data` へ Mount できる。
- Container を削除しても Volume が残る。
- 別 Container から同じ Volume のデータを読める。

## 学習ポイント

### 3 つは別の問題を解決する

```text
Port
= 通信をつなぐ

Bind Mount
= Host の File / Directory を Container と共有する

Volume
= Container から独立してデータを永続化する
```

### Port は通信経路

```text
Browser
↓
Host Port
↓
Container Port
↓
Process
```

### Bind Mount は Host Path との直接共有

```text
Host File
↕
Bind Mount
↕
Container Path
```

Host で直接編集・管理したい Source Code や設定 File に向いている。

### Volume は Docker 管理の永続 Storage

```text
Container A
     ↘
      Volume
     ↗
Container B
```

Volume の寿命は Container の寿命とは別である。

### Container は交換可能な実行単位として考える

```text
Application Process
→ Container

Persistent Data
→ Volume など
```

Container を削除・再作成しても残す必要があるデータは、Container 固有の Writable Layer に依存させない。

### Bind Mount と Volume の大まかな判断

```text
Host 側で直接編集・管理したい
→ Bind Mount

Docker Application が継続して保持したい
→ Volume
```

## 完了条件

以下をすべて満たしたら Unit 03 を完了とする。

- Host Port と Container Port を区別できる。
- `-p HOST_PORT:CONTAINER_PORT` の左右を説明できる。
- `-p 8080:80` で Host から Nginx Container へアクセスできる。
- Browser → `localhost` → Host Port → Container Port → Nginx の通信経路を説明できる。
- `docker container port` で Port Mapping を確認できる。
- Bind Mount が Host の File / Directory を Container へ直接 Mount する仕組みであることを説明できる。
- `--mount type=bind` の `src` / `dst` を区別できる。
- Host 側 HTML を Nginx から配信できる。
- Host 側 File の変更が Container 再作成なしで反映されることを確認できる。
- Container の Writable Layer だけに保存したデータが Container 削除後には引き継がれないことを確認できる。
- Named Volume が Docker によって管理される永続 Storage であることを説明できる。
- `docker volume create` / `ls` / `inspect` / `rm` を使用できる。
- `--mount type=volume` の `src` / `dst` を区別できる。
- Container を削除しても Named Volume が残ることを確認できる。
- 別 Container から同じ Volume のデータを読み出せる。
- Bind Mount と Volume の違いを説明できる。
- 次の 3 つを用途で区別できる。

```text
通信
→ Port

Host File / Directory の共有
→ Bind Mount

Container から独立したデータ永続化
→ Volume
```

ここまで確認できれば、Unit 04「Dockerfile と Image Build」へ進む。
