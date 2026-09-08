# 05. React / Vite アプリの Docker 化

## この項目の目的

この Unit では、React / Vite で作られた最小の Frontend Application を Docker Image として Build し、Container 内で起動して Browser から確認する。  
React / Vite 自体の学習が目的ではなく、Unit 04 までに学んだ Dockerfile、Base Image、Build Context、`COPY`、`RUN`、Port Mapping、Build Cache といった Docker の共通的な考え方が Frontend Application にもそのまま適用できることを経験する。

Unit 04 では HTML を Nginx Image に組み込んだ。  
今回は Application の実行に Node.js と npm が必要になるため、Dockerfile で Node.js Base Image を利用し、Container 内へ npm Dependency と React / Vite Source Code を組み込む。

```text
React / Vite Source Code
        +
package.json
        +
Dockerfile
        ↓
docker image build
        ↓
React / Vite Application Image
        ↓
docker container run
        ↓
Container 内で Vite Server 起動
        ↓
Host Browser からアクセス
```

最終的には、「Frontend だから Docker の考え方が特別に変わる」のではなく、必要な Runtime と Application を Image にまとめ、Container として実行するという共通の流れで扱えることを理解する。

## 学習内容

### React / Vite Application と Docker

React / Vite Application も Docker から見ると、Container 内で実行する Application の 1 つである。  
Docker は React Component の書き方や JSX の仕組みを理解して Container を作るわけではない。

Docker の観点では、主に次の要素が重要になる。

```text
Application を実行する Runtime
→ Node.js

Application の Dependency 定義
→ package.json

Dependency の Installation
→ npm install

Application Source
→ index.html / src/ / vite.config.js

Application の起動
→ npm run dev

Application が待ち受ける Port
→ 5173
```

つまり、この Unit で理解する中心は React の内部ではなく、

```text
必要な Runtime
+
Dependency
+
Source Code
+
起動 Command
```

を Docker Image にどう組み込むかである。

### 今回の React / Vite Sample

今回の Application は文字列を表示するだけの最小構成とする。

```text
05-react-vite-docker/
├─ README.md
├─ Dockerfile
├─ .dockerignore
├─ package.json
├─ vite.config.js
├─ index.html
└─ src/
   ├─ main.jsx
   ├─ App.jsx
   └─ styles.css
```

React の State、Hook、Routing、Form、API 通信などは扱わない。  
これらを追加しても Docker の基本的な Container 化の考え方は変わらないため、この Unit では Application を小さく保つ。

### Node.js Base Image

React / Vite Application の Development Server を実行するには Node.js が必要である。  
Host Windows に Node.js が Installation 済みかどうかに依存させず、Container 内へ Node.js Runtime を用意するため Node.js Official Image を Base Image として利用する。

今回使用する Base Image は次である。

```text
node:24.20.0-alpine3.24
```

この Image には Node.js と npm が含まれている。

```text
Node.js Base Image
├─ Linux Environment
├─ node
└─ npm
```

その上へ Application Dependency と Source Code を追加する。

```text
node:24.20.0-alpine3.24
        ↓
npm Dependency
        ↓
React / Vite Source
        ↓
unit05-react-vite Image
```

この関係は Unit 04 の Nginx Base Image と同じである。

```text
Unit 04
Nginx Base Image
+ HTML
→ 自作 Image

Unit 05
Node.js Base Image
+ npm Dependency
+ React / Vite Source
→ 自作 Image
```

### Node.js Version

今回の Base Image では Node.js `24.20.0` を使用する。  
Node.js 24 系は LTS Release であり、今回使用する Vite 8 の Node.js Requirement も満たしている。

Version を明示する理由は Unit 04 と同じである。

```text
FROM node:latest
```

のように `latest` だけへ依存するのではなく、

```text
FROM node:24.20.0-alpine3.24
```

とすることで、どの Runtime / Linux Base を使って学習したかを Dockerfile から確認できる。

### `package.json`

`package.json` は Node.js / npm Project の Dependency や Script などを定義する File である。  
今回の Application では主に次を定義している。

```text
dependencies
├─ react
└─ react-dom

devDependencies
├─ vite
└─ @vitejs/plugin-react
```

さらに、

```json
"scripts": {
  "dev": "vite"
}
```

により、

```bash
npm run dev
```

で Vite Development Server を起動できる。

React / npm 自体の体系的な学習はこの Unit の範囲外である。  
Docker の観点では、

```text
package.json
= Application を動かすために必要な Dependency 情報
```

として扱う。

### Dependency と `node_modules`

`npm install` を実行すると、`package.json` に定義された Package が Install され、通常 `node_modules` に配置される。

```text
package.json
   ↓
npm install
   ↓
node_modules
```

今回重要なのは、**Host 側で作成済みの `node_modules` を Image へそのまま Copy するのではなく、Image Build 中に Container の Linux Environment 上で `npm install` する**ことである。

```text
Host node_modules
→ Image に Copy しない

Docker Build
→ npm install
→ Image 内に Dependency を作る
```

これにより、Container が動作する Environment に合わせた Dependency を Image 内へ用意する。

### 今回の Dependency Version

この Unit では学習時の構成を明確にするため、`package.json` で Version を固定している。

```text
React                 19.2.8
React DOM             19.2.8
Vite                   8.2.2
@vitejs/plugin-react   6.1.1
```

Dependency Version の選定・更新自体は Docker 学習の目的ではない。  
この Unit では「どの Dependency を Image Build 中に Install しているか」を明確にするための固定である。

なお、この最小教材では `package-lock.json` を成果物に含めず、Version を明示した `package.json` と `npm install` を利用する。  
実際の Application Repository では `package-lock.json` などの Lock File を Version Control へ含め、`npm ci` による再現性の高い Installation を利用する構成が一般的である。  
この Unit では Docker の基本的な Dependency Layer / Cache の理解に焦点を絞る。

### Dockerfile の全体構造

今回の Dockerfile は次の流れで構成する。

```text
FROM
Node.js Base Image
   ↓
WORKDIR
Application Directory
   ↓
COPY package.json
Dependency 定義だけを先に Copy
   ↓
RUN npm install
Dependency を Install
   ↓
COPY .
Application Source を Copy
   ↓
EXPOSE 5173
Vite Port の意図を記録
   ↓
CMD
Vite Development Server を起動
```

Unit 04 で学んだ Dockerfile の各命令を、実際の Application Container 化へ適用している。

### `WORKDIR /app`

Dockerfile では次を指定する。

```dockerfile
WORKDIR /app
```

これにより、Container Image 内の `/app` を Application の作業 Directory とする。

```text
Container
└─ /app
   ├─ package.json
   ├─ node_modules/
   ├─ index.html
   ├─ vite.config.js
   └─ src/
```

`npm install` や `npm run dev` もこの Directory を基準に実行される。

### Dependency 定義を先に `COPY` する理由

今回の Dockerfile では最初から、

```dockerfile
COPY . .
RUN npm install
```

とはしていない。

先に、

```dockerfile
COPY package.json ./
RUN npm install
```

を行い、その後で、

```dockerfile
COPY . .
```

としている。

この順序は Build Cache と関係する。

```text
package.json
↓
npm install
↓
Source Code
```

Application 開発では `src/App.jsx` などの Source Code は頻繁に変更される一方、Dependency 定義である `package.json` は Source Code より変更頻度が低いことが多い。

そのため、

```text
Source Code だけ変更
↓
package.json は変更なし
↓
npm install Step の Cache を再利用しやすい
↓
Source Code の COPY 以降だけ再評価
```

という構成にできる。

Unit 04 で学んだ、

```text
変わりにくいものを先
変わりやすいものを後
```

という Build Cache の考え方が、実際の Application Dockerfile で使われている。

### `RUN npm install`

```dockerfile
RUN npm install
```

は **Image Build 時**に実行される。

```text
docker image build
      ↓
RUN npm install
      ↓
Dependency を Image 内へ Install
      ↓
Image に結果を保持
```

Container 起動のたびに Internet から React / Vite を Install する構成ではない。  
Dependency が含まれた Build 済み Image を基に Container が作られる。

これは Unit 04 で学んだ `RUN` の実行タイミングそのものである。

### Source Code の `COPY`

Dependency Installation 後に次を実行する。

```dockerfile
COPY . .
```

Build Context にある Application Source を `/app` へ Copy する。

ただし、本当に Context 内の全 File が Copy されるわけではない。  
`.dockerignore` に一致した File は Build Context から除外される。

```text
Host Unit Directory
      ↓
.dockerignore
      ↓
必要な Application File
      ↓
COPY . .
      ↓
Image /app
```

### `.dockerignore` と `node_modules`

`.dockerignore` では次を除外する。

```text
node_modules/
dist/
README.md
.git/
.env
...
```

特に `node_modules/` が重要である。

もし Host 側に `node_modules` が存在しても、この Unit の Image では Docker Build 中に `npm install` して Dependency を用意する。  
そのため Host の `node_modules` を Build Context へ含める必要はない。

```text
Host node_modules
×
.dockerignore で除外

Docker Image
RUN npm install
↓
Image 内 node_modules
```

Host Windows 用の Dependency を Linux Container へ不用意に持ち込まないという意味でも、境界を明確にできる。

### Vite Development Server

Vite は Development Server を起動し、Browser から Frontend Application を確認できる。

今回の `package.json` では、

```text
npm run dev
↓
vite
```

となっている。

Vite の Default Port は通常 `5173` であるため、Dockerfile でも、

```dockerfile
EXPOSE 5173
```

としている。

ただし Unit 04 の `EXPOSE 80` と同様、`EXPOSE` だけでは Host Port へ Publish されない。

Container 起動時には、

```text
-p 5173:5173
```

を使用する。

```text
Host : 5173
    ↓
Docker Port Mapping
    ↓
Container : 5173
    ↓
Vite Development Server
```

### Vite の `localhost` と `0.0.0.0`

この Unit で特に重要な Container 固有のポイントが、Vite Server の待受 Address である。

Vite を通常どおり、

```bash
npm run dev
```

で起動すると、Development Server は通常 `localhost` を基準に待ち受ける。

Container 内で考えると、

```text
Vite から見た localhost
= Vite が動いている Container 自身
```

である。

Container 内部だけで待ち受ける状態では、Docker Port Mapping を設定しても Host 側から到達できない場合がある。

そのため Dockerfile の `CMD` では、

```dockerfile
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
```

と指定する。

`0.0.0.0` は、その Environment が持つ Network Interface からの接続を受け付けるための待受 Address である。

概念的には次の違いになる。

```text
localhost で待受
→ Container 自身からの接続を中心に受け付ける

0.0.0.0 で待受
→ Container の Network Interface から到達できる状態にする
```

その上で Docker の Port Mapping により Host Browser から接続する。

```text
Windows Browser
      ↓
localhost:5173
      ↓
Windows Host : 5173
      ↓
Docker Port Mapping
      ↓
Container : 5173
      ↓
Vite 0.0.0.0:5173
```

Unit 03 で学んだ「`localhost` はどこから見ているかで変わる」という考え方が、ここで実際の Application 起動設定につながる。

### `0.0.0.0` と Port Publish は別の役割

`--host 0.0.0.0` と `-p 5173:5173` は同じ設定ではない。

```text
--host 0.0.0.0
→ Container 内の Vite がどの Interface で待ち受けるか

-p 5173:5173
→ Host Port と Container Port を Docker がどう接続するか
```

両方の役割が揃うことで、今回の Host Browser からアクセスできる。

### Development Server を使う理由

この Unit では Vite Development Server を Container 内で直接実行する。

これは、

```text
React / Vite Source
↓
Node.js Container
↓
Vite Development Server
```

という Container 化の基本を小さな構成で確認するためである。

Production Environment で Vite Development Server をそのまま公開することを推奨する意図ではない。  
実際には Frontend Source を Production Build し、生成された静的 File を Nginx などから配信する構成も利用される。

```text
Development 学習
Node.js + Vite Server

Production の一例
Node.js で Build
↓
Static File
↓
Nginx で配信
```

Build 用 Image と実行用 Image を分ける Multi-stage Build は Unit 12 で扱う。  
この Unit では Node.js Container 内で Vite Application を動かすところまでに範囲を限定する。

## 使用するもの

この Unit では以下を使用する。

### Node.js Base Image

```text
node:24.20.0-alpine3.24
```

### Application Dependency

```text
React                 19.2.8
React DOM             19.2.8
Vite                   8.2.2
@vitejs/plugin-react   6.1.1
```

### Build する Image

```text
unit05-react-vite:1.0.0
```

### Container

```text
unit05-react-vite
```

### Port

```text
Host      : 5173
Container : 5173
```

## 事前準備

Unit 04 が完了しており、Docker Desktop が起動していることを前提とする。  
今回の Branch は以下とする。

```text
feature/05-react-vite-docker
```

Git Bash で次の Unit Directory へ移動した状態から操作する。

```text
units/05-react-vite-docker
```

現在位置と File を確認する。

```bash
pwd
ls -la
```

次の構成が存在することを確認する。

```text
Dockerfile
.dockerignore
package.json
vite.config.js
index.html
src/
```

`src/` も確認する。

```bash
ls src
```

```text
App.jsx
main.jsx
styles.css
```

Host 側で `npm install` を実行する必要はない。  
この Unit では Dependency Installation も Docker Build の中で行う。

## ハンズオン

### 1. Sample Application の構成を確認する

まず `package.json` を確認する。

```bash
cat package.json
```

React / React DOM と Vite 関連 Package が定義されていることを確認する。

続いて Application Source を確認する。

```bash
cat src/App.jsx
```

画面へ固定文字列を表示するだけの小さな Component であることを確認する。

この Unit では React Code の詳細を読み解く必要はない。

### 2. Dockerfile を読む

```bash
cat Dockerfile
```

命令の大きな流れを確認する。

```text
FROM
↓
WORKDIR
↓
COPY package.json
↓
RUN npm install
↓
COPY .
↓
EXPOSE
↓
CMD
```

Unit 04 の Dockerfile より Application Dependency Installation が追加されていることに注目する。

### 3. `.dockerignore` を確認する

```bash
cat .dockerignore
```

特に次を確認する。

```text
node_modules/
dist/
README.md
.env
```

Host 側の `node_modules` を Image へ Copy せず、Image Build の `npm install` で Dependency を作る構成になっている。

### 4. React / Vite Image を Build する

次を実行する。

```bash
docker image build \
  -t unit05-react-vite:1.0.0 \
  .
```

初回 Build では Node.js Base Image の取得や `npm install` が行われるため、Source File の Copy だけより時間がかかる場合がある。

Build Output で、大まかに次の処理を確認する。

```text
FROM node:24.20.0-alpine3.24
↓
COPY package.json
↓
RUN npm install
↓
COPY .
↓
Image Export
```

Dependency Installation が **Container 起動時ではなく Image Build 時**に行われていることを意識する。

### 5. Build した Image を確認する

```bash
docker image ls unit05-react-vite
```

次が表示されることを確認する。

```text
unit05-react-vite   1.0.0
```

ここまでで、

```text
React / Vite Source
+
Node.js Base Image
↓
unit05-react-vite:1.0.0
```

という自作 Image が作られた。

### 6. Image の Node.js Version を確認する

Image から一時 Container を作り、Node.js Version を確認する。

```bash
docker container run \
  --rm \
  unit05-react-vite:1.0.0 \
  node --version
```

Base Image で指定した Node.js `24.20.0` 系が確認できる。

`--rm` により、この確認用 Container は Process 終了後に自動削除される。

ここで、

```text
Host にある Node.js
```

ではなく、

```text
Image に含まれている Node.js
```

を実行していることを意識する。

### 7. React / Vite Container を起動する

```bash
docker container run \
  -d \
  --name unit05-react-vite \
  -p 5173:5173 \
  unit05-react-vite:1.0.0
```

確認する。

```bash
docker container ls
```

`unit05-react-vite` が Running であることと、Port Mapping を確認する。

```text
Host 5173
→ Container 5173
```

### 8. Vite の Logs を確認する

```bash
docker container logs unit05-react-vite
```

Vite Development Server が起動していることを示す Log を確認する。

環境によって細かな表示は異なるが、Vite Version や Server の Address が表示される。

Application が Browser から見えない場合でも、まず Container Status と Logs を確認するという Unit 02 からの基本を再利用する。

### 9. Browser から Application を確認する

Windows Browser で次を開く。

```text
http://localhost:5173
```

次の見出しが表示されることを確認する。

```text
Unit 05 - React / Vite in Docker
```

通信経路を整理する。

```text
Windows Browser
      ↓
localhost:5173
      ↓
Windows Host : 5173
      ↓
Docker Port Mapping
      ↓
Container : 5173
      ↓
Vite Development Server
      ↓
React Application
```

### 10. Container 内の Application を確認する

Container 内の作業 Directory を確認する。

```bash
docker container exec unit05-react-vite pwd
```

次が表示される。

```text
/app
```

File を確認する。

```bash
docker container exec unit05-react-vite ls
```

概ね次のようなものが存在する。

```text
index.html
node_modules
package.json
src
vite.config.js
```

Host File を直接 Bind Mount しているのではなく、Dockerfile の `COPY` と `RUN npm install` により Image 内へ作られた Application Environment である。

### 11. Container 内の npm Dependency を確認する

次を実行する。

```bash
docker container exec unit05-react-vite \
  npm list --depth=0
```

React / Vite など、`package.json` に定義した主要 Dependency が Container 内へ Install されていることを確認する。

```text
package.json
↓
Docker Build の npm install
↓
Image 内 node_modules
↓
Container から利用
```

という流れを確認する。

### 12. 同じ内容でもう一度 Build して Cache を確認する

Source Code を変更せず、再度 Build する。

```bash
docker image build \
  -t unit05-react-vite:1.0.0 \
  .
```

Build Output で `RUN npm install` などが Cache から再利用されることを確認する。

Unit 04 の Build Cache が実際の npm Application でも同じ考え方で働いている。

### 13. React Source のみ変更する

Editor で次を開く。

```text
src/App.jsx
```

次の文章を、

```text
React / Vite アプリを Docker Image から Container として起動しています。
```

たとえば次へ変更する。

```text
Source Code を変更して Docker Image を再 Build しました。
```

保存する。

現在の Container は Host File を Bind Mount していないため、Browser を Reload しても既存 Container の表示は自動的には変わらない。

```text
Host Source 変更
≠
既存 Image の変更
≠
既存 Container の変更
```

Unit 04 の `COPY` と同じ考え方である。

### 14. Source 変更後に Image を再 Build する

同じ Tag で再 Build する。

```bash
docker image build \
  -t unit05-react-vite:1.0.0 \
  .
```

Build Output を確認する。

`package.json` は変更していないため、

```text
COPY package.json
RUN npm install
```

は Cache を再利用できる一方、

```text
COPY .
```

は Source Code 変更のため再評価されることを確認する。

この順番が Dockerfile で Dependency 定義を Source より先に Copy した理由である。

### 15. 既存 Container を作り直す

再 Build しても、すでに Running の Container が自動的に新しい Image へ置き換わるわけではない。

まず既存 Container を削除する。

```bash
docker container rm -f unit05-react-vite
```

新しく Build した Image から再度起動する。

```bash
docker container run \
  -d \
  --name unit05-react-vite \
  -p 5173:5173 \
  unit05-react-vite:1.0.0
```

Browser を Reload し、変更後の文章が表示されることを確認する。

```text
Source 変更
↓
Image 再 Build
↓
旧 Container 削除
↓
新 Image から Container 再作成
↓
変更後 Application
```

### 16. `0.0.0.0` の役割を Dockerfile と Logs から整理する

Dockerfile の最後を再確認する。

```dockerfile
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
```

ここで、

```text
--host 0.0.0.0
```

は Vite が Container 内の Network Interface から接続を受け付けられるようにするための設定である。

一方、

```text
-p 5173:5173
```

は Docker が Host と Container の Port をつなぐ設定である。

```text
Vite の待受
→ --host 0.0.0.0

Docker の通信経路
→ -p 5173:5173
```

この 2 つを区別する。

### 17. Unit 05 の Container を削除する

学習終了時に Container を削除する。

```bash
docker container rm -f unit05-react-vite
```

確認する。

```bash
docker container ls -a \
  --filter 'name=unit05-react-vite'
```

Container が残っていないことを確認する。

### 18. Unit 05 の Image を削除する

今回 Build した Image も整理する。

```bash
docker image rm unit05-react-vite:1.0.0
```

確認する。

```bash
docker image ls unit05-react-vite
```

表示されなくなればよい。

Base Image の `node:24.20.0-alpine3.24` は自作 Image とは別であり、後続 Unit で再利用する可能性もあるため、この Unit では無理に削除する必要はない。

## 動作・確認ポイント

### Node.js Base Image

以下を確認する。

- Node.js Base Image を利用することで Container 内に Node.js / npm を用意できる。
- Host Windows の Node.js Installation に依存せず Image を Build できる。
- Base Image と Application 固有の File / Dependency を分けて考えられる。

### npm Dependency

以下を確認する。

- `package.json` が Application Dependency の定義である。
- `RUN npm install` が Image Build 時に実行される。
- Dependency が Image 内の `node_modules` へ Install される。
- Host 側 `node_modules` を `.dockerignore` で Build Context から除外している。

### Dockerfile / Build Cache

以下を確認する。

- `package.json` を先に Copy し、その後 `npm install` を行う理由を説明できる。
- Source Code だけを変更したとき `npm install` Layer の Cache を再利用しやすい。
- `COPY . .` によって Application Source が Image へ組み込まれる。
- Source Code を変更しただけでは既存 Image / Container は変わらない。

### Vite Server / Port

以下を確認する。

- Vite Development Server が Container Port `5173` で動作する。
- `-p 5173:5173` で Host と Container の Port を Mapping できる。
- Browser から `http://localhost:5173` へアクセスできる。
- `--host 0.0.0.0` と Docker Port Mapping の役割を区別できる。

### Container

以下を確認する。

- Build した React / Vite Image から Container を起動できる。
- `docker container logs` で Vite Server の起動を確認できる。
- `docker container exec` で `/app`、Source、Dependency を確認できる。
- Image 再 Build 後は Container を再作成して新しい Image を利用する必要がある。

## 学習ポイント

### Frontend Application も Docker の基本構造は同じ

React / Vite だから Docker に特別な仕組みが必要になるわけではない。

```text
Runtime
+
Dependency
+
Application
+
起動 Command
↓
Image
↓
Container
```

という構造は、今後 Spring Boot Application を扱う場合にも共通する。

違うのは Application ごとに必要な Runtime、Build 手順、起動 Command、Port である。

### Base Image は Application が必要とする Runtime で選ぶ

Unit 04 では Nginx を実行するため Nginx Image を利用した。  
Unit 05 では React / Vite Tooling を実行するため Node.js Image を利用する。

```text
何を Container 内で実行したいか
↓
必要な Runtime は何か
↓
Base Image を選ぶ
```

という流れで考える。

### Host Environment に依存させない

今回、Host 側で `npm install` を行わなくても Docker Build は成立する。

```text
Host
→ Dockerfile / Source を保持

Image
→ Node.js / npm / Dependency / Source を保持
```

Application 実行に必要な Environment を Image にまとめることで、「Host に何を Installation しているか」と Application Container の実行環境を分離できる。

### npm Dependency も Image の一部になる

Dockerfile の、

```dockerfile
RUN npm install
```

により Dependency は Image Build 中に用意される。

```text
package.json
↓
npm install
↓
node_modules
↓
Image
```

Container はその Image を利用するため、毎回 Dependency Installation をゼロから行う必要はない。

### Dockerfile の命令順が実際の Build 時間につながる

Unit 04 では Dockerfile の記述順と Cache を概念として学んだ。  
Unit 05 では `npm install` という比較的重い処理が登場するため、命令順の意味がより分かりやすくなる。

```text
package.json が同じ
↓
npm install の結果を Cache
↓
Source Code だけ変更
↓
Dependency Installation を再利用
```

Application Dockerfile では、Dependency 定義と Source Code を分けて Copy することが重要になる。

### Host `node_modules` と Container `node_modules` を区別する

同じ名前でも存在する Environment が違う。

```text
Host node_modules
= Host Environment 用

Container /app/node_modules
= Container Image Build 中に作成
```

Docker 学習では File 名だけではなく、「Host 側か Container 側か」を常に意識する。

### `localhost` の視点が Application Server 設定にも影響する

Unit 03 の `localhost` の考え方は、単なる Network の知識ではなく Application Server の設定にも関係する。

```text
Vite が Container localhost だけで待受
↓
Host から到達しにくい

Vite が 0.0.0.0 で待受
+
Docker Port Mapping
↓
Host Browser から到達可能
```

Container 内 Application がどの Address / Port で Listen しているかは、Docker の Port Mapping とセットで確認する。

### `EXPOSE` / Server Listen / `-p` はそれぞれ別

今回の通信には 3 つの観点がある。

```text
Vite
--host 0.0.0.0
→ Server がどの Interface で Listen するか

Dockerfile
EXPOSE 5173
→ Image が利用する Port の意図

docker container run
-p 5173:5173
→ Host と Container の実際の Port Mapping
```

これらを 1 つの「Port 設定」としてまとめず、役割を区別する。

### `COPY` した Application は Image Build 時点で固定される

Unit 03 の Bind Mount とは異なり、今回 Source Code は Image に `COPY` される。

```text
Host Source を変更
↓
既存 Image は変わらない
↓
再 Build
↓
新しい Image
↓
Container 再作成
```

この一連の流れは、Application を Version 付き Image として扱う際の基本になる。

### Development Container と Production Container を区別する

今回 Vite Development Server を利用するのは、Node.js Application Container の基本を学ぶためである。

```text
Unit 05
Node.js Container
└─ Vite Development Server
```

Production 向けには、Frontend を Build して静的 File を配信するなど別の構成が考えられる。

この Unit では「Docker 化できた = このまま Production へ Deploy する構成」とは考えない。  
Dockerfile の用途や実行 Environment に応じて構成を変えるという認識を持つ。

### Unit 06 へのつながり

次の Unit 06 では Spring Boot Application を Docker 化する。

```text
Unit 05
Node.js Runtime
+
React / Vite
↓
Container

Unit 06
Java Runtime
+
Spring Boot
↓
Container
```

Application Technology は変わっても、

```text
Base Image
↓
Dependency / Artifact
↓
Dockerfile
↓
Image Build
↓
Container 起動
↓
Port 経由で確認
```

という共通の Docker の流れは変わらない。

## 完了条件

以下を満たしたら Unit 05 を完了とする。

- React / Vite Application も、Runtime・Dependency・Source Code・起動 Command を Image にまとめて Container として実行できることを説明できる。
- Node.js Base Image の役割を説明し、Host の Node.js Environment と Container の Node.js Environment を区別できる。
- `package.json` と `npm install` が Image 内の Dependency 構築にどう関係するか説明できる。
- Host 側 `node_modules` を Image へ持ち込まず、Docker Build 中に Dependency を Install する理由を理解している。
- Dependency 定義を Source Code より先に `COPY` することで Build Cache を利用しやすくなる理由を説明できる。
- Dockerfile から React / Vite Application Image を Build し、その Image から Container を起動できる。
- Vite Development Server の Container Port と Host Port を Mapping し、Browser から Application を確認できる。
- Vite の `--host 0.0.0.0`、Dockerfile の `EXPOSE`、`docker container run -p` の役割を区別できる。
- `docker container logs` / `exec` を使って Container 内の Application、Runtime、Dependency の状態を確認できる。
- Source Code を変更した場合、Image の再 Build と Container の再作成が必要になることを説明・実行できる。
- Unit 03 の Bind Mount と Dockerfile の `COPY` の違いを、Source Code の反映方法から説明できる。
- Vite Development Server を利用した今回の構成が学習用の Development 構成であり、Production 構成とは分けて考える必要があることを理解している。

ここまで確認できれば、Unit 06「Spring Boot Application の Docker 化」へ進む。
