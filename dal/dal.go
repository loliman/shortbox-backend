package dal

import (
	"database/sql"
	"strconv"
	"sort"
	"crypto/rand"
	"encoding/base64"
	"log"
	"strings"
	"bitbucket.org/zombiezen/cardcpx/natsort"
	"github.com/StefanSchroeder/Golang-Roman"
)

type Header struct {
	Header string
	Amount int
	Price []Price
}

type Message struct {
	Type string
	Message string
	Number int
	Amount  int
}

type Search struct {
	Lists []int

	Start  int
	Offset int

	Issue  Issue
	Issue2 Issue

	STitle         string
	SPublisher     string
	SSeries        string
	SReleasedate   string
	SNumber        string
	SOriginalissue string
	SPages         string
	SPrice         string
	SAmount        string
	SCoverurl      string
	SRead          string
	SDuplicateIn   string

	GOne    string
	GOneDir string
	GTwo    string
	GTwoDir string

	IsExport bool

	OrgIssue bool
	OrgIssueS *Search
}

/*
 * List
 */
type List struct {
	Id      int64
	Type    string
	Name    string
	Sort    int
	GroupBy string
	Amount  int
	Objects []interface{}
	Search  Search
}

func (l List) Insert(db *sql.Tx) ([]List, error) {
	res, err := db.Exec("INSERT INTO List (name, sort, groupBy) VALUES (?, ?, ?)", l.Name, l.Sort, l.GroupBy)
	l.Id, err = res.LastInsertId()

	ret, err := l.MultiSelect(db)

	return ret, err
}

func (l List) Select(db *sql.Tx) (List, error) {
	var err error

	var objects []interface{}
	if l.Type != "meta" {
		var i Issue
		issues, amount, err := i.MultiSelect(db, l.Search)

		if err != nil {
			return l, err
		}

		if l.Id != 0 {
			err = db.QueryRow("SELECT name, id, sort, groupBy FROM List WHERE id = ?", l.Id).Scan(&l.Name, &l.Id, &l.Sort, &l.GroupBy)

			if err != nil {
				return l, err
			}
		} else if l.Id == 0 {
			if len(l.Search.Lists) > 1 {
				l.Name = "Kombiniert"
			} else {
				l.Name = "Archiv"
			}
		}

		l.Amount = amount
		l.Type = "issue"

		objects = make([]interface{}, len(issues))
		for i, v := range issues {
			objects[i] = v
		}
	} else {
		if l.Id == 0 {
			var ts Series
			ts.Title = l.Search.Issue.Title
			var count = 0
			res, count, err := ts.MultiSelect(db)

			if err != nil {
				return l, err
			}

			l.Amount = count
			objects = make([]interface{}, len(res))
			for i, v := range res {
				objects[i] = v
			}
		} else if l.Id == 1 {
			var tp Publisher
			tp.Name = l.Search.Issue.Title
			var count = 0
			res, count, err := tp.MultiSelect(db)

			if err != nil {
				return l, err
			}

			l.Amount = count
			objects = make([]interface{}, len(res))
			for i, v := range res {
				objects[i] = v
			}
		}
	}

	l.Amount = len(objects)

	if l.Amount == 0 {
		return l, err
	}

	if !l.Search.IsExport {
		var header *Header = new(Header)
		header.Price = make([]Price, 0)

		for i := 0; i < l.Search.Start; i++ {
			if i >= len(objects) {
				break
			}

			headline := ""
			if l.Type == "issue" {
				switch l.Search.GOne {
				case "i.title":
					headline = objects[i].(Issue).Title
				case "s.title":
					fallthrough
				case "s.startyear":
					yearstr := strconv.FormatInt(objects[i].(Issue).Series.Startyear, 10)
					if objects[i].(Issue).Series.Endyear != 0 {
						yearstr += "-" + strconv.FormatInt(objects[i].(Issue).Series.Endyear, 10)
					} else {
						yearstr += "-..."
					}
					headline = objects[i].(Issue).Series.Title + " (" + roman.Roman(int(objects[i].(Issue).Series.Volume)) + ") (" + yearstr + ") (" + objects[i].(Issue).Series.Publisher.Name + ") "
				case "p.name":
					headline = objects[i].(Issue).Series.Publisher.Name
				case "i.releasedate.day":
					headline = strings.Split(objects[i].(Issue).Releasedate, "-")[2] + "." + strings.Split(objects[i].(Issue).Releasedate, "-")[1] + "." + strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				case "i.releasedate.month":
					month := strings.Split(objects[i].(Issue).Releasedate, "-")[1]
					switch month {
					case "01":
						month = "Januar"
					case "02":
						month = "Februar"
					case "03":
						month = "März"
					case "04":
						month = "April"
					case "05":
						month = "Mai"
					case "06":
						month = "Juni"
					case "07":
						month = "Juli"
					case "08":
						month = "August"
					case "09":
						month = "September"
					case "10":
						month = "Oktober"
					case "11":
						month = "November"
					case "12":
						month = "Dezember"
					}

					headline = month + " " + strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				case "i.releasedate.year":
					headline = strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				}
			} else {
				if l.Id == 0 {
					headline = string(objects[i].(Series).Title)
				} else {
					headline = string(objects[i].(Publisher).Name)
				}
			}

			if headline != header.Header {
				header = new(Header)
				header.Header = headline
				header.Amount = 1
				if l.Type == "issue" {
					found := false

					for idx, price := range header.Price {
						if objects[i].(Issue).Price.Currency == price.Currency {
							header.Price[idx].Price += objects[i].(Issue).Price.Price
							found = true
						}
					}

					if !found {
						header.Price = append(header.Price, *new(Price))
						header.Price[len(header.Price)-1].Currency = objects[i].(Issue).Price.Currency
						header.Price[len(header.Price)-1].Price = objects[i].(Issue).Price.Price
					}
				}
			} else {
				header.Amount++
				if l.Type == "issue" {
					for i, price := range header.Price {
						if objects[i].(Issue).Price.Currency == price.Currency {
							header.Price[i].Price += objects[i].(Issue).Price.Price
						}
					}
				}
			}
		}

		for i := l.Search.Start; i < l.Search.Start+l.Search.Offset; i++ {
			if i >= len(objects) {
				break
			}

			headline := ""
			if l.Type == "issue" {
				switch l.Search.GOne {
				case "i.title":
					headline = objects[i].(Issue).Title
				case "s.title":
					fallthrough
				case "s.startyear":
					yearstr := strconv.FormatInt(objects[i].(Issue).Series.Startyear, 10)
					if objects[i].(Issue).Series.Endyear != 0 {
						yearstr += "-" + strconv.FormatInt(objects[i].(Issue).Series.Endyear, 10)
					} else {
						yearstr += "-..."
					}
					headline = objects[i].(Issue).Series.Title + " (" + roman.Roman(int(objects[i].(Issue).Series.Volume)) + ") (" + yearstr + ") (" + objects[i].(Issue).Series.Publisher.Name + ") "
				case "p.name":
					headline = objects[i].(Issue).Series.Publisher.Name
				case "i.releasedate.day":
					headline = strings.Split(objects[i].(Issue).Releasedate, "-")[2] + "." + strings.Split(objects[i].(Issue).Releasedate, "-")[1] + "." + strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				case "i.releasedate.month":
					month := strings.Split(objects[i].(Issue).Releasedate, "-")[1]
					switch month {
					case "01":
						month = "Januar"
					case "02":
						month = "Februar"
					case "03":
						month = "März"
					case "04":
						month = "April"
					case "05":
						month = "Mai"
					case "06":
						month = "Juni"
					case "07":
						month = "Juli"
					case "08":
						month = "August"
					case "09":
						month = "September"
					case "10":
						month = "Oktober"
					case "11":
						month = "November"
					case "12":
						month = "Dezember"
					}

					headline = month + " " + strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				case "i.releasedate.year":
					headline = strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				}
			} else {
				if l.Id == 0 {
					headline = string(objects[i].(Series).Title)
				} else {
					headline = string(objects[i].(Publisher).Name)
				}
			}

			if headline != header.Header {
				header = new(Header)
				header.Header = headline
				l.Objects = append(l.Objects, header)
			}

			header.Amount += 1
			if l.Type == "issue" {
				found := false

				for idx, price := range header.Price {
					if objects[i].(Issue).Price.Currency == price.Currency {
						header.Price[idx].Price += objects[i].(Issue).Price.Price
						found = true
					}
				}

				if !found {
					header.Price = append(header.Price, *new(Price))
					header.Price[len(header.Price)-1].Currency = objects[i].(Issue).Price.Currency
					header.Price[len(header.Price)-1].Price = objects[i].(Issue).Price.Price
				}
			}
			l.Objects = append(l.Objects, objects[i])
		}

		for i := l.Search.Start + l.Search.Offset; i < len(objects); i++ {
			headline := ""
			if l.Type == "issue" {
				switch l.Search.GOne {
				case "i.title":
					headline = objects[i].(Issue).Title
				case "s.title":
					fallthrough
				case "s.startyear":
					yearstr := strconv.FormatInt(objects[i].(Issue).Series.Startyear, 10)
					if objects[i].(Issue).Series.Endyear != 0 {
						yearstr += "-" + strconv.FormatInt(objects[i].(Issue).Series.Endyear, 10)
					} else {
						yearstr += "-..."
					}
					headline = objects[i].(Issue).Series.Title + " (" + roman.Roman(int(objects[i].(Issue).Series.Volume)) + ") (" + yearstr + ") (" + objects[i].(Issue).Series.Publisher.Name + ") "
				case "p.name":
					headline = objects[i].(Issue).Series.Publisher.Name
				case "i.releasedate.day":
					headline = strings.Split(objects[i].(Issue).Releasedate, "-")[2] + "." + strings.Split(objects[i].(Issue).Releasedate, "-")[1] + "." + strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				case "i.releasedate.month":
					month := strings.Split(objects[i].(Issue).Releasedate, "-")[1]
					switch month {
					case "01":
						month = "Januar"
					case "02":
						month = "Februar"
					case "03":
						month = "März"
					case "04":
						month = "April"
					case "05":
						month = "Mai"
					case "06":
						month = "Juni"
					case "07":
						month = "Juli"
					case "08":
						month = "August"
					case "09":
						month = "September"
					case "10":
						month = "Oktober"
					case "11":
						month = "November"
					case "12":
						month = "Dezember"
					}

					headline = month + " " + strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				case "i.releasedate.year":
					headline = strings.Split(objects[i].(Issue).Releasedate, "-")[0]
				}
			} else {
				if l.Id == 0 {
					headline = string(objects[i].(Series).Title)
				} else {
					headline = string(objects[i].(Publisher).Name)
				}
			}

			if headline == header.Header {
				header.Amount++
				if l.Type == "issue" {
					found := false

					for idx, price := range header.Price {
						if objects[i].(Issue).Price.Currency == price.Currency {
							header.Price[idx].Price += objects[i].(Issue).Price.Price
							found = true
						}
					}

					if !found {
						header.Price = append(header.Price, *new(Price))
						header.Price[len(header.Price)-1].Currency = objects[i].(Issue).Price.Currency
						header.Price[len(header.Price)-1].Price = objects[i].(Issue).Price.Price
					}
				}
			} else {
				break
			}
		}
	} else {
		l.Objects = objects
	}

	return l, err
}

func (l List) Delete(db *sql.Tx) ([]List, error) {
	_, err := db.Exec("DELETE FROM List WHERE id = ?", l.Id)

	ret, err := l.MultiSelect(db)

	return ret, err
}

func (l List) Update(db *sql.Tx) ([]List, error) {
	_, err := db.Exec("UPDATE List SET name = ?, sort = ?, groupBy = ? WHERE id = ?", l.Name, l.Sort, l.GroupBy, l.Id)

	ret, err := l.MultiSelect(db)

	return ret, err
}

func (l List) Move(db *sql.Tx) (List, error) {
	var err error

	if l.Search.Lists[0] != 0 {
		_, err = db.Exec("UPDATE Issue_List SET fk_list = ? where fk_list = ?", l.Search.Lists[0], l.Id)
	}

	if err != nil {
		return l, err
	}

	return l, err
}

func (l List) MultiSelect(db *sql.Tx) ([]List, error) {
	rows, err := db.Query("SELECT * FROM List ORDER BY sort")

	defer rows.Close()
	if err != nil {
		return make([]List, 1), err
	}

	result := make([]List, 0)
	var tl List

	for rows.Next() {
		err := rows.Scan(&tl.Id, &tl.Name, &tl.Sort, &tl.GroupBy)

		if err != nil {
			return make([]List, 1), err
		}

		tl.Type = "issue"

		result = append(result, tl)
	}

	return result, nil
}

/*
 * Publisher
 */
type Publisher struct {
	Id   int64
	Name string
}

func (p Publisher) Select(db *sql.Tx) (Publisher, error) {
	var err error
	if p.Id == 0 {
		err = db.QueryRow("SELECT id, name FROM Publisher WHERE name = ?", p.Name).Scan(&p.Id, &p.Name)
	} else {
		err = db.QueryRow("SELECT id, name FROM Publisher WHERE id = ?", p.Id).Scan(&p.Id, &p.Name)
	}

	if err != nil {
		return p, err
	}
	return p, nil
}

func (p Publisher) Update(db *sql.Tx) (Publisher, error) {
	_, err := db.Exec("UPDATE Publisher SET name = ? WHERE id = ?", p.Name, p.Id)

	return p, err
}

func (p Publisher) MultiSelect(db *sql.Tx) ([]Publisher, int, error) {
	publishers := make([]Publisher, 0)

	query := "SELECT COUNT(*) FROM Publisher"

	count := 0
	var err error
	if p.Name != "" {
		query += " WHERE name LIKE ?"
		err = db.QueryRow(query, "%"+p.Name+"%").Scan(&count)
	} else {
		err = db.QueryRow(query).Scan(&count)
	}

	if err != nil {
		return publishers, 0, err
	}

	if count <= 0 {
		return publishers, 0, err
	}

	query = "SELECT * FROM Publisher"

	var rows *sql.Rows
	if p.Name != "" {
		query += " WHERE name LIKE ? ORDER BY (CASE WHEN name LIKE ? THEN 2 ELSE (CASE WHEN name LIKE ? THEN 1 ELSE 0 END) END) DESC, MATCH (name) AGAINST (? IN BOOLEAN MODE) DESC, name"
		rows, err = db.Query(query, "%"+p.Name+"%", p.Name, p.Name+"%", "\""+p.Name+"\"")
	} else {
		query += " ORDER BY name"
		rows, err = db.Query(query)
	}

	defer rows.Close()
	if err != nil {
		return publishers, 0, err
	}

	for rows.Next() {
		var p Publisher
		if err := rows.Scan(&p.Id, &p.Name); err != nil {
			return publishers, 0, err
		}
		publishers = append(publishers, p)
	}

	if err := rows.Err(); err != nil {
		return publishers, 0, err
	}

	return publishers, count, err
}

/*
 * Series
 */
type Series struct {
	Id        int64
	Title     string
	Startyear int64
	Endyear   int64
	Volume    int64
	Issuecount int64
	Original int64
	Publisher Publisher
}

func (s Series) Select(db *sql.Tx) (Series, error) {
	var err error
	if s.Id == 0 {
		if s.Publisher.Name != "Original Publisher" {
			err = db.QueryRow("SELECT id, title, startyear, endyear, volume, issuecount, original, fk_publisher FROM Series WHERE title = ? AND volume = ? AND fk_publisher = ?", s.Title, s.Volume, s.Publisher.Id).Scan(&s.Id, &s.Title, &s.Startyear, &s.Endyear, &s.Volume, &s.Issuecount, &s.Original, &s.Publisher.Id)
		} else {
			err = db.QueryRow("SELECT id, title, startyear, endyear, volume, issuecount, original, fk_publisher FROM Series WHERE title = ? AND volume = ?", s.Title, s.Volume).Scan(&s.Id, &s.Title, &s.Startyear, &s.Endyear, &s.Volume, &s.Issuecount, &s.Original, &s.Publisher.Id)
		}
	} else {
		err = db.QueryRow("SELECT id, title, startyear, endyear, volume, issuecount, original, fk_publisher FROM Series WHERE id = ?", s.Id).Scan(&s.Id, &s.Title, &s.Startyear, &s.Endyear, &s.Volume, &s.Issuecount, &s.Original, &s.Publisher.Id)
	}

	if err != nil {
		return s, err
	}

	s.Publisher, err = s.Publisher.Select(db)

	return s, err
}

func (s Series) MultiSelect(db *sql.Tx) ([]Series, int, error) {
	series := make([]Series, 0)

	query := "SELECT COUNT(*) FROM Series s LEFT JOIN Publisher p ON s.fk_publisher = p.id"

	count := 0
	var err error
	if s.Title != "" {
		query += " WHERE s.title LIKE ?"

		if s.Original == 1 {
			query += " AND s.original = ?"
			err = db.QueryRow(query, "%"+s.Title+"%", s.Original).Scan(&count)
		} else {
			err = db.QueryRow(query, "%"+s.Title+"%").Scan(&count)
		}
	} else {
		if s.Original == 1 {
			query += " WHERE s.original = ?"
			err = db.QueryRow(query, s.Original).Scan(&count)
		} else {
			err = db.QueryRow(query).Scan(&count)
		}
	}

	if err != nil {
		return series, 0, err
	}

	if count <= 0 {
		return series, 0, err
	}

	query = "SELECT * FROM Series s LEFT JOIN Publisher p ON s.fk_publisher = p.id"

	var rows *sql.Rows
	if s.Title != "" {
		query += " WHERE s.title LIKE ?"

		if s.Original == 1 {
			query += " AND s.original = ?"
		}

		query += "  ORDER BY (CASE WHEN title LIKE ? THEN 2 ELSE (CASE WHEN title LIKE ? THEN 1 ELSE 0 END) END) DESC, MATCH (title) AGAINST (? IN BOOLEAN MODE) DESC, s.title, s.volume, s.fk_Publisher"

		if s.Original == 1 {
			rows, err = db.Query(query, "%"+s.Title+"%", s.Original, s.Title, s.Title+"%", "\""+s.Title+"\"")
		} else {
			rows, err = db.Query(query, "%"+s.Title+"%", s.Title, s.Title+"%", "\""+s.Title+"\"")
		}
	} else {
		if s.Original == 1 {
			query += " WHERE s.original = ?"
		}

		query += " ORDER BY s.title, s.volume, s.fk_Publisher"

		if s.Original == 1 {
			rows, err = db.Query(query, s.Original)
		} else {
			rows, err = db.Query(query)
		}
	}

	defer rows.Close()
	if err != nil {
		return series, 0, err
	}

	for rows.Next() {
		var s Series
		if err := rows.Scan(&s.Id, &s.Title, &s.Startyear, &s.Endyear, &s.Volume, &s.Issuecount, &s.Original, &s.Publisher.Id, &s.Publisher.Id, &s.Publisher.Name); err != nil {
			return series, 0, err
		}
		series = append(series, s)
	}

	if err := rows.Err(); err != nil {
		return series, 0, err
	}

	if err != nil {
		log.Print(err)
	}

	return series, count, err
}

func (s Series) Update(db *sql.Tx) (Series, error) {
	//Publisher
	res, err := db.Exec("INSERT INTO Publisher (name) VALUES (?)", s.Publisher.Name)

	if err != nil {
		err = db.QueryRow("SELECT * FROM Publisher WHERE name = ?", s.Publisher.Name).Scan(&s.Publisher.Id, &s.Publisher.Name)
	} else {
		s.Publisher.Id, err = res.LastInsertId()
	}

	if err != nil {
		return s, err
	}

	//Series
	_, err = db.Exec("UPDATE Series SET title = ?, startyear = ?, endyear = ?, volume = ?, issuecount = ?, original = ?, fk_publisher = ? WHERE id = ?", s.Title, s.Startyear, s.Endyear, s.Volume, s.Issuecount, s.Original, s.Publisher.Id, s.Id)

	return s, err
}

/*
 * Story
 */
type Story struct {
	Id            int64
	Title         string
	Number        int64
	AdditionalInfo        string
	OriginalIssue Issue
	Issues        []Issue
}

func (s Story) Update(db *sql.Tx) (Story, error) {
	_, err := db.Exec("UPDATE Story SET title = ? WHERE id = ?", s.Title, s.Id)

	return s, err
}

/*
 * Issue
 */
type Price struct {
	Price float64
	Currency string
}

type Issue struct {
	Id                int64
	Title             string
	Series            Series
	Number            string
	Stories           []Story
	Lists             []List
	Format            string
	Language          string
	Pages             int
	Releasedate       string
	Price             Price
	Coverurl          string
	Quality           string
	QualityAdditional string
	Amount            int
	Read              int
	Originalissue     int
}

func (i Issue) Insert(db *sql.Tx) (Issue, error) {
	//Publisher
	res, err := db.Exec("INSERT INTO Publisher (name) VALUES (?)", i.Series.Publisher.Name)

	if err != nil {
		err = db.QueryRow("SELECT * FROM Publisher WHERE name = ?", i.Series.Publisher.Name).Scan(&i.Series.Publisher.Id, &i.Series.Publisher.Name)
	} else {
		i.Series.Publisher.Id, err = res.LastInsertId()
	}

	if err != nil {
		return i, err
	}

	//Series
	res, err = db.Exec("INSERT INTO Series (title, startyear, volume, fk_publisher) VALUES (?, ?, ?, ?)", i.Series.Title, i.Series.Startyear, i.Series.Volume, i.Series.Publisher.Id)

	if err != nil {
		err = db.QueryRow("SELECT * FROM Series WHERE title = ? AND volume = ? AND fk_publisher = ?", i.Series.Title, i.Series.Volume, i.Series.Publisher.Id).Scan(&i.Series.Id, &i.Series.Title, &i.Series.Startyear, &i.Series.Endyear, &i.Series.Volume, &i.Series.Issuecount, &i.Series.Original, &i.Series.Publisher.Id)
	} else {
		i.Series.Id, err = res.LastInsertId()
	}

	if err != nil {
		return i, err
	}

	//Issue
	res, err = db.Exec("INSERT INTO Issue (title, fk_series, number, format, language, pages, releasedate, price, currency, coverurl, quality, qualityAdditional, amount, isread, originalissue) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", i.Title, i.Series.Id, i.Number, i.Format, i.Language, i.Pages, i.Releasedate, i.Price.Price, i.Price.Currency, i.Coverurl, i.Quality, i.QualityAdditional, i.Amount, i.Read, i.Originalissue)

	if err != nil {
		itoadd, err := i.Select(db)

		if err != nil {
			return itoadd, err
		}

		itoadd.Lists = append(itoadd.Lists, i.Lists[0])

		return itoadd.Update(db)
	}

	i.Id, err = res.LastInsertId()

	if err != nil {
		return i, err
	}

	//Lists
	for _, list := range i.Lists {
		if list.Id != 0 {
			res, err = db.Exec("INSERT INTO Issue_List (fk_issue, fk_list) VALUES (?, ?)", i.Id, list.Id)

			if err != nil {
				return i, err
			}
		}
	}

	//Stories
	for _, story := range i.Stories {
		//Story
		res, err := db.Exec("INSERT INTO Story (title, number, additionalInfo) VALUES (?, ?, ?)", story.Title, story.Number, story.AdditionalInfo)

		if err != nil {
			err = db.QueryRow("SELECT * FROM Story WHERE title = ? AND number = ? AND additionalInfo = ?", story.Title, story.Number, story.AdditionalInfo).Scan(&story.Id, &story.Title, &story.Number, &story.AdditionalInfo)
		} else {
			story.Id, err = res.LastInsertId()
		}

		if err != nil {
			return i, err
		}

		//Issue_Story
		res, err = db.Exec("INSERT INTO Issue_Story (fk_issue, fk_story) VALUES (?, ?)", i.Id, story.Id)

		if err != nil {
			return i, err
		}

		//Publisher OI
		res, err = db.Exec("INSERT INTO Publisher (name) VALUES (?)", story.OriginalIssue.Series.Publisher.Name)

		if err != nil {
			err = db.QueryRow("SELECT * FROM Publisher WHERE name = ?", story.OriginalIssue.Series.Publisher.Name).Scan(&story.OriginalIssue.Series.Publisher.Id, &story.OriginalIssue.Series.Publisher.Name)
		} else {
			story.OriginalIssue.Series.Publisher.Id, err = res.LastInsertId()
		}

		if err != nil {
			return i, err
		}

		//Series OI
		res, err = db.Exec("INSERT INTO Series (title, startyear, volume, fk_publisher) VALUES (?, ?, ?, ?)", story.OriginalIssue.Series.Title, story.OriginalIssue.Series.Startyear, story.OriginalIssue.Series.Volume, story.OriginalIssue.Series.Publisher.Id)

		if err != nil {
			err = db.QueryRow("SELECT * FROM Series WHERE title = ? AND volume = ? AND fk_publisher = ?", story.OriginalIssue.Series.Title, story.OriginalIssue.Series.Volume, story.OriginalIssue.Series.Publisher.Id).Scan(&story.OriginalIssue.Series.Id, &story.OriginalIssue.Series.Title, &story.OriginalIssue.Series.Startyear, &story.OriginalIssue.Series.Endyear, &story.OriginalIssue.Series.Volume, &story.OriginalIssue.Series.Issuecount, &story.OriginalIssue.Series.Original, &story.OriginalIssue.Series.Publisher.Id)
		} else {
			story.OriginalIssue.Series.Id, err = res.LastInsertId()
		}

		if err != nil {
			return i, err
		}

		//Issue OI
		res, err = db.Exec("INSERT INTO Issue (title, fk_series, number, format, language, pages, releasedate, price, currency, coverurl, quality, qualityAdditional, amount, isread, originalissue) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", story.OriginalIssue.Title, story.OriginalIssue.Series.Id, story.OriginalIssue.Number, story.OriginalIssue.Format, story.OriginalIssue.Language, story.OriginalIssue.Pages, story.OriginalIssue.Releasedate, story.OriginalIssue.Price.Price, story.OriginalIssue.Price.Currency, story.OriginalIssue.Coverurl, story.OriginalIssue.Quality, story.OriginalIssue.QualityAdditional, story.OriginalIssue.Amount, story.OriginalIssue.Read, story.OriginalIssue.Originalissue)

		if err != nil {
			err = db.QueryRow("SELECT * FROM Issue WHERE fk_series = ? AND number = ?", story.OriginalIssue.Series.Id, story.OriginalIssue.Number).Scan(&story.OriginalIssue.Id, &story.OriginalIssue.Title, &story.OriginalIssue.Series.Id, &story.OriginalIssue.Number, &story.OriginalIssue.Format, &story.OriginalIssue.Language, &story.OriginalIssue.Pages, &story.OriginalIssue.Releasedate, &story.OriginalIssue.Price.Price, &story.OriginalIssue.Price.Currency, &story.OriginalIssue.Coverurl, &story.OriginalIssue.Quality, &story.OriginalIssue.QualityAdditional, &story.OriginalIssue.Amount, &story.OriginalIssue.Read, &story.OriginalIssue.Originalissue)

			if err != nil {
				return i, err
			}
		} else {
			story.OriginalIssue.Id, err = res.LastInsertId()

			if err != nil {
				return i, err
			}
		}

		//Issue_Story OI
		//Ignore error. Might be already there
		db.Exec("INSERT INTO Issue_Story (fk_issue, fk_story) VALUES (?, ?)", story.OriginalIssue.Id, story.Id)
	}

	return i, err
}

func (i Issue) Select(db *sql.Tx) (Issue, error) {
	var err error

	//Issue
	if i.Id == 0 {
		err = db.QueryRow("SELECT * FROM Issue WHERE title = ? AND fk_series = ? AND number = ?", i.Title, i.Series.Id, i.Number).Scan(&i.Id, &i.Title, &i.Series.Id, &i.Number, &i.Format, &i.Language, &i.Pages, &i.Releasedate, &i.Price.Price, &i.Price.Currency, &i.Coverurl, &i.Quality, &i.QualityAdditional, &i.Amount, &i.Read, &i.Originalissue)
	} else {
		err = db.QueryRow("SELECT * FROM Issue WHERE id = ?", i.Id).Scan(&i.Id, &i.Title, &i.Series.Id, &i.Number, &i.Format, &i.Language, &i.Pages, &i.Releasedate, &i.Price.Price, &i.Price.Currency, &i.Coverurl, &i.Quality, &i.QualityAdditional, &i.Amount, &i.Read, &i.Originalissue)
	}

	if err != nil {
		return i, err
	}

	//Series
	err = db.QueryRow("SELECT * FROM Series WHERE id = ?", i.Series.Id).Scan(&i.Series.Id, &i.Series.Title, &i.Series.Startyear, &i.Series.Endyear, &i.Series.Volume, &i.Series.Issuecount, &i.Series.Original, &i.Series.Publisher.Id)

	if err != nil {
		return i, err
	}

	//Publisher
	err = db.QueryRow("SELECT * FROM Publisher WHERE id = ?", i.Series.Publisher.Id).Scan(&i.Series.Publisher.Id, &i.Series.Publisher.Name)

	if err != nil {
		return i, err
	}

	//Lists
	rows, err := db.Query("SELECT fk_list FROM Issue_List WHERE fk_issue = ?", i.Id)

	defer rows.Close()
	if err != nil {
		return i, err
	}

	i.Lists = make([]List, 0)

	list := new(List)
	lists := make([]List, 0)
	for rows.Next() {
		list = new(List)
		err := rows.Scan(&list.Id)

		if err != nil {
			return i, err
		}

		lists = append(lists, *list)
	}

	for _, l := range lists {
		//List
		list = new(List)
		err = db.QueryRow("SELECT * FROM List WHERE id = ?", l.Id).Scan(&list.Id, &list.Name, &list.Sort, &list.GroupBy)

		if err != nil {
			return i, err
		}

		i.Lists = append(i.Lists, *list)
	}

	//Stories
	rows, err = db.Query("SELECT fk_story FROM Issue_Story WHERE fk_issue = ?", i.Id)

	defer rows.Close()
	if err != nil {
		return i, err
	}

	i.Stories = make([]Story, 0)
	stories := make([]Story, 0)
	for rows.Next() {
		story := new(Story)
		err := rows.Scan(&story.Id)

		if err != nil {
			return i, err
		}

		stories = append(stories, *story)
	}

	for _, story := range stories {
		//Story
		err = db.QueryRow("SELECT * FROM Story WHERE id = ?", story.Id).Scan(&story.Id, &story.Title, &story.Number, &story.AdditionalInfo)

		if err != nil {
			return i, err
		}

		//Issues
		rows, err := db.Query("SELECT fk_issue FROM Issue_Story WHERE fk_story = ?", story.Id)

		defer rows.Close()
		if err != nil {
			return i, err
		}

		issues := make([]Issue, 0)
		for rows.Next() {
			issue := new(Issue)
			err = rows.Scan(&issue.Id)

			issues = append(issues, *issue)
		}

		for _, issue := range issues {
			//Issue
			err = db.QueryRow("SELECT * FROM Issue WHERE id = ?", issue.Id).Scan(&issue.Id, &issue.Title, &issue.Series.Id, &issue.Number, &issue.Format, &issue.Language, &issue.Pages, &issue.Releasedate, &issue.Price.Price, &issue.Price.Currency, &issue.Coverurl, &issue.Quality, &issue.QualityAdditional, &issue.Amount, &issue.Read, &issue.Originalissue)

			if err != nil {
				return i, err
			}

			//Series
			err = db.QueryRow("SELECT * FROM Series WHERE id = ?", issue.Series.Id).Scan(&issue.Series.Id, &issue.Series.Title, &issue.Series.Startyear, &issue.Series.Endyear, &issue.Series.Volume, &issue.Series.Issuecount, &issue.Series.Original, &issue.Series.Publisher.Id)

			if err != nil {
				return i, err
			}

			//Publisher
			err = db.QueryRow("SELECT * FROM Publisher WHERE id = ?", issue.Series.Publisher.Id).Scan(&issue.Series.Publisher.Id, &issue.Series.Publisher.Name)

			if err != nil {
				return i, err
			}

			if issue.Originalissue == 0 {
				story.Issues = append(story.Issues, issue)
			}

			if issue.Originalissue == 1 {
				story.OriginalIssue = issue
			}
		}

		i.Stories = append(i.Stories, story)
	}

	sort.Slice(i.Stories[:], func(k, l int) bool {
		return natsort.Less(i.Stories[k].OriginalIssue.Series.Title+" "+strconv.FormatInt(i.Stories[k].OriginalIssue.Series.Startyear, 10)+" "+i.Stories[k].OriginalIssue.Number+ " (" + roman.Roman(int(i.Stories[k].Number)) + ")", i.Stories[l].OriginalIssue.Series.Title+" "+strconv.FormatInt(i.Stories[l].OriginalIssue.Series.Startyear, 10)+" "+i.Stories[l].OriginalIssue.Number + " (" + roman.Roman(int(i.Stories[l].Number)) + ")")
	})

	return i, err
}

func (i Issue) Delete(db *sql.Tx) (Issue, error) {
	issue, err := i.Select(db)

	if err != nil {
		return issue, err
	}

	//List Relations
	for _, l := range issue.Lists {
		if l.Id == i.Lists[0].Id {
			_, err = db.Exec("DELETE FROM Issue_List WHERE fk_issue = ? AND fk_list = ?", i.Id, l.Id)

			if err != nil {
				return issue, err
			}
		}
	}

	archive := false
	for _, l := range i.Lists {
		if l.Id == 0 {
			archive = true
		}
	}

	if !archive {
		return issue, err
	}

	//Stories Relations
	_, err = db.Exec("DELETE FROM Issue_Story WHERE fk_issue = ?", issue.Id)

	if err != nil {
		return issue, err
	}

	for _, story := range issue.Stories {
		if len(story.Issues) == 1 {
			_, err = db.Exec("DELETE FROM Issue_Story WHERE fk_story = ?", story.Id)

			if err != nil {
				return issue, err
			}

			_, err = db.Exec("DELETE FROM Story WHERE id = ?", story.Id)

			if err != nil {
				return issue, err
			}
		}

		//Try to delete Originalissue. This will be skipped if it is still in use
		db.Exec("DELETE FROM Issue WHERE id = ?", story.OriginalIssue.Id)
		db.Exec("DELETE FROM Series WHERE id = ?", story.OriginalIssue.Series.Id)
		db.Exec("DELETE FROM Publisher WHERE id = ?", story.OriginalIssue.Series.Publisher.Id)
	}

	_, err = db.Exec("DELETE FROM Issue WHERE id = ?", issue.Id)

	if err != nil {
		return i, err
	}

	db.Exec("DELETE FROM Series WHERE id = ?", issue.Series.Id)
	db.Exec("DELETE FROM Publisher WHERE id = ?", issue.Series.Publisher.Id)

	return issue, err
}

func (i Issue) Update(db *sql.Tx) (Issue, error) {
	old, err := i.Select(db)

	//Try to delete Series and Publisher, ignore error because of foreign key exception
	db.Exec("DELETE FROM Series WHERE id = ?", i.Series.Id)
	db.Exec("DELETE FROM Publisher WHERE id = ?", i.Series.Publisher.Id)

	//Publisher
	res, err := db.Exec("INSERT INTO Publisher (name) VALUES (?)", i.Series.Publisher.Name)

	if err != nil {
		err = db.QueryRow("SELECT * FROM Publisher WHERE name = ?", i.Series.Publisher.Name).Scan(&i.Series.Publisher.Id, &i.Series.Publisher.Name)
	} else {
		i.Series.Publisher.Id, err = res.LastInsertId()
	}

	if err != nil {
		return i, err
	}

	//Series
	res, err = db.Exec("INSERT INTO Series (title, startyear, volume, fk_publisher) VALUES (?, ?, ?, ?)", i.Series.Title, i.Series.Startyear, i.Series.Volume, i.Series.Publisher.Id)

	if err != nil {
		err = db.QueryRow("SELECT * FROM Series WHERE title = ? AND volume = ? AND fk_publisher = ?", i.Series.Title, i.Series.Volume, i.Series.Publisher.Id).Scan(&i.Series.Id, &i.Series.Title, &i.Series.Startyear, &i.Series.Endyear, &i.Series.Volume, &i.Series.Issuecount, &i.Series.Original, &i.Series.Publisher.Id)
	} else {
		i.Series.Id, err = res.LastInsertId()
	}

	if err != nil {
		return i, err
	}

	//Issue
	_, err = db.Exec("UPDATE Issue SET title = ?, fk_series = ?, number = ?, format = ?, language = ?, pages = ?, releasedate = ?, price = ?, currency = ?, coverurl = ?, quality = ?, qualityAdditional = ?, amount = ?, isread = ?, originalissue = ? WHERE id = ?", i.Title, i.Series.Id, i.Number, i.Format, i.Language, i.Pages, i.Releasedate, i.Price.Price, i.Price.Currency, i.Coverurl, i.Quality, i.QualityAdditional, i.Amount, i.Read, i.Originalissue, i.Id)

	if err != nil {
		return i, err
	}

	//Lists
	for _, oldList := range old.Lists {
		found := false

		for _, newList := range i.Lists {
			if oldList.Id == newList.Id {
				found = true
				break
			}
		}

		if !found {
			if oldList.Id != 0 {
				_, err = db.Exec("DELETE FROM Issue_List WHERE fk_issue = ? AND fk_list = ?", i.Id, oldList.Id)

				if err != nil {
					return i, err
				}
			}
		}
	}

	for _, newList := range i.Lists {
		found := false

		for _, oldList := range old.Lists {
			if newList.Id == oldList.Id {
				found = true
				break
			}
		}

		if !found {
			if newList.Id != 0 {
				_, err = db.Exec("INSERT INTO Issue_List (fk_issue, fk_list) VALUES (?, ?)", i.Id, newList.Id)

				if err != nil {
					return i, err
				}
			}
		}
	}

	//Story
	if len(i.Stories) != 0 {
		for _, oldStory := range old.Stories {
			found := false

			for _, newStory := range i.Stories {
				if oldStory.Number == newStory.Number &&
					oldStory.Title == newStory.Title &&
					oldStory.AdditionalInfo == newStory.AdditionalInfo &&
					oldStory.OriginalIssue.Number == newStory.OriginalIssue.Number &&
					oldStory.OriginalIssue.Series.Title == newStory.OriginalIssue.Series.Title &&
					oldStory.OriginalIssue.Series.Volume == newStory.OriginalIssue.Series.Volume &&
					oldStory.OriginalIssue.Series.Publisher.Name == newStory.OriginalIssue.Series.Publisher.Name {
					found = true
					break
				}
			}

			if !found {
				_, err = db.Exec("DELETE FROM Issue_Story WHERE fk_issue = ? AND fk_story = ?", i.Id, oldStory.Id)

				if err != nil {
					return i, err
				}

				if len(oldStory.Issues) == 1 {
					_, err = db.Exec("DELETE FROM Issue_Story WHERE fk_story = ?", oldStory.Id)

					if err != nil {
						return i, err
					}

					_, err = db.Exec("DELETE FROM Story WHERE id = ?", oldStory.Id)

					if err != nil {
						return i, err
					}
				}

				//Try to delete Originalissue. This will be skipped if it is still in use
				db.Exec("DELETE FROM Issue WHERE id = ?", oldStory.OriginalIssue.Id)
				db.Exec("DELETE FROM Series WHERE id = ?", oldStory.OriginalIssue.Series.Id)
				db.Exec("DELETE FROM Publisher WHERE id = ?", oldStory.OriginalIssue.Series.Publisher.Id)
			}
		}

		for _, newStory := range i.Stories {
			found := false

			for _, oldStory := range old.Stories {
				if oldStory.Number == newStory.Number &&
					oldStory.Title == newStory.Title &&
					oldStory.AdditionalInfo == newStory.AdditionalInfo &&
					oldStory.OriginalIssue.Number == newStory.OriginalIssue.Number &&
					oldStory.OriginalIssue.Series.Title == newStory.OriginalIssue.Series.Title &&
					oldStory.OriginalIssue.Series.Volume == newStory.OriginalIssue.Series.Volume &&
					oldStory.OriginalIssue.Series.Publisher.Name == newStory.OriginalIssue.Series.Publisher.Name {
					found = true
					break
				}
			}

			if !found {
				//Story
				res, err := db.Exec("INSERT INTO Story (title, number, additionalInfo) VALUES (?, ?, ?)", newStory.Title, newStory.Number, newStory.AdditionalInfo)

				if err != nil {
					err = db.QueryRow("SELECT * FROM Story WHERE title = ? AND number = ? AND additionalInfo = ?", newStory.Title, newStory.Number, newStory.AdditionalInfo).Scan(&newStory.Id, &newStory.Title, &newStory.Number, &newStory.AdditionalInfo)
				} else {
					newStory.Id, err = res.LastInsertId()
				}

				if err != nil {
					return i, err
				}

				//Issue_Story
				res, err = db.Exec("INSERT INTO Issue_Story (fk_issue, fk_story) VALUES (?, ?)", i.Id, newStory.Id)

				if err != nil {
					return i, err
				}

				//Publisher OI
				res, err = db.Exec("INSERT INTO Publisher (name) VALUES (?)", newStory.OriginalIssue.Series.Publisher.Name)

				if err != nil {
					err = db.QueryRow("SELECT * FROM Publisher WHERE name = ?", newStory.OriginalIssue.Series.Publisher.Name).Scan(&newStory.OriginalIssue.Series.Publisher.Id, &newStory.OriginalIssue.Series.Publisher.Name)
				} else {
					newStory.OriginalIssue.Series.Publisher.Id, err = res.LastInsertId()
				}

				if err != nil {
					return i, err
				}

				//Series OI
				res, err = db.Exec("INSERT INTO Series (title, startyear, volume, fk_publisher) VALUES (?, ?, ?, ?)", newStory.OriginalIssue.Series.Title, newStory.OriginalIssue.Series.Startyear, newStory.OriginalIssue.Series.Volume, newStory.OriginalIssue.Series.Original, newStory.OriginalIssue.Series.Publisher.Id)

				if err != nil {
					err = db.QueryRow("SELECT * FROM Series WHERE title = ? AND volume = ? AND fk_publisher = ?", newStory.OriginalIssue.Series.Title, newStory.OriginalIssue.Series.Volume, newStory.OriginalIssue.Series.Publisher.Id).Scan(&newStory.OriginalIssue.Series.Id, &newStory.OriginalIssue.Series.Title, &newStory.OriginalIssue.Series.Startyear, &newStory.OriginalIssue.Series.Endyear, &newStory.OriginalIssue.Series.Volume, &newStory.OriginalIssue.Series.Issuecount, &newStory.OriginalIssue.Series.Original, &newStory.OriginalIssue.Series.Publisher.Id)
				} else {
					newStory.OriginalIssue.Series.Id, err = res.LastInsertId()
				}

				if err != nil {
					return i, err
				}

				//Issue OI
				res, err = db.Exec("INSERT INTO Issue (title, fk_series, number, format, language, pages, releasedate, price, currency, coverurl, quality, qualityAdditional, amount, isread, originalissue) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", newStory.OriginalIssue.Title, newStory.OriginalIssue.Series.Id, newStory.OriginalIssue.Number, newStory.OriginalIssue.Format, newStory.OriginalIssue.Language, newStory.OriginalIssue.Pages, newStory.OriginalIssue.Releasedate, newStory.OriginalIssue.Price.Price, newStory.OriginalIssue.Price.Currency, newStory.OriginalIssue.Coverurl, newStory.OriginalIssue.Quality, newStory.OriginalIssue.QualityAdditional, newStory.OriginalIssue.Amount, newStory.OriginalIssue.Read, newStory.OriginalIssue.Originalissue)

				if err != nil {
					err = db.QueryRow("SELECT * FROM Issue WHERE fk_series = ? AND number = ?", newStory.OriginalIssue.Series.Id, newStory.OriginalIssue.Number).Scan(&newStory.OriginalIssue.Id, &newStory.OriginalIssue.Title, &newStory.OriginalIssue.Series.Id, &newStory.OriginalIssue.Number, &newStory.OriginalIssue.Format, &newStory.OriginalIssue.Language, &newStory.OriginalIssue.Pages, &newStory.OriginalIssue.Releasedate, &newStory.OriginalIssue.Price.Price, &newStory.OriginalIssue.Price.Currency, &newStory.OriginalIssue.Coverurl, &newStory.OriginalIssue.Quality, &newStory.OriginalIssue.QualityAdditional, &newStory.OriginalIssue.Amount, &newStory.OriginalIssue.Read, &newStory.OriginalIssue.Originalissue)

					if err != nil {
						return i, err
					}
				} else {
					newStory.OriginalIssue.Id, err = res.LastInsertId()

					if err != nil {
						return i, err
					}
				}

				//Issue_Story OI
				//Ignore error. Might be already there
				db.Exec("INSERT INTO Issue_Story (fk_issue, fk_story) VALUES (?, ?)", newStory.OriginalIssue.Id, newStory.Id)
			}
		}
	}

	return i, err
}

func (i Issue) MultiSelect(db *sql.Tx, s Search) ([]Issue, int, error) {
	if s.Offset == -1 {
		return make([]Issue, 0), 0, nil
	}

	issues := make([]Issue, 0)

	query, args := createStatement(true, s)
	var count int
	var trash int
	err := db.QueryRow(query, args...).Scan(&count, &trash, &trash)

	if err != nil {
		if strings.Contains(err.Error(), "no rows in result set") {
			return issues, 0, nil
		} else {
			return issues, 0, err
		}
	}

	if count <= 0 {
		return issues, 0, err
	}

	query, args = createStatement(false, s)

	rows, err := db.Query(query, args...)

	defer rows.Close()
	if err != nil {
		if strings.Contains(err.Error(), "no rows in result set") {
			return issues, 0, nil
		} else {
			return issues, 0, err
		}
	}

	for rows.Next() {
		var storyCountMax int
		var storyCountMin int
		var i Issue
		if err := rows.Scan(&i.Id, &i.Title, &i.Series.Title, &i.Series.Startyear, &i.Series.Endyear, &i.Series.Volume, &i.Series.Issuecount, &i.Series.Publisher.Name, &i.Number, &i.Language, &i.Releasedate, &i.Amount, &i.Format, &i.Price.Price, &i.Price.Currency, &i.Pages, &i.Originalissue, &i.Read, &i.Coverurl, &i.Quality, &i.QualityAdditional, &storyCountMax, &storyCountMin); err != nil {
			return issues, 0, err
		}
		issues = append(issues, i)
	}

	if err := rows.Err(); err != nil {
		return issues, 0, err
	}

	if strings.HasPrefix(s.GOne, "s.title") && strings.HasPrefix(s.GTwo, "s.startyear") {
		sort.Slice(issues[:], func(k, l int) bool {
			return natsort.Less(issues[k].Series.Title + " (" + roman.Roman(int(issues[k].Series.Volume))+ ") (" + strconv.FormatInt(issues[k].Series.Startyear, 10) + ") #" + issues[k].Number, issues[l].Series.Title + " (" + roman.Roman(int(issues[l].Series.Volume))+ ") (" + strconv.FormatInt(issues[l].Series.Startyear, 10) + ") #" + issues[l].Number)
		})
	}

	return issues, count, err
}

func createStatement(count bool, s Search) (string, []interface{}) {
	var query string
	args := make([]interface{}, 0)

	query = ""
	if s.SDuplicateIn != "BO" {
		query += "SELECT * FROM ("
	}

	if count {
		query += "SELECT COUNT(*) AS count"
	} else {
		query += "SELECT i.id, i.title, s.title AS stitle, s.startyear, s.endyear, s.volume, s.issuecount, p.name, i.number, i.language, i.releasedate, i.amount, i.format, i.price, i.currency, i.pages, i.originalissue, i.isread, i.coverurl, i.quality, i.qualityAdditional"
	}

	if s.SDuplicateIn != "BO" {
		query += ", MAX(t.storyCount) AS storyCountMax, MIN(t.storyCount) AS storyCountMin"
	} else {
		query += ", 1 AS storyCountMax, 1 AS storyCountMin"
	}

	query += " FROM Issue i LEFT JOIN Series s ON i.fk_series = s.id LEFT JOIN Publisher p ON s.fk_publisher = p.id LEFT JOIN Issue_List il ON i.id = il.fk_issue LEFT JOIN List l ON il.fk_list = l.id"

	if s.OrgIssue {
		query += " LEFT JOIN Issue_Story iss ON iss.fk_issue = i.id"
	}

	if s.SDuplicateIn != "BO" {
		query += " LEFT JOIN Issue_Story iss2 ON i.id = iss2.fk_issue LEFT JOIN (SELECT fk_story, COUNT(*) AS storyCount FROM Issue_Story iss2 LEFT JOIN Issue i ON iss2.fk_issue = i.id LEFT JOIN Issue_List il ON il.fk_issue = i.id WHERE i.amount > 0"

		for _, id := range s.Lists {
			if id != 0 {
				query += " OR il.fk_list = ?"
				args = append(args, id)
			}
		}

		query += " GROUP BY fk_story) t ON iss2.fk_story = t.fk_story"
	}

	query += " WHERE"

	if s.OrgIssue {
		query += " i.id IN (SELECT DISTINCT fk_issue FROM Issue_Story iss LEFT JOIN Issue i ON iss.fk_issue = i.id WHERE iss.fk_story IN (SELECT fk_story FROM (SELECT DISTINCT i1.id FROM Issue_Story iss LEFT JOIN Issue i1 ON iss.fk_issue =  i1.id LEFT JOIN Issue_List l ON l.fk_issue = i1.id WHERE "

		subquery, subargs := createStatement(false, *s.OrgIssueS)

		query += "i1.id IN (SELECT sub.id FROM (" + subquery + ") sub)"
		args = append(args, subargs...)

		/*for idx, id := range s.Lists {
			if idx != 0 {
				query += " OR"
			}

			if id != 0 {
				query += " l.fk_list = ?"
				args = append(args, id)
			} else {
				query += " i1.amount > 0"
			}
		}*/

		query += ") temp LEFT JOIN Issue_Story iss ON temp.id = iss.fk_issue) AND i.originalissue = 1) AND"
	}

	if s.STitle != "" {
		switch s.STitle {
		case "GR":
			query += " i.title >= ?"
			args = append(args, s.Issue.Title)
		case "LE":
			query += " i.title <= ?"
			args = append(args, s.Issue.Title)
		case "GQ":
			query += " i.title like ?"
			args = append(args, s.Issue.Title+"%")
		case "LQ":
			query += " i.title like ?"
			args = append(args, "%"+s.Issue.Title)
		case "EQ":
			query += " i.title = ?"
			args = append(args, s.Issue.Title)
		}
	} else {
		query += " i.title like ?"
		args = append(args, "%"+s.Issue.Title+"%")
	}

	if s.SSeries != "" {
		switch s.SSeries {
		case "GR":
			query += " AND s.title >= ?"
			args = append(args, s.Issue.Series.Title)
		case "LE":
			query += " AND s.title <= ?"
			args = append(args, s.Issue.Series.Title)
		case "GQ":
			query += " AND s.title like ?"
			args = append(args, s.Issue.Series.Title+"%")
		case "LQ":
			query += " AND s.title like ?"
			args = append(args, "%"+s.Issue.Series.Title)
		case "EQ":
			query += " AND s.title = ?"
			args = append(args, s.Issue.Series.Title)
		}
	} else {
		query += " AND s.title like ?"
		args = append(args, "%"+s.Issue.Series.Title+"%")
	}

	if s.Issue.Series.Startyear != 0 {
		query += " AND s.startyear = ?"
		args = append(args, s.Issue.Series.Startyear)
	}

	if s.SPublisher != "" {
		switch s.SPublisher {
		case "GR":
			query += " AND s.title >= ?"
			args = append(args, s.Issue.Series.Publisher.Name)
		case "LE":
			query += " AND s.title <= ?"
			args = append(args, s.Issue.Series.Publisher.Name)
		case "GQ":
			query += " AND p.name like ?"
			args = append(args, s.Issue.Series.Publisher.Name+"%")
		case "LQ":
			query += " AND p.name like ?"
			args = append(args, "%"+s.Issue.Series.Publisher.Name)
		case "EQ":
			query += " AND p.name = ?"
			args = append(args, s.Issue.Series.Publisher.Name)
		}
	} else {
		query += " AND p.name like ?"
		args = append(args, "%"+s.Issue.Series.Publisher.Name+"%")
	}

	if s.Issue.Format != "" {
		query += " AND i.format like ?"
		args = append(args, "%"+s.Issue.Format+"%")
	}

	if s.Issue.Pages != 0 {
		switch s.SPages {
		case "GR":
			query += " AND i.pages > ?"
			args = append(args, s.Issue.Pages)
		case "LE":
			query += " AND i.pages < ?"
			args = append(args, s.Issue.Pages)
		case "EQ":
			query += " AND i.pages = ?"
			args = append(args, s.Issue.Pages)
		case "BT":
			query += " AND i.pages BETWEEN ? AND ? "
			args = append(args, s.Issue.Pages)
			args = append(args, s.Issue2.Pages)
		}
	}

	if s.Issue.Quality != "" {
		query += " AND i.quality = ?"
		args = append(args, s.Issue.Quality)
	}

	if s.SCoverurl == "YE" {
		query += " AND i.coverurl != ''"
	} else if s.SCoverurl == "NO" {
		query += " AND i.coverurl = ''"
	}

	if s.Issue.Releasedate != "0000-00-00" {
		switch s.SReleasedate {
		case "GR":
			query += " AND i.releasedate > ?"
			args = append(args, s.Issue.Releasedate)
		case "LE":
			query += " AND i.releasedate < ?"
			args = append(args, s.Issue.Releasedate)
		case "EQ":
			query += " AND i.releasedate = ?"
			args = append(args, s.Issue.Releasedate)
		case "BT":
			query += " AND i.releasedate BETWEEN ? AND ? "
			args = append(args, s.Issue.Releasedate)
			args = append(args, s.Issue2.Releasedate)
		}
	}

	if s.Issue.Number != "" {
		q := "i.number"

		if _, err := strconv.Atoi(s.Issue.Number); err == nil {
			if s.SNumber == "BT" {
				if _, err := strconv.Atoi(s.Issue2.Number); err == nil {
					q = "CAST(i.number AS SIGNED)"
				}
			} else {
				q = "CAST(i.number AS SIGNED)"
			}
		}

		switch s.SNumber {
		case "GR":
			query += " AND " + q + " > ?"
			args = append(args, s.Issue.Number)
		case "LE":
			query += " AND " + q + " < ?"
			args = append(args, s.Issue.Number)
		case "EQ":
			query += " AND " + q + " = ?"
			args = append(args, s.Issue.Number)
		case "BT":
			query += " AND " + q + " BETWEEN ? AND ?"
			args = append(args, s.Issue.Number)
			args = append(args, s.Issue2.Number)
		}
	}

	if s.SOriginalissue == "YE" {
		query += " AND i.originalissue = 1"
	} else if s.SOriginalissue == "NO" {
		query += " AND i.originalissue = 0"
	}

	if s.Issue.Language != "" {
		query += " AND i.language = ?"
		args = append(args, s.Issue.Language)
	}

	if s.Issue.Price.Price != 0 {
		switch s.SPrice {
		case "GR":
			query += " AND i.price > ?"
			args = append(args, s.Issue.Price)
		case "LE":
			query += " AND i.price < ?"
			args = append(args, s.Issue.Price)
		case "EQ":
			query += " AND i.price = ?"
			args = append(args, s.Issue.Price)
		case "BT":
			query += " AND i.price BETWEEN ? AND ? "
			args = append(args, s.Issue.Price)
			args = append(args, s.Issue2.Price)
		}
	}

	if s.Issue.Amount != 0 {
		switch s.SAmount {
		case "GR":
			query += " AND i.amount > ?"
			args = append(args, s.Issue.Amount)
		case "LE":
			query += " AND i.amount < ?"
			args = append(args, s.Issue.Amount)
		case "EQ":
			query += " AND i.amount = ?"
			args = append(args, s.Issue.Amount)
		case "BT":
			query += " AND i.amount BETWEEN ? AND ? "
			args = append(args, s.Issue.Amount)
			args = append(args, s.Issue2.Amount)
		}
	}

	if s.SRead == "YE" {
		query += " AND i.isread = 1"
	} else if s.SRead == "NO" {
		query += " AND i.isread = 0"
	}

	if s.OrgIssue {
		query += " AND i.originalissue = 1"
	} else if len(s.Lists) > 0 && s.Lists[0] != 0 {
		query += " AND ("
		for idx, id := range s.Lists {
			if idx != 0 {
				query += " OR"
			}

			if id != 0 {
				query += " l.id = ?"
				args = append(args, id)
			}
		}

		query += ")"
	} else {
		query += " AND i.originalissue = 0"
	}

	groupby := s.GOne

	if strings.HasPrefix(s.GOne, "i.releasedate") {
		groupby = "i.releasedate"
	}

	query += " GROUP BY i.id"

	query += " ORDER BY " + groupby + " " + s.GOneDir

	if s.GTwo != "" {
		query += "," + s.GTwo + " " + s.GTwoDir
	}

	if strings.HasPrefix(s.GOne, "s.") || strings.HasPrefix(s.GTwo, "s.") {
		query += ", p.Name ASC"
	}

	if strings.HasPrefix(s.GTwo, "i.title") {
		query += ", i.number ASC"
	} else if strings.HasPrefix(s.GTwo, "s.") {
		query += ", i.number ASC"
	}

	if s.SDuplicateIn == "YE" {
		query += ") t WHERE storyCountMax >= 2"
	} else if s.SDuplicateIn == "NO" {
		query += ") t WHERE storyCountMax = 1"
	} else if s.SDuplicateIn == "CO" {
		query += ") t WHERE storyCountMax = storyCountMin AND storyCountMax >= 2"
	}

	return query, args
}

/*
 * User
 */
type User struct {
	Id        int64
	Name      string
	Password  string
	SessionId string
}

func (u User) CheckSession(db *sql.Tx) (bool, error) {
	var currentSessionid string = ""

	err := db.QueryRow("SELECT sessionid FROM User WHERE name = ?", u.Name).Scan(&currentSessionid)
	if err != nil {
		return false, err
	}

	if currentSessionid == u.SessionId {
		return true, err
	} else {
		return false, err
	}
}

func (u User) Login(db *sql.Tx) (string, error) {
	err := db.QueryRow("SELECT sessionid FROM User WHERE name = ? AND password = ?", u.Name, u.Password).Scan(&u.SessionId)

	if err != nil {
		return "", err
	}

	sessionid, err := GenerateRandomString(32)
	_, err = db.Exec("UPDATE User SET sessionid = ? WHERE name = ? AND password = ?", sessionid, u.Name, u.Password)

	if err != nil {
		return "", err
	}

	return sessionid, err
}

func (u User) Logout(db *sql.Tx) (string, error) {
	_, err := db.Exec("UPDATE User SET sessionid = ?", "")

	if err != nil {
		return "", err
	}

	return "", err
}

func GenerateRandomBytes(n int) ([]byte, error) {
	b := make([]byte, n)
	_, err := rand.Read(b)
	// Note that err == nil only if we read len(b) bytes.
	if err != nil {
		return nil, err
	}

	return b, nil
}

func GenerateRandomString(s int) (string, error) {
	b, err := GenerateRandomBytes(s)
	return base64.URLEncoding.EncodeToString(b), err
}