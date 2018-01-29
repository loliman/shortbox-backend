shortbox.de
======
**Shortbox-Backend** is the shortbox backend.

## Technology
* [Golang](https://golang.org/) 
* [MySQL](https://www.mysql.com/de/)
* [go-mysql-driver](https://github.com/go-sql-driver/mysql)
* [Golang-Roman](https://github.com/StefanSchroeder/Golang-Roman)
* [goquery](https://github.com/PuerkitoBio/goquery)
* [Gorilla Websockets](https://github.com/gorilla/websocket)
* [AWS](https://aws.amazon.com/de/)

## Installation
1. Sync sandbox to your server
2. Create eb instance using eb create shortbox-backend --database.engine mysql --database.password password --database.username shortbox --elb-type application --platform "64bit Amazon Linux 2017.09 v2.7.5 running Go 1.9" --region us-east-1
3. Login to AWS console and change api.shortbox.xyz URL to ALB in Route 53
4. Open EB Console, go to Configuration/Software and add DB_URL env var (user:pw@tcp(url:port)/schema)
(5. Grant DB Inbound from everywhere)

##Usage
1. Run ./build to build shortbox-backend locally
2. Run ./deploy to deploy shortbox-backend to your eb
