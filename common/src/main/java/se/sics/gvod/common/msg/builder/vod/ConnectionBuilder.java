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

package se.sics.gvod.common.msg.builder.vod;

import se.sics.gvod.common.msg.builder.GVoDMsgBuilder;
import se.sics.gvod.common.msg.builder.GVoDMsgBuilder.IncompleteException;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.common.util.VodDescriptor;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ConnectionBuilder {
    public static class Request extends GVoDMsgBuilder.Request {
        protected VodDescriptor desc;
        
        public void setVodDescriptor(VodDescriptor desc) {
            this.desc = desc;
        }
        
        @Override 
        public boolean checkComplete() {
            if(!super.checkComplete()) {
                return false;
            }
            return desc != null;
        }
        
        @Override
        public Connection.Request finalise() throws IncompleteException {
            if(!checkComplete()) {
                throw new IncompleteException();
            }
            return new Connection.Request(id, desc);
        }
        
    }
    
    public static class Response extends GVoDMsgBuilder.Response {

        @Override
        public Connection.Response finalise() throws IncompleteException {
            if(!checkComplete()) {
                throw new IncompleteException();
            }
            return new Connection.Response(id, status);
        }
        
    }
    
    public static class Update extends GVoDMsgBuilder.OneWay {
        protected VodDescriptor desc;
        
         
        public void setVodDescriptor(VodDescriptor desc) {
            this.desc = desc;
        }
        
        @Override 
        public boolean checkComplete() {
            if(!super.checkComplete()) {
                return false;
            }
            return desc != null;
        }
        
        @Override
        public Connection.Update finalise() throws IncompleteException {
            if(!checkComplete()) {
                throw new IncompleteException();
            }
            return new Connection.Update(id, desc);
        }
        
    }
    
    public static class Close extends GVoDMsgBuilder.OneWay {

        @Override
        public Connection.Close finalise() throws IncompleteException {
            if(!checkComplete()) {
                throw new IncompleteException();
            }
            return new Connection.Close(id);
        }
        
    }
}
