# syntax=docker/dockerfile:1

# ----- Build Stage -----
# React / Vite の Build にだけ Node.js を利用する。
FROM node:24.20.0-alpine3.24 AS build

WORKDIR /app

# Dependency 定義を先に Copy することで、
# Source Code だけ変更した場合に npm install Layer を再利用しやすくする。
COPY package.json ./

RUN npm install

# Application Source は Dependency Install 後に Copy する。
COPY . .

# Vite の Production Build 時に Browser から利用する API URL を渡す。
# VITE_ Prefix の値は Frontend Code に公開され得るため Secret を渡さない。
ARG VITE_API_BASE_URL=http://localhost:8080

RUN npm run build

# ----- Runtime Stage -----
# Runtime には Node.js / npm / Source Code を持ち込まず、
# Build 済みの静的 File だけを Nginx から配信する。
FROM nginx:1.30.4-alpine3.24 AS runtime

COPY --from=build /app/dist/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
