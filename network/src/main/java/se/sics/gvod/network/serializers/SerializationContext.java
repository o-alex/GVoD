/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.gvod.network.serializers;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface SerializationContext {
    public <E extends Object> SerializationContext registerSerializer(String serializerName, Serializer<E> serializer) throws DuplicateException;
    public SerializationContext registerMessageCode(Class<?> messageClass, byte messageCode) throws DuplicateException;
    public SerializationContext registerClass(String serializerName, Class<?> serializedClass) throws DuplicateException, MissingException;
    
    public <E extends Object> Serializer<E> getSerializer(Class<E> serializedClass) throws MissingException;
    public Class<?> getMessageClass(Byte mcode) throws MissingException;
    public Byte getOpcode(Class<?> messageClass) throws MissingException;
    
    public static class DuplicateException extends Exception {
        public DuplicateException() {
            super();
        }
    }
    
    public static class MissingException extends Exception {
        public MissingException() {
            super();
        }
    }
}
