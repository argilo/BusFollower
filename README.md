```
#
# Copyright 2012-2018 Clayton Smith
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
```

Ottawa Bus Follower
===================

Author: Clayton Smith  
Email: <argilo@gmail.com>

https://play.google.com/store/apps/details?id=net.argilo.busfollower

Tired of freezing in the cold waiting for a bus that's late?  This app
will let you stay warm inside until your bus is near.  It displays the
locations and estimated arrival times of Ottawa buses using GPS data
supplied by OC Transpo.

To begin, search for a stop by number or name, or choose one from the
map.  Then choose the bus route you're interested in, and you'll see
data for the next three arrivals at that stop.  You can refresh the
results at any time to stay up to date.  Tapping on a bus will bring
up more information about it.

Please note that OC Transpo does not yet supply GPS information for
all buses, and that you will see GPS information only for buses that
have begun their trip toward your stop.  These are limitations of OC
Transpo's systems, and there is little my app can do about them.  When
GPS data is not available, scheduled arrival times will appear instead
of estimated times.

This is an open source project released under the GPL.  Source code is
available at https://github.com/argilo/BusFollower.  Issues are
tracked at https://github.com/argilo/BusFollower/issues.  Bug reports
and code contributions are welcome!

I would like to thank the following contributors:
* Alnoor Allidina - beta testing, bug reporting
* Greg Fletcher - beta testing, French translation
* Sonja Kisa - French translation
* Karen Schindel - beta testing
* Teodora Alexandrova - beta testing

-----

Tu en as assez de geler en attendant l'autobus en retard? Cette
application te permet de rester au chaud jusqu'à ce que l'autobus soit
proche. Elle affiche où se trouvent les autobus d'Ottawa et quand ils
devraient arriver selon les données GPS fournies par OC Transpo.

Pour commencer, cherche un arrêt par numéro ou par nom ou sélectionne
un arrêt sur le plan. Ensuite choisis le circuit désiré et voilà les
infos pour les trois prochaines arrivées à cet arrêt. Tu peux
rafraîchir les résultats n'importe quand pour mettre à jour. Pour plus
de renseignements sur un autobus, tape sur celui-ci.

Prière de noter que OC Transpo ne fournit pas encore les infos GPS sur
tous ses autobus. Tu verras seulement les renseignements GPS pour les
autobus qui ont commencé leur trajet vers cet arrêt. C'est une
limitation des systèmes d'OC Transpo et il n'y a pas grand-chose que
mon application puisse faire à cet égard. Lorsque les coordonnées GPS
ne sont pas disponibles, les heures de l'horaire régulier sont
affichées au lieu des heures prévues.

Ceci est un logiciel libre publié sous la licence GPL. Le code source
est disponible à https://github.com/argilo/BusFollower. Les problèmes
sont suivis à https://github.com/argilo/BusFollower/issues. Les relevés
de bogues et les contributions de code sont appréciés!

-----

Build dependencies:
* Android SDK 27
* Inkscape
* Python 2.x
* SQLite
* GoogleTransitDataFeed:
    https://github.com/google/transitfeed/wiki
* GTFS data from OC Transpo:
    http://www.octranspo1.com/files/google_transit.zip
