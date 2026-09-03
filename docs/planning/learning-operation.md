# Docker Foundations Study - Learning Operation

## 1. 文書の目的

本ドキュメントは、`docker-foundations-study` の学習カリキュラムを、GitHub リポジトリ上でどのように進め、どのような成果物として残すかを定義する。

学習対象・学習順序・各 Unit の内容そのものは `learning-curriculum.md` で管理し、本ドキュメントでは以下のような運用面を扱う。

- リポジトリ構成
- Unit ごとの成果物
- Unit README の共通形式
- サンプルコード・設定ファイルの扱い
- 学習の進め方
- Unit の生成単位
- Git / GitHub 運用
- 学習開始前の準備
- Unit 完了基準
- 最終的なリポジトリ完成状態

学習の目的は Docker / Container を実際に操作しながら理解することであり、ドキュメント整備やリポジトリの見栄え自体を目的にはしない。

---

## 2. リポジトリの位置づけ

リポジトリ名は以下とする。

```text
docker-foundations-study
```

本リポジトリは、Docker / Container の基礎を体系的に一周し、実際に手を動かして学習した内容と成果物を整理して残すための学習用リポジトリとする。

主な目的は以下のとおり。

- Docker / Container を実際に操作しながら理解する。
- Image / Container / Port / Volume / Network / Dockerfile / Docker Compose などを段階的に学ぶ。
- React / Spring Boot / PostgreSQL を題材として、実際の Web システムに近い複数 Container 構成まで経験する。
- 学習後に自分で見返した際、各段階で何を学び、何を動かしたのか分かる状態にする。
- 他者からの見え方やポートフォリオ性より、学習のしやすさ・理解のしやすさ・振り返りやすさを優先する。

---

## 3. リポジトリ構成

学習開始前の計画資料と、実際の Unit ごとの学習成果を分離した Hybrid 構成とする。

```text
docker-foundations-study/
├─ docs/
│  └─ planning/
│     ├─ learning-curriculum.md
│     └─ learning-operation.md
│
└─ units/
   ├─ 01-docker-container-basics/
   ├─ 02-image-container-operations/
   ├─ 03-ports-bind-mounts-volumes/
   ├─ 04-dockerfile-image-build/
   ├─ 05-react-vite-docker/
   ├─ 06-spring-boot-docker/
   ├─ 07-postgresql-container/
   ├─ 08-docker-network/
   ├─ 09-docker-compose/
   ├─ 10-spring-boot-postgresql/
   ├─ 11-react-spring-boot-postgresql/
   └─ 12-docker-best-practices/
```

### 3.1 `docs/planning/`

学習開始前に確定した計画資料を格納する。

- `learning-curriculum.md`
  - 何を・どの順番で・どこまで学ぶかを定義する。
- `learning-operation.md`
  - リポジトリ上でどのように学習を進め、成果物を残すかを定義する。

これらは学習開始時点の計画として残す。

### 3.2 `units/`

実際の学習成果を Unit 単位で格納する。

各 Unit ディレクトリは、その Unit の学習一式が完結する場所とする。

ドキュメントと実践ファイルを別ディレクトリへ分散させず、その Unit で使用するものは原則として Unit ディレクトリ配下にまとめる。

---

## 4. Unit ディレクトリ

Unit ディレクトリ名は学習開始前に固定し、途中で都度命名しない。

| Unit | ディレクトリ名                    |
| ---- | --------------------------------- |
| 01   | `01-docker-container-basics`      |
| 02   | `02-image-container-operations`   |
| 03   | `03-ports-bind-mounts-volumes`    |
| 04   | `04-dockerfile-image-build`       |
| 05   | `05-react-vite-docker`            |
| 06   | `06-spring-boot-docker`           |
| 07   | `07-postgresql-container`         |
| 08   | `08-docker-network`               |
| 09   | `09-docker-compose`               |
| 10   | `10-spring-boot-postgresql`       |
| 11   | `11-react-spring-boot-postgresql` |
| 12   | `12-docker-best-practices`        |

Unit ごとに必要なファイル構成は異なるため、Unit 配下の内部構成までは一律に統一しない。

たとえば概念・CLI 操作が中心の Unit では `README.md` のみでもよく、React / Spring Boot / PostgreSQL / Compose を扱う Unit では、内容に応じてソースコード・Dockerfile・設定ファイルなどを配置する。

---

## 5. Unit ごとの成果物

基本ルールは以下とする。

> 1 Unit = 1 `README.md` + その Unit に必要な実践成果物

### 5.1 全 Unit 共通

すべての Unit に以下を作成する。

```text
README.md
```

`README.md` は、その Unit の学習内容・手順・確認方法をまとめた学習ガイド兼記録とする。

### 5.2 必要な Unit のみ作成するもの

Unit の内容に応じて、以下のようなファイルを作成する。

- Dockerfile
- `.dockerignore`
- `compose.yaml`
- HTML / CSS / JavaScript
- React / Vite のソースコード
- Spring Boot / Java のソースコード
- `pom.xml`
- SQL
- `.env.example`
- その他 Docker 学習に必要な設定ファイル

すべての Unit に無理に同じ種類の成果物を用意しない。

---

## 6. Unit README の共通フォーマット

各 Unit の `README.md` は、原則として以下の共通見出しを使用する。

```markdown
# XX. Unit名

## この項目の目的

## 学習内容

## 使用するもの

## 事前準備

## ハンズオン

## 動作・確認ポイント

## 学習ポイント

## 完了条件
```

### 6.1 この項目の目的

その Unit で何を理解し、何ができるようになることを目指すのかを記載する。

### 6.2 学習内容

その Unit で扱う Docker / Container の概念・機能・役割を説明する。

単なる用語一覧ではなく、必要に応じて「何のためのものか」「他の仕組みとどう違うか」まで説明する。

### 6.3 使用するもの

その Unit で使用する主な Image、アプリケーション、ツール、ファイルなどを記載する。

### 6.4 事前準備

その Unit の学習・実行に必要な技術的準備を記載する。

例:

- 必要なディレクトリやファイルの準備
- JAR の Build
- `.env` の準備
- 使用 Port の確認
- 必要な Image の確認
- 実行前に必要な設定

### 6.5 ハンズオン

実際に行う操作・コマンドを順序立てて記載する。

初めて登場するコマンドや重要な Option については、コマンドだけを提示せず、意味や役割もその場で説明する。

同じ基本コマンドが後続 Unit で再登場する場合、毎回まったく同じ詳細説明を繰り返す必要はなく、その Unit で必要な補足を中心にする。

### 6.6 動作・確認ポイント

操作後に何を確認すればよいかを記載する。

例:

- Container が起動しているか
- ブラウザで何が表示されるか
- API がどのレスポンスを返すか
- PostgreSQL にデータが存在するか
- Container 再作成後も Volume のデータが残っているか
- Logs のどこを見るか
- Network 上でどの名前で接続されているか

### 6.7 学習ポイント

実際のハンズオンから理解してほしい本質を整理する。

例:

- Image と Container の違い
- Bind Mount と Volume の違い
- Host と Container の Port の関係
- Container 内部の `localhost`
- Docker Compose が何をまとめているのか

### 6.8 完了条件

その Unit を完了と判断するための条件を記載する。

---

## 7. README のボリューム方針

共通化するのは見出し構成であり、各見出しの文章量・情報量ではない。

Unit ごとに学習対象・実践内容・重要なポイントが異なるため、必要な箇所を必要なだけ記載する。

たとえば以下のような差を許容する。

- CLI 操作が中心の Unit では `ハンズオン` が厚くなる。
- Dockerfile を扱う Unit では `学習内容` や命令の解説が厚くなる。
- Network / Compose を扱う Unit では `学習ポイント` や `動作・確認ポイント` が厚くなる。
- 3 Container 構成では、接続関係やトラブルシューティングの説明量が増える。

Unit 間で文章量や各項目の行数をそろえるための目安は設けない。

学習に必要な説明が不足しないことを優先する。

---

## 8. コマンド・操作の説明方針

Docker コマンドや各種操作は、単なる手順一覧として記載しない。

以下の役割分担を基本とする。

- 概念・仕組みそのものの説明
  - `学習内容`
- 実際のコマンド・操作と、そのコマンドや Option の意味
  - `ハンズオン`
- 実行結果の見方
  - `動作・確認ポイント`
- 操作を通して理解すべき本質
  - `学習ポイント`

初学者が「なぜその操作をするのか」が分からないままコマンドを実行する状態を避ける。

---

## 9. サンプルコード・アプリの方針

React / Spring Boot / PostgreSQL などは Docker 学習のための題材として使用し、アプリケーション自体は必要最低限にする。

### 9.1 作り込みを避ける

Docker 学習に直接必要でない以下の内容は最小限にする。

- UI デザイン
- 状態管理
- 認証・認可
- 複雑な CRUD
- 本格的な DB 設計
- 高度な API 設計
- 複雑なドメインロジック

### 9.2 Unit ごとの成果を独立して保持する

後続 Unit で前の Unit の成果物を直接上書きし続けない。

たとえば以下はそれぞれ別の Unit 成果として保持する。

- React 単体 Container
- Spring Boot 単体 Container
- Spring Boot + PostgreSQL
- React + Spring Boot + PostgreSQL

後続 Unit で前の Unit の構成を利用したい場合は、必要に応じてコピー・再構成する。

コードの重複を減らすことより、各学習段階の完成状態が明確に残ることを優先する。

---

## 10. ソースコード・設定ファイルのコメント方針

各 Unit 内のドキュメント以外の生成物には、学習しやすさを優先してコメントを記載する。

### 10.1 Docker 学習に直接関係する箇所

Docker / Container / Network / Volume / Compose など、今回の学習対象に直接関係する実装・設定には、意図や仕組みが分かるようにコメントをしっかり記載する。

例:

- Dockerfile の各命令をこの順番にしている理由
- Build Context に関係する設定
- Port の意味
- Volume の用途
- Network / Service 名による接続
- 環境変数を Container 外から渡す理由
- Compose の各設定の役割
- Multi-stage Build の各 Stage の役割

### 10.2 Docker 学習の本筋ではないサンプルコード

React / Spring Boot / Java などのサンプルコードにも、最低限以下が分かるコメントを付ける。

- そのプログラムが何をするものか
- どの処理が何のために存在するか
- Docker 学習上、そのコードがどのような役割を持つか
- Container 内でどのように実行・利用されるか

React / Java 自体の詳細な言語解説までは不要とする。

### 10.3 サンプルコード内の Docker 関連部分

アプリケーションコード内であっても、Container / Network / Environment など Docker 学習と関係する箇所は丁寧にコメントする。

### 10.4 判断基準

コメントを付けるべきか迷う場合は、コメントを付ける側に寄せる。

ただし、コードをそのまま読み上げるだけで学習上意味のないコメントは避ける。

---

## 11. Unit の基本的な進め方

各 Unit は原則として以下の流れで進める。

1. Unit の学習目的・学習内容を確認する。
2. 必要なサンプルコード・Dockerfile・設定ファイルなどを準備する。
3. README の手順に沿って、ユーザー自身がローカル環境で Docker コマンドや操作を実行する。
4. Container 状態・ブラウザ表示・API レスポンス・DB 接続などを確認する。
5. 必要に応じて設定変更・再実行・Container 再作成などを行う。
6. README の学習ポイントを確認し、操作と概念を結び付ける。
7. 完了条件を満たしていることを確認し、Unit を完了する。
8. Git / GitHub 上で Unit の成果を main へ取り込む。

Dockerfile やソースコードを読むだけで完了せず、可能な範囲で実際のローカル環境で動かすことを前提とする。

---

## 12. Unit の生成依頼単位

生成依頼は原則として以下の方針とする。

> 原則 1 Unit = 1 回の生成依頼

ただし、成果物量や構成が重い Unit は複数回に分割する。

ユーザー側で分割要否を判断する必要がないよう、学習計画上で生成回数をあらかじめ定める。

各 Unit 開始時にも、生成依頼前に生成回数と生成対象を明示する。

| Unit | 生成回数 | 基本方針                                                |
| ---- | -------: | ------------------------------------------------------- |
| 01   |      1回 | README 中心                                             |
| 02   |      1回 | README 中心                                             |
| 03   |      1回 | README + 最小サンプル                                   |
| 04   |      1回 | README + Dockerfile / `.dockerignore` 等                |
| 05   |      1回 | 最小 React / Vite アプリ + Docker 関連ファイル + README |
| 06   |      1回 | 最小 Spring Boot アプリ + Docker 関連ファイル + README  |
| 07   |      1回 | PostgreSQL 学習用成果物 + README                        |
| 08   |      1回 | Network 確認用成果物 + README                           |
| 09   |      1回 | Compose 確認用成果物 + README                           |
| 10   |      2回 | 1回目: 実践成果物一式 / 2回目: README                   |
| 11   |      2回 | 1回目: 実践成果物一式 / 2回目: README                   |
| 12   |      2回 | 1回目: 改善後の実践成果物 / 2回目: README               |

### 12.1 分割する Unit の考え方

Unit 10〜12 は、複数アプリ・複数 Dockerfile・Compose・DB・環境変数・改善対象などが絡み、成果物量が多くなる。

そのため以下の順番で確定する。

1. 実際に動かす成果物を先に生成・確認する。
2. 確定した成果物を前提に README を生成する。

これにより、ドキュメントと実際の成果物の不一致を防ぐ。

---

## 13. Unit 完了基準

各 Unit は、ファイルを生成・配置しただけでは完了としない。

原則として以下の 3 点を満たすことを完了条件とする。

### 13.1 実行できる

README に記載した Docker / Docker Compose などの操作を実際に実行できる。

### 13.2 期待する状態を確認できる

Unit に応じて、以下のような結果を確認する。

- Container が起動している。
- Nginx や React の画面をブラウザで確認できる。
- Spring Boot API がレスポンスを返す。
- PostgreSQL へ接続できる。
- Volume にデータが残る。
- Container 間通信ができる。
- Docker Compose で複数 Service を起動できる。
- React → Spring Boot → PostgreSQL の流れを確認できる。

### 13.3 主要な意味を理解できる

その Unit の主要概念について、大まかに説明できる。

毎 Unit でテスト問題・自己採点・学習時間の記録などを設ける必要はない。

---

## 14. トラブルシューティングの扱い

トラブルシューティングは独立 Unit にせず、必要な Unit の中で横断的に扱う。

主に以下を確認できるようにする。

- Container が起動しているか
- Container が終了していないか
- Logs
- Inspect
- Container 内部
- Port Mapping
- 環境変数
- Network
- Service 名 / Host 名
- Volume
- Build Error
- Image / Container の状態

特に後半 Unit では、問題発生時に以下のような順序で切り分ける考え方を身につける。

```text
問題発生
↓
どの Container / Service が問題か確認
↓
Logs
↓
Port / Network / Environment
↓
接続設定
↓
必要に応じて Container 内部を確認
```

---

## 15. Git / GitHub 運用

Unit ごとに学習履歴を明確に残しつつ、Git 操作自体が Docker 学習の負担にならない構成とする。

### 15.1 Branch

原則として 1 Unit = 1 feature branch とする。

ブランチ名は毎回新たに考えず、Unit ディレクトリ名をそのまま利用する。

形式:

```text
feature/<Unitディレクトリ名>
```

例:

```text
feature/01-docker-container-basics
feature/05-react-vite-docker
feature/11-react-spring-boot-postgresql
```

### 15.2 Commit

1 Unit = 1 Commit には固定しない。

サンプル追加、Dockerfile 追加、README 追加など、作業上自然な区切りで複数 Commit してよい。

### 15.3 Pull Request

Unit 完了ごとに Pull Request を作成し、main へ取り込む。

Pull Request が Unit 全体のまとまりとなる。

### 15.4 Issue

Docker 学習とは別の管理作業を増やしすぎないため、Unit ごとの GitHub Issue 作成は必須としない。

---

## 16. Version・設定ファイルの基本方針

### 16.1 Container

Windows PC 上で Docker Desktop を利用し、学習対象は Linux Container とする。

Windows Container 自体は今回の学習対象外とする。

### 16.2 Docker Compose

現在の Docker CLI に統合された以下の形式を使用する。

```text
docker compose
```

旧形式の `docker-compose` を学習の基本にはしない。

### 16.3 Base Image / Version

Base Image は原則として Version を明示する。

安易に `latest` のみに依存しない。

Node.js / Java / PostgreSQL などの具体的 Version は、該当 Unit の成果物を作成する時点で決定し、Unit README に記録する。

### 16.4 `.gitignore`

ルート `.gitignore` は学習開始前のリポジトリ準備で作成する。

以下のような、学習途中で誤って Commit したくないものを必要に応じて対象とする。

- `.env`
- `node_modules`
- Java の Build 成果物
- IDE 関連ファイル
- OS 固有ファイル
- その他 Unit で生成される不要なローカルファイル

### 16.5 `.env.example`

`.env.example` はルート共通ファイルとして事前作成しない。

環境変数が必要な Unit で、その Unit ディレクトリ配下に作成する。

秘密情報を含む `.env` 自体は Git に Commit しない。

---

## 17. 学習開始前の準備

学習開始前には、Unit 01 の学習内容を先取りしすぎない範囲で、Unit 01 を開始できる状態だけ整える。

### 17.1 リポジトリ準備

- `docker-foundations-study` を作成する。
- Git 管理を開始する。
- `docs/planning/` を用意する。
- `learning-curriculum.md` を配置する。
- `learning-operation.md` を配置する。
- ルート `.gitignore` を作成する。
- 計画ドキュメントと開始時の共通ファイルを Commit する。

### 17.2 Docker 環境の成立確認

過去に Docker を利用したことがある前提でも、Unit 01 開始前に最低限以下を確認する。

- Docker Desktop がインストールされている。
- Docker Desktop が正常に起動する。
- WSL 2 を利用できる状態である。
- Docker CLI を利用できる状態である。
- Unit 01 を開始できない環境上の問題がない。

### 17.3 Unit 01 に残す内容

以下は事前準備で完了させず、Unit 01 の学習内容として扱う。

- Docker CLI が何をするものか
- Docker Engine / Docker Desktop との関係
- `docker version` 等の確認内容の意味
- Image / Container の概念
- 最初の Container 実行
- `hello-world` 等を実行した際に内部で何が起きているか

事前準備は「Docker を学べる状態にする」ためのものとし、「Docker の学習そのもの」は Unit 01 から開始する。

---

## 18. ルート README の扱い

学習開始時にはルート `README.md` を作成しない。

仮 README や途中版も作成しない。

全 12 Unit 完了後、実際に完成したリポジトリを基準として、最終成果物として初めてルート `README.md` を作成する。

最終 README では、必要に応じて以下を整理する。

- リポジトリの目的
- 学習範囲
- 12 Unit 一覧
- リポジトリ構成
- 計画ドキュメントへの導線
- 各 Unit への導線
- 学習完了時点の内容

---

## 19. 成果物の受け渡し方針

生成物を連携する場合は、必要なファイルを zip 形式にまとめて提供する。

### 19.1 新規生成

その生成依頼で必要な成果物をすべて zip に格納する。

zip 内では、`docker-foundations-study` に配置する前提のディレクトリ構成が分かる形で格納する。

### 19.2 修正・追加

既に生成済みの成果物に修正・追加が発生した場合は、以下のみを zip に格納する。

- 修正したファイル
- 新規追加したファイル

変更のない既存ファイルは再同梱しない。

これにより、どのファイルを差し替え・追加すればよいか明確にする。

---

## 20. リポジトリ完成時の状態

全学習完了時には、少なくとも以下が存在する状態とする。

### 20.1 事前計画ドキュメント

```text
docs/planning/
├─ learning-curriculum.md
└─ learning-operation.md
```

### 20.2 全 12 Unit

```text
units/
├─ 01-docker-container-basics/
├─ 02-image-container-operations/
├─ 03-ports-bind-mounts-volumes/
├─ 04-dockerfile-image-build/
├─ 05-react-vite-docker/
├─ 06-spring-boot-docker/
├─ 07-postgresql-container/
├─ 08-docker-network/
├─ 09-docker-compose/
├─ 10-spring-boot-postgresql/
├─ 11-react-spring-boot-postgresql/
└─ 12-docker-best-practices/
```

各 Unit には `README.md` があり、必要な Unit には Dockerfile、compose.yaml、サンプルアプリ、SQL、設定ファイルなどの実践成果物が存在する。

### 20.3 ルート共通ファイル

- `.gitignore`
- 学習完了後に作成した `README.md`

### 20.4 Git / GitHub 上の履歴

各 Unit が feature branch → Pull Request → main の単位で進められ、学習の区切りが履歴として残っている。

---

## 21. 運用上の基本判断

本学習では、以下の優先順位で判断する。

1. Docker / Container を実際に理解・操作できること
2. 学習途中で迷わず進められること
3. 後から自分で見返したときに内容が分かること
4. リポジトリ内の構成が整理されていること
5. ファイル数や文章量を機械的に統一すること

迷った場合は、形式をそろえることより、学習内容が理解しやすく残ることを優先する。
