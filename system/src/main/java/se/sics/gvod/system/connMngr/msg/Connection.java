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
package se.sics.gvod.system.connMngr.msg;

import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.tags.OverlayTag;
import se.sics.gvod.common.tags.Tagged;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timeout;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Connection {

    public static class Request extends GvodMsg.Request implements Tagged<OverlayTag> {

        public final VodDescriptor desc;

        public Request(UUID id, VodDescriptor desc) {
            super(id);
            this.desc = desc;
        }

        @Override
        public Request copy() {
            return new Request(id, desc);
        }

        @Override
        public String toString() {
            return "Connect.Request<" + id + ">";
        }

        public Response fail() {
            return new Response(id, ReqStatus.FAIL, desc.overlayId);
        }

        public Response accept() {
            return new Response(id, ReqStatus.SUCCESS, desc.overlayId);
        }

        @Override
        public OverlayTag getTag() {
            return new OverlayTag(desc.overlayId);
        }
    }

    public static class Response extends GvodMsg.Response implements Tagged<OverlayTag> {
        public final int overlayId;
        
        public Response(UUID id, ReqStatus status, int overlayId) {
            super(id, status);
            this.overlayId = overlayId;
        }

        @Override
        public Response copy() {
            return new Response(id, status, overlayId);
        }

        @Override
        public String toString() {
            return "Connect.Response<" + id + "> " + status;
        }

        @Override
        public OverlayTag getTag() {
            return new OverlayTag(overlayId);
        }
    }

    public static class Update extends GvodMsg.OneWay implements Tagged<OverlayTag> {

        public final VodDescriptor desc;

        public Update(UUID id, VodDescriptor desc) {
            super(id);
            this.desc = desc;
        }

        @Override
        public Update copy() {
            return new Update(id, desc);
        }

        @Override
        public String toString() {
            return "Connect.Update<" + id + ">";
        }
        
        @Override
        public OverlayTag getTag() {
            return new OverlayTag(desc.overlayId);
        }
    }

    public static class UpdateTimeout extends Timeout {

        public UpdateTimeout(SchedulePeriodicTimeout schedule) {
            super(schedule);
        }
        
        @Override
        public String toString() {
            return "Connection.UpdateTimeout<" + getTimeoutId() + ">";
        }
        
    }

    public static class Close extends GvodMsg.OneWay implements Tagged<OverlayTag> {

        public final int overlayId;

        public Close(UUID id, int overlayId) {
            super(id);
            this.overlayId = overlayId;
        }

        @Override
        public Close copy() {
            return new Close(id, overlayId);
        }

        @Override
        public String toString() {
            return "Connect.Close<" + id + ">";
        }
        
        @Override
        public OverlayTag getTag() {
            return new OverlayTag(overlayId);
        }
    }
}
