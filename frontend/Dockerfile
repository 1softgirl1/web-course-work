FROM node:24-bookworm-slim AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

FROM node:24-bookworm-slim

WORKDIR /app

ENV PORT=80 \
    BACKEND_URL=http://backend:8080

COPY docker-server.mjs ./docker-server.mjs
COPY --from=build /app/dist ./dist

EXPOSE 80

CMD ["node", "docker-server.mjs"]
