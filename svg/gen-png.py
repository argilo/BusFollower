#
# Copyright 2012-2015 Clayton Smith
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

import os

densities = {
       'ldpi': 120,
       'mdpi': 160,
       'hdpi': 240,
      'xhdpi': 320,
     'xxhdpi': 480,
    'xxxhdpi': 640,
}

for filename in os.listdir('svg'):
    if filename.endswith('.svg'):
        prefix = filename[0:-4]
        for density, dpi in densities.iteritems():
            density_dir = 'app/src/main/res/drawable-' + density
            if not os.path.exists(density_dir):
                os.makedirs(density_dir)
            os.system('inkscape -e ' + density_dir + '/' + prefix + '.png' \
                      + ' -d ' + str(dpi) + ' svg/' + prefix + '.svg')
    if filename == 'launcher_icon.svg':
        os.system('inkscape -e google-play-icon.png -h 512 -w 512 svg/' + prefix + '.svg')
