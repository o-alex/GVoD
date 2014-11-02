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
package se.sics.gvod.common.msg.builder;

import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GVoDMsgBuilder {

    public static abstract class Base {
        protected UUID id;
        
        public void setId(UUID id) {
            this.id = id;
        }
        
        public boolean checkComplete() {
            return id != null;
        }
        
        public abstract <E extends GvodMsg.Base> E finalise() throws IncompleteException;
    }
    
    public static abstract class Request extends Base {
    }
    
    public static abstract class Response extends Base {
        protected ReqStatus status;
        
        public void setStatus(ReqStatus status) {
            this.status = status;
        }
        
        @Override
        public boolean checkComplete() {
            if(super.checkComplete() == false) {
                return false;
            }
            return status != null;
        }
    }
    
    public static abstract class OneWay extends Base {
    }
    
    public static class IncompleteException extends Exception {
        public IncompleteException() {
            super();
        }
    }
}
