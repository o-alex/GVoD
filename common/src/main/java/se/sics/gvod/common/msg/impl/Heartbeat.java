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
package se.sics.gvod.common.msg.impl;

import com.google.common.base.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timeout;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Heartbeat {

    public static class OneWay extends GvodMsg.OneWay {

        public final Map<Integer, Integer> overlaysUtility;

        public OneWay(UUID id, Map<Integer, Integer> overlays) {
            super(id);
            this.overlaysUtility = overlays;
        }

        @Override
        public OneWay copy() {
            return new OneWay(id, new HashMap<Integer, Integer>(overlaysUtility));
        }

        @Override
        public String toString() {
            return "Heartbeat " + id.toString();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + Objects.hashCode(overlaysUtility);
            hash = 89 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final OneWay other = (OneWay) obj;
            if(!Objects.equal(this.id, other.id)) {
                return false;
            }
            if (!Objects.equal(this.overlaysUtility, other.overlaysUtility)) {
                return false;
            }
            return true;
        }
        
        
    }

    public static class PeriodicTimeout extends Timeout {

        public PeriodicTimeout(SchedulePeriodicTimeout schedule) {
            super(schedule);
        }
    }
}
