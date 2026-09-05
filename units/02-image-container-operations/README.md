# 02. Image・Container の基本操作とライフサイクル

## この項目の目的

この Unit では、Docker CLI を使って Image / Container の基本操作を繰り返し行い、両者を区別して扱えるようにする。  
Unit 01 では、Registry → Image → Container → Process という基本的な流れを確認した。  
この Unit ではそこから一歩進み、Image の取得・確認・Tag・削除と、Container の作成・起動・停止・再起動・削除などを実際に操作する。  
また、バックグラウンド実行、対話的実行、Logs、Container 内部でのコマンド実行、Inspect など、今後の Docker 学習で頻繁に利用する基本操作も扱う。

この Unit で最も重要なのは、コマンドを暗記することではなく、次の問いを意識しながら操作することである。

```text
今操作している対象は Image か？
それとも Container か？

Container は今どの状態か？

この操作によって Image / Container のどちらが作成・変更・削除されるか？
```

## 学習内容

### Image の基本操作

Image は Container を作成するための Template である。  
Docker CLI では、主に `docker image ...` という形式で Image を操作できる。

この Unit では以下を扱う。

- Registry から Image を取得する。
- Local に存在する Image を一覧表示する。
- Image に別の Tag を付ける。
- Image の詳細情報を確認する。
- 不要になった Image を削除する。

主に使用するコマンドは以下である。

```text
docker image pull
docker image ls
docker image tag
docker image inspect
docker image rm
```

### Image Tag

Docker Image は、一般的に次の形式で識別する。

```text
IMAGE_NAME:TAG
```

例:

```text
alpine:3.24.1
nginx:1.30.4-alpine3.24
```

Tag は Image の Version や用途などを識別するための名前として使われる。  
Tag を省略した場合は通常 `latest` が使われるが、`latest` は「常に最新 Version を保証する特別な仕組み」ではなく、単なる Tag 名である。  
今回の学習では、どの Image を使用したか後から分かるよう、Version を含む Tag を明示する。

同じ Image に複数の Tag を付けることもできる。

```text
同じ Image
├─ alpine:3.24.1
└─ unit02-alpine:practice
```

Tag を追加しても Image の内容自体が複製されるわけではない。  
別の名前から同じ Image を参照できるようになる、と考える。

### Container の基本操作

Container は Image を基に作成される実行単位である。  
Docker CLI では、主に `docker container ...` という形式で Container を操作できる。

この Unit では以下を扱う。

- Container を作成する。
- Container を作成してそのまま起動する。
- Container の一覧を確認する。
- Container を起動する。
- Container を停止する。
- Container を再起動する。
- Container を削除する。
- Container に名前を付ける。
- Container をバックグラウンドで実行する。
- Container を対話的に実行する。
- Logs を確認する。
- 実行中 Container 内でコマンドを実行する。
- Container の詳細情報を確認する。

主に使用するコマンドは以下である。

```text
docker container create
docker container run
docker container ls
docker container start
docker container stop
docker container restart
docker container rm
docker container logs
docker container exec
docker container inspect
```

`docker run` や `docker ps` のような短い形式も広く利用されるが、この Unit では操作対象を意識しやすくするため、可能な範囲で `docker image ...` / `docker container ...` の形式を使用する。

### `create` と `run` の違い

`docker container create` は、Image から Container を**作成するところまで**を行う。

```text
Image
  ↓ create
Container
  ↓
Created 状態
```

一方、`docker container run` は大まかには次をまとめて行う。

```text
Image
  ↓
Container を作成
  ↓
Container を起動
  ↓
Process を実行
```

したがって、次のように整理できる。

```text
docker container create
= Container を作成

docker container start
= 作成済み Container を起動

docker container run
= create + start に相当する一連の操作
```

実際には `run` では起動時の Option なども同時に指定できるため、日常的には `run` を頻繁に利用する。

### Container の基本的なライフサイクル

Container は作成されてから削除されるまで、状態を変化させる。  
この Unit では、基本的に次の流れを扱う。

```text
Image
  ↓
create / run
  ↓
Container
  ↓
Created
  ↓ start
Running
  ↓ stop
Exited
  ↓ start
Running
  ↓ restart
Running
  ↓ stop
Exited
  ↓ rm
削除
```

重要なのは、`stop` と `rm` が異なる操作であることである。

```text
stop
= Container は残るが Process は停止する

rm
= Container 自体を削除する
```

また、Container を削除しても、その元になった Image が自動的に削除されるわけではない。

### バックグラウンド実行

Web Server や Database のように継続して動作する Process は、Terminal を占有せずバックグラウンドで実行することが多い。  
`docker container run` に `-d` を付けると Detached Mode で実行できる。

```text
-d
= detached
= Container をバックグラウンドで実行
```

この Unit では Nginx をバックグラウンドで起動し、状態・Logs・Container 内部などを確認する。

### 対話的実行

Linux Container 内部の Shell などを操作したい場合は、対話的な Terminal を利用する。  
主に以下の Option を組み合わせる。

```text
-i
= interactive
= 標準入力を開いたままにする

-t
= tty
= 疑似 Terminal を割り当てる
```

一般的には次のようにまとめて指定する。

```text
-it
```

この Unit では Alpine Linux Container 内で `sh` を起動し、Container 内部の Shell を操作する。

### Logs

Container 内で動作する Process が標準出力・標準エラー出力へ出した内容は、Docker の Logs として確認できる。

```text
docker container logs CONTAINER
```

Container が動かない場合や、Application の状態を確認する場合に非常に重要な基本操作である。  
後続 Unit でも繰り返し利用する。

### `exec`

`docker container exec` は、**すでに実行中の Container 内で追加のコマンドを実行する**ために使用する。

```text
Running Container
  ↓
docker container exec
  ↓
Container 内で追加 Command を実行
```

`docker container run` が新しい Container を作成するのに対し、`docker container exec` は既存の実行中 Container を対象にする。  
この違いは重要である。

### Inspect

`docker image inspect` / `docker container inspect` を使うと、Image / Container の詳細情報を JSON 形式で確認できる。

```text
docker image inspect IMAGE

docker container inspect CONTAINER
```

表示される情報量は多いため、最初からすべて理解する必要はない。  
この Unit では、Docker が Image / Container について多くの Metadata を保持しており、必要なときに Inspect で確認できることを理解する。

### 不要リソースの整理と `prune`

Docker を使い続けると、停止済み Container や不要 Image などが Local Environment に残ることがある。  
個別に対象を指定して削除する以外に、`prune` 系コマンドで不要リソースをまとめて削除する方法がある。

例:

```text
docker container prune
docker image prune
docker system prune
```

ただし `prune` は複数リソースをまとめて削除するため、対象を理解せずに実行すると、別の学習や開発で残していたリソースまで削除する可能性がある。  
そのため、この学習では「とりあえず定期的に `prune` する」という使い方はしない。  
この Unit では、Container に Unit 02 用 Label を付け、対象を限定した `docker container prune` のみ実際に確認する。  
`docker image prune` / `docker system prune` は仕組みと注意点を理解するところまでとし、無条件では実行しない。

## 使用するもの

この Unit では、Docker Official Image から以下を使用する。

### Alpine Linux

```text
alpine:3.24.1
```

Alpine Linux は小さな Linux Distribution であり、対話的な Shell 操作や Container のライフサイクル確認に使用する。

### Nginx

```text
nginx:1.30.4-alpine3.24
```

Nginx は Web Server であり、この Unit では継続して動作する Process の例として使用する。  
Port Mapping や Browser からのアクセスは Unit 03 で扱うため、この Unit では Nginx 自体への Browser Access は行わない。

追加の Source Code、Dockerfile、設定ファイルなどは使用しない。

## 事前準備

Unit 01 が完了しており、Docker Desktop が起動していることを前提とする。  
Git Bash から以下が正常に利用できる状態で学習を開始する。

```bash
docker version
```

今回の Branch は以下とする。

```text
feature/02-image-container-operations
```

Unit 01 で作成した `hello-world` Image / Container が残っていても問題ない。  
この Unit では Unit 02 用に名前を分けて操作する。

## ハンズオン

### 1. Alpine Image を明示的に取得する

Unit 01 の `docker run hello-world` では、Local に Image が存在しなかったため Docker が自動的に Image を取得した。  
今回は Image の取得そのものを明示的に行う。

```bash
docker image pull alpine:3.24.1
```

基本形式は次のとおり。

```text
docker image pull NAME:TAG
```

今回の場合は以下の意味になる。

| 要素           | 意味                           |
| -------------- | ------------------------------ |
| `docker image` | Image を操作する               |
| `pull`         | Registry から Image を取得する |
| `alpine`       | Image Repository 名            |
| `3.24.1`       | 使用する Tag                   |

実行後、Docker Hub から Image が Local Docker Environment へ取得される。

```text
Docker Hub
   ↓ pull
alpine:3.24.1
   ↓
Local Docker Environment
```

### 2. Local Image の一覧を確認する

次を実行する。

```bash
docker image ls
```

一覧の中に以下が存在することを確認する。

```text
alpine   3.24.1
```

Unit 01 の `hello-world` Image が残っている場合は、それも同じ一覧に表示される。

主な列は以下のように見る。

| 列           | 意味                   |
| ------------ | ---------------------- |
| `REPOSITORY` | Image の Repository 名 |
| `TAG`        | Image に付いている Tag |
| `IMAGE ID`   | Image を識別する ID    |
| `CREATED`    | Image の作成時期       |
| `SIZE`       | Image の大きさ         |

ここでは `IMAGE ID` の値を暗記する必要はない。

### 3. Image の詳細情報を確認する

次を実行する。

```bash
docker image inspect alpine:3.24.1
```

JSON 形式で多くの情報が表示される。  
現時点では内容をすべて読む必要はない。  
たとえば Image ID、Architecture、OS、Config、RootFS、Metadata などが含まれていることを確認する。

必要な値だけを絞り込むこともできる。

```bash
docker image inspect --format '{{.Os}}/{{.Architecture}}' alpine:3.24.1
```

環境に応じて、たとえば以下のような値が表示される。

```text
linux/amd64
```

使用している PC の Architecture により `amd64` 以外になる場合もある。  
重要なのは、今回使用しているものが Linux Container Image であることを確認する点である。

### 4. Alpine Image に別の Tag を付ける

次を実行する。

```bash
docker image tag alpine:3.24.1 unit02-alpine:practice
```

一覧を確認する。

```bash
docker image ls
```

次の 2 つが表示されることを確認する。

```text
alpine          3.24.1
unit02-alpine   practice
```

両者の `IMAGE ID` を比較する。  
同じ `IMAGE ID` であれば、別の Image が複製されたのではなく、同じ Image に別の Tag が付いたことを確認できる。

```text
同じ Image ID
├─ alpine:3.24.1
└─ unit02-alpine:practice
```

### 5. 追加した Tag を削除する

Unit 02 で追加した Tag を削除する。

```bash
docker image rm unit02-alpine:practice
```

再度確認する。

```bash
docker image ls
```

`unit02-alpine:practice` はなくなり、元の `alpine:3.24.1` は残っていることを確認する。  
この操作から、Tag と Image 本体を完全に同一視しないことを意識する。

### 6. `create` で Container だけを作成する

次は Container のライフサイクルを確認する。  
Alpine Image から、すぐには起動しない Container を作成する。

```bash
docker container create \
  --name unit02-lifecycle \
  --label study.unit=02 \
  alpine:3.24.1 \
  sleep 300
```

複数行にしているが、1 つのコマンドである。  
Git Bash では行末の `\` によりコマンドを次の行へ継続している。

| 要素                      | 意味                                                 |
| ------------------------- | ---------------------------------------------------- |
| `create`                  | Image から Container を作成する                      |
| `--name unit02-lifecycle` | Container 名を指定する                               |
| `--label study.unit=02`   | Unit 02 用 Container であることを示す Label を付ける |
| `alpine:3.24.1`           | 元になる Image                                       |
| `sleep 300`               | Container 起動時に 300 秒待機する Process            |

この時点では Container を作成しただけで、まだ Process は実行されていない。

### 7. Created 状態を確認する

次を実行する。

```bash
docker container ls -a
```

`unit02-lifecycle` の `STATUS` が `Created` となっていることを確認する。

```text
alpine:3.24.1 Image
       ↓ create
unit02-lifecycle Container
       ↓
Created
```

`docker container ls` だけでは通常、Running 状態の Container のみが表示される。  
停止済みや Created 状態を含めて確認したい場合は `-a` を付ける。

```bash
docker container ls
docker container ls -a
```

この違いを確認する。

### 8. 作成済み Container を起動する

次を実行する。

```bash
docker container start unit02-lifecycle
```

続いて確認する。

```bash
docker container ls
```

`unit02-lifecycle` が表示され、Status が `Up ...` となっていることを確認する。

```text
Created
  ↓ start
Running
```

### 9. Container を停止する

次を実行する。

```bash
docker container stop unit02-lifecycle
```

確認する。

```bash
docker container ls -a
```

Status が `Exited` になっていることを確認する。

```text
Running
  ↓ stop
Exited
```

Container は停止しただけであり、削除されてはいない。

### 10. 停止した Container を再び起動する

次を実行する。

```bash
docker container start unit02-lifecycle
```

確認する。

```bash
docker container ls
```

同じ `unit02-lifecycle` Container が再び Running になっていることを確認する。  
新しい Container を作ったわけではなく、作成済みの Container を再度起動している。

### 11. Container を再起動する

Container が Running の状態で次を実行する。

```bash
docker container restart unit02-lifecycle
```

再度確認する。

```bash
docker container ls
```

`unit02-lifecycle` が Running のまま存在することを確認する。

```text
Running
  ↓ restart
Running
```

### 12. Container の詳細情報を Inspect する

次を実行する。

```bash
docker container inspect unit02-lifecycle
```

JSON 形式で Container の詳細情報が表示される。  
Container ID、作成日時、使用 Image、実行 Command、State、Network 設定、Mount 設定、Config、Label などが含まれる。

必要な情報だけを表示してみる。

```bash
docker container inspect \
  --format '{{.Name}} : {{.State.Status}}' \
  unit02-lifecycle
```

次のように Container 名と状態を確認できる。

```text
/unit02-lifecycle : running
```

Inspect の出力は今後 Network / Volume などを学習した際にも利用する。

### 13. Container を停止して削除する

まず停止する。

```bash
docker container stop unit02-lifecycle
```

Container 自体を削除する。

```bash
docker container rm unit02-lifecycle
```

確認する。

```bash
docker container ls -a
```

`unit02-lifecycle` が一覧からなくなっていることを確認する。

### 14. Container を削除しても Image が残ることを確認する

次を実行する。

```bash
docker image ls alpine:3.24.1
```

`unit02-lifecycle` Container は削除したが、`alpine:3.24.1` Image は残っていることを確認する。

```text
Container を rm
≠
Image を rm
```

### 15. Nginx Image を取得する

継続的に動作する Container の操作へ進む。

```bash
docker image pull nginx:1.30.4-alpine3.24
```

取得後に確認する。

```bash
docker image ls nginx:1.30.4-alpine3.24
```

### 16. Nginx Container をバックグラウンドで実行する

次を実行する。

```bash
docker container run \
  -d \
  --name unit02-nginx \
  --label study.unit=02 \
  nginx:1.30.4-alpine3.24
```

`-d` により Detached Mode で Container がバックグラウンド実行される。  
コマンド実行後、Container ID が表示され、Git Bash の Prompt がすぐ戻る。

確認する。

```bash
docker container ls
```

`unit02-nginx` が Running になっていることを確認する。  
この Unit では Port Mapping を設定していないため、Browser から Nginx を開く必要はない。

### 17. Nginx Container の Logs を確認する

次を実行する。

```bash
docker container logs unit02-nginx
```

Nginx Container の起動時に出力された Log を確認できる。

```text
Container 内の Process
  ↓ stdout / stderr
Docker
  ↓
docker container logs
  ↓
User が確認
```

### 18. 実行中 Container 内でコマンドを実行する

Nginx Container は現在 Running である。  
この既存 Container 内で Nginx の Version を確認する。

```bash
docker container exec unit02-nginx nginx -v
```

ここでは新しい Container は作られていない。

```text
run
= Image から新しい Container を作成して実行

exec
= 実行中の既存 Container 内で Command を実行
```

### 19. Nginx Container 内の Shell を開く

次を実行する。

```bash
docker container exec -it unit02-nginx sh
```

Prompt が変わり、現在は Host 側の Git Bash ではなく、Nginx Container 内部の `sh` を操作している状態になる。

Container 内で次を実行する。

```sh
pwd
cat /etc/alpine-release
```

Container 内部の File System が存在することを確認したら、Shell を終了する。

```sh
exit
```

Git Bash の Prompt に戻る。

```text
Windows Host
└─ Git Bash
   └─ docker container exec -it ...
      └─ Linux Container
         └─ sh
```

### 20. Nginx Container を停止・再開する

停止する。

```bash
docker container stop unit02-nginx
```

確認する。

```bash
docker container ls -a
```

再び起動する。

```bash
docker container start unit02-nginx
```

確認する。

```bash
docker container ls
```

同じ Container を停止・再開できることを確認する。

### 21. Alpine Container を対話的に `run` する

今度は、Container 作成時から対話的な Shell を起動する。

```bash
docker container run \
  -it \
  --name unit02-interactive \
  --label study.unit=02 \
  alpine:3.24.1 \
  sh
```

Container 内で次を実行する。

```sh
pwd
cat /etc/alpine-release
```

Shell を終了する。

```sh
exit
```

Container 内の主 Process である `sh` が終了するため、Container も停止する。  
Host 側へ戻ったら確認する。

```bash
docker container ls -a
```

`unit02-interactive` が `Exited` になっていることを確認する。

### 22. Detached Mode と Interactive Mode を整理する

Detached Mode:

```bash
docker container run -d ...
```

```text
Container
└─ Process はバックグラウンドで継続

Host Terminal
└─ Prompt はすぐ戻る
```

Interactive Mode:

```bash
docker container run -it ... sh
```

```text
Host Terminal
  ↓
Container 内 Shell と対話
```

### 23. `prune` 確認用 Container を作成する

`docker container prune` の対象を安全に限定して確認するため、Unit 02 用 Label を付けた停止 Container をもう 1 つ作る。

```bash
docker container create \
  --name unit02-prune-target \
  --label study.unit=02 \
  alpine:3.24.1 \
  echo prune-target
```

一度起動し、Process を終了させる。

```bash
docker container start -a unit02-prune-target
```

`-a` は Container の標準出力などを現在の Terminal へ Attach する Option である。  
次が表示される。

```text
prune-target
```

確認する。

```bash
docker container ls -a \
  --filter 'label=study.unit=02'
```

### 24. Unit 02 の停止済み Container だけを `prune` する

まず Nginx を停止する。

```bash
docker container stop unit02-nginx
```

現在の対象をもう一度確認する。

```bash
docker container ls -a \
  --filter 'label=study.unit=02'
```

対象を理解したうえで、次を実行する。

```bash
docker container prune \
  --filter 'label=study.unit=02'
```

確認を求められるため、対象を理解したうえで進める。  
実行後、確認する。

```bash
docker container ls -a \
  --filter 'label=study.unit=02'
```

Unit 02 で作成した停止済み Container が削除されていることを確認する。  
今回は他の Unit や別 Project の Container を巻き込まないよう、Unit 02 用 Label で対象を限定している。

### 25. `image prune` / `system prune` は実行せず意味を確認する

次のコマンドは、この Unit では無条件に実行しない。

```text
docker image prune
docker image prune -a
docker system prune
```

主な違いを整理する。

```text
docker image prune
= 基本的に Dangling Image を削除

docker image prune -a
= Container から参照されていない Image まで削除対象を広げる

docker system prune
= 複数種類の不要 Docker Resource をまとめて整理する
```

これらは便利だが、「削除してよいもの」を確認せずに使うコマンドとして覚えない。

### 26. Unit 02 で使用した Nginx Image を削除する

Container は先ほどの `prune` で削除済みである。  
Nginx Image が残っていることを確認する。

```bash
docker image ls nginx:1.30.4-alpine3.24
```

Image を明示的に削除する。

```bash
docker image rm nginx:1.30.4-alpine3.24
```

確認する。

```bash
docker image ls nginx:1.30.4-alpine3.24
```

Container を削除しただけでは Image は残り、Image を消すには Image に対する削除操作が必要であることを再確認する。

### 27. Alpine Image も削除する

Unit 02 用 Container が残っていないことを確認する。

```bash
docker container ls -a \
  --filter 'label=study.unit=02'
```

何も表示されなければ、Alpine Image を削除する。

```bash
docker image rm alpine:3.24.1
```

確認する。

```bash
docker image ls alpine:3.24.1
```

これで Unit 02 で新たに取得・作成した主要な Docker Resource は整理された状態になる。  
Unit 01 の `hello-world` Image / Container が残っている場合は、この Unit では無理に削除しなくてもよい。

## 動作・確認ポイント

### Image の取得・一覧・Tag・Inspect

以下を確認する。

- `docker image pull` で Image を明示的に取得できる。
- `docker image ls` で Local Image を確認できる。
- Repository / Tag / Image ID が別の情報であることを確認できる。
- `docker image tag` で別の Tag を付けられる。
- 元 Tag と追加 Tag の `IMAGE ID` が同じになる。
- Tag を付けても Image 本体を複製しているわけではない。
- `docker image inspect` で JSON 形式の詳細情報を取得できる。
- 必要に応じて `--format` で特定情報を取り出せる。

### Container の作成・状態・ライフサイクル

以下を確認する。

- `docker container create` 直後は `Created` になる。
- `docker container start` により作成済み Container が Running になる。
- `stop` 後も Container 自体は残る。
- 停止済み Container を `start` で再開できる。
- Running Container を `restart` できる。
- `rm` すると Container 自体が一覧からなくなる。
- Container を削除しても元 Image は残る。

### Container 名・Detached / Interactive

以下を確認する。

- `--name` を指定すると Container を名前で操作できる。
- `-d` で Container をバックグラウンド実行できる。
- `-it` で Container 内 Shell と対話できる。
- Host 側 Git Bash と Container 内 `sh` を区別できる。
- `exit` によって主 Process の Shell が終了すると Container も停止する。

### Logs / `exec` / Inspect

以下を確認する。

- `docker container logs` で Container の Logs を確認できる。
- `docker container exec` で Running Container 内の Command を実行できる。
- `run` は新しい Container、`exec` は既存 Container を対象にする。
- `docker container inspect` で State、Image、Command、Network、Mount、Label などを確認できる。

### 削除・`prune`

以下を確認する。

- Container と Image は別々に削除する。
- `docker container prune` は停止済み Container をまとめて削除する操作である。
- Label Filter によって対象を限定できる。
- `docker image prune` / `docker system prune` は影響範囲を理解してから使用する必要がある。
- 「不要そうだからとりあえず prune」という使い方をしない。

## 学習ポイント

### Image と Container の操作対象を明確にする

Docker CLI には Image / Container の両方を扱うコマンドがある。  
操作するときは、コマンド名だけでなく対象を意識する。

```text
docker image ...
= Image を操作

docker container ...
= Container を操作
```

たとえば次の 2 つは異なる。

```text
docker container rm
= Container を削除

docker image rm
= Image を削除
```

### Image は Container より前に存在する

基本的な依存関係は次の向きである。

```text
Image
  ↓
Container
```

Container は Image を基に作成される。  
一方、Container を削除しても Image は自動的には削除されない。

### Container の状態と存在を分けて考える

停止した Container は「存在しない Container」ではない。

```text
Running
= Container が存在し、Process が動いている

Exited
= Container は存在するが、Process は停止している

rm 後
= Container 自体が存在しない
```

### `create` / `start` / `run` を区別する

```text
create
= Container を作成する

start
= 作成済み Container を起動する

run
= 新しい Container を作成して起動する
```

`run` を毎回実行すると、基本的には新しい Container が作られる。  
既存 Container を再び動かしたい場合は `start` を使う。

### `run` / `exec` を区別する

```text
docker container run
= Image から新しい Container を作って Command を実行

docker container exec
= Running Container 内で追加 Command を実行
```

Container 内部へ入る操作をすべて `run` と考えないことが重要である。

### Container の主 Process とライフサイクル

Container は内部の主 Process と強く結び付いている。  
Nginx は Server Process が継続して動くため Container も Running を維持する。  
一方、対話的 Alpine Container では主 Process である `sh` を `exit` すると Container も停止する。

```text
主 Process が動作中
↓
Container Running

主 Process が終了
↓
Container Exited
```

### Image Tag は Image を識別する名前の 1 つ

同じ Image に複数 Tag を付けられる。

```text
同じ Image ID
├─ alpine:3.24.1
└─ unit02-alpine:practice
```

したがって、Tag と Image の実体を完全に同一視しない。

### Logs / Inspect / Exec は今後も繰り返し使う

Docker で問題が発生した場合、まず確認する基本的な手段として以下がある。

```text
Container の状態
↓
docker container ls -a

Process の出力
↓
docker container logs

設定・状態の詳細
↓
docker container inspect

実行中 Container 内部
↓
docker container exec
```

これらは Unit 02 だけのコマンドではなく、後続 Unit のトラブルシューティングでも繰り返し利用する。

### `prune` は便利だが対象範囲を意識する

個別削除では対象を明示する。

```bash
docker container rm unit02-example
docker image rm example:tag
```

一方、`prune` は条件に合う複数 Resource をまとめて削除する。  
そのため、学習・開発環境では「何が削除されるか」を確認してから使う。

```text
まず個別削除を理解する
↓
必要な場合だけ prune を使う
↓
対象範囲を確認する
```

## 完了条件

以下をすべて満たしたら Unit 02 を完了とする。

- Image と Container を別の操作対象として説明できる。
- `docker image pull` で Image を取得できる。
- `docker image ls` で Local Image を確認できる。
- Repository / Tag / Image ID の役割を大まかに説明できる。
- `docker image tag` で同じ Image に別 Tag を付けられる。
- Tag を追加しても Image 本体が複製されるわけではないことを理解している。
- `docker image inspect` で Image の詳細情報を確認できる。
- `docker image rm` で対象 Image / Tag を削除できる。
- `docker container create` と `docker container run` の違いを説明できる。
- `docker container start` で作成済み Container を起動できる。
- `docker container stop` で Container を停止できる。
- 停止済み Container が削除されたわけではないことを理解している。
- `docker container restart` で Running Container を再起動できる。
- `docker container rm` で Container 自体を削除できる。
- Container を削除しても Image が残ることを確認できる。
- `--name` を使って Container に名前を付け、名前で操作できる。
- `-d` による Detached Mode を説明・実行できる。
- `-it` による Interactive Mode を説明・実行できる。
- Host 側 Git Bash と Container 内部の `sh` を区別できる。
- `docker container logs` で Container の Logs を確認できる。
- `docker container exec` で Running Container 内の Command を実行できる。
- `run` と `exec` の違いを説明できる。
- `docker container inspect` で Container の詳細情報を確認できる。
- Container の基本ライフサイクルを説明できる。

```text
Image
  ↓ create / run
Container Created
  ↓ start
Running
  ↓ stop
Exited
  ↓ start
Running
  ↓ stop
Exited
  ↓ rm
Container 削除
```

- `docker container prune` が停止済み Container をまとめて削除する操作であることを理解している。
- `prune` 系コマンドは影響範囲を確認してから使用する必要があることを理解している。
- 次の問いに対して、自分で操作対象を判断できる。

```text
Image を取得したい
→ docker image ...

Container を起動・停止したい
→ docker container ...

実行中 Container の Log を見たい
→ docker container logs

実行中 Container 内で Command を実行したい
→ docker container exec

Container を削除したい
→ docker container rm

Image を削除したい
→ docker image rm
```

ここまで確認できれば、Unit 03「Port・Bind Mount・Volume」へ進む。
