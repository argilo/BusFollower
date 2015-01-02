# coding=utf-8

#
# Copyright 2012 Clayton Smith
#
# This file is part of Ottawa Bus Follower.
#
# Ottawa Bus Follower is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 3, or (at
# your option) any later version.
#
# Ottawa Bus Follower is distributed in the hope that it will be
# useful, but WITHOUT ANY WARRANTY; without even the implied warranty
# of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Ottawa Bus Follower; see the file COPYING.  If not, see
# <http://www.gnu.org/licenses/>.
#

import transitfeed
import os
import sqlite3
import re

def normalizeStopCode(stopCode):
    if re.match(r"^\d\d\d\d$", stopCode):
        return stopCode
    elif re.match(r"^\d\d\d$", stopCode):
        return "0" + stopCode
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
stop_lon INT,
total_departures INT
)''')
c.execute('CREATE INDEX stop_code ON stops(stop_code)')
c.execute('CREATE INDEX stop_lon ON stops(stop_lon)')

loader = transitfeed.Loader(os.path.join('gtfs','google_transit.zip'))
schedule = loader.Load()

days_active = {}
for service_period_id, service_period in schedule.service_periods.items():
    days_active[service_period_id] = len(service_period.ActiveDates())

for stop_id, stop in schedule.stops.items():
    total_departures = 0
    for trip in stop.GetTrips():
        total_departures += days_active[trip.service_id]
    
    values = [stop.stop_id, \
              normalizeStopCode(stop.stop_code), \
              normalizeStopName(stop.stop_name), \
              int(0.5 + 1000000 * stop.stop_lat), \
              int(-0.5 + 1000000 * stop.stop_lon),
              total_departures]
    c.execute('INSERT INTO stops VALUES (?,?,?,?,?,?)', values)

    # Warn about unparseable stop codes so we can check for problems.
    if values[1] == None:
        print('Warning: Couldn\'t parse stop code "' + stop.stop_code + \
              '" (' + values[2] + ')')

conn.commit()
c.close()
print('Created database "db".')
