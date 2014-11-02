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
package se.sics.gvod.common.msg.peerMngr;

import java.util.HashSet;
import com.google.common.base.Objects;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapGlobal {

    public static class Request extends GvodMsg.Request {

        public Request(UUID reqId) {
            super(reqId);
        }

        public Response success(Set<VodAddress> systemSample) {
            return new Response(id, ReqStatus.SUCCESS, systemSample);
        }

        public Response fail() {
            return new Response(id, ReqStatus.FAIL, null);
        }

        @Override
        public Request copy() {
            return new Request(id);
        }

        @Override
        public String toString() {
            return "BootstrapGlobalRequest " + id.toString();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object that) {
            if (that == null) {
                return false;
            }
            if (getClass() != that.getClass()) {
                return false;
            }
            final Request other = (Request) that;
            if (!Objects.equal(this.id, other.id)) {
                return false;
            }
            return true;
        }
    }

    public static class Response extends GvodMsg.Response {

        public final Set<VodAddress> systemSample;

        public Response(UUID reqId, ReqStatus status, Set<VodAddress> systemSample) {
            super(reqId, status);
            this.systemSample = systemSample;
        }

        @Override
        public Response copy() {
            return new Response(id, status, new HashSet<VodAddress>(systemSample));
        }

        @Override
        public String toString() {
            return "BootstrapGlobalResponse<" + status.toString() + "> " + id.toString();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + Objects.hashCode(this.systemSample);
            hash = 47 * hash + Objects.hashCode(this.status);
            hash = 47 * hash + Objects.hashCode(this.id);
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
            final Response other = (Response) obj;
            if (!Objects.equal(this.systemSample, other.systemSample)) {
                return false;
            }
            if (this.status != other.status) {
                return false;
            }
            if (!Objects.equal(this.id, other.id)) {
                return false;
            }
            return true;
        }
    }
}
