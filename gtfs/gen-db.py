# coding=utf-8

import os
import sqlite3
import csv
import re

def stringToMicroDegrees(degreesString):
    degreesString = degreesString.strip()
    return int(0.5 + (1000000.0 * float(degreesString)))

def normalizeStopCode(stopCode):
    if re.match(r"^\d\d\d\d$", stopCode):
        return stopCode
    else:
        return None

def normalizeStopName(stopName):
    stopName = stopName.replace("''", "'")
    stopName = stopName.replace("À", "A")
    stopName = stopName.replace("Â", "A")
    stopName = stopName.replace("Æ", "AE")
    stopName = stopName.replace("Ç", "C")
    stopName = stopName.replace("È", "E")
    stopName = stopName.replace("É", "E")
    stopName = stopName.replace("Ê", "E")
    stopName = stopName.replace("Ë", "E")
    stopName = stopName.replace("Î", "I")
    stopName = stopName.replace("Ï", "I")
    stopName = stopName.replace("Ô", "O")
    stopName = stopName.replace("Ö", "O")
    stopName = stopName.replace("Ù", "U")
    stopName = stopName.replace("Û", "U")
    stopName = stopName.replace("Ü", "U")
    stopName = stopName.replace("Œ", "OE")
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

with open(os.path.join('gtfs','stops.txt')) as f:
    reader = csv.reader(f)
    columns = reader.next()

    stopIdCol = columns.index('stop_id')
    stopCodeCol = columns.index('stop_code')
    stopNameCol = columns.index('stop_name')
    stopLatCol = columns.index('stop_lat')
    stopLonCol = columns.index('stop_lon')
    
    for row in reader:
        values = [row[stopIdCol], \
                  normalizeStopCode(row[stopCodeCol]), \
                  normalizeStopName(row[stopNameCol]), \
                  stringToMicroDegrees(row[stopLatCol]), \
                  stringToMicroDegrees(row[stopLonCol])]
        c.execute('INSERT INTO stops VALUES (?,?,?,?,?)', values)

conn.commit()
c.close()
print('Created database "db".')
