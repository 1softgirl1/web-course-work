FROM golang:1.25-alpine AS build
WORKDIR /src
COPY go.mod *.go ./
RUN go test ./... && CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /notifier .

# Static binary + CA roots for HTTPS to the messenger; nothing else in the image.
FROM scratch
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /notifier /notifier
USER 65534
EXPOSE 8080
ENTRYPOINT ["/notifier"]
