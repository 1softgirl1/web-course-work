# downloading dependencies stage
FROM node:20-alpine as dependencies

WORKDIR /app

COPY package*.json .
RUN npm ci

# build stage
FROM node:20-alpine as build

WORKDIR /app

COPY --from=dependencies /app/node_modules ./node_modules
COPY . .

RUN mkdir -p ./dist
VOLUME "/app/dist"

CMD ["npm", "run", "build"]
