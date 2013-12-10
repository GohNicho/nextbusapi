-- SQL DDL for NextBus Vehicle Location Recorder (Apache Derby  or DB2)
-- Note is has no Primary Key, or Indices, as it is intended for rapid insert
DROP TABLE NEXTBUS.VEHLCN;
CREATE TABLE NEXTBUS.VEHLCN (
        AGENCY  CHAR(8) NOT NULL,
        ROUTE   CHAR(12) NOT NULL,
        VEHICLE CHAR(12) NOT NULL,
        LONGITUDE DOUBLE,
        LATITUDE DOUBLE,
        HEADING DOUBLE,
        SPEEDMPH DOUBLE,
        LASTREPORT TIMESTAMP,
        TIMESKEW BIGINT,
        CRTIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 
DROP TABLE NEXTBUS.PREDXN;
CREATE TABLE NEXTBUS.PREDXN (
	AGENCY  CHAR(8),
	ROUTEID CHAR(8),
        STOPID CHAR(8),
        DIRECTIONID CHAR(16),
	VEHICLE CHAR(8),
  	BLOCK CHAR(8),
        BRANCH CHAR(8),
        TRIP CHAR(8),
	PREDICTION TIMESTAMP,
        ARRIVAL SMALLINT,
        DEPARTURE SMALLINT,
        SCHEDULEPDN SMALLINT,
        HEURISTICPDN SMALLINT,
        DELAYED SMALLINT,
        CRTIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP );


-- A table for storing Serialized Java POJOs
DROP TABLE NEXTBUS.SERIALIZED;
CREATE TABLE NEXTBUS.SERIALIZED (
   SERIALIZED BLOB NOT NULL,
   CLASS VARCHAR(64) NOT NULL,
   CRTIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);