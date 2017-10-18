package dal

func (u User) Convert(i interface {}) (User) {
	m := i.(map[string]interface {})

	if m["Name"] != nil {
		u.Name = m["Name"].(string)
	}

	if m["Sessionid"] != nil {
		u.SessionId = m["Sessionid"].(string)
	}

	if m["Password"] != nil {
		u.Password = m["Password"].(string)
	}

	return u
}

func (s Series) Convert(i interface {}) (Series) {
	m := i.(map[string]interface {})

	if m["Id"] != nil {
		s.Id = int64(m["Id"].(float64))
	}

	if m["Title"] != nil {
		s.Title = m["Title"].(string)
	}

	if m["Volume"] != nil {
		s.Volume = int64(m["Volume"].(float64))
	}

	if m["Startyear"] != nil {
		s.Startyear = int64(m["Startyear"].(float64))
	}

	if m["Endyear"] != nil {
		s.Endyear = int64(m["Endyear"].(float64))
	}

	if m["Issuecount"] != nil {
		s.Issuecount = int64(m["Issuecount"].(float64))
	}

	if m["Publisher"] != nil {
		s.Publisher = s.Publisher.Convert(m["Publisher"])
	}

	return s
}

func (p Publisher) Convert(i interface {}) (Publisher) {
	m := i.(map[string]interface {})

	if m["Id"] != nil {
		p.Id = int64(m["Id"].(float64))
	}

	if m["Name"] != nil {
		p.Name = m["Name"].(string)
	}

	return p
}

func (l List) Convert(i interface {}) (List) {
	m := i.(map[string]interface {})

	if m["Id"] != nil {
		l.Id = int64(m["Id"].(float64))
	}

	if m["Type"] != nil {
		l.Type = m["Type"].(string)
	}

	if m["Name"] != nil {
		l.Name = m["Name"].(string)
	}

	if m["Sort"] != nil {
		l.Sort = int(m["Sort"].(float64))
	}

	if m["GroupBy"] != nil {
		l.GroupBy = m["GroupBy"].(string)
	}

	if m["Amount"] != nil {
		l.Amount = int(m["Amount"].(float64))
	}

	//Ignore Objects

	if m["Search"] != nil {
		var s Search
		l.Search = s.Convert(m["Search"])
	}

	return l
}

func (s Search) Convert(i interface {}) (Search) {
	m := i.(map[string]interface {})

	if m["Lists"] != nil {
		for _, l := range m["Lists"].([]interface {}) {
			s.Lists = append(s.Lists, int(l.(float64)))
		}
	}

	if m["Start"] != nil {
		s.Start = int(m["Start"].(float64))
	}

	if m["Offset"] != nil {
		s.Offset = int(m["Offset"].(float64))
	}

	var is Issue
	if m["Issue"] != nil {
		s.Issue = is.Convert(m["Issue"])
	}

	if m["Issue2"] != nil {
		s.Issue2 = is.Convert(m["Issue2"])
	}

	if m["STitle"] != nil {
		s.STitle = m["STitle"].(string)
	}

	if m["SPublisher"] != nil {
		s.SPublisher = m["SPublisher"].(string)
	}

	if m["SSeries"] != nil {
		s.SSeries = m["SSeries"].(string)
	}

	if m["SReleasedate"] != nil {
		s.SReleasedate = m["SReleasedate"].(string)
	}

	if m["SNumber"] != nil {
		s.SNumber = m["SNumber"].(string)
	}

	if m["SOriginalissue"] != nil {
		s.SOriginalissue = m["SOriginalissue"].(string)
	}

	if m["SPages"] != nil {
		s.SPages = m["SPages"].(string)
	}

	if m["SPrice"] != nil {
		s.SPrice = m["SPrice"].(string)
	}

	if m["SAmount"] != nil {
		s.SAmount = m["SAmount"].(string)
	}

	if m["SCoverurl"] != nil {
		s.SCoverurl = m["SCoverurl"].(string)
	}

	if m["SRead"] != nil {
		s.SRead = m["SRead"].(string)
	}

	if m["SDuplicateIn"] != nil {
		s.SDuplicateIn = m["SDuplicateIn"].(string)
	}

	if m["GOne"] != nil {
		s.GOne = m["GOne"].(string)
	}

	if m["GOneDir"] != nil {
		s.GOneDir = m["GOneDir"].(string)
	}

	if m["GTwo"] != nil {
		s.GTwo = m["GTwo"].(string)
	}

	if m["GTwoDir"] != nil {
		s.GTwoDir = m["GTwoDir"].(string)
	}

	if m["IsExport"] != nil {
		s.IsExport = m["IsExport"].(bool)
	}

	if m["OrgIssue"] != nil {
		s.OrgIssue = m["OrgIssue"].(bool)
	}

	if m["OrgIssueS"] != nil {
		var st Search
		st = st.Convert(m["OrgIssueS"])
		s.OrgIssueS = &st
	}

	return s
}

func (s Story) Convert(i interface {}) (Story) {
	m := i.(map[string]interface {})

	if m["Id"] != nil {
		s.Id = int64(m["Id"].(float64))
	}

	if m["Title"] != nil {
		s.Title = m["Title"].(string)
	}

	if m["Number"] != nil {
		s.Number = int64(m["Number"].(float64))
	}

	if m["AdditionalInfo"] != nil {
		s.AdditionalInfo = m["AdditionalInfo"].(string)
	}

	var is Issue
	if m["OriginalIssue"] != nil {
		var is Issue
		s.OriginalIssue = is.Convert(m["OriginalIssue"])
	}

	if m["Issues"] != nil {
		for _, iss := range m["Issues"].([]interface {}) {
			is = is.Convert(iss)
			s.Issues = append(s.Issues, is)
		}
	}

	return s
}

func (is Issue) Convert(i interface {}) (Issue) {
	m := i.(map[string]interface {})

	if m["Id"] != nil {
		is.Id = int64(m["Id"].(float64))
	}

	if m["Title"] != nil {
		is.Title = m["Title"].(string)
	}

	if m["Series"] != nil {
		var s Series
		is.Series = s.Convert(m["Series"])
	}

	if m["Number"] != nil {
		is.Number = m["Number"].(string)
	}

	if m["Lists"] != nil {
		for _, lt := range m["Lists"].([]interface {}) {
			var l List
			l = l.Convert(lt)
			is.Lists = append(is.Lists, l)
		}
	}

	if m["Price"] != nil {
		price := m["Price"].(map[string]interface {})

		if price["Price"] != nil {
			is.Price.Price = price["Price"].(float64)
		}

		if price["Currency"] != nil {
			is.Price.Currency = price["Currency"].(string)
		}
	}

	if m["Stories"] != nil {
		for _, ls := range m["Stories"].([]interface {}) {
			var s Story
			s = s.Convert(ls)
			is.Stories = append(is.Stories, s)
		}
	}

	if m["Amount"] != nil {
		is.Amount = int(m["Amount"].(float64))
	}

	if m["Language"] != nil {
		is.Language = m["Language"].(string)
	}

	if m["Releasedate"] != nil {
		is.Releasedate = m["Releasedate"].(string)
	}

	if m["Pages"] != nil {
		is.Pages = int(m["Pages"].(float64))
	}

	if m["Coverurl"] != nil {
		is.Coverurl = m["Coverurl"].(string)
	}

	if m["Format"] != nil {
		is.Format = m["Format"].(string)
	}

	if m["Quality"] != nil {
		is.Quality = m["Quality"].(string)
	}

	if m["QualityAdditional"] != nil {
		is.QualityAdditional = m["QualityAdditional"].(string)
	}

	if m["Read"] != nil {
		is.Read = int(m["Read"].(float64))
	}

	if m["Originalissue"] != nil {
		is.Originalissue = int(m["Originalissue"].(float64))
	}

	return is
}

