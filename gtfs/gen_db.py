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


def normalize_stop_code(stop_code):
    if stop_code == "0000":
        return None
    elif re.match(r"^\d\d\d\d$", stop_code):
        return stop_code
    else:
        return None


def remove_accents(stop_name):
    stop_name = stop_name.replace("À", "A")
    stop_name = stop_name.replace("Â", "A")
    stop_name = stop_name.replace("Æ", "AE")
    stop_name = stop_name.replace("Ç", "C")
    stop_name = stop_name.replace("È", "E")
    stop_name = stop_name.replace("É", "E")
    stop_name = stop_name.replace("Ê", "E")
    stop_name = stop_name.replace("Ë", "E")
    stop_name = stop_name.replace("Î", "I")
    stop_name = stop_name.replace("Ï", "I")
    stop_name = stop_name.replace("Ô", "O")
    stop_name = stop_name.replace("Ö", "O")
    stop_name = stop_name.replace("Ù", "U")
    stop_name = stop_name.replace("Û", "U")
    stop_name = stop_name.replace("Ü", "U")
    stop_name = stop_name.replace("Œ", "OE")
    return stop_name


def normalize_stop_name(stop_name):
    stop_name = remove_accents(stop_name.upper())
    original = stop_name
    stop_name = stop_name.strip()
    stop_name = stop_name.replace("LAURIER E / GOULBOURN", "LAURIER E / GOULBURN")
    stop_name = stop_name.replace("SOMERSET E / GOULBOURN", "SOMERSET E / GOULBURN")
    stop_name = stop_name.replace("MANN / GOULBOURN", "MANN / GOULBURN")
    stop_name = stop_name.replace("KANATA / GOULBURN", "KANATA / GOULBOURN")
    stop_name = stop_name.replace("SUMMERFIELDS # 1", "SUMMERFIELDS #1")
    stop_name = stop_name.replace("EVANSHAN", "EVANSHEN")
    stop_name = stop_name.replace("BARETTE", "BARRETTE")
    stop_name = stop_name.replace("BARRETE", "BARRETTE")
    stop_name = stop_name.replace("MER BELUE", "MER BLEUE")
    stop_name = stop_name.replace("/", " / ")
    stop_name = re.sub(" +", " ", stop_name)
    stop_name = re.sub("\\b(STE?)-", "\\1 ", stop_name)
    if stop_name != original:
        print("Info: Corrected '{}' to '{}'".format(original, stop_name))
    return stop_name


def gtfs_table(gtfs_zip, filename):
    with gtfs_zip.open(filename) as table:
        reader = csv.DictReader(io.TextIOWrapper(table, "utf-8"))
        for line in reader:
            yield line


def parse_date(date_string):
    year = int(date_string[0:4])
    month = int(date_string[4:6])
    day = int(date_string[6:8])
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

with zipfile.ZipFile(os.path.join("gtfs", "google_transit.zip")) as gtfs:
    service_days = {}
    for service in gtfs_table(gtfs, "calendar.txt"):
        service_id = service["service_id"]
        service_days[service_id] = set()
        start_date = parse_date(service["start_date"])
        end_date = parse_date(service["end_date"])
        date = start_date
        while date <= end_date:
            if service[date.strftime("%A").lower()] == "1":
                service_days[service_id].add(date)
            date += datetime.timedelta(days=1)

    for calendar_date in gtfs_table(gtfs, "calendar_dates.txt"):
        service_id = calendar_date["service_id"]
        date = parse_date(calendar_date["date"])
        if service_id not in service_days:
            service_days[service_id] = set()
        if calendar_date["exception_type"] == "1":
            service_days[service_id].add(date)
        else:
            service_days[service_id].discard(date)

    for service_id in service_days:
        service_days[service_id] = len(service_days[service_id])

    trip_service_id = {}
    for trip in gtfs_table(gtfs, "trips.txt"):
        trip_service_id[trip["trip_id"]] = trip["service_id"]

    stop_id_stops = {}
    for stop_time in gtfs_table(gtfs, "stop_times.txt"):
        trip_id = stop_time["trip_id"]
        service_id = trip_service_id[trip_id]
        departures = service_days.get(service_id, 0)
        stop_id_stops[stop_time["stop_id"]] = stop_id_stops.get(stop_time["stop_id"], 0) + departures

    for stop in gtfs_table(gtfs, "stops.txt"):
        total_departures = stop_id_stops[stop["stop_id"]]
        values = [stop["stop_id"],
                  normalize_stop_code(stop["stop_code"]),
                  normalize_stop_name(stop["stop_name"]),
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
