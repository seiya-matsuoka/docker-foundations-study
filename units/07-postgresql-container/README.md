# 07. PostgreSQL Container とデータ永続化

## この項目の目的

この Unit では、PostgreSQL Official Image を利用して Database Server を Container として起動し、Named Volume に Database Data を保存する。  
Unit 03 で学んだ Volume の基本を、実際の Database Data の永続化へ適用し、Container を削除・再作成しても Data を保持できることを確認する。

Unit 06 までは Application Container を中心に扱ったが、Database も Docker では Image から Container を作成して実行するという基本構造は同じである。  
一方、Database には Application Container とは異なり、Container を作り直しても失いたくない Data があるため、Container と Data のライフサイクルを分離することが特に重要になる。

```text
PostgreSQL Official Image
        ↓
Environment Variable で初期設定
        ↓
PostgreSQL Container
        ↓
Database Data
        ↓
Named Volume
        ↓
Container を削除
        ↓
同じ Volume で Container を再作成
        ↓
Database Data が残る
```

最終的には、次の役割を分けて説明できる状態を目指す。

```text
Image
→ PostgreSQL を実行するための Template

Container
→ PostgreSQL Server を実行する環境

Named Volume
→ PostgreSQL の Database Data を保持する Storage
```

## 学習内容

### PostgreSQL Official Image

PostgreSQL には Docker Official Image が提供されている。  
自分で Linux Image に PostgreSQL を Install して Image を作るのではなく、PostgreSQL を実行するための構成が用意された Official Image を利用できる。

今回使用する Image は次である。

```text
postgres:18.6-alpine3.24
```

```text
postgres
= Image Name

18.6-alpine3.24
= Image Tag
```

Dockerfile を自分で作らなくても、Official Image に Environment Variable、Port、Volume などの Runtime 設定を渡すことで PostgreSQL Server を Container として利用できる。

```text
PostgreSQL Official Image
+
Runtime 設定
↓
PostgreSQL Container
```

### PostgreSQL Container

PostgreSQL Container の中では PostgreSQL Server Process が動作する。

```text
Windows Host
        ↓ Docker
PostgreSQL Container
└─ PostgreSQL Server
   └─ Port 5432
```

PostgreSQL の Default Port は `5432` である。  
Host から PostgreSQL に接続したい場合は、Unit 03 で学んだ Port Mapping により Host Port と Container Port を接続する。

```text
Host : 5432
      ↓
Docker Port Mapping
      ↓
Container : 5432
      ↓
PostgreSQL Server
```

### DB 初期設定用 Environment Variable

PostgreSQL Official Image では、初回起動時の Database Cluster 初期化に Environment Variable を利用できる。  
この Unit では次の 3 つを使用する。

```text
POSTGRES_USER
POSTGRES_PASSWORD
POSTGRES_DB
```

それぞれの役割は次のとおり。

```text
POSTGRES_USER
→ 初期作成する PostgreSQL User

POSTGRES_PASSWORD
→ その User の Password

POSTGRES_DB
→ 初期作成する Database
```

今回使用する学習用の値は次である。

```text
POSTGRES_USER=unit07_user
POSTGRES_PASSWORD=unit07-password
POSTGRES_DB=unit07_db
```

`unit07-password` は Local 学習専用の固定値であり、実際の Credential として利用するものではない。

### 「初期設定用」という意味

`POSTGRES_USER`、`POSTGRES_PASSWORD`、`POSTGRES_DB` は、主に PostgreSQL の Data Directory が空の状態で最初に初期化されるときに利用される。

```text
空の Volume
↓
PostgreSQL Container 初回起動
↓
Environment Variable を利用
↓
Database Cluster 初期化
↓
User / Database 作成
↓
Data が Volume に保存
```

一度初期化済みの Data が Volume に存在する場合、その Volume を使って Container を再作成しても、新しい空 Database を毎回初期化するわけではない。

```text
既存 Volume
↓
既存 PostgreSQL Data を検出
↓
その Data を利用して PostgreSQL 起動
```

そのため、初期化用 Environment Variable と通常の Runtime 設定を完全に同じものとして考えない。

### PostgreSQL への接続

この Unit では Host に PostgreSQL Client を追加 Installation せず、PostgreSQL Container に含まれる `psql` Client を利用する。

```text
Windows Host
↓ docker container exec
PostgreSQL Container
↓ psql
PostgreSQL Server
```

次のような Command で Database を確認できる。

```bash
docker container exec unit07-postgres \
  psql -U unit07_user -d unit07_db \
  -c 'SELECT current_database();'
```

PostgreSQL 自体の SQL 学習は目的ではないため、扱う SQL は Table 作成、Insert、Select の最小限にする。

### Host / Container / Database の関係

今回の構成を整理すると次のようになる。

```text
Windows Host
│
├─ Docker Engine
├─ Host Port 5432
└─ Docker Named Volume
   └─ unit07-postgres-data
          ↑
          │ Mount
          │
PostgreSQL Container
├─ PostgreSQL Server
├─ Container Port 5432
└─ PostgreSQL Data Directory
```

Database は PostgreSQL Server が管理する論理的な Data の単位であり、その実データ File は PostgreSQL の Data Directory に保存される。

```text
Database
unit07_db
      ↓
PostgreSQL が管理
      ↓
Database Data File
      ↓
Named Volume
```

### Container の Writable Layer に DB Data を置かない

Volume を Mount しなければ、Container 内の書き込みは基本的にその Container の Writable Layer に属する。

```text
PostgreSQL Container
└─ Writable Layer
   └─ Database Data
```

この状態で Container を削除すると、Container 固有の Writable Layer も削除される。  
Database は通常、「Container は作り直したいが Data は残したい」という性質を持つ。

```text
Container
→ PostgreSQL Server の実行環境

Volume
→ PostgreSQL Data の保存場所
```

これは Unit 03 で学んだ Volume の代表的な実践用途である。

### Named Volume

この Unit では次の Named Volume を使用する。

```text
unit07-postgres-data
```

明示的に作成する。

```bash
docker volume create unit07-postgres-data
```

Container 起動時にこの Volume を PostgreSQL の Data 保存領域へ Mount する。

### PostgreSQL 18 の Data Directory

PostgreSQL Official Image は PostgreSQL 18 以降で Data Directory / Volume の構成が変更されている。

PostgreSQL 18 では Default の `PGDATA` は次である。

```text
/var/lib/postgresql/18/docker
```

Official Image が Volume の Mount 先として想定している Parent Directory は次である。

```text
/var/lib/postgresql
```

そのため、この Unit では Named Volume を次のように Mount する。

```text
unit07-postgres-data
        ↓
/var/lib/postgresql
        ↓
/var/lib/postgresql/18/docker
        ↓
実際の PostgreSQL Data
```

Command では次のようになる。

```text
--mount type=volume,src=unit07-postgres-data,dst=/var/lib/postgresql
```

PostgreSQL 17 以前の例でよく使われる `/var/lib/postgresql/data` を、PostgreSQL 18 の標準構成としてそのまま使わないことに注意する。

### Git Bash と Container Path

この学習環境では Windows 上の Git Bash を標準 Shell として使用している。  
Git Bash は `/var/lib/postgresql` のような Linux Path を Windows Path に自動変換する場合がある。

Volume の `dst=/data` で Path 変換が発生するため、Container Path を含む `docker container run` に `MSYS_NO_PATHCONV=1` を付けて Git Bash の自動 Path 変換を無効化する。

```bash
MSYS_NO_PATHCONV=1 docker container run ...
```

これは Docker / PostgreSQL の機能ではなく、Windows Git Bash 固有の Path 変換への対処である。

### Volume のライフサイクル

Named Volume は Container とは独立して存在する。

```text
Volume 作成
↓
Container A へ Mount
↓
Database Data 作成
↓
Container A 削除
↓
Volume は残る
↓
Container B へ同じ Volume を Mount
↓
既存 Database Data を利用
```

この Unit では実際に Table と Record を作成した後、Container を削除し、同じ Volume を使って PostgreSQL Container を再作成する。  
その後 `SELECT` を実行し、最初の Container で作成した Record が残っていることを確認する。

### Volume を削除するとどうなるか

Container を削除するだけでは Named Volume は残る。

```text
docker container rm
→ Container を削除

docker volume rm
→ Volume を削除
```

Database Data を完全に不要と判断した場合は、Container を削除した後に Volume 自体を削除する。

```text
Container 削除
≠
Database Data 削除
```

という関係を意識する。

## 使用するもの

### PostgreSQL Official Image

```text
postgres:18.6-alpine3.24
```

### Container

```text
unit07-postgres
```

### Named Volume

```text
unit07-postgres-data
```

### PostgreSQL User

```text
unit07_user
```

### PostgreSQL Password

```text
unit07-password
```

Local 学習専用の固定値として使用する。

### Database

```text
unit07_db
```

### Port

```text
Host      : 5432
Container : 5432
```

## 事前準備

Unit 06 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/07-postgresql-container
```

Git Bash で Repository 内の次の Unit Directory へ移動した状態から操作する。

```text
units/07-postgresql-container
```

現在位置を確認する。

```bash
pwd
```

この Unit では PostgreSQL Client を Host に追加 Installation する必要はない。  
PostgreSQL Official Image に含まれる `psql` を Container 内で利用する。

Host Port `5432` を別の PostgreSQL や Container が利用していないことも確認する。

```bash
docker container ls
```

## ハンズオン

### 1. PostgreSQL Official Image を取得する

```bash
docker image pull postgres:18.6-alpine3.24
```

取得した Image を確認する。

```bash
docker image ls postgres:18.6-alpine3.24
```

PostgreSQL 18.6 / Alpine Linux 3.24 を明示した Official Image を使用する。

### 2. Named Volume を作成する

```bash
docker volume create unit07-postgres-data
```

確認する。

```bash
docker volume ls
```

`unit07-postgres-data` が存在することを確認する。  
この時点では PostgreSQL Data はまだ作成されていない。

### 3. Named Volume の情報を確認する

```bash
docker volume inspect unit07-postgres-data
```

主に `Name`、`Driver`、`Mountpoint` を確認する。  
Volume の実体は Docker が管理するため、Host の `Mountpoint` を直接編集することは今回の学習では行わない。

### 4. PostgreSQL Container を起動する

Git Bash の Path 変換を無効化して実行する。

```bash
MSYS_NO_PATHCONV=1 docker container run \
  -d \
  --name unit07-postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=unit07_user \
  -e POSTGRES_PASSWORD=unit07-password \
  -e POSTGRES_DB=unit07_db \
  --mount type=volume,src=unit07-postgres-data,dst=/var/lib/postgresql \
  postgres:18.6-alpine3.24
```

この Command では複数の役割を同時に設定している。

```text
-p 5432:5432
→ Host / Container Port Mapping

POSTGRES_USER
POSTGRES_PASSWORD
POSTGRES_DB
→ 初回 Database 初期設定

--mount ...
→ Database Data を Named Volume に保存

postgres:18.6-alpine3.24
→ 実行する PostgreSQL Image
```

### 5. Container の起動状態を確認する

```bash
docker container ls
```

`unit07-postgres` が Running であることを確認する。  
PostgreSQL は初回起動時に Database Cluster を初期化するため、Container が起動直後の場合は準備に少し時間が必要なことがある。

続いて Logs を確認する。

```bash
docker container logs unit07-postgres
```

PostgreSQL が接続を受け付ける状態になったことを示す Log を確認する。

### 6. PostgreSQL Version を確認する

Container 内の `psql` を利用する。

```bash
docker container exec unit07-postgres \
  psql --version
```

PostgreSQL 18.6 系であることを確認する。  
Host に Installation された `psql` ではなく、Container 内の Client を実行している。

### 7. 初期作成された Database へ接続する

```bash
docker container exec unit07-postgres \
  psql -U unit07_user -d unit07_db \
  -c 'SELECT current_database(), current_user;'
```

`unit07_db` と `unit07_user` が確認できればよい。

```text
POSTGRES_USER
POSTGRES_DB
↓
初回 Container 起動時の初期化
↓
User / Database が作成
```

という流れを確認する。

### 8. PostgreSQL の Data Directory を確認する

```bash
docker container exec unit07-postgres \
  printenv PGDATA
```

次が表示されることを確認する。

```text
/var/lib/postgresql/18/docker
```

Container に Mount した Path は Parent Directory の `/var/lib/postgresql` である。

```text
Named Volume
↓
/var/lib/postgresql
└─ 18/docker
   └─ PostgreSQL Data
```

という関係を確認する。

### 9. Container の Mount 情報を確認する

```bash
docker container inspect \
  --format '{{json .Mounts}}' \
  unit07-postgres
```

Named Volume `unit07-postgres-data` が `/var/lib/postgresql` へ Mount されていることを確認する。

### 10. 学習用 Table を作成する

PostgreSQL 自体の SQL 学習が目的ではないため、Data 永続化確認用の最小 Table だけを作る。

```bash
docker container exec unit07-postgres \
  psql -U unit07_user -d unit07_db \
  -c 'CREATE TABLE learning_notes (id integer PRIMARY KEY, message text NOT NULL);'
```

Table が作成されたことを確認する。

```bash
docker container exec unit07-postgres \
  psql -U unit07_user -d unit07_db \
  -c '\dt'
```

`learning_notes` が表示されればよい。

### 11. Data を Insert する

```bash
docker container exec unit07-postgres \
  psql -U unit07_user -d unit07_db \
  -c "INSERT INTO learning_notes (id, message) VALUES (1, 'Data stored in named volume');"
```

確認する。

```bash
docker container exec unit07-postgres \
  psql -U unit07_user -d unit07_db \
  -c 'SELECT * FROM learning_notes;'
```

次の内容が確認できればよい。

```text
1 | Data stored in named volume
```

この Record は PostgreSQL が管理する Database Data として Named Volume 側へ保存されている。

### 12. Container と Volume の存在を整理する

現在は次の 2 つが別々に存在している。

```text
Container
unit07-postgres

Named Volume
unit07-postgres-data
```

確認する。

```bash
docker container ls -a \
  --filter 'name=unit07-postgres'

docker volume ls
```

Container と Volume が別の Docker Resource であることを改めて確認する。

### 13. PostgreSQL Container を削除する

```bash
docker container rm -f unit07-postgres
```

Container がなくなったことを確認する。

```bash
docker container ls -a \
  --filter 'name=unit07-postgres'
```

続いて Volume を確認する。

```bash
docker volume ls
```

`unit07-postgres-data` は残っている。

```text
Container
→ 削除済み

Named Volume
→ 存在

Database Data
→ Volume 内に保持
```

という状態である。

### 14. 同じ Volume で PostgreSQL Container を再作成する

最初と同じ Named Volume を Mount して Container を再作成する。

```bash
MSYS_NO_PATHCONV=1 docker container run \
  -d \
  --name unit07-postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=unit07_user \
  -e POSTGRES_PASSWORD=unit07-password \
  -e POSTGRES_DB=unit07_db \
  --mount type=volume,src=unit07-postgres-data,dst=/var/lib/postgresql \
  postgres:18.6-alpine3.24
```

ここでは新しい Container を作っている。

```text
以前の Container
→ 削除済み

今回の Container
→ 新規作成

利用する Volume
→ 同じ unit07-postgres-data
```

### 15. 再作成後の Logs を確認する

```bash
docker container logs unit07-postgres
```

初回の空 Volume からの初期化とは異なり、既存の PostgreSQL Data を利用して起動していることを確認する。  
この段階では、Container の Environment Variable から空の Database を新しく作り直しているのではない。

### 16. Container 削除前の Data が残っていることを確認する

```bash
docker container exec unit07-postgres \
  psql -U unit07_user -d unit07_db \
  -c 'SELECT * FROM learning_notes;'
```

Container 削除前に Insert した次の Record が表示されることを確認する。

```text
1 | Data stored in named volume
```

これによって、

```text
Data が残った理由
≠ Container が残っていたから

Data が残った理由
= Named Volume が残っていたから
```

ということを確認できる。

### 17. 初期設定 Environment Variable と既存 Volume の関係を整理する

再作成した Container には、最初と同じ `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` を指定している。  
ただし、Volume にはすでに初期化済み PostgreSQL Data が存在する。

```text
空 Volume
→ Environment Variable を使って初期化

初期化済み Volume
→ 既存 Data を使って起動
```

この違いを理解することが重要である。  
学習中に `POSTGRES_DB` などを書き換えたのに新しい Database が作られない場合、既存 Volume の初期化済み Data を利用している可能性がある。

### 18. Unit 07 の Container を削除する

```bash
docker container rm -f unit07-postgres
```

確認する。

```bash
docker container ls -a \
  --filter 'name=unit07-postgres'
```

Container が残っていないことを確認する。

### 19. Named Volume を削除する

今回は学習用 Data も不要になるため、最後に Named Volume を明示的に削除する。

```bash
docker volume rm unit07-postgres-data
```

確認する。

```bash
docker volume ls
```

`unit07-postgres-data` が表示されなくなればよい。  
この操作によって Database Data の保存先そのものを削除した。

### 20. PostgreSQL Image を確認する

```bash
docker image ls postgres:18.6-alpine3.24
```

Container / Volume を削除しても PostgreSQL Image は別 Resource として残っている。

```text
Image
→ PostgreSQL Container を作る Template

Container
→ 削除済み

Volume
→ 削除済み
```

後続 Unit でも PostgreSQL Official Image を利用するため、この Unit では Image を無理に削除する必要はない。

## 動作・確認ポイント

### PostgreSQL Official Image

以下を確認する。

- PostgreSQL Official Image を取得して利用できる。
- Image Tag で PostgreSQL / Alpine Linux の Version を明示できる。
- 自分で PostgreSQL 用 Dockerfile を作らなくても Runtime 設定を渡して Container を起動できる。

### Environment Variable

以下を確認する。

- `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` の基本的な役割を説明できる。
- これらが主に空の Data Directory を初期化するときに利用されることを理解している。
- 初期化済み Volume を再利用した場合は既存 Database Data が利用されることを理解している。

### PostgreSQL Container / Port

以下を確認する。

- PostgreSQL Server が Container Port `5432` で動作する。
- Host Port `5432` と Container Port `5432` を Mapping できる。
- Container 内の `psql` を利用して Database に接続できる。

### Named Volume

以下を確認する。

- `unit07-postgres-data` を明示的に作成・確認・削除できる。
- PostgreSQL 18 では `/var/lib/postgresql` を Volume の Mount 先として使用する。
- PostgreSQL の `PGDATA` が `/var/lib/postgresql/18/docker` であることを確認できる。
- Container と Named Volume が独立した Docker Resource であることを説明できる。

### Data 永続化

以下を確認する。

- PostgreSQL に Table / Record を作成できる。
- Container を削除しても Named Volume が残る。
- 同じ Volume を Mount した新しい PostgreSQL Container から既存 Record を読み出せる。
- Database Data が Container ではなく Volume のライフサイクルで保持されていることを説明できる。

## 学習ポイント

### Database Container では「実行環境」と「Data」を分離する

PostgreSQL Container の役割は PostgreSQL Server を実行することである。

```text
Container
→ PostgreSQL Process
```

一方、Database Data は Container の存在期間より長く保持したい。

```text
Named Volume
→ PostgreSQL Data
```

したがって、

```text
実行環境
→ Container

永続 Data
→ Volume
```

と分離する。

### Unit 03 の Volume が Database で実用的な意味を持つ

Unit 03 では `message.txt` を Volume に保存して永続化を確認した。

```text
Unit 03
File
↓
Named Volume
```

Unit 07 では同じ仕組みを PostgreSQL Data に適用する。

```text
Unit 07
Database Data
↓
Named Volume
```

Volume の仕組みが Database Container で重要になる理由を、実際の Data を使って確認する Unit である。

### Container の再作成は Database の再作成ではない

Named Volume が残っていれば、PostgreSQL Container を削除して作り直しても既存 Data を利用できる。

```text
Container A
↓
Database Data → Volume
↓
Container A 削除

Container B
↓
同じ Volume
↓
同じ Database Data
```

Container と Database Data を同一視しないことが重要である。

### Initial Configuration と Persistent Data を区別する

`POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` は、毎回 Container 起動時に Database を作り直す設定ではない。

```text
空の Data Directory
→ 初期化設定を利用

既存 Data
→ 既存 Database Cluster を利用
```

この性質を知らないと、Environment Variable を変更したのに Database が変わらない、という混乱につながる。

### PostgreSQL 18 の Volume Path を Version と結び付ける

PostgreSQL Official Image 18 以降では Volume / `PGDATA` の構成が変更されている。

```text
PostgreSQL 18
Volume Mount
→ /var/lib/postgresql

Default PGDATA
→ /var/lib/postgresql/18/docker
```

利用している Image Version の Official Documentation を確認する考え方が重要である。

### Official Image は設定して利用する

Unit 04〜06 では Dockerfile を作成して自分の Image を Build した。  
Unit 07 では PostgreSQL Official Image をそのまま利用する。

```text
Application
→ 自分の Dockerfile から Image を作る場合がある

Database
→ Official Image に Runtime 設定を渡して利用する場合がある
```

Docker を使うからといって、すべての Software に自作 Dockerfile が必要なわけではない。

### Environment Variable は設定と Data の役割が異なる

Environment Variable は Container の設定を渡す仕組みであり、Database Data 自体を保存する場所ではない。

```text
Environment Variable
→ 設定

Named Volume
→ Data
```

これらを混同しない。

### Unit 08 へのつながり

Unit 07 では Host から単一 PostgreSQL Container を操作した。  
次の Unit 08 では Docker Network を扱い、Container 同士を接続する。

```text
Unit 07
Host
↓
PostgreSQL Container

Unit 08
Container
↓ Docker Network
Container
```

その後 Unit 10 では Spring Boot Container と PostgreSQL Container を同じ Network / Compose 構成で接続する。

Unit 07 で、

```text
PostgreSQL Container
+
Persistent Volume
```

を単体で理解しておくことが、後の複数 Container 構成の前提になる。

## 完了条件

以下を満たしたら Unit 07 を完了とする。

- PostgreSQL Official Image を利用し、Image Tag を明示して Container を起動できる。
- `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` の役割と、初回 Data 初期化との関係を説明できる。
- PostgreSQL Container の Port `5432` を Host へ Publish し、Container 内の `psql` から Database を確認できる。
- Host / PostgreSQL Container / PostgreSQL Server / Database / Named Volume の関係を説明できる。
- Named Volume を作成し、PostgreSQL 18 の `/var/lib/postgresql` へ Mount できる。
- PostgreSQL 18 の Default `PGDATA` と Volume Mount Path の関係を大まかに説明できる。
- Table / Record を作成し、その Database Data が Named Volume に保持されることを理解している。
- PostgreSQL Container を削除した後、同じ Named Volume で新しい Container を作成できる。
- Container 再作成後も以前の Database Data を読み出し、Volume による永続化を確認できる。
- Container と Volume が別のライフサイクルを持つことを Database の例で説明できる。
- 初期化済み Volume では初期設定用 Environment Variable が毎回新しい Database を作るわけではないことを理解している。

ここまで確認できれば、Unit 08「Docker Network」へ進む。
