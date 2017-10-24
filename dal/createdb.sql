# Export von Tabelle List
# ------------------------------------------------------------

DROP TABLE IF EXISTS `List`;

CREATE TABLE `List` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `sort` int(11) unsigned NOT NULL DEFAULT '1',
  `groupBy` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Export von Tabelle Publisher
# ------------------------------------------------------------

DROP TABLE IF EXISTS `Publisher`;

CREATE TABLE `Publisher` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `publisher`
	ADD FULLTEXT INDEX `fulltext` (`name`);

# Export von Tabelle Series
# ------------------------------------------------------------

DROP TABLE IF EXISTS `Series`;

CREATE TABLE `Series` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL DEFAULT '',
  `startyear` int(6) NOT NULL,
  `endyear` int(6) NOT NULL,
  `volume` int(6) NOT NULL,
  `issuecount` int(6) NOT NULL,
  `fk_publisher` int(11) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `title` (`title`,`volume`,`fk_publisher`),
  KEY `fk_publisher` (`fk_publisher`),
  CONSTRAINT `series_ibfk_1` FOREIGN KEY (`fk_publisher`) REFERENCES `Publisher` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `Series`
	ADD FULLTEXT INDEX `fulltext` (`title`);

# Export von Tabelle Issue
# ------------------------------------------------------------

DROP TABLE IF EXISTS `Issue`;

CREATE TABLE `Issue` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT '',
  `fk_series` int(11) unsigned,
  `number` varchar(255) NOT NULL,
  `format` varchar(255) DEFAULT NULL,
  `language` varchar(255) DEFAULT NULL,
  `pages` int(11) DEFAULT NULL,
  `releasedate` date DEFAULT NULL,
  `price` float DEFAULT NULL,
  `currency` varchar(255) DEFAULT NULL,
  `coverurl` varchar(255) DEFAULT NULL,
  `quality` varchar(4) DEFAULT NULL,
  `qualityAdditional` varchar(255) DEFAULT NULL,
  `amount` int(11) DEFAULT NULL,
  `isread` int(1) DEFAULT 0,
  `originalissue` int(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `fk_series` (`fk_series`),
  UNIQUE KEY `number` (`number`,`fk_series`,`format`),
  CONSTRAINT `issue_ibfk_1` FOREIGN KEY (`fk_series`) REFERENCES `Series` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Export von Tabelle Story
# ------------------------------------------------------------

DROP TABLE IF EXISTS `Story`;

CREATE TABLE `Story` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL DEFAULT '',
  `number` int(11) DEFAULT NULL,
  `additionalInfo` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `title` (`title`,`number`,`additionalInfo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Export von Tabelle Issue_List
# ------------------------------------------------------------

DROP TABLE IF EXISTS `Issue_List`;

CREATE TABLE `Issue_List` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `fk_issue` int(11) unsigned NOT NULL,
  `fk_list` int(11) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fk_issue` (`fk_issue`,`fk_list`),
  KEY `fk_list` (`fk_list`),
  CONSTRAINT `issue_list_ibfk_1` FOREIGN KEY (`fk_issue`) REFERENCES `Issue` (`id`),
  CONSTRAINT `issue_list_ibfk_2` FOREIGN KEY (`fk_list`) REFERENCES `List` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Export von Tabelle Issue_Story
# ------------------------------------------------------------

DROP TABLE IF EXISTS `Issue_Story`;

CREATE TABLE `Issue_Story` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `fk_issue` int(11) unsigned NOT NULL,
  `fk_story` int(11) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fk_issue` (`fk_issue`,`fk_story`),
  KEY `fk_story` (`fk_story`),
  CONSTRAINT `issue_story_ibfk_1` FOREIGN KEY (`fk_issue`) REFERENCES `Issue` (`id`),
  CONSTRAINT `issue_story_ibfk_2` FOREIGN KEY (`fk_story`) REFERENCES `Story` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Export von Tabelle User
# ------------------------------------------------------------

DROP TABLE IF EXISTS `User`;

CREATE TABLE `User` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `password` varchar(255) NOT NULL DEFAULT '',
  `sessionid` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;