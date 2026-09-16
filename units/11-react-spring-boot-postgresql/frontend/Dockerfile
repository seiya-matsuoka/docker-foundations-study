# syntax=docker/dockerfile:1

# Unit 05 と同じ Node.js / Alpine Linux Version を利用する。
FROM node:24.20.0-alpine3.24

WORKDIR /app

# Dependency 定義を Source Code より先に Copy し、
# package.json が変わらない場合に npm install の Build Cache を再利用しやすくする。
COPY package.json ./

RUN npm install

# React / Vite Application の Source Code と設定を Image へ取り込む。
COPY . .

# Vite Development Server の Container Port。
EXPOSE 5173

# Vite は通常 localhost で待ち受けるため、
# Host Browser から Container へ接続できるよう 0.0.0.0 で待ち受ける。
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
