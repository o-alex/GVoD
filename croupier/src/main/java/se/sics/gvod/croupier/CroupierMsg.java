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

import java.util.UUID;
import se.sics.kompics.KompicsEvent;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierMsg {

    public static abstract class CroupierBase implements KompicsEvent {

        public final UUID id;
        public final int croupierId;

        public CroupierBase(UUID id, int croupierId) {
            this.id = id;
            this.croupierId = croupierId;
        }

        public abstract <E extends CroupierBase> E copy();
    }

    //***************************************************************************************************
    public static abstract class Request extends CroupierBase {

        public Request(UUID id, int croupierId) {
            super(id, croupierId);
        }
    }

    public static abstract class Response extends CroupierBase {

        public Response(UUID id, int croupierId) {
            super(id, croupierId);
        }
    }

    public static abstract class OneWay extends CroupierBase {

        public OneWay(UUID id, int croupierId) {
            super(id, croupierId);
        }
    }
}
