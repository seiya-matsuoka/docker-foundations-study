# syntax=docker/dockerfile:1

# Node.js / npm / Vite を Container 内で実行するための Base Image。
# LTS の Node.js 24.20.0 と Alpine Linux 3.24 を明示的に固定する。
FROM node:24.20.0-alpine3.24

# Application の作業 Directory。
WORKDIR /app

# Dependency 定義を Source Code より先に Copy する。
# package.json が変わらない限り、後続の npm install の Build Cache を再利用しやすくする。
COPY package.json ./

# Build 時に Application Dependency を Container Image 内へ Install する。
RUN npm install

# React / Vite Application の Source Code と設定 File を Image へ取り込む。
# .dockerignore により node_modules などの不要 File は Build Context から除外する。
COPY . .

# Vite Development Server が Container 内で利用する Port。
# EXPOSE だけでは Host 側へ Port Publish されない。
EXPOSE 5173

# Vite は通常 localhost のみで待ち受けるため、
# Container 外の Host Browser から到達できるよう 0.0.0.0 で待ち受ける。
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
