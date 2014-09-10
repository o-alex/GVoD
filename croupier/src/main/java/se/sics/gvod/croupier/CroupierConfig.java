/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package se.sics.gvod.croupier;

import java.util.Random;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierConfig {
    public static final int DEFAULT_PORT = 58022;
    public static final int DEFAULT_STUN_PORT = 3478;
    public static final int DEFAULT_STUN_PORT_2 = 3479;
    
    public final Random rand;
    public final int viewSize;
    public final long shufflePeriod;
    public final int shuffleLength;
    public final CroupierSelectionPolicy policy;
    
    public CroupierConfig(Random rand, int viewSize, long shufflePeriod, int shuffleLength, CroupierSelectionPolicy policy) {
        this.rand = rand;
        this.viewSize = viewSize;
        this.shufflePeriod = shufflePeriod;
        this.shuffleLength = shuffleLength;
        this.policy = policy;
    }
}
