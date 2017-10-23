#!/bin/bash
echo '------------------------------'
echo ' Building shortbox-backend... '
echo '------------------------------'
cd $GOPATH/src/github.com/loliman/shortbox-backend
rm -rf ./dist
go get
go install
mkdir dist
cd dist
env GOOS=linux GOARCH=amd64 go build -v ../server.go
cd ..
tar -cf ./dist/shortbox_backend.tar ./dist/server
echo 'Build done!'