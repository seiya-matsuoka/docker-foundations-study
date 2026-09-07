# syntax=docker/dockerfile:1

# Nginx Official Image を Base Image として利用する。
# Unit 03 では既存 Image をそのまま実行したが、この Unit では
# ここから独自の File / 設定を追加して新しい Image を Build する。
FROM nginx:1.30.4-alpine3.24

# 以降の RUN / COPY などで基準となる作業 Directory を明示する。
WORKDIR /usr/share/nginx/html

# Base Image に含まれている Nginx Default Content を削除する。
# RUN は Build 時に実行され、その結果が Image に反映される。
RUN rm -rf ./*

# Build Context 内の site/ を Image 内へ Copy する。
# Bind Mount と異なり、Build 時点の File が Image の一部として固定される。
COPY site/ ./

# Build 時だけ利用する値。--build-arg APP_VERSION=... で上書きできる。
ARG APP_VERSION=1.0.0

# Image / Container 実行時にも利用できる Environment Variable。
ENV APP_ENV=learning \
    APP_VERSION=${APP_VERSION}

# この Image が Port 80 を利用する想定であることを Metadata として示す。
# EXPOSE だけでは Host Port へ Publish されない。
EXPOSE 80

# Container 起動時の Default Command。
# Nginx を Foreground で動かし、主 Process として継続させる。
CMD ["nginx", "-g", "daemon off;"]
