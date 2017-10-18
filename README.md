shortbox.de
======
**Shortbox-Backend** is the shortbox backend.

## Technology
* [Golang](https://golang.org/) 
* [MariaDB](https://mariadb.org/)
* [go-mysql-driver](https://github.com/go-sql-driver/mysql)
* [Golang-Roman](https://github.com/StefanSchroeder/Golang-Roman)
* [goquery](https://github.com/PuerkitoBio/goquery)
* [Gorilla Websockets](https://github.com/gorilla/websocket)

## Usage
1. Sync sandbox to your server
2. Change HTTPS and WSS-URL in 'frontend/src/app/config.ts'
4. Change directory to 'dist'
3. Run 'env GOOS=linux GOARCH=amd64 go build -v ../server.go'
4. Copy 'server' to your server directory