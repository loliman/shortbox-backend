echo '------------------------------'
echo ' Building shortbox-backend... '
echo '------------------------------'
call go env GOPATH
call go get
call go build -o server server.go
echo 'Build done!'