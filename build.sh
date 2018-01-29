#!/bin/bash
echo '------------------------------'
echo ' Building shortbox-backend... '
echo '------------------------------'

go env GOPATH

go get
go build -o server server.go