#!/usr/bin/env python3

#
# Copyright 2012-2020 Clayton Smith
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
# <https://www.gnu.org/licenses/>.
#

import csv
import datetime
import io
import os
import sqlite3
import re
import zipfile


def normalizeStopCode(stopCode):
    if stopCode == "0000":
        return None
    elif re.match(r"^\d\d\d\d$", stopCode):
        return stopCode
    else:
        return None


def removeAccents(stopName):
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


def normalizeStopName(stopName):
    stopName = removeAccents(stopName.upper())
    original = stopName
    stopName = stopName.strip()
    stopName = stopName.replace("LAURIER E / GOULBOURN", "LAURIER E / GOULBURN")
    stopName = stopName.replace("SOMERSET E / GOULBOURN", "SOMERSET E / GOULBURN")
    stopName = stopName.replace("MANN / GOULBOURN", "MANN / GOULBURN")
    stopName = stopName.replace("KANATA / GOULBURN", "KANATA / GOULBOURN")
    stopName = stopName.replace("SUMMERFIELDS # 1", "SUMMERFIELDS #1")
    stopName = stopName.replace("EVANSHAN", "EVANSHEN")
    stopName = stopName.replace("BARETTE", "BARRETTE")
    stopName = stopName.replace("BARRETE", "BARRETTE")
    stopName = stopName.replace("MER BELUE", "MER BLEUE")
    stopName = stopName.replace("/", " / ")
    stopName = re.sub(" +", " ", stopName)
    stopName = re.sub("\\b(STE?)-", "\\1 ", stopName)
    if stopName != original:
        print("Info: Corrected '{}' to '{}'".format(original, stopName))
    return stopName


def parseDate(dateString):
    year = int(dateString[0:4])
    month = int(dateString[4:6])
    day = int(dateString[6:8])
    return datetime.date(year, month, day)


assets_dir = os.path.join("app", "src", "main", "assets")
if not os.path.exists(assets_dir):
    os.makedirs(assets_dir)

out_file = "db"
conn = sqlite3.connect(os.path.join(assets_dir, out_file))
c = conn.cursor()

c.execute("DROP TABLE IF EXISTS android_metadata")
c.execute("CREATE TABLE android_metadata (locale TEXT)")
c.execute("INSERT INTO android_metadata VALUES ('en_US')")

c.execute("DROP TABLE IF EXISTS stops")
c.execute("""CREATE TABLE stops (
stop_id TEXT PRIMARY KEY,
stop_code TEXT,
stop_name TEXT,
stop_lat REAL,
stop_lon REAL,
total_departures INT
)""")
c.execute("CREATE INDEX stop_code ON stops(stop_code)")
c.execute("CREATE INDEX stop_lon ON stops(stop_lon)")

with zipfile.ZipFile(os.path.join("gtfs", "google_transit.zip")) as zip:
    service_days = {}
    with zip.open("calendar.txt") as calendar:
        reader = csv.DictReader(io.TextIOWrapper(calendar, "utf-8"))
        for service in reader:
            service_id = service["service_id"]
            service_days[service_id] = set()
            start_date = parseDate(service["start_date"])
            end_date = parseDate(service["end_date"])
            date = start_date
            while date <= end_date:
                if service[date.strftime("%A").lower()] == "1":
                    service_days[service_id].add(date)
                date += datetime.timedelta(days=1)

    with zip.open("calendar_dates.txt") as calendar_dates:
        reader = csv.DictReader(io.TextIOWrapper(calendar_dates, "utf-8"))
        for calendar_date in reader:
            service_id = calendar_date["service_id"]
            date = parseDate(calendar_date["date"])
            if service_id not in service_days:
                service_days[service_id] = set()
            if calendar_date["exception_type"] == "1":
                service_days[service_id].add(date)
            else:
                service_days[service_id].discard(date)

    for service_id in service_days:
        service_days[service_id] = len(service_days[service_id])

    trip_service_id = {}
    with zip.open("trips.txt") as trips:
        reader = csv.DictReader(io.TextIOWrapper(trips, "utf-8"))
        for trip in reader:
            trip_service_id[trip["trip_id"]] = trip["service_id"]

    stop_id_stops = {}
    with zip.open("stop_times.txt") as stop_times:
        reader = csv.DictReader(io.TextIOWrapper(stop_times, "utf-8"))
        for stop_time in reader:
            trip_id = stop_time["trip_id"]
            service_id = trip_service_id[trip_id]
            departures = service_days.get(service_id, 0)
            stop_id_stops[stop_time["stop_id"]] = stop_id_stops.get(stop_time["stop_id"], 0) + departures

    with zip.open("stops.txt") as stops:
        reader = csv.DictReader(io.TextIOWrapper(stops, "utf-8"))
        for stop in reader:
            total_departures = stop_id_stops[stop["stop_id"]]
            values = [stop["stop_id"],
                      normalizeStopCode(stop["stop_code"]),
                      normalizeStopName(stop["stop_name"]),
                      float(stop["stop_lat"]),
                      float(stop["stop_lon"]),
                      total_departures]
            c.execute("INSERT INTO stops VALUES (?,?,?,?,?,?)", values)

            # Warn about unparseable stop codes so we can check for problems.
            if values[1] is None:
                print("Warning: Couldn't parse stop code '{}' ({})".format(stop["stop_code"], values[2]))


conn.commit()
c.close()
print("Created database '{}'.".format(out_file))
