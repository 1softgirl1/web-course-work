FROM node:24-alpine AS dependencies

WORKDIR /app

COPY package*.json ./
RUN npm ci

FROM node:24-alpine AS build

WORKDIR /app

COPY --from=dependencies /app/node_modules ./node_modules
COPY . .

ARG VITE_API_BASE_URL=
ARG VITE_BASE_PATH=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_BASE_PATH=${VITE_BASE_PATH}

RUN mkdir -p ./dist
VOLUME ["/app/dist"]

CMD ["npm", "run", "build"]
