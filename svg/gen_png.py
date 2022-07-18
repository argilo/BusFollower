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

"""Convert SVG images to bitmaps at various pixel densities."""

import os
import subprocess

DENSITIES = {
    "ldpi": 120,
    "mdpi": 160,
    "hdpi": 240,
    "xhdpi": 320,
    "xxhdpi": 480,
    "xxxhdpi": 640,
}

for filename in os.listdir("svg"):
    if filename.endswith(".svg"):
        prefix = filename[0:-4]
        infile = os.path.join("svg", f"{prefix}.svg")
        for density, dpi in DENSITIES.items():
            density_dir = os.path.join("app", "src", "main", "res", f"drawable-{density}")
            if not os.path.exists(density_dir):
                os.makedirs(density_dir)
            outfile = os.path.join(density_dir, f"{prefix}.png")
            subprocess.run(["inkscape", "--export-type=png", f"--export-filename={outfile}",
                            f"--export-dpi={dpi}", infile], check=True)
        if prefix == "launcher_icon":
            subprocess.run(["inkscape", "--export-type=png", "--export-filename=google-play-icon.png", "-h",
                            "512", "-w", "512", infile], check=True)
