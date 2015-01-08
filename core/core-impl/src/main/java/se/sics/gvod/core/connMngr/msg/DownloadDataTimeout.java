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
package se.sics.gvod.core.connMngr.msg;

import java.util.UUID;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DownloadDataTimeout extends Timeout {

    public final VodAddress target;
    public final UUID reqId;

    public DownloadDataTimeout(ScheduleTimeout schedule, VodAddress target, UUID reqId) {
        super(schedule);
        this.target = target;
        this.reqId = reqId;
    }

    @Override
    public String toString() {
        return "DownloadTimeout<" + getTimeoutId() + ">";
    }
}
