FROM golang:1.12.6-alpine3.10 as builder

WORKDIR /go/src/app
COPY . .

RUN go build -o ./server . 
FROM alpine:3.10

WORKDIR /app
COPY --from=builder /go/src/app/server .
EXPOSE 8080
CMD ["./server"]


