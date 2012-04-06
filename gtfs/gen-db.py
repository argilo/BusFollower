# coding=utf-8

import transitfeed
import os
import sqlite3
import re

def normalizeStopCode(stopCode):
    if re.match(r"^\d\d\d\d$", stopCode):
        return stopCode
    else:
        return None

def normalizeStopName(stopName):
    stopName = stopName.replace(u"''", "'")
    stopName = stopName.replace(u"À", "A")
    stopName = stopName.replace(u"Â", "A")
    stopName = stopName.replace(u"Æ", "AE")
    stopName = stopName.replace(u"Ç", "C")
    stopName = stopName.replace(u"È", "E")
    stopName = stopName.replace(u"É", "E")
    stopName = stopName.replace(u"Ê", "E")
    stopName = stopName.replace(u"Ë", "E")
    stopName = stopName.replace(u"Î", "I")
    stopName = stopName.replace(u"Ï", "I")
    stopName = stopName.replace(u"Ô", "O")
    stopName = stopName.replace(u"Ö", "O")
    stopName = stopName.replace(u"Ù", "U")
    stopName = stopName.replace(u"Û", "U")
    stopName = stopName.replace(u"Ü", "U")
    stopName = stopName.replace(u"Œ", "OE")
    return stopName

conn = sqlite3.connect(os.path.join('assets','db'))
c = conn.cursor()

c.execute('DROP TABLE IF EXISTS android_metadata')
c.execute('CREATE TABLE android_metadata (locale TEXT)')
c.execute('INSERT INTO android_metadata VALUES ("en_US")')

c.execute('DROP TABLE IF EXISTS stops')
c.execute('''CREATE TABLE stops (
stop_id TEXT PRIMARY KEY,
stop_code TEXT,
stop_name TEXT,
stop_lat INT,
stop_lon INT
)''')
c.execute('CREATE INDEX stop_code ON stops(stop_code)')

loader = transitfeed.Loader(os.path.join('gtfs','google_transit.zip'))
schedule = loader.Load()

for stop_id, stop in schedule.stops.items():
    values = [stop.stop_id, \
              normalizeStopCode(stop.stop_code), \
              normalizeStopName(stop.stop_name), \
              int(0.5 + 1000000 * stop.stop_lat), \
              int(0.5 + 1000000 * stop.stop_lon)]
    c.execute('INSERT INTO stops VALUES (?,?,?,?,?)', values)

conn.commit()
c.close()
print('Created database "db".')
