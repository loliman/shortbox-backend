package crawler

import (
	"github.com/loliman/shortbox-backend/dal"
	"strings"
	"github.com/PuerkitoBio/goquery"
	"log"
	"strconv"
	"github.com/StefanSchroeder/Golang-Roman"
	"unicode"
	"fmt"
	"regexp"
	"math"
	"database/sql"
	"golang.org/x/net/html"
	"github.com/gorilla/websocket"
)

type Response struct {
	Type string
	Message string
	Payload interface{}
}

func CrawlOi(conn *websocket.Conn, db *sql.Tx) (string) {
	failedIssues := ""

	//Series
	var series dal.Series
	series.Original = 1
	seriess, samount, err := series.MultiSelect(db)

	for idx, s := range seriess {
		url := s.Title + "_Vol_" + strconv.FormatInt(s.Volume, 10)
		url = strings.Replace(url, " ", "_", -1)
		url = strings.Replace(url, "?", "%3F", -1)
		url = strings.Replace(url, "&", "%26", -1)
		url =  "http://marvel.wikia.com/wiki/" + url

		//Send current item to shortbox
		var rsp Response
		rsp.Type = "INFO"
		rsp.Message = ""

		item := new(dal.Message)
		item.Type = "Series"
		item.Message = s.Title + " (" + roman.Roman(int(s.Volume)) + ") (" + strconv.FormatInt(s.Startyear, 10) + ") "
		item.Number = idx+1
		item.Amount = samount

		rsp.Payload = item

		conn.WriteJSON(rsp)

		doc, err := goquery.NewDocument(url)
		if err != nil {
			failedIssues += "SERIES;" + url + ";Invalid URL\n"
			continue
		}

		issues := doc.Find(".wikia-gallery-item")

		if len(issues.Nodes) == 0 {
			failedIssues += "SERIES;" + url + ";Not found (no Marvel Series?)\n"
			continue
		}

		s.Issuecount = int64(len(issues.Nodes))

		//remove textless covers
		//textlesscount := 0
		textless := doc.Find(".gallery-image-wrapper")

		for idx := range textless.Nodes {
			for _, attr := range textless.Nodes[idx].Attr {
				if attr.Key == "id" && (strings.Index(attr.Val, "Variant") != -1 || strings.Index(attr.Val, "Cover") != -1 || strings.Index(attr.Val, "Textless") != -1) {
					s.Issuecount--
				}
			}
		}

		msgBox := doc.Find("#messageBox")

		text := msgBox.Text()
		text = text[strings.Index(text, "("):]
		text = text[:strings.Index(text, ".")]

		if strings.Index(text, ")") == -1 {
			failedIssues += "SERIES;" + url + ";No Year\n"
			continue
		}

		yearstr := text[1:strings.Index(text, ")")]

		if strings.Index(yearstr, "-") == -1 {
			startyear, _ := strconv.ParseInt(yearstr, 10, 64)
			if startyear == 0 {
				failedIssues += "SERIES;" + url + ";No Year\n"
				continue
			}
			s.Startyear = startyear
			s.Endyear = 0
		} else {
			s.Startyear, _ = strconv.ParseInt(yearstr[:strings.Index(yearstr, "-")], 10, 64)
			s.Endyear, _ = strconv.ParseInt(yearstr[strings.Index(yearstr, "-")+1:], 10, 64)
		}

		if s.Endyear == 0 {
			text = strings.Replace(text, "(" + strconv.FormatInt(s.Startyear, 10) + ")", "", 1)
		} else {
			text = strings.Replace(text, "(" + strconv.FormatInt(s.Startyear, 10) + "-" + strconv.FormatInt(s.Endyear, 10) + ")", "", 1)
		}

		if text != "" && strings.Index(text, "published by") != -1 {
			text = strings.Replace(text, "(published by ", "", 1)
			text = strings.Replace(text, ")", "", 1)
			s.Publisher.Name = strings.TrimSpace(text)
		}

		s.Update(db)
	}

	//Issues
	var ls []dal.List
	var tl dal.List

	ls, err = tl.MultiSelect(db)

	l := new(dal.List)

	var s dal.Search
	s.Lists = make([]int, 0)
	for _, tl := range ls {
		s.Lists = append(s.Lists, int(tl.Id))
	}

	s.STitle = ""
	s.SPublisher = ""
	s.SSeries = ""
	s.SReleasedate = ""
	s.SNumber = "EQ"
	s.SOriginalissue = "BO"
	s.SPages = "EQ"
	s.SPrice = "EQ"
	s.SAmount = "EQ"
	s.SCoverurl = "BO"
	s.SRead = "BO"
	s.SDuplicateIn = "BO"

	s.Start = 0
	s.Offset = 0
	s.OrgIssue = true
	s.IsExport = true

	s.GOne = "s.title"
	s.GOneDir = "ASC"
	s.GTwo = "s.startyear"
	s.GTwoDir = "ASC"

	l.Search = s
	l.Search.OrgIssueS = &s
	l.Search.OrgIssueS.OrgIssue = false

	lnew, err := l.Select(db)

	if err != nil {
		log.Print(err)
		return ""
	}

	for idx, i := range lnew.Objects {
		issue := i.(dal.Issue)

		issue, err = issue.Select(db)

		if err != nil {
			log.Print(err)
			return ""
		}

		if issue.Series.Volume != 0 {
			number := issue.Number

			if strings.HasSuffix(number, ".0") {
				number = strings.Replace(number, ".0", "", 1)
			}

			url := issue.Series.Title + "_Vol_" + strconv.FormatInt(issue.Series.Volume, 10) + "_" + number
			url = strings.Replace(url, " ", "_", -1)
			url = strings.Replace(url, "?", "%3F", -1)
			url = strings.Replace(url, "&", "%26", -1)
			url =  "http://marvel.wikia.com/wiki/" + url

			//Send current item to shortbox
			var rsp Response
			rsp.Type = "INFO"
			rsp.Message = ""

			item := new(dal.Message)
			item.Type = "Issue"
			item.Message = issue.Series.Title + " (" + roman.Roman(int(issue.Series.Volume)) + ") (" + strconv.FormatInt(issue.Series.Startyear, 10) + ") " + number
			item.Number = idx+1
			item.Amount = lnew.Amount

			rsp.Payload = item

			conn.WriteJSON(rsp)

			doc, err := goquery.NewDocument(url)
			if err != nil {
				failedIssues += "ISSUE;" + url + ";Invalid URL\n"
				continue
			}

			infobox := doc.Find(".infobox")

			issue.Language = "English"
			issue.Format.Format = "Heft"

			image := infobox.Find(".image-thumbnail")

			if image.Nodes != nil && len(image.Nodes) > 0 {
				issue.Coverurl = strings.Split(image.Nodes[0].Attr[0].Val, ".jpg")[0] + ".jpg"
			} else {
				failedIssues += "ISSUE;" + url + ";Issue not found, wrong URL?\n"
				continue
			}

			datecontainer := infobox.Nodes[0].FirstChild.NextSibling.NextSibling.NextSibling.FirstChild.NextSibling.NextSibling

			datestr := ""
			if strings.Contains(datecontainer.Attr[0].Val, "line-height:2em;") {
				datestr = strings.Replace(datecontainer.FirstChild.Attr[1].Val, "Category:", "", 1)
			} else {
				datestr = strings.Replace(datecontainer.FirstChild.NextSibling.NextSibling.NextSibling.NextSibling.NextSibling.FirstChild.Attr[1].Val, "Category:", "", 1)
			}

			day := "01"
			month := "01"
			if len(strings.Split(datestr, ",")) > 1 {
				month = strings.TrimSpace(strings.Split(datestr, ",")[1])
			}
			year := strings.TrimSpace(strings.Split(datestr, ",")[0])

			switch month {
			case "January":
				month = "01"
			case "February":
				month = "02"
			case "March":
				month = "03"
			case "April":
				month = "04"
			case "May":
				month = "05"
			case "June":
				month = "06"
			case "July":
				month = "07"
			case "August":
				month = "08"
			case "September":
				month = "09"
			case "October":
				month = "10"
			case "November":
				month = "11"
			case "December":
				month = "12"
			}

			issue.Releasedate = year + "-" + month + "-" + day

			issue.Update(db)

			collapsibles := doc.Find(".collapsible")
			storycontainer := make([]*html.Node, 0)

			for _, c := range collapsibles.Nodes {
				if strings.HasSuffix(c.Attr[1].Val, "text-align:left;") {
					storycontainer = append(storycontainer, c)
				}
			}

			stories := make([]string, 0)
			for _, s := range storycontainer {
				if s.FirstChild.FirstChild.FirstChild.NextSibling.FirstChild.FirstChild.FirstChild.Data != "a" {
					if s.FirstChild.FirstChild.FirstChild.NextSibling.FirstChild.FirstChild.FirstChild.NextSibling == nil {
						stories = append(stories, "Untitled")
					} else {
						stories = append(stories, s.FirstChild.FirstChild.FirstChild.NextSibling.FirstChild.FirstChild.FirstChild.NextSibling.FirstChild.Data)
					}
				} else {
					if s.FirstChild.FirstChild.FirstChild.NextSibling.FirstChild.FirstChild.FirstChild == nil {
						stories = append(stories, "Untitled")
					} else {
						stories = append(stories, s.FirstChild.FirstChild.FirstChild.NextSibling.FirstChild.FirstChild.FirstChild.FirstChild.Data)
					}
				}
			}

			error := false
			//No number in DB but in Marvel Wikia
			if len(stories) > 1 {
				for _, story := range issue.Stories {
					if story.Number == 0 {
						error = true
					}
				}
			}

			//Number in DB but not Marvel Wikia
			if len(stories) == 1 {
				for _, story := range issue.Stories {
					if story.Number != 0 {
						error = true
					}
				}
			}

			//Number in DB to large for Marvel Wikia
			for _, story := range issue.Stories {
				if story.Number != 0 && int(story.Number) > len(stories) {
					error = true
				}
			}

			if error {
				failedIssues += "ISSUE;" + url + ";Stories\n"
				continue
			}

			//Combine duplicate Stories
			for i := 0; i < len(issue.Stories); i++ {
				for j := 0; j < len(issue.Stories); j++ {
					if i != j && issue.Stories[i].Number == issue.Stories[j].Number && issue.Stories[i].AdditionalInfo == issue.Stories[j].AdditionalInfo {
						_, err = db.Exec("DELETE FROM Issue_Story WHERE fk_issue = ? AND fk_story = ?", issue.Id, issue.Stories[j].Id)

						if err != nil {
							return "merge error"
						}

						_, err = db.Exec("UPDATE Issue_Story SET fk_story = ? WHERE fk_story = ?", issue.Stories[i].Id, issue.Stories[j].Id)

						if err != nil {
							return "merge error"
						}

						_, err = db.Exec("DELETE FROM Story WHERE id = ?", issue.Stories[j].Id)

						if err != nil {
							return "merge error"
						}

						issue.Stories = append(issue.Stories[:j], issue.Stories[j+1:]...)

						break
					}
				}
			}

			for _, s := range issue.Stories {
				number := s.Number

				if number != 0 {
					number--
				}

				s.Title = stories[number]
				_, err = s.Update(db)

				if err != nil {
					s.Title = s.OriginalIssue.Title + " (" + roman.Roman(int(s.OriginalIssue.Series.Volume)) + ") (" + strconv.FormatInt(s.OriginalIssue.Series.Startyear, 10) + ") #"

					number := s.OriginalIssue.Number

					if strings.HasSuffix(number, ".0") {
						number = strings.Replace(number, ".0", "", 1)
					}

					s.Title += number

					_, err = s.Update(db)
				}
			}

			for idx, s := range stories {
				var found = false

				for _, s := range issue.Stories {
					if int(s.Number) == idx+1 || s.Number == 0 {
						found = true
					}
				}

				if !found {
					var story = new(dal.Story)

					story.Title = s
					story.Number = int64(idx+1)
					story.OriginalIssue = issue

					issue.Stories = append(issue.Stories, *story)
				}
			}

			issue.Update(db)

			if error {
				failedIssues += "ISSUE;" + url + ";Stories\n"
				continue
			}
		}
	}

	return failedIssues
}

func Crawl(url string) (dal.Issue) {
	if strings.Contains(url, "comichunters.net") && strings.Contains(url, "t=4") {
		url = strings.Replace(url, "t=4", "t=1", 1)
	}

	doc, err := goquery.NewDocument(url)
	if err != nil {
		log.Fatal(err)
	}

	var i dal.Issue

	if strings.Contains(url, "comichunters.net") {
		id, _ := strconv.ParseInt(strings.Replace(url, "http://comichunters.net/?t=1&comic=", "", 1), 10, 64)

		i = ExtractComicHuntersMetadata(doc, id)
	} else if strings.Contains(url, "paninishop.de") {
		i = ExtractPaniniMetadata(doc)
	}

	return i
}

func ExtractComicHuntersMetadata(doc *goquery.Document, id int64) (dal.Issue) {
	var i dal.Issue

	i.Title = doc.Find("h1").Nodes[0].FirstChild.Data

	metadata := doc.Find(".comicfont")

	var sortNumber string
	if len(metadata.Nodes) == 0 || metadata.Nodes[0].FirstChild == nil {
		sortNumber = "0"
	} else {
		sortNumber = metadata.Nodes[0].FirstChild.Data
	}

	if sortNumber != "0" && strings.HasPrefix(sortNumber, "Classic") {
		sortNumber = strings.Replace(sortNumber, "Classic ", "", 1)
		i.Number = strconv.FormatInt(int64(roman.Arabic(sortNumber)), 10)
	} else {
		i.Number = sortNumber
	}

	var s dal.Series
	s.Title = doc.Find(".Menu3").Nodes[0].Attr[2].Val
	s.Title = strings.Replace(s.Title, "&amp", "&", 1)
	s.Title = strings.Replace(s.Title, "& ", " &", 1)
	s.Startyear = 0

	var p dal.Publisher
	p.Name = doc.Find(".Menu2").Nodes[0].Attr[2].Val
	p.Name = strings.Replace(p.Name, "Verlag ", "", 1)
	p.Name = strings.Replace(p.Name, "&amp", "&", 1)
	p.Name = strings.Replace(p.Name, "& ", " &", 1)

	s.Publisher = p
	i.Series = s

	if metadata.Nodes[10].FirstChild == nil {
		i.Format.Format = ""
	} else {
		i.Format.Format = metadata.Nodes[10].FirstChild.Data
	}

	if metadata.Nodes[9].FirstChild == nil {
		i.Language = ""
	} else {
		i.Language = metadata.Nodes[9].FirstChild.Data
	}

	if metadata.Nodes[3].FirstChild == nil {
		i.Pages = 0
	} else {
		pages, _ := strconv.ParseInt(metadata.Nodes[3].FirstChild.Data, 10, 64)
		i.Pages = int(pages)
	}

	if metadata.Nodes[6].FirstChild == nil {
		i.Releasedate = ""
	} else {
		releasedate := metadata.Nodes[6].FirstChild.Data
		releasedate = strings.Replace(releasedate, " ", "", 3)
		releasedate = strings.Replace(releasedate, "Jänner", "01.", 3)
		releasedate = strings.Replace(releasedate, "Februar", "02.", 3)
		releasedate = strings.Replace(releasedate, "März", "03.", 3)
		releasedate = strings.Replace(releasedate, "April", "04.", 3)
		releasedate = strings.Replace(releasedate, "Mai", "05.", 3)
		releasedate = strings.Replace(releasedate, "Juni", "06.", 3)
		releasedate = strings.Replace(releasedate, "Juli", "07.", 3)
		releasedate = strings.Replace(releasedate, "August", "08.", 3)
		releasedate = strings.Replace(releasedate, "September", "09.", 3)
		releasedate = strings.Replace(releasedate, "Oktober", "10.", 3)
		releasedate = strings.Replace(releasedate, "November", "11.", 3)
		releasedate = strings.Replace(releasedate, "Dezember", "12.", 3)

		if strings.Count(releasedate, ".") == 1 {
			releasedate = "01." + releasedate
		} else if strings.Count(releasedate, ".") == 0 {
			releasedate = "01.01." + releasedate
		}

		sd := strings.Split(releasedate, ".")

		i.Releasedate = sd[2] + "-" + sd[1] + "-" + sd[0]
	}

	if metadata.Nodes[7].FirstChild == nil {
		i.Price.Price = 0
		i.Price.Currency = "EUR"
	} else {
		i.Price.Price, _ = strconv.ParseFloat(strings.Split(metadata.Nodes[7].FirstChild.Data, " ")[0], 64)
		i.Price.Currency = strings.Split(metadata.Nodes[7].FirstChild.Data, " ")[1]
	}

	originalTitle := ""
	if metadata.Nodes[1].FirstChild != nil {
		originalTitle = metadata.Nodes[1].FirstChild.Data
	}

	if originalTitle != "" {
		originalTitle = fixOriginalTitle(originalTitle, i.Title)
		trimedTitle := trimTitle(originalTitle)

		splittedTrimedTitle := splitTrimedTitle(trimedTitle)

		var lastIn dal.Story
		lastIn.OriginalIssue.Title = ""

		for _, s := range splittedTrimedTitle {
			issues := make([]dal.Story, 0)
			if len(splittedTrimedTitle) > 1 {
				issues = getIssues(strings.TrimSpace(s), nil, lastIn)
			} else {
				issues = getIssues(strings.TrimSpace(s), &i, lastIn)
			}

			if len(issues) > 0 {
				lastIn = issues[0]
			}

			for _, is := range issues {
				i.Stories = append(i.Stories, is)
			}
		}
	} else {
		i.Stories = make([]dal.Story, 0)
	}

	images := doc.Find("img").Nodes
	for _, image := range images {
		if strings.Contains(image.Attr[0].Val, "./images") && strings.Contains(image.Attr[0].Val, "thumb") {
			i.Coverurl = "http://comichunters.net" + strings.Replace(image.Attr[0].Val[1:len(image.Attr[0].Val)], "thumb", "norm", 1)
		}
	}

	return i
}

func ExtractPaniniMetadata(doc *goquery.Document) (dal.Issue) {
	var i dal.Issue

	i.Title = doc.Find("h1").Nodes[0].FirstChild.Data
	i.Number = "0"

	tree := doc.Find(".category-tree-item")

	var s dal.Series
	sTitle := tree.Nodes[len(tree.Nodes)-1].FirstChild.Data
	sTitle = strings.Replace(sTitle, "\n", "", 2)
	sTitle = strings.TrimSpace(sTitle)
	s.Title = sTitle
	s.Startyear = 0

	var p dal.Publisher
	pName := tree.Nodes[2].FirstChild.Data
	pName = strings.Replace(pName, "\n", "", 2)
	pName = strings.TrimSpace(pName)
	p.Name = "Panini " + pName

	s.Publisher = p
	i.Series = s

	content := doc.Find(".content")

	i.Format.Format = content.Nodes[1].FirstChild.Data
	i.Language = "Deutsch"
	pages, _ := strconv.ParseInt(content.Nodes[2].FirstChild.Data, 10, 64)
	i.Pages = int(pages)

	dateParts := strings.Split(content.Nodes[0].FirstChild.Data, ".")
	i.Releasedate = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0]

	price := doc.Find(".price").Eq(0).Children().Nodes[1].Attr[1].Val
	price = strings.Replace(price, "\n", "", 2)
	price = strings.TrimSpace(price)
	price = strings.Replace(price, ",", ".", 1)
	i.Price.Price, _ = strconv.ParseFloat(price, 64)
	i.Price.Currency = "EUR"

	originalNodes := content.Eq(5).Children().Nodes
	originalTitle := ""
	for _, n := range originalNodes {
		originalTitle += n.FirstChild.Data + ","
	}
	originalTitle = originalTitle[0:len(originalTitle)-1]
	trimedTitle := trimTitle(originalTitle)
	splittedTrimedTitle := splitTrimedTitle(trimedTitle)

	var lastIn dal.Story
	lastIn.OriginalIssue.Title = ""

	for _, s := range splittedTrimedTitle {
		issues := make([]dal.Story, 0)
		if len(splittedTrimedTitle) > 1 {
			issues = getIssues(strings.TrimSpace(s), nil, lastIn)
		} else {
			issues = getIssues(strings.TrimSpace(s), &i, lastIn)
		}

		if len(issues) > 0 {
			lastIn = issues[0]
		}

		for _, is := range issues {
			i.Stories = append(i.Stories, is)
		}
	}

	i.Coverurl = "https://www.paninishop.de" + doc.Find("#glasscase").Children().Nodes[0].FirstChild.Attr[0].Val

	return i
}

func reverse(s string) string {
	runes := []rune(s)
	for i, j := 0, len(runes)-1; i < j; i, j = i+1, j-1 {
		runes[i], runes[j] = runes[j], runes[i]
	}
	return string(runes)
}

func isNumber(s string) bool {
	_, err := strconv.ParseFloat(s, 64)
	return err == nil
}

func getIssues(s string, i *dal.Issue, lastIn dal.Story) ([]dal.Story) {
	title := "asdf"

	if i != nil {
		title = i.Title
	}

	issues := make([]dal.Story, 0)

	reversedString := reverse(s)

	//asdf 1
	if isNumber(string(reversedString[0])) {
		for _, issue := range getNumericIssues(reversedString, lastIn) {
			issues = append(issues, issue)
		}
	} /*asdf OGN*/ else if strings.HasPrefix(reversedString, "NGO") {
		for _, issue := range getOGNIssues(reversedString, lastIn) {
			issues = append(issues, issue)
		}
	} else if strings.HasPrefix(reversedString, ")") {
		if strings.HasPrefix(reversedString, ")") {
			reversedString = reversedString[1:]
		}

		//asdf 1 (I)
		if !isNumber(string(reversedString[0])) && roman.IsRoman(string(reversedString[0])) {
			var language = "Deutsch"
			if i != nil {
				language = i.Language
			}
			for _, issue := range getSubNumberIssues(reversedString, title, language, lastIn) {
				issues = append(issues, issue)
			}
		} /*asdf (2016)*/ else {
			var language = "Deutsch"
			if i == nil {
				language = i.Language
			}
			for _, issue := range getIssueWithoutNumber(reversedString, title, language, lastIn) {
				issue.OriginalIssue.Number = i.Number
				issues = append(issues, issue)
			}
		}
	} /*II)*/ else if roman.IsRoman(string(reversedString[0])) {
		var language = "Deutsch"
		if i != nil {
			language = i.Language
		}
		for _, issue := range getSubNumberIssues(reversedString, title, language, lastIn) {
			issues = append(issues, issue)
		}
	} else {
		//asdf 1 AU
		if numberFound(s) {
			story := reverse(reversedString[0:strings.Index(reversedString, " ")])
			for _, issue := range getIssueWithNameBack(reversedString, lastIn) {
				issue.Number, _ = strconv.ParseInt(story, 10, 64)
				issues = append(issues, issue)
			}
		} else {
			//asdf
			issue := createIssue(strings.TrimSpace(s), lastIn, "")
			issue.OriginalIssue.Number = i.Number
			issues = append(issues, issue)
		}
	}

	for _, issue := range issues {
		if issue.OriginalIssue.Releasedate == "" {
			issue.OriginalIssue.Releasedate = lastIn.OriginalIssue.Releasedate
		}
	}

	return issues
}

func numberFound(s string) (bool) {
	bracketFound := false

	for i := 0; i < len(s); i++ {
		if string(s[i:i]) == "(" {
			bracketFound = true
		}

		if string(s[i:i]) == ")" {
			bracketFound = false
		}

		if isNumber(string(s[i])) && !bracketFound {
			return true
		}
	}

	return false
}

func createIssue(name string, lastIn dal.Story, number string) (dal.Story) {
	changedName := false
	var issue dal.Story

	issue.OriginalIssue.Title = name
	issue.OriginalIssue.Series.Title = name

	if strings.TrimSpace(issue.OriginalIssue.Title) == "" {
		issue.OriginalIssue.Title = lastIn.OriginalIssue.Title
		issue.OriginalIssue.Series.Title = lastIn.OriginalIssue.Series.Title
		changedName = true
	}

	if strings.Contains(issue.OriginalIssue.Title, "(") && strings.Contains(issue.OriginalIssue.Title, ")") {
		year := issue.OriginalIssue.Title[strings.Index(issue.OriginalIssue.Title, "(")+1: strings.Index(issue.OriginalIssue.Title, ")")]
		if isNumber(year) {
			newName := strings.Replace(issue.OriginalIssue.Title, "("+year+")", "", 1)

			if newName == issue.OriginalIssue.Title {
				newName = strings.Replace(issue.OriginalIssue.Title, "("+year+")", "", 1)
			}

			issue.OriginalIssue.Title = strings.TrimSpace(newName)
			issue.OriginalIssue.Series.Title = strings.TrimSpace(newName)
			issue.OriginalIssue.Series.Startyear, _ = strconv.ParseInt(year, 10,64)
		} else {
			issue.OriginalIssue.Series.Startyear = 0
		}

		if issue.OriginalIssue.Series.Startyear == 0 && changedName {
			issue.OriginalIssue.Series.Startyear = lastIn.OriginalIssue.Series.Startyear
		}
	} else {
		issue.OriginalIssue.Series.Startyear = lastIn.OriginalIssue.Series.Startyear
	}


	if strings.HasSuffix(number, ".0") {
		number = strings.Replace(number, ".0", "", 1)
	}

	issue.OriginalIssue.Number = number

	return issue
}

func getIssueWithNameBack(reversedString string, lastIn dal.Story) ([]dal.Story) {
	nameBack := reversedString[0:strings.Index(reversedString, " ")]

	reversedString = strings.Replace(reversedString, nameBack, "", -1)

	issues := getNumericIssues(strings.TrimSpace(reversedString), lastIn)

	for _, i := range issues {
		nameBack = reverse(nameBack)

		i.Title = i.Title + " " + nameBack
	}

	return issues
}

func getIssueWithoutNumber(reversedString string, title string, lang string, lastIn dal.Story) ([]dal.Story) {
	issues := make([]dal.Story, 0)

	if strings.Contains(reversedString, "(") {
		reversedString = ")" + reversedString
	}

	number := ""

	for i := 0; i < len(title); i++ {
		if isNumber(string(title[i])) {
			number += string(title[i:i])
		} else {
			fmt.Println("not possible: " + title)
			break
		}
	}

	if isNumber(number) {
		numberAsInt := 1
		if lang == "Englisch" {
			numberAsInt, _ = strconv.Atoi(number)
		}

		reversedString = reverse(reversedString)

		issue := createIssue(strings.TrimSpace(reversedString), lastIn, strconv.FormatInt(int64(numberAsInt), 10))
		issues = append(issues, issue)
	} else {
		reversedString = reverse(reversedString)

		issue := createIssue(strings.TrimSpace(reversedString), lastIn, "")

		issues = append(issues, issue)
	}

	return issues
}

func getSubNumberIssues(reversedString string, title string, lang string, lastIn dal.Story) ([]dal.Story) {
	issues := make([]dal.Story, 0)

	deleteString := ""
	lastIssue := ""
	firstIssue := ""

	i := 0
	for len(reversedString) != i && string(reversedString[i]) != "-" && string(reversedString[i]) != "(" && string(reversedString[i]) != " " {
		deleteString += string(reversedString[i])
		lastIssue += string(reversedString[i])
		i++
	}

	if len(reversedString) != i {
		deleteString += string(reversedString[i])
		i++

		if !isNumber(string(reversedString[i])) {
			for string(reversedString[i]) != " " && string(reversedString[i]) != "(" {
				deleteString += string(reversedString[i])
				firstIssue += string(reversedString[i])
				i++

				if len(reversedString) == i {
					break
				}
			}

			firstIssue = reverse(firstIssue)

			if len(reversedString) != i {
				deleteString += string(reversedString[i])
			}
		}
	}

	reversedString = strings.Replace(reversedString, deleteString, "", 1)
	reversedString = reverse(reversedString)

	var lastIssueAsInt int
	if !isNumber(firstIssue) && firstIssue != "" {
		lastIssueAsInt = roman.Arabic(reverse(lastIssue))
		firstIssueAsInt := roman.Arabic(reverse(firstIssue))

		//I-III
		reversedString = reverse(reversedString)
		var issue dal.Story

		if len(strings.TrimSpace(reversedString)) != 0 {
			temp := getNumericIssues(strings.TrimSpace(reversedString), lastIn)
			issue = temp[0]
		} else {
			issue = createIssue(lastIn.Title, lastIn, lastIn.OriginalIssue.Number)
		}

		for j := firstIssueAsInt; j <= lastIssueAsInt; j++ {
			fmt.Println(issue.Title)
			issue.Number = int64(j)
			issues = append(issues, issue)
		}

	} else {
		//I
		reversedString = reverse(reversedString)
		newIssues := make([]dal.Story, 0)

		if len(strings.TrimSpace(reversedString)) != 0 {
			temp := make([]dal.Story, 0)

			if !isNumber(string(reversedString[0])) {
				temp = getIssueWithoutNumber(strings.TrimSpace(reversedString), title, lang, lastIn)
			} else {
				temp = getNumericIssues(strings.TrimSpace(reversedString), lastIn)
			}

			for _, i := range temp {
				i.Number = int64(roman.Arabic(reverse(lastIssue)))
				newIssues = append(newIssues, i)
			}
		} else {
			issue := createIssue(lastIn.Title, lastIn, lastIn.OriginalIssue.Number)

			issue.Number = int64(roman.Arabic(reverse(lastIssue)))

			newIssues = append(newIssues, issue)
		}

		for _, i := range newIssues {
			issues = append(issues, i)
		}
	}
	return issues
}

func getOGNIssues(reversedString string, lastIn dal.Story) ([]dal.Story) {
	issues := make([]dal.Story, 0)

	reversedString = strings.Replace(reversedString, "NGO", "", 1)
	reversedString = reverse(reversedString)

	issue := createIssue(strings.TrimSpace(reversedString), lastIn, "")
	issue.OriginalIssue.Number = "1"
	issue.Number = 0
	issues = append(issues, issue)

	return issues
}

var IsLetter = regexp.MustCompile(`^[a-zA-Z]+$`).MatchString

func getNumericIssues(reversedString string, lastIn dal.Story) ([]dal.Story) {
	issues := make([]dal.Story, 0)

	deleteStringFront := ""
	deleteStringBack := ""
	lastIssue := ""
	firstIssue := ""

	i := 0
	for len(reversedString) != i && (isNumber(string(reversedString[i])) || string(reversedString[i]) == ".") {
		deleteStringFront += string(reversedString[i])
		lastIssue += string(reversedString[i])
		i++
	}

	lastIssue = reverse(lastIssue)

	if len(reversedString) != i {
		deleteStringFront += string(reversedString[i])
		i++

		//look for -1
		if len(reversedString) == i || (string(reversedString[i]) == " " && !isNumber(string(reversedString[i+1]))) {
			lastIssue = "-1"
		} else {
			if string(reversedString[i]) == " " {
				reversedString = string(reversedString[0:i]) + "" + string(reversedString[i:1])
			}

			for string(reversedString[i]) != " " {
				deleteStringBack += string(reversedString[i])
				firstIssue += string(reversedString[i])
				i++

				if len(reversedString) == i {
					break
				}
			}
		}

		firstIssue = reverse(firstIssue)

		if len(reversedString) != i {
			deleteStringBack += string(reversedString[i])
		}
	}

	if IsLetter(deleteStringFront[len(deleteStringFront)-1:]) {
		deleteStringFront = strings.TrimSpace(string(deleteStringFront[0:len(deleteStringFront)-1]))
	}

	reversedString = strings.Replace(reversedString, deleteStringFront, "", 1)
	reversedString = strings.Replace(reversedString, deleteStringBack, "", 1)

	lastIssueAsInt, _ := strconv.ParseFloat(lastIssue, 64)

	if isNumber(firstIssue) && firstIssue != "" {
		firstIssueAsInt, _ := strconv.ParseFloat(firstIssue, 64)

		reversedString = reverse(reversedString)

		//Deadpool fix...
		if lastIssueAsInt < firstIssueAsInt {
			temp := lastIssueAsInt
			lastIssueAsInt = firstIssueAsInt
			firstIssueAsInt = temp
		}

		if math.Mod(lastIssueAsInt, 1) == 0 && math.Mod(firstIssueAsInt, 1) == 0 {
			//1-6
			for j := firstIssueAsInt; j <= lastIssueAsInt; j++ {
				issue := createIssue(strings.TrimSpace(reversedString), lastIn, strconv.FormatFloat(j, 'f', 0, 64))
				issues = append(issues, issue)
			}
		} else if int64(lastIssueAsInt) != int64(firstIssueAsInt) {
			//1.1 - 6.1
			for j := firstIssueAsInt; j <= lastIssueAsInt; j++ {
				issue := createIssue(strings.TrimSpace(reversedString), lastIn, strconv.FormatFloat(j, 'f', 1, 64))
				issues = append(issues, issue)
			}
		} else if math.Mod(lastIssueAsInt, 1) != math.Mod(firstIssueAsInt, 1) && math.Mod(lastIssueAsInt, 1) != 0 && math.Mod(firstIssueAsInt, 1) != 0 {
			//1.1 - 1.6
			for j := firstIssueAsInt; j <= lastIssueAsInt; j += 0.1 {
				issue := createIssue(strings.TrimSpace(reversedString), lastIn, strconv.FormatFloat(j, 'f', 1, 64))
				issues = append(issues, issue)

				js := strconv.FormatFloat(j, 'f', 1, 64)
				j, _ = strconv.ParseFloat(js, 64)
			}
		}
	} else {
		//1
		reversedString := reverse(reversedString)

		if strings.Contains(reversedString, "(") {
			reversedString = strings.Replace(reversedString, firstIssue, "", -1)
		}

		issue := createIssue(strings.TrimSpace(reversedString), lastIn, strconv.FormatFloat(lastIssueAsInt, 'f', 1, 64))

		if issue.OriginalIssue.Series.Startyear == 0 {
			issue.OriginalIssue.Series.Startyear = lastIn.OriginalIssue.Series.Startyear
		}

		if strings.Contains(firstIssue, "(") && strings.Contains(firstIssue, ")") {
			issue.OriginalIssue.Series.Startyear, _ = strconv.ParseInt(firstIssue[strings.Index(firstIssue, "(")+1: strings.Index(firstIssue, ")")], 10,64)
		}

		issues = append(issues, issue)
	}

	return issues
}

var usStarter = [...]string{"US:", "US-", "JP:"}

func trimTitle(title string) (string) {
	trimedTitle := ""
	title = strings.TrimSpace(title)

	for i := 0; i < len(title); i++ {
		trimedTitle += string(title[i])
	}

	for _, uss := range usStarter {
		if strings.Contains(trimedTitle, uss) {
			trimedTitle = strings.Replace(trimedTitle, uss, "", -1)
		}
	}

	trimedTitle = strings.Replace(trimedTitle, "#", "", -1)
	trimedTitle = strings.Replace(trimedTitle, "-", "-", -1)
	trimedTitle = strings.Replace(trimedTitle, "�", "-", -1)
	trimedTitle = strings.Replace(trimedTitle, "�", "-", -1)
	trimedTitle = strings.Replace(trimedTitle, " - ", "-", -1)

	return trimedTitle
}

var splitter = [...]string{"&", ",", ";", "\n"}

func splitTrimedTitle(trimedTitle string) ([]string) {
	splittedTrimedTitle := make([]string, 0)

	splittedTrimedTitle = append(splittedTrimedTitle, trimedTitle)
	for _, split := range splitter {
		temp := make([]string, 0)

		for _, s := range splittedTrimedTitle {
			splitted := strings.Split(s, split)

			for _, t := range splitted {
				temp = append(temp, t)
			}
		}

		splittedTrimedTitle = temp
	}

	return splittedTrimedTitle
}

func fixOriginalTitle(ot string, title string) (string) {
	originalTitle := ""

	if title == "5. Civil War: Konsequenzen" {
		originalTitle = "New Avengers: Illuminati (2007) 1, Fantastic Four (1961) 536-537, Civil War: The Return (2007) 1, Iron Man (2005) 15, Captain America (2004) 25, Civil War: The Confession (2007) 1, Civil War: The Initiative (2007) 1, Fallen Son: The Death of Captain America (2007) 1-5 & What if? Civil War (2008) 1"
	} else if title == "1. Harley Quinn Anthologie" {
		originalTitle = "Batman: Harley Quinn (1999), Harley and Ivy: Love on the Lam (2001), Batman: Gotham Knights (2000) 14, 30, Harley Quinn (2000) 18, 19, Detective Comics (1937) 831, 837, Joker's Asylum: Harley Quinn (2010) 1, Batman: Black and White (2013) 1, 3, Detective Comics (2011) 23.2, Harley Quinn (2014) 0, Harley Quinn invades Comic-Con International: San Diego (2014)"
	} else if strings.TrimSpace(ot) == "" {
		originalTitle = title
	} else {
		originalTitle = ot
	}

	if strings.Contains(originalTitle, "&") {
		for i := 0; i < len(originalTitle); i++ {
			if originalTitle[i:i] == "&" {
				firstRight := false
				if unicode.IsLetter(rune(originalTitle[i])) && originalTitle[i:i+1] != " " {
					firstRight = true
				}
				secondRight := false
				if unicode.IsLetter(rune(originalTitle[i])) && originalTitle[i:i+2] != " " {
					secondRight = true
				}
				firstLeft := false
				if unicode.IsLetter(rune(originalTitle[i])) && originalTitle[i:i-1] != " " {
					firstLeft = true
				}
				secondLeft := false
				if unicode.IsLetter(rune(originalTitle[i])) && originalTitle[i:i-2] != " " {
					secondLeft = true
				}

				if (firstRight && firstLeft) || (secondRight && secondLeft) || (firstRight && secondLeft) || (secondRight && firstLeft) {
					originalTitle = originalTitle[0:  i] + "and" + originalTitle[i+1: ]
				}
			}
		}
	}

	return originalTitle
}
