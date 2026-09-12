# syntax=docker/dockerfile:1

# Unit 04 / 08 でも利用した Nginx Official Image を Base Image として固定する。
FROM nginx:1.30.4-alpine3.24

WORKDIR /usr/share/nginx/html

# Compose 自体の学習が目的のため、Application は静的 HTML 1 File に限定する。
COPY site/ ./

# Image 側の Default Environment Variable。
# compose.yaml の environment から同名変数を渡すと Container Runtime では上書きされる。
ENV APP_STAGE=container-default

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
