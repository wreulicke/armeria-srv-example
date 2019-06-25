package main

import (
	"log"
	"net/http"
	"time"
)

func main() {
	m := http.NewServeMux()
	m.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("Hello World!\n"))
	})
	srv := &http.Server{
		Addr:         ":8080",
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		Handler:      m,
	}
	log.Println("started 8080")
	log.Println(srv.ListenAndServe())
}
