# 01. Docker・Container の基本概念と環境確認

## この項目の目的

この Unit では、Docker を操作していくための最初の土台として、Docker / Container / Image がそれぞれ何を表し、どのように関係しているのかを理解する。  
また、Windows 上で Docker Desktop を利用して Linux Container を動かす今回の環境について、Docker CLI / Docker Engine / WSL 2 の役割を大まかに整理する。  
最後に、Docker Hub で公開されている最小の Image を使って実際に Container を起動し、Image の取得から Container の実行までに何が起きているのかを確認する。  
この Unit では、Docker の詳細な操作方法を一度に覚えることは目的としない。  
まずは、今後の Unit で繰り返し登場する基本用語と全体像を、実際の操作と結び付けて理解することを目標とする。

## 学習内容

### Docker とは

Docker は、アプリケーションとその実行に必要な環境を Container として扱うためのプラットフォームである。  
アプリケーション本体だけでなく、実行時に必要となる Library や設定などを Image としてまとめ、その Image を基に Container を実行できる。  
従来、アプリケーションを別の PC や Server で動かす場合には、OS 上へ必要な Runtime、Library、Middleware などを個別に準備し、それぞれの Version や設定をそろえる必要があった。  
Docker では、実行環境を Image として定義・配布し、その Image から Container を作ることで、環境差異を小さくしやすくなる。

Docker は主に次の仕組みをまとめて扱う。

- Image を取得・作成・管理する。
- Image から Container を作成・実行する。
- Container の停止・削除・状態確認を行う。
- Container と Host の Port、File、Network などを接続する。
- Image を Registry へ保存・共有する。

これらは後続 Unit で段階的に扱う。

### Container とは

Container は、アプリケーションなどの Process を周囲から分離された環境で実行する仕組みである。  
Docker では、Container ごとに File System、Process、Network などが分離され、Host や他の Container への影響を小さくした状態でアプリケーションを動かせる。  
重要なのは、Container を「小さな仮想マシン」と考えすぎないことである。  
Container の中心にあるのは、独立した OS 全体ではなく、隔離された Process とその Process が利用する実行環境である。

たとえば Web Application、API、Database をそれぞれ別 Container として動かす場合、概念的には次のようになる。

```text
Host
├─ Container A
│  └─ Frontend Process
├─ Container B
│  └─ Backend Process
└─ Container C
   └─ Database Process
```

各 Container は独立して管理できるため、ある Container を停止・削除しても、それだけで他の Container が削除されるわけではない。

### Image とは

Image は、Container を作成するための読み取り専用の Template である。  
アプリケーションの実行に必要な File、Library、Runtime、初期設定、起動時の命令などを含めることができる。

```text
Image
  ↓ Container を作成
Container
  ↓ Process を実行
Application
```

1 つの Image から複数の Container を作成できる。

```text
            ┌─ Container A
Image ──────┼─ Container B
            └─ Container C
```

Image 自体と、Image から作られて実際に動く Container は別の存在である。  
この区別は Docker 学習全体を通して非常に重要になる。

### Image と Container の関係

Image は「実行環境の元になる Template」、Container は「その Image を基に作られた実行単位」と考える。  
たとえば `hello-world` という Image を利用する場合、Docker はその Image を基に Container を作成し、Image に定義された Process を Container 内で実行する。

```text
hello-world Image
        ↓
Container を作成
        ↓
Container 内の Process を実行
        ↓
Process 終了
        ↓
Container は停止状態になる
```

Process が終了しても、Image が自動的に削除されるわけではない。  
また、停止した Container と Image も別々に残る。  
Container のライフサイクルや削除操作については Unit 02 で詳しく扱う。

### Container と仮想マシンの違い

仮想マシンと Container は、どちらも実行環境を分離するために利用できるが、仕組みが異なる。  
仮想マシンでは、一般的に仮想 Hardware の上で Guest OS 全体を起動する。

```text
Physical Machine
└─ Host OS / Hypervisor
   ├─ Virtual Machine A
   │  ├─ Guest OS
   │  └─ Application
   └─ Virtual Machine B
      ├─ Guest OS
      └─ Application
```

一方、Container は OS Kernel を共有しながら Process を分離する。

```text
Machine
└─ OS Kernel
   ├─ Container A
   │  └─ Application
   └─ Container B
      └─ Application
```

このため一般的に Container は、Guest OS 全体を起動する仮想マシンより軽量に起動・破棄しやすい。  
ただし、Container と仮想マシンはどちらか一方だけを使うものではなく、仮想マシン上で Docker を動かす構成も存在する。

### Host と Container

Host は Docker を動かしている側の Machine / OS を指す。  
今回の学習環境では、普段操作している Windows PC が Host 側になる。

```text
Host
└─ Windows PC
```

Docker Desktop は Windows 上で Linux Container を利用できる環境を提供する。  
Container は Host から分離されているため、Container 内の File、Process、Network は Host のものと完全に同じではない。  
今後 Port、Bind Mount、Volume、Network などを学ぶ際には、「今見ているものが Host 側なのか Container 側なのか」を常に意識する。

### Docker Engine

Docker Engine は、Image や Container を実際に管理・実行する中心的な仕組みである。  
Docker CLI から送られた要求を受け取り、Image の取得や Container の作成・起動などを行う。

```text
User
  ↓
Docker CLI
  ↓
Docker Engine
  ├─ Image を管理
  └─ Container を管理・実行
```

Docker Engine の中心となる Daemon は、Docker CLI とは別の役割を持つ。  
コマンドを入力する側と、実際に Container を管理する側を分けて考える。

### Docker CLI

Docker CLI は、Terminal から `docker` コマンドを使って Docker Engine へ操作を依頼するための Interface である。

```bash
docker version
docker run hello-world
```

今回の学習では、通常の操作を Git Bash から行う。  
Git Bash は Docker Engine そのものではなく、ユーザーが Docker CLI コマンドを入力するために使用する Shell である。

```text
Git Bash
  ↓
docker コマンド
  ↓
Docker CLI
  ↓
Docker Engine
```

### Docker Desktop

Docker Desktop は、Windows や macOS などで Docker を利用しやすくするための Desktop Application である。  
Docker Engine を動かすための環境、Docker CLI、GUI、各種設定などをまとめて提供する。  
今回の学習では、Docker Desktop の GUI を主な操作手段にはしない。  
Docker の仕組みと CLI 操作を理解するため、基本的には Git Bash から Docker CLI を使用する。  
ただし Docker Desktop は裏側の Docker 環境を提供しているため、Docker CLI を使う場合でも Docker Desktop が正常に起動している必要がある。

### Windows / WSL 2 / Linux Container の大まかな関係

今回の学習環境では Windows 上で Docker Desktop を使用し、Docker Desktop は WSL 2 backend を利用して Linux Container を実行する。

```text
Windows
├─ Git Bash
│  └─ Docker CLI
│
└─ Docker Desktop
   └─ WSL 2 backend
      └─ Linux Container
```

普段の学習では、WSL 2 の内部構造を意識して操作する必要はない。  
C Drive 配下にある `docker-foundations-study` Repository 上で Git Bash を使用し、通常どおり Docker CLI を実行する。  
WSL 2 は Docker Desktop が Windows 上で Linux Container を動かすための基盤として利用している、と大まかに理解しておけばよい。

### Registry

Registry は、Container Image を保存・配布するための仕組みである。  
Local PC だけで Image を管理するのではなく、Registry を利用することで別の Machine や他の利用者と Image を共有できる。

```text
Registry
   ↓ Image を取得
Local Docker Environment
   ↓
Container
```

Registry には Public / Private のものがあり、Docker Hub 以外にも Cloud Provider などが提供する Registry が存在する。

### Docker Hub

Docker Hub は Docker の代表的な Public Registry であり、多くの Container Image が公開されている。  
Docker CLI で Image 名だけを指定した場合、一般的な構成では Docker Hub から Image を取得する。  
今回使用する `hello-world` も Docker Hub で公開されている Official Image である。  
この Unit では Docker Hub への Account 作成や Login、Image の Push は行わない。  
自分で作成した Image を Docker Hub へ Push する操作は Unit 12 で扱う。

### Image から Container が実行される基本的な流れ

`docker run hello-world` を初めて実行する場合、大まかには次の処理が行われる。

```text
1. Git Bash で docker run hello-world を実行
             ↓
2. Docker CLI が Docker Engine へ実行を依頼
             ↓
3. Docker Engine が Local に hello-world Image があるか確認
             ↓
4. Image がなければ Registry から取得
             ↓
5. Image を基に新しい Container を作成
             ↓
6. Container を起動
             ↓
7. Container 内の Process が実行される
             ↓
8. hello-world の Message が表示される
             ↓
9. Process が終了
             ↓
10. Container も停止状態になる
```

`docker run` は単純に「既存 Container を起動するコマンド」ではない。  
指定した Image を基に新しい Container を作成して実行するコマンドである。

## 使用するもの

この Unit では以下を使用する。

- Windows
- Docker Desktop
- WSL 2 backend
- Linux Container
- Git Bash
- Docker CLI
- `hello-world` Official Image
- Docker Hub

追加の Source Code、Dockerfile、設定ファイルなどは使用しない。

## 事前準備

学習開始前の環境構築・事前準備が完了していることを前提とする。  
最低限、以下が成立していることを確認する。

- Docker Desktop が起動している。
- Docker Desktop が Linux Container を利用できる状態である。
- Git Bash から Docker CLI を利用できる。
- Git Bash から Docker Compose CLI を利用できる。
- `docker-foundations-study` Repository の Unit 01 用 Branch で作業している。

今回の Branch は以下とする。

```text
feature/01-docker-container-basics
```

この Unit のハンズオンでは Docker Desktop を起動した状態で操作する。

## ハンズオン

### 1. Docker CLI と Docker Engine の接続を確認する

Repository 上で Git Bash を開き、次を実行する。

```bash
docker version
```

`docker --version` ではなく `docker version` を使用する点に注目する。  
`docker --version` は主に Docker CLI 自体の Version を簡潔に表示する。  
一方、`docker version` は Docker CLI が Docker Engine と通信し、Client / Server の双方の情報を表示する。

出力には大きく次の 2 つが表示される。

```text
Client:
 ...

Server:
 ...
```

ここでは細かな Version 番号を覚える必要はない。  
重要なのは、次の対応関係である。

```text
Client
= Docker CLI 側

Server
= Docker Engine 側
```

`Client` と `Server` の両方が表示されていれば、Docker CLI から Docker Engine へ通信できている。

### 2. `hello-world` Container を実行する

次を実行する。

```bash
docker run --name unit01-hello hello-world
```

今回初めて重要になるコマンドは `docker run` である。

```text
docker run [OPTIONS] IMAGE
```

という形で使用し、指定した Image から新しい Container を作成して実行する。

| 要素                  | 意味                                                    |
| --------------------- | ------------------------------------------------------- |
| `docker`              | Docker CLI を実行する                                   |
| `run`                 | Image から新しい Container を作成して実行する           |
| `--name unit01-hello` | 作成する Container に `unit01-hello` という名前を付ける |
| `hello-world`         | 使用する Image                                          |

`--name` は Container を後から識別しやすくするために付けている。  
Container の名前や詳細な管理方法は Unit 02 で改めて扱う。

初回実行時、Local に `hello-world` Image が存在しなければ、出力の途中に Image を取得する処理が表示される。  
典型的には、次のような流れが確認できる。

```text
Unable to find image 'hello-world:latest' locally
...
Pulling from library/hello-world
...
Hello from Docker!
```

具体的な Digest、ID、取得表示などは環境や実行時点によって異なるため、完全に同じ表示になる必要はない。

### 3. 実行時に何が起きたか整理する

先ほどの操作を、コマンドと Docker 内部の動きに対応させる。

```text
docker run --name unit01-hello hello-world
                     │
                     ▼
               Docker CLI
                     │
                     ▼
            Docker Engine
                     │
          Local に Image があるか確認
                     │
          ┌──────────┴──────────┐
          │                     │
       ある                    ない
          │                     │
          │              Docker Hub から取得
          │                     │
          └──────────┬──────────┘
                     │
                     ▼
           hello-world Image
                     │
                     ▼
      unit01-hello Container 作成
                     │
                     ▼
            Container 起動
                     │
                     ▼
        Message を標準出力へ表示
                     │
                     ▼
             Process 終了
                     │
                     ▼
          Container も停止
```

`Hello from Docker!` という Message が表示されたこと自体より、そこへ至るまでに Image と Container がどのように使われたかを理解することが重要である。

### 4. 作成された Container を確認する

次を実行する。

```bash
docker container ls -a
```

`docker container ls` は Container の一覧を確認するコマンドである。  
`-a` は停止済みを含むすべての Container を表示する Option である。  
このコマンド自体の詳細な使い方は Unit 02 で扱う。  
この Unit では、先ほど作成した `unit01-hello` が存在することだけ確認する。

一覧の `NAMES` 列に次が表示されることを確認する。

```text
unit01-hello
```

また、`IMAGE` 列には次が表示される。

```text
hello-world
```

ここから、次の関係を実際の Container 一覧で確認できる。

```text
hello-world Image
        ↓
unit01-hello Container
```

### 5. Container が停止していることを確認する

`hello-world` は Message を表示すると Process が終了する。  
Container は内部で動かしている主 Process が終了すると停止するため、`unit01-hello` は実行後も動き続けない。  
`docker container ls -a` の `STATUS` を確認し、`Exited` となっていることを確認する。

```text
Container 作成
    ↓
Container 起動
    ↓
hello-world Process 実行
    ↓
Process 終了
    ↓
Container 停止
```

この結果から、「Container は常に起動し続ける仮想 PC ではなく、その中で実行する Process と密接に関係している」ことを確認する。

### 6. Local に取得された Image を確認する

次を実行する。

```bash
docker image ls hello-world
```

`docker image ls` は Local に存在する Image を確認するコマンドである。  
ここでは `hello-world` Image のみを対象として表示している。

一覧に次の Repository が表示されることを確認する。

```text
hello-world
```

これにより、現在は概念的に次の 2 つが別々に存在していることが分かる。

```text
Local Image
└─ hello-world

Container
└─ unit01-hello
   └─ Status: Exited
```

Container 内の Process はすでに終了しているが、Image がなくなったわけではない。

### 7. 同じ Image からもう 1 つ Container を実行する

1 つの Image から複数の Container を作れることを確認するため、もう一度実行する。

```bash
docker run --name unit01-hello-second hello-world
```

今度はすでに Local に `hello-world` Image が存在するため、通常は初回のような Image 取得処理をせず、Local Image を利用して新しい Container が作成される。

続いて確認する。

```bash
docker container ls -a
```

次の 2 つが存在することを確認する。

```text
unit01-hello
unit01-hello-second
```

どちらの `IMAGE` 列も `hello-world` である。

```text
hello-world Image
├─ unit01-hello Container
└─ unit01-hello-second Container
```

これが Image と Container を別物として考える重要な理由の 1 つである。

### 8. この Unit の操作結果を整理する

ここまでで Local Docker Environment には、概ね次のものが存在している。

```text
Docker Environment
├─ Image
│  └─ hello-world
│
└─ Containers
   ├─ unit01-hello
   │  └─ Exited
   └─ unit01-hello-second
      └─ Exited
```

この Unit では、これらを削除せずそのまま残してよい。  
Container / Image の削除、再起動、整理などのライフサイクル操作は Unit 02 で扱う。

## 動作・確認ポイント

### `docker version`

以下を確認する。

- Command Error にならない。
- `Client` が表示される。
- `Server` が表示される。
- Docker CLI と Docker Engine が別の役割であることを意識できる。

### 1 回目の `docker run`

以下を確認する。

- `hello-world` Image が Local にない場合、取得処理が行われる。
- `Hello from Docker!` を含む Message が表示される。
- Container の Process が終了すると Command も終了する。

### `docker container ls -a`

以下を確認する。

- `unit01-hello` が存在する。
- `unit01-hello-second` が存在する。
- どちらも `hello-world` Image を使用している。
- Status が `Exited` になっている。
- Container と Image の名前が別々の列に表示される。

### `docker image ls hello-world`

以下を確認する。

- Local に `hello-world` Image が存在する。
- Container の Process が終了した後も Image は残っている。

### 2 回目の `docker run`

以下を確認する。

- 同じ `hello-world` Image から別の Container を作成できる。
- 初回のような Image 取得が通常は発生しない。
- 1 Image : 1 Container ではないことを確認できる。

## 学習ポイント

### Docker の基本構造

最初に押さえる基本構造は次である。

```text
Registry
   ↓
Image
   ↓
Container
   ↓
Process
```

Docker Hub などの Registry に Image が保存される。  
その Image を Local Docker Environment へ取得し、Image を基に Container を作成する。  
Container 内で Process が実行される。

### Image と Container は別物

今回のハンズオンでは、1 つの `hello-world` Image から 2 つの Container を作成した。

```text
hello-world Image
├─ unit01-hello
└─ unit01-hello-second
```

Image は Container の元になる Template であり、Container はその Image から作られた個別の実行単位である。

### Container は Process と結び付いている

`hello-world` Container がすぐ `Exited` になったのは異常ではない。  
Container 内で実行する `hello-world` の Process が目的の処理を終えたため、Container も停止した。  
Web Server や Database のように長時間動く Process を実行する Container は、Process が動き続ける限り起動状態を保つ。  
この違いは後続 Unit で実際に確認する。

### `docker run` の意味

`docker run` は、「Image を実行する」という表現だけで覚えない。  
より正確には、今回の範囲では次のように理解する。

```text
指定した Image
   ↓
新しい Container を作成
   ↓
Container を起動
   ↓
Container 内の Process を実行
```

さらに Local に Image が存在しない場合は、必要に応じて Registry から Image を取得する。

### Docker CLI と Docker Engine

Git Bash に入力している `docker` コマンドは Docker CLI である。  
実際に Image や Container を管理している中心は Docker Engine である。

```text
User
  ↓
Git Bash
  ↓
Docker CLI
  ↓
Docker Engine
  ↓
Image / Container
```

この Client / Server 的な関係を理解しておくと、後に「Docker Command は入力できるが Docker Engine に接続できない」といった問題も切り分けやすくなる。

### Docker Desktop と WSL 2

今回の Windows 環境では、Docker Desktop が WSL 2 backend を利用して Linux Container を実行する。  
通常の学習では WSL 2 を直接操作する必要はなく、C Drive 配下の Repository で Git Bash から Docker CLI を実行すればよい。

```text
Windows
├─ Git Bash → Docker CLI
└─ Docker Desktop
   └─ WSL 2 backend
      └─ Linux Container
```

Docker を学ぶうえでは、この Unit の段階で WSL 2 の内部実装まで理解する必要はない。

### Container と仮想マシン

Container は Guest OS 全体を個別に起動する仮想マシンとは異なり、OS Kernel を共有しながら Process を分離する。  
そのため、「Container = 小型の仮想マシン」と完全に同一視しないことが重要である。

### Host と Container を区別する

今後の Docker 学習では、次の問いを繰り返し意識する。

```text
これは Host 側のものか？
それとも Container 側のものか？
```

Port、File、Volume、Network、Environment Variable など、多くの Docker の概念はこの境界と関係する。

## 完了条件

以下をすべて満たしたら Unit 01 を完了とする。

- Docker / Container / Image の違いを大まかに説明できる。
- Image が Container を作成するための Template であることを説明できる。
- 1 つの Image から複数 Container を作成できることを理解している。
- Container と仮想マシンが同じ仕組みではないことを説明できる。
- Host と Container を区別して考える必要があることを理解している。
- Docker CLI と Docker Engine の役割の違いを大まかに説明できる。
- Docker Desktop が Windows 上で Docker を利用するための環境を提供していることを理解している。
- 今回は Docker Desktop が WSL 2 backend を利用して Linux Container を動かしていることを大まかに説明できる。
- Registry が Image を保存・配布する仕組みであることを説明できる。
- Docker Hub が代表的な Public Registry であることを理解している。
- `docker version` を実行し、Client / Server の双方を確認できる。
- `docker run --name unit01-hello hello-world` を正常に実行できる。
- 初回実行時に、Local に Image がなければ Registry から取得される流れを確認できる。
- `docker container ls -a` で作成された Container と元の Image の関係を確認できる。
- `docker image ls hello-world` で Local Image を確認できる。
- 同じ `hello-world` Image から 2 つの Container を作成できる。
- `hello-world` の Process 終了後に Container が `Exited` になる理由を説明できる。
- 次の基本的な流れを自分の言葉で説明できる。

```text
Registry
↓
Image を取得
↓
Image から Container を作成
↓
Container を起動
↓
Container 内で Process を実行
```

ここまで確認できれば、Unit 02「Image・Container の基本操作とライフサイクル」へ進む。
