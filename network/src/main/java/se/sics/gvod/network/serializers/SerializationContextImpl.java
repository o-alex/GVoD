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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import se.sics.gvod.common.newmsg.NetworkMsg;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
//Serializer name - String - Serializer relation
//Message opcode - Byte - MessageClass relation
//Serialized Class - Class - Serializer relation
public class SerializationContextImpl implements SerializationContext {

    private final ReentrantReadWriteLock rwLock;

    private final Map<String, Serializer<?>> serializers;
    private final Map<Class<? extends NetworkMsg>, Byte> classToMcode;
    private final Map<Class<?>, Serializer<?>> classToSerializer;

    public SerializationContextImpl() {
        this.rwLock = new ReentrantReadWriteLock();

        this.serializers = new HashMap<String, Serializer<?>>();
        this.classToMcode = new HashMap<Class<? extends NetworkMsg>, Byte>();
        this.classToSerializer = new HashMap<Class<?>, Serializer<?>>();
    }

    @Override
    public <E> SerializationContext registerSerializer(String serializerName, Serializer<E> serializer) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            if (serializers.containsKey(serializerName)) {
                throw new DuplicateException();
            }
            serializers.put(serializerName, serializer);
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public SerializationContext registerMessageCode(Class<? extends NetworkMsg> messageClass, byte messageCode) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            if (classToMcode.containsKey(messageCode)) {
                throw new DuplicateException();
            }
            classToMcode.put(messageClass, messageCode);
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public SerializationContext registerClass(String serializerName, Class<?> serializedClass) throws DuplicateException, MissingException {
        rwLock.writeLock().lock();
        try {
            if (!serializers.containsKey(serializerName)) {
                throw new MissingException();
            }
            if (classToSerializer.containsKey(serializedClass)) {
                throw new DuplicateException();
            }
            classToSerializer.put(serializedClass, serializers.get(serializerName));
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    //TODO improve - the iterator will slow performance quite a bit
    @Override
    public Class<? extends NetworkMsg> getMessageClass(Byte mcode) throws MissingException {
        rwLock.readLock().lock();
        try {
            Iterator<Map.Entry<Class<? extends NetworkMsg>, Byte>> it = classToMcode.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<? extends NetworkMsg>, Byte> e = it.next();
                if (e.getValue().equals(mcode)) {
                    return e.getKey();
                }
            }
            throw new MissingException();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Byte getOpcode(Class<? extends NetworkMsg> messageClass) throws MissingException {
        rwLock.readLock().lock();
        try {
            if (classToMcode.containsKey(messageClass)) {
                throw new MissingException();
            }
            return classToMcode.get(messageClass);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public <E extends Object> Serializer<E> getSerializer(Class<E> serializedClass) throws MissingException {
        rwLock.readLock().lock();
        try {
            if(!classToSerializer.containsKey(serializedClass)) {
                throw new MissingException();
            }
            return (Serializer<E>) classToSerializer.get(serializedClass);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
