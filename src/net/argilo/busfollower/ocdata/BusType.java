/*
 * Copyright 2012 Clayton Smith
 *
 * This file is part of Ottawa Bus Follower.
 *
 * Ottawa Bus Follower is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3, or (at
 * your option) any later version.
 *
 * Ottawa Bus Follower is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ottawa Bus Follower; see the file COPYING.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package net.argilo.busfollower.ocdata;

import java.io.Serializable;

public class BusType implements Serializable {
    private static final long serialVersionUID = 1L;

    private String busType;
    
    public BusType(String busType) {
        this.busType = busType;
    }
    
    public int getLength() {
        if (busType.contains("4") && !busType.contains("6")) {
            return 40;
        } else if (busType.contains("6") && !busType.contains("4")) {
            return 60;
        } else {
            return -1;
        }
    }

    public boolean hasBikeRack() {
        return busType.contains("B");
    }
    
    public boolean isHybrid() {
        return busType.contains("DEH");
    }
    
    public boolean isDoubleDecker() {
        return busType.contains("DD");
    }
}
