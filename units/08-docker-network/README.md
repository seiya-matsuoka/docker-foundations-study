# 08. Docker Network と Container 間通信

## この項目の目的

この Unit では、複数の Container を Docker Network に接続し、Container 同士がどのように通信するかを確認する。  
Docker Compose はまだ使用せず、Network の作成・接続・切断を Docker CLI から明示的に操作することで、Compose が後から自動化する仕組みを先に理解する。

Unit 03 では Host から Container へ Port Mapping を通してアクセスした。  
この Unit では視点を Container 間通信へ広げ、次の 2 つを区別する。

```text
Host
↓ Port Publishing
Container

Container A
↓ Docker Network
Container B
```

特に重要なのは、Container 内の `localhost` はその Container 自身を指し、別 Container へ接続するときは同じ Docker Network 上で相手の Container 名を Host 名として利用できるという点である。

```text
Container A の localhost
→ Container A 自身

Container A → Container B
→ Container B の名前を Host 名として利用
```

最終的には、`Container A → Docker Network → Container B` という通信経路を、Docker Compose に頼らず説明・操作できる状態を目指す。

## 学習内容

### Docker Network

Docker Network は、Container 同士や Container と外部の間で通信するための Network 機能である。  
Container は単独の Process だけではなく、自分の Network Interface や IP Address を持つ独立した Network Environment として扱われる。

複数 Container を同じ Docker Network に接続すると、Network を介して相互に通信できる。

```text
Container A
   │
   ├──── Docker Network ────┐
   │                        │
Container B              Container C
```

この Unit では Docker Network の中でも、Local Docker Host 上の Container 間通信で基本となる `bridge` Driver を扱う。

### Docker Network Driver

Docker Network には複数の Driver がある。  
この学習では詳細を広げず、主に次の 3 つが存在することだけ把握する。

```text
bridge
→ 1 台の Docker Host 内で Container を接続する基本的な Network

host
→ Host の Network を Container が直接利用する方式

none
→ Container に通常の Network 接続を持たせない方式
```

今回実際に利用するのは `bridge` である。

### Default Bridge Network

Docker には最初から `bridge` という名前の Default Bridge Network が存在する。

```bash
docker network ls
```

Container 起動時に `--network` を指定しない場合、通常は Default Bridge Network に接続される。

```text
docker container run ...
↓
Default bridge
```

ただし、Docker で複数 Container を明示的に連携させる場合は、自分で作成した User-defined Bridge Network を利用する方が扱いやすい。

### User-defined Bridge Network

この Unit では次の User-defined Bridge Network を作成する。

```text
unit08-network
```

```bash
docker network create \
  --driver bridge \
  unit08-network
```

User-defined Bridge Network には、同じ Network に接続された Container を名前で解決できる仕組みがある。

```text
unit08-client
      ↓
Host 名: unit08-web
      ↓ Docker Network
unit08-web
```

この Container 名による通信が、Unit 10 以降で Application Container から Database Container へ接続するときの重要な基礎になる。

### Network と Container

Container は Network に接続されることで、その Network 内で通信できるようになる。

Container 起動時に接続する場合は `--network` を利用する。

```bash
docker container run \
  --network unit08-network \
  ...
```

起動済み Container を後から Network へ接続することもできる。

```bash
docker network connect unit08-network <container>
```

反対に、Network から切断することもできる。

```bash
docker network disconnect unit08-network <container>
```

Network は Container とは別の Docker Resource であり、Container の接続関係を変更できる。

### Container 間通信

この Unit では次の 2 Container を利用する。

```text
unit08-web
→ Nginx Web Server

unit08-client
→ Alpine Linux の通信確認用 Container
```

両方を同じ `unit08-network` へ接続する。

```text
unit08-network
├─ unit08-web
└─ unit08-client
```

`unit08-client` から `unit08-web` の Port `80` へアクセスする。

```text
unit08-client
      ↓
http://unit08-web:80
      ↓
Docker Network
      ↓
unit08-web
      ↓
Nginx
```

ここでは Host Port を経由しない。

### Container 名による名前解決

User-defined Bridge Network では、Docker が同じ Network 上の Container 名を名前解決に利用できるようにする。

例えば、

```text
unit08-web
```

という Container が同じ Network に存在すれば、Client Container から次のように指定できる。

```text
http://unit08-web
```

毎回 Container の IP Address を直接調べて指定する必要はない。

```text
Container Name
↓ Docker の名前解決
Container IP Address
↓
対象 Container
```

Container の IP Address は Container 再作成などで変わる可能性があるため、通常は固定 IP Address よりも名前を利用して接続する方が扱いやすい。

### Docker の名前解決と User-defined Bridge

User-defined Bridge Network では Docker の Embedded DNS により、同じ Network 上の Container 名を解決できる。

```text
unit08-client
↓ DNS Query
Docker の名前解決
↓
unit08-web の Network 内 IP Address
↓
unit08-web
```

この仕組みがあるため、後の Spring Boot + PostgreSQL 構成では、

```text
localhost
```

ではなく、

```text
postgres
```

のような相手 Service / Container の名前を Database Host として利用する考え方につながる。

### Container 内の `localhost`

`localhost` は常に「現在その Process が動いている Network Environment 自身」を指す。

Host の Git Bash から、

```text
localhost
```

と指定した場合は Windows Host 自身である。

一方、`unit08-client` Container 内から、

```text
localhost
```

と指定した場合は `unit08-client` 自身である。

```text
Windows Host
localhost
→ Windows Host

unit08-client
localhost
→ unit08-client

unit08-web
localhost
→ unit08-web
```

したがって `unit08-client` から `unit08-web` へ接続したい場合、

```text
http://localhost
```

ではなく、

```text
http://unit08-web
```

を使用する。

### Host → Container と Container → Container

Host から Container へアクセスする場合、通常は Port Publishing を利用する。

```text
Windows Host
localhost:8080
↓
Host Port 8080
↓ Docker Port Publishing
Container Port 80
↓
Nginx
```

一方、同じ Docker Network 上の Container 同士は、相手 Container が Listen している Container Port へ直接接続する。

```text
unit08-client
↓
unit08-web:80
↓ Docker Network
Nginx
```

このとき Host Port `8080` は通信経路に入らない。

```text
Host → unit08-web
localhost:8080

unit08-client → unit08-web
unit08-web:80
```

### Port Publishing と Docker Network は別の仕組み

Port Publishing と Docker Network はどちらも通信に関係するため混同しやすいが、役割は異なる。

```text
Port Publishing
→ Docker Host の外側から Container へ入口を作る

Docker Network
→ Container 同士を Network 上で接続する
```

Container 間通信のためだけであれば、対象 Container の Port を Host へ Publish する必要はない。

この Unit では最初に Nginx Container を `-p` なしで起動し、それでも Client Container から通信できることを確認する。

### Container Port と Host Port

Nginx は Container 内で Port `80` を Listen する。

Host へ次のように Publish した場合、

```text
-p 8080:80
```

意味は次のとおり。

```text
Host Port
8080

Container Port
80
```

Container 同士で通信するときは Host Port `8080` ではなく、Nginx が Container 内で Listen している Port `80` を利用する。

```text
Container 間通信
unit08-web:80

Host からの通信
localhost:8080
```

### Network に接続していない状態

`unit08-client` を `unit08-network` から切断すると、その Network を通した `unit08-web` との通信はできなくなる。

```text
切断前

unit08-client
      ↓
unit08-network
      ↓
unit08-web
```

```text
切断後

unit08-client

unit08-network
└─ unit08-web
```

その後 `docker network connect` で再接続すれば、同じ名前による通信が再び可能になる。

Network は単なる一覧上の Group ではなく、Container 間通信の接続範囲を構成する Resource である。

### Network と IP Address

Docker Network に接続された Container には、その Network 内で利用する IP Address が割り当てられる。

```bash
docker network inspect unit08-network
```

から確認できる。

ただし、この Unit では IP Address を直接指定して通信することを目的にしない。

```text
IP Address
→ Network の仕組みを確認する情報

Container Name
→ Application から相手へ接続するときに利用しやすい名前
```

という位置付けで理解する。

### Unit 09 の Docker Compose との関係

次の Unit 09 では Docker Compose を利用する。

Compose を利用すると、複数 Service を同じ Compose Project の Network へ接続する処理などを自動化できる。

```text
Unit 08
docker network create
docker container run --network ...
docker network connect
docker network disconnect

Unit 09
compose.yaml
↓
docker compose up
```

しかし Compose を先に使うだけでは、その裏で Container がどの Network に接続され、なぜ名前で通信できるのかが見えにくい。

Unit 08 では CLI で明示的に Network を操作し、Compose が後から簡略化する仕組みを理解する。

## 使用するもの

### Network

```text
unit08-network
```

### Network Driver

```text
bridge
```

### Web Server Container

```text
unit08-web
```

### Web Server Image

```text
nginx:1.30.4-alpine3.24
```

### Client Container

```text
unit08-client
```

### Client Image

```text
alpine:3.24.1
```

### Nginx Container Port

```text
80
```

### Host へ Publish するときの Port

```text
Host      : 8080
Container : 80
```

## 事前準備

Unit 07 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/08-docker-network
```

Git Bash で Repository 内の次の Unit Directory へ移動した状態から操作する。

```text
units/08-docker-network
```

現在位置を確認する。

```bash
pwd
```

以前の学習で同名 Container / Network が残っていないことを確認する。

```bash
docker container ls -a \
  --filter 'name=unit08-'

docker network ls
```

`unit08-web`、`unit08-client`、`unit08-network` が残っている場合は、前回作業の状態を確認してから削除する。

## ハンズオン

### 1. 現在の Docker Network を確認する

```bash
docker network ls
```

通常、Docker が用意する次の Network などを確認できる。

```text
bridge
host
none
```

この Unit では `bridge` Driver を使うが、既存の Default `bridge` Network そのものではなく、User-defined Bridge Network を新しく作成する。

### 2. Default Bridge Network を確認する

```bash
docker network inspect bridge
```

`Driver` が `bridge` であることを確認する。  
Default Network が最初から Docker に存在することを確認できればよく、この Network の詳細設定を変更する必要はない。

### 3. User-defined Bridge Network を作成する

```bash
docker network create \
  --driver bridge \
  unit08-network
```

作成結果として Network ID が表示される。

続いて確認する。

```bash
docker network ls
```

`unit08-network` が存在し、Driver が `bridge` であることを確認する。

### 4. 作成した Network の情報を確認する

```bash
docker network inspect unit08-network
```

主に次を確認する。

```text
Name
Driver
IPAM
Containers
```

まだ Container を接続していないため、`Containers` には対象 Container が存在しない状態である。

### 5. Nginx Container を Network に接続して起動する

この段階では Host への Port Publishing を行わない。

```bash
docker container run \
  -d \
  --name unit08-web \
  --network unit08-network \
  nginx:1.30.4-alpine3.24
```

起動状態を確認する。

```bash
docker container ls \
  --filter 'name=unit08-web'
```

Nginx は Container 内の Port `80` で動作しているが、`-p` を指定していないため Host Port は Publish されていない。

### 6. Nginx Container の Port Publishing 状態を確認する

```bash
docker container port unit08-web
```

Host へ Publish された Port がないため、Port Mapping は表示されない。

```text
Nginx
Container Port 80
→ Listen している

Host Port
→ Publish していない
```

Container 間通信と Host への Port Publishing が別であることを確認するため、この状態を維持する。

### 7. Client Container を同じ Network に接続して起動する

通信確認用の Alpine Container を起動する。

```bash
docker container run \
  -d \
  --name unit08-client \
  --network unit08-network \
  alpine:3.24.1 \
  sleep 1d
```

`sleep 1d` により Container の Main Process を継続させ、後続の `docker container exec` から Network 通信を確認できる状態にする。

確認する。

```bash
docker container ls \
  --filter 'name=unit08-'
```

`unit08-web` と `unit08-client` の両方が Running であることを確認する。

### 8. Network に接続された Container を確認する

```bash
docker network inspect unit08-network
```

`Containers` に次の 2 Container が存在することを確認する。

```text
unit08-web
unit08-client
```

それぞれに Network 内 IP Address が割り当てられていることも確認する。  
IP Address は確認対象ではあるが、後続の通信では直接指定しない。

### 9. Container 名で Nginx へ通信する

`unit08-client` から `unit08-web` へ HTTP Request を送る。

```bash
docker container exec unit08-client \
  wget -qO- http://unit08-web
```

Nginx の Default HTML が出力されればよい。

ここで指定した `unit08-web` は IP Address ではなく Container 名である。

```text
unit08-client
↓
名前解決: unit08-web
↓
Docker Network
↓
unit08-web:80
↓
Nginx
```

という通信が成立している。

### 10. Container 名が名前解決されることを確認する

Alpine Container から `ping` を実行する。

```bash
docker container exec unit08-client \
  ping -c 1 unit08-web
```

`unit08-web` が Network 内 IP Address に解決され、応答を確認できればよい。  
重要なのは IP Address 自体ではなく、Container 名が Network 内 Address に名前解決されていることである。

### 11. Client Container 内の DNS 設定を確認する

```bash
MSYS_NO_PATHCONV=1 docker container exec unit08-client \
  cat /etc/resolv.conf
```

User-defined Network では Docker の Embedded DNS が Container 名の名前解決に利用される。

Environment により表示される補足情報は異なる場合があるため、ここでは `/etc/resolv.conf` の細かな全項目を暗記する必要はない。  
「Container 名による通信の裏で Docker が名前解決を提供している」と理解できればよい。

### 12. Container 内の `localhost` を確認する

`unit08-client` 内から `localhost` の Port `80` へ接続を試す。

```bash
docker container exec unit08-client \
  sh -c 'wget -qO- http://localhost 2>/dev/null || echo "Connection failed as expected"'
```

次のような出力になればよい。

```text
Connection failed as expected
```

`unit08-client` 自身では Web Server を起動していないためである。

```text
unit08-client 内 localhost
→ unit08-client 自身

unit08-web
→ 別 Container
```

したがって、別 Container の Nginx に接続する場合は `localhost` ではなく `unit08-web` を使用する。

### 13. Host Port を使わず Container 間通信できていることを整理する

現在 `unit08-web` は Host へ Port を Publish していない。

確認する。

```bash
docker container port unit08-web
```

それでも Step 9 の通信は成功した。

```text
Container → Container
→ 同じ Docker Network を利用
→ Host Port Publishing は不要
```

という関係を確認する。

### 14. Client Container を Network から切断する

```bash
docker network disconnect \
  unit08-network \
  unit08-client
```

Network の状態を確認する。

```bash
docker network inspect unit08-network
```

`unit08-client` が `Containers` から外れ、`unit08-web` だけが残っていることを確認する。

### 15. Network 切断後は Container 名で通信できないことを確認する

```bash
docker container exec unit08-client \
  sh -c 'wget -qO- http://unit08-web 2>/dev/null || echo "Connection failed as expected"'
```

次のような出力になればよい。

```text
Connection failed as expected
```

Container 自体は Running のままだが、`unit08-network` を通した `unit08-web` への通信経路を失っている。

```text
Container が Running
≠
すべての Container と通信できる
```

ということを確認する。

### 16. 起動済み Container を Network へ再接続する

```bash
docker network connect \
  unit08-network \
  unit08-client
```

Network を確認する。

```bash
docker network inspect unit08-network
```

再び `unit08-web` と `unit08-client` の両方が接続されていることを確認する。

### 17. 再接続後に通信が復旧することを確認する

```bash
docker container exec unit08-client \
  wget -qO- http://unit08-web
```

再び Nginx の HTML が取得できればよい。

```text
disconnect
→ 通信不可

connect
→ 通信可能
```

Network への接続状態が Container 間通信を決める要素であることを確認する。

### 18. Host → Container の通信を確認するため Nginx Container を再作成する

ここから Host と Container 間通信との違いを確認する。

まず Nginx Container を削除する。

```bash
docker container rm -f unit08-web
```

同じ名前・同じ Network で、今度は Host Port `8080` を Publish して再作成する。

```bash
docker container run \
  -d \
  --name unit08-web \
  --network unit08-network \
  -p 8080:80 \
  nginx:1.30.4-alpine3.24
```

Port Mapping を確認する。

```bash
docker container port unit08-web
```

Host `8080` から Container `80` への Mapping が確認できる。

### 19. Host から Nginx へアクセスする

Git Bash から実行する。

```bash
curl http://localhost:8080
```

Browser で次を開いてもよい。

```text
http://localhost:8080
```

Nginx の Default Page を確認する。

通信経路は次のとおり。

```text
Windows Host
localhost:8080
↓
Host Port 8080
↓
Docker Port Publishing
↓
unit08-web:80
↓
Nginx
```

### 20. Container 間通信では引き続き Container Port を利用する

Host Port を Publish した後も、`unit08-client` からの通信は次で行う。

```bash
docker container exec unit08-client \
  wget -qO- http://unit08-web:80
```

`8080` ではなく Container 側で Nginx が Listen している `80` を利用する。

```text
Host → Container
localhost:8080

Container → Container
unit08-web:80
```

Host Port と Container 間通信の Port を混同しない。

### 21. Container の Network 情報を個別に確認する

Nginx Container を確認する。

```bash
docker container inspect \
  --format '{{json .NetworkSettings.Networks}}' \
  unit08-web
```

Client Container も確認する。

```bash
docker container inspect \
  --format '{{json .NetworkSettings.Networks}}' \
  unit08-client
```

両方が `unit08-network` に接続され、それぞれ異なる Network 内 IP Address を持っていることを確認する。

### 22. Unit 08 の Container を削除する

```bash
docker container rm -f \
  unit08-web \
  unit08-client
```

確認する。

```bash
docker container ls -a \
  --filter 'name=unit08-'
```

対象 Container が残っていないことを確認する。

### 23. Unit 08 の Network を確認する

```bash
docker network inspect unit08-network
```

Container を削除した後も `unit08-network` 自体は残っている。

```text
Container
→ 削除済み

Network
→ 存在
```

Container と Network が別の Docker Resource であることを確認する。

### 24. Unit 08 の Network を削除する

```bash
docker network rm unit08-network
```

確認する。

```bash
docker network ls
```

`unit08-network` が表示されなくなればよい。

Nginx / Alpine Image は後続の学習でも利用できるため、この Unit では無理に削除する必要はない。

## 動作・確認ポイント

### Docker Network

以下を確認する。

- Docker Network が Container 間通信を構成する Docker Resource である。
- `bridge` Driver を使った User-defined Network を作成・確認・削除できる。
- Container と Network が別のライフサイクルを持つことを確認できる。

### Container の Network 接続

以下を確認する。

- `--network` で Container 起動時に Network へ接続できる。
- `docker network connect` / `disconnect` で起動済み Container の接続状態を変更できる。
- Network から切断すると、同じ Network を使った相手 Container との通信が成立しなくなる。

### Container 間通信

以下を確認する。

- 同じ User-defined Bridge Network 上の Container 同士で通信できる。
- Container 名 `unit08-web` を Host 名として Nginx へ接続できる。
- Container の IP Address を直接指定せず、Docker の名前解決を利用できる。

### `localhost`

以下を確認する。

- Windows Host の `localhost` と Container 内の `localhost` は指す Network Environment が異なる。
- `unit08-client` 内の `localhost` は `unit08-client` 自身を指す。
- 別 Container へ接続するときは、同じ Network 上の相手 Container 名を利用できる。

### Port Publishing と Network

以下を確認する。

- Host → Container では `-p 8080:80` による Port Publishing を利用できる。
- Container → Container では Host Port を経由せず `unit08-web:80` へ接続できる。
- Container 間通信のためだけに Host Port を Publish する必要はない。

## 学習ポイント

### Network は Container 間通信の接続範囲を作る

同じ Docker Host 上に Container が存在するだけで、どの Container とも無条件に同じ方法で通信するわけではない。

```text
同じ Network
→ Network を介して通信可能

Network から切断
→ その Network を介した通信不可
```

Container をどの Network に所属させるかは、複数 Container 構成の重要な設計要素である。

### User-defined Bridge Network では名前で相手を指定できる

Container の IP Address は Docker が割り当てる値であり、Container 再作成によって変わる可能性がある。

そのため、

```text
172.x.x.x
```

のような Address を Application 設定へ直接書くのではなく、

```text
unit08-web
```

のような Network 上の名前を利用する。

```text
Name
↓ Docker DNS
Current IP Address
↓
Target Container
```

という間接化が、Container を作り直しやすくする。

### `localhost` は「自分自身」である

複数 Container 構成では `localhost` の意味を正確に捉えることが重要である。

```text
Spring Boot Container 内 localhost
→ Spring Boot Container 自身

PostgreSQL Container 内 localhost
→ PostgreSQL Container 自身
```

したがって将来、

```text
Spring Boot
↓
PostgreSQL
```

と接続するとき、Spring Boot 側から Database Host に `localhost` を指定すると Spring Boot Container 自身を探してしまう。

同じ Network 上の PostgreSQL の名前を Host として指定する必要がある。

### Port Publishing と Container 間通信を分けて考える

```text
-p 8080:80
```

は Host から Container Port `80` へ到達するための入口を作る。

一方、

```text
unit08-client
↓
unit08-web:80
```

という Container 間通信は Docker Network 内で直接行われる。

```text
外から入る
→ Port Publishing

Container 同士
→ Docker Network
```

という切り分けで考えると整理しやすい。

### Container Port は Application が実際に Listen する Port

Nginx は Container 内で `80` を Listen する。

Host 側で `8080` に Publish しても Nginx 自体の Listen Port が `8080` に変わるわけではない。

```text
Host
8080
↓ Mapping
Container
80
```

Container 間では Nginx の実際の Container Port `80` へ接続する。

### Network も独立した Docker Resource である

これまでに扱った Resource はそれぞれ独立している。

```text
Image
→ Container の Template

Container
→ Process の実行環境

Volume
→ Persistent Data

Network
→ Container 間通信
```

Container を削除しても User-defined Network は自動的には削除されない。  
不要になった Network は `docker network rm` で明示的に削除できる。

### Default Bridge と User-defined Bridge を区別する

どちらも `bridge` Driver を利用するが、学習上は次のように整理する。

```text
Default bridge
→ Docker が最初から用意する Network

User-defined bridge
→ Application / Container Group 用に自分で作る Network
```

複数 Container を名前で連携させる学習では User-defined Bridge Network を基本として扱う。

### Docker Network を CLI で学ぶ意味

Unit 09 の Docker Compose では Network 周りの操作が簡略化される。

それ自体は便利だが、先に CLI で、

```text
Network を作る
↓
Container を接続する
↓
名前で通信する
```

という仕組みを理解しておくことで、Compose の設定を単なる記述方法としてではなく、Docker Network の自動化として理解できる。

### Unit 09 へのつながり

次の Unit 09 では複数 Container の定義を `compose.yaml` にまとめる。

```text
Unit 08
Network を CLI で明示的に操作

Unit 09
Compose が Network を含む複数 Resource をまとめて管理
```

その後 Unit 10 では、

```text
Spring Boot Container
↓ Docker Network
PostgreSQL Container
```

という実際の Application / Database 間通信に発展する。

Unit 08 で学ぶ Container 名、`localhost`、Container Port の区別が、そのまま Database 接続設定の理解につながる。

## 完了条件

以下を満たしたら Unit 08 を完了とする。

- Docker Network の役割と `bridge` Network の基本的な位置付けを説明できる。
- User-defined Bridge Network を CLI から作成・確認・削除できる。
- Container 起動時の `--network` と、起動後の `docker network connect` / `disconnect` を使える。
- 同じ Docker Network 上の複数 Container が通信できることを実際に確認できる。
- User-defined Bridge Network 上で Container 名を Host 名として利用できる理由を説明できる。
- Container の Network 内 IP Address を直接固定して使うより、名前による接続が扱いやすい理由を理解している。
- Host の `localhost` と Container 内の `localhost` の違いを説明できる。
- 別 Container へ接続するときに相手 Container の名前を Host 名として利用できる。
- Host → Container の Port Publishing と Container → Container の Docker Network を区別できる。
- Container 間通信では Host Port ではなく、相手 Application が Listen している Container Port へ接続することを理解している。
- Container を Network から切断・再接続し、Network 接続状態によって通信可否が変わることを確認できる。
- Docker Compose を使う前提として、`Container A → Docker Network → Container B` の通信構造を CLI ベースで説明できる。

ここまで確認できれば、Unit 09「Docker Compose」へ進む。
