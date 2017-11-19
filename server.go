package main

import (
	"database/sql"
	"encoding/json"
	_ "github.com/go-sql-driver/mysql"
	"github.com/loliman/shortbox-backend/dal"
	"log"
	"net/http"
	"strconv"
	"github.com/loliman/shortbox-backend/crawler"
	"github.com/loliman/shortbox-backend/conf"
	"github.com/gorilla/websocket"
	"fmt"
	"io"
	"github.com/StefanSchroeder/Golang-Roman"
	"errors"
)

type Request struct {
	Sessionid string
	Username string
	Payload interface{}
}

type Response struct {
	Type string
	Message string
	Payload interface{}
}

//Main function
func main() {
	db := setupDb()

	defer db.Close()

	log.Fatal(http.ListenAndServeTLS(":3000", conf.PATH_TO_CERT, conf.PATH_TO_KEY, createMux(db)))
}

//Connect to the Database
func setupDb() *sql.DB {
	db, err := sql.Open("mysql", conf.DATASOURCE)

	if err != nil {
		log.Fatal("Failed to connect to database")
	}

	if db == nil {
		log.Fatal("No database connection available")
	}
	
	return db
}

var upgrader = websocket.Upgrader{
	ReadBufferSize: 1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

//Create routers
func createMux(db *sql.DB) http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("/create/list", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var l dal.List
		res, err := l.Convert(r.Payload).Insert(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/create/issue", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var i dal.Issue
		res, err := i.Convert(r.Payload).Insert(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/get/issue", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var i dal.Issue
		res, err := i.Convert(r.Payload).Select(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/get/list", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var l dal.List
		res, err := l.Convert(r.Payload).Select(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/get/lists", func(w http.ResponseWriter, req *http.Request) {
		_, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var l dal.List
		res, err := l.MultiSelect(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/export", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var l dal.List
		l = l.Convert(r.Payload)
		l.Search.IsExport = true
		res, err := l.Select(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		result := "Title;Nummer;Serie;Verlag;Format;Sprache;Seiten;Erscheinungsdatum;Preis\n"
		for _, it := range res.Objects {
			var i dal.Issue = it.(dal.Issue)
			number := i.Number
			syear := strconv.FormatInt(i.Series.Startyear, 10)
			if syear[len(syear)-1:] == "0" {
				syear = syear[0:len(syear)-2]
			}
			pages := strconv.Itoa(i.Pages)
			price := strconv.FormatFloat(i.Price.Price, 'f', 2, 64)
			price += " " + i.Price.Currency

			format := i.Format.Format
			if i.Format.Variant != "" {
				format += " (Variant Cover " + i.Format.Variant + ")"
			}

			result += i.Title + ";=\"" + number + "\";" + i.Series.Title + " (" + syear + ");" + i.Series.Publisher.Name + ";" + format + ";" + i.Language + ";=\"" + pages + "\";" + i.Releasedate + ";=\"" + price + "\"\n"
		}

		finalize(tx, encoder, result)
	})

	mux.HandleFunc("/delete/list", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var l dal.List
		res, err := l.Convert(r.Payload).Delete(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/delete/multi/issue", func(w http.ResponseWriter, req *http.Request) {
		r, tx, conn, err := prepareWS(w, req, db)

		if err != nil || tx == nil || conn == nil {
			return
		}

		for idx, p := range r.Payload.([]interface {}) {
			var i dal.Issue
			i = i.Convert(p)

			var rsp Response
			rsp.Type = "INFO"
			rsp.Message = ""

			item := new(dal.Message)
			item.Type = "Delete"
			item.Message = i.Series.Title + " (" + roman.Roman(int(i.Series.Volume)) + ") (" + strconv.FormatInt(i.Series.Startyear, 10) + ") #" + i.Number
			item.Number = idx+1
			item.Amount = len(r.Payload.([]interface {}))

			rsp.Payload = item

			conn.WriteJSON(rsp)

			_, err = i.Delete(tx)

			if err != nil {
				tx.Rollback()
				log.Print(err)
				conn.WriteMessage(websocket.TextMessage, []byte("error"))
				return
			}
		}


		finalizeWS(tx, conn, "done")
	})

	mux.HandleFunc("/update/list", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var l dal.List
		res, err := l.Convert(r.Payload).Update(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/move/list", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var l dal.List
		res, err := l.Convert(r.Payload).Move(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/update/multi/issue", func(w http.ResponseWriter, req *http.Request) {
		r, tx, conn, err := prepareWS(w, req, db)

		if err != nil || tx == nil || conn == nil {
			return
		}

		for idx, p := range r.Payload.([]interface {}) {
			var i dal.Issue
			i = i.Convert(p)

			var rsp Response
			rsp.Type = "INFO"
			rsp.Message = ""

			item := new(dal.Message)
			item.Type = "Update"
			item.Message = i.Series.Title + " (" + roman.Roman(int(i.Series.Volume)) + ") (" + strconv.FormatInt(i.Series.Startyear, 10) + ") #" + i.Number
			item.Number = idx+1
			item.Amount = len(r.Payload.([]interface {}))

			rsp.Payload = item

			conn.WriteJSON(rsp)

			_, err = i.Update(tx)

			if err != nil {
				tx.Rollback()
				log.Print(err)
				conn.WriteMessage(websocket.TextMessage, []byte("error"))
				return
			}
		}

		finalizeWS(tx, conn, "done")
	})


	mux.HandleFunc("/update/series", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var s dal.Series
		res, err := s.Convert(r.Payload).Update(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/update/publisher", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var p dal.Publisher
		res, err := p.Convert(r.Payload).Update(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/update/story", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var s dal.Story
		res, err := s.Convert(r.Payload).Update(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/delete/issue", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var i dal.Issue
		res, err := i.Convert(r.Payload).Delete(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/update/issue", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var i dal.Issue
		res, err := i.Convert(r.Payload).Update(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/get/publishers", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var p dal.Publisher
		p.Name = r.Payload.(string)
		res, _, err := p.MultiSelect(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/get/series", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var s dal.Series
		s.Title = r.Payload.(string)
		res, _, err := s.MultiSelect(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/import", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, true)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		res := crawler.Crawl(r.Payload.(string))

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/import/oi", func(w http.ResponseWriter, req *http.Request) {
		_, tx, conn, err := prepareWS(w, req, db)

		if err != nil || tx == nil || conn == nil {
			return
		}

		res := crawler.CrawlOi(conn, tx)
		res += "\n"

		var rsp Response
		rsp.Payload = "success"
		rsp.Message = ""
		rsp.Payload = res

		finalizeWS(tx, conn, rsp)
	})

	mux.HandleFunc("/login", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, false)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var u dal.User
		res, err := u.Convert(r.Payload).Login(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/logout", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, false)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var u dal.User
		res, err := u.Convert(r.Payload).Logout(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})

	mux.HandleFunc("/checksession", func(w http.ResponseWriter, req *http.Request) {
		r, tx, encoder, err := prepare(w, req, db, false)

		if err != nil || tx == nil || encoder == nil {
			return
		}

		var u dal.User
		res, err := u.Convert(r.Payload).CheckSession(tx)

		if err != nil {
			handleError(err, encoder, tx)
			return
		}

		finalize(tx, encoder, res)
	})


	return mux
}

func prepareWS(w http.ResponseWriter, req *http.Request, db *sql.DB) (Request, *sql.Tx, *websocket.Conn, error) {
	var r Request
	conn, err := upgrader.Upgrade(w, req, nil)

	if err != nil {
		fmt.Println(err)
		return r, nil, nil, err
	}

	w, encoder, tx, err := prepareTransaction(nil, db)

	if err != nil {
		handleError(err, encoder, tx)
		return r, nil, nil, err
	}

	var rsp Response
	rsp.Type = "INFO"
	rsp.Message = ""
	rsp.Payload = "Hello there!"
	conn.WriteJSON(rsp)
	_, reader, err := conn.NextReader()

	r, err = decodeRequestWs(reader)

	if err != nil {
		handleError(err, encoder, tx)
		return r, nil, nil, err
	}

	if !checkUserSession(r, tx) {
		err = errors.New("INVALID SESSIONID")
		handleError(err, encoder, tx)
		return r, nil, nil, err
	}

	return r, tx, conn, nil
}

func prepare(w http.ResponseWriter, req *http.Request, db *sql.DB, chk bool) (Request, *sql.Tx, *json.Encoder, error) {
	var r Request

	if handleOptionsHeader(w, req) {
		return r, nil, nil, nil
	}

	w, encoder, tx, err := prepareTransaction(w, db)

	if err != nil {
		handleError(err, encoder, tx)
		return r, nil, nil, err
	}

	r, err = decodeRequest(req)

	if err != nil {
		handleError(err, encoder, tx)
		return r, nil, nil, err
	}

	if chk {
		if !checkUserSession(r, tx) {
			err = errors.New("INVALID SESSIONID")
			handleError(err, encoder, tx)
			return r, nil, nil, err
		}
	}

	return r, tx, encoder, nil
}

func checkUserSession(r Request, tx *sql.Tx) (bool) {
	var u dal.User
	u.SessionId = r.Sessionid
	u.Name = r.Username
	res, err := u.CheckSession(tx)

	if !res || err != nil {
		return false
	}

	return true
}

func prepareTransaction(w http.ResponseWriter, db *sql.DB) (http.ResponseWriter, *json.Encoder, *sql.Tx, error) {
	var encoder *json.Encoder
	if w != nil {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")

		encoder = json.NewEncoder(w)
	}

	tx, err := db.Begin()

	return w, encoder, tx, err
}

func handleOptionsHeader(w http.ResponseWriter, req *http.Request) (bool) {
	if req.Method == "OPTIONS" {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
		w.Write([]byte("Hello there!"))
		return true
	}

	return false
}

func decodeRequest(req *http.Request) (Request, error) {
	var r Request

	decoder := json.NewDecoder(req.Body)
	err := decoder.Decode(&r)

	defer req.Body.Close()

	return r, err
}

func decodeRequestWs(req io.Reader) (Request, error) {
	var r Request

	decoder := json.NewDecoder(req)
	err := decoder.Decode(&r)
	return r, err
}

func handleError(err error, encoder *json.Encoder, tx *sql.Tx) {
	var rsp Response
	rsp.Type = "danger"
	rsp.Message = err.Error()
	rsp.Payload = nil

	if tx != nil {
		defer tx.Rollback()
		log.Print(err)
	} else {
		rsp.Message = "No DB connection available"
		log.Print(rsp.Message)
	}

	err = encoder.Encode(rsp)
}

func finalize(tx *sql.Tx, encoder *json.Encoder, res interface{}) {
	defer tx.Commit()

	var rsp Response
	rsp.Type = "success"
	rsp.Message = ""
	rsp.Payload = res

	err := encoder.Encode(&rsp)

	if err != nil {
		handleError(err, encoder, tx)
	}
}

func finalizeWS(tx *sql.Tx, conn *websocket.Conn, res interface{}) {
	defer tx.Commit()

	var rsp Response
	rsp.Type = "success"
	rsp.Message = ""
	rsp.Payload = res
	conn.WriteJSON(rsp)

	conn.Close()
}