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
package se.sics.gvod.network;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.common.msg.peerMngr.OverlaySample;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.NetMsg;
import se.sics.gvod.network.netmsg.OverlayHeaderField;
import se.sics.gvod.network.netmsg.bootstrap.NetAddOverlay;
import se.sics.gvod.network.netmsg.bootstrap.NetBootstrapGlobal;
import se.sics.gvod.network.netmsg.bootstrap.NetHeartbeat;
import se.sics.gvod.network.netmsg.bootstrap.NetJoinOverlay;
import se.sics.gvod.network.netmsg.bootstrap.NetOverlaySample;
import se.sics.gvod.network.netmsg.vod.NetConnection;
import se.sics.gvod.network.netmsg.vod.NetDownload;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.bootstrap.AddOverlaySerializer;
import se.sics.gvod.network.serializers.bootstrap.BootstrapGlobalSerializer;
import se.sics.gvod.network.serializers.bootstrap.HeartbeatSerializer;
import se.sics.gvod.network.serializers.bootstrap.JoinOverlaySerializer;
import se.sics.gvod.network.serializers.bootstrap.OverlaySampleSerializer;
import se.sics.gvod.network.serializers.net.base.SerializerAdapter;
import se.sics.gvod.network.serializers.net.vod.NetConnectionSerializer;
import se.sics.gvod.network.serializers.net.vod.NetDownloadSerializer;
import se.sics.gvod.network.serializers.net.bootstrap.NetAddOverlaySerializer;
import se.sics.gvod.network.serializers.net.bootstrap.NetBootstrapGlobalSerializer;
import se.sics.gvod.network.serializers.net.bootstrap.NetHeartbeatSerializer;
import se.sics.gvod.network.serializers.net.bootstrap.NetJoinOverlaySerializer;
import se.sics.gvod.network.serializers.net.bootstrap.NetOverlaySampleSerializer;
import se.sics.gvod.network.serializers.util.FileMetadataSerializer;
import se.sics.gvod.network.serializers.util.OverlayHeaderFieldSerializer;
import se.sics.gvod.network.serializers.util.ReqStatusSerializer;
import se.sics.gvod.network.serializers.util.UUIDSerializer;
import se.sics.gvod.network.serializers.util.VodAddressSerializer;
import se.sics.gvod.network.serializers.util.VodDescriptorSerializer;
import se.sics.gvod.network.serializers.vod.ConnectionSerializer;
import se.sics.gvod.network.serializers.vod.DownloadSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GVoDNetworkSettings {

    private static SerializationContext context = null;

    public static void setContext(SerializationContext setContext) {
        context = setContext;
    }

    public static SerializationContext getContext() {
        return context;
    }
    
    public static boolean checkPreCond() {
        Set<String> gvodAliases = new HashSet<String>();
        gvodAliases.add(MsgAliases.GVOD_NET_ONEWAY.toString());
        gvodAliases.add(MsgAliases.GVOD_NET_REQUEST.toString());
        gvodAliases.add(MsgAliases.GVOD_NET_RESPONSE.toString());
        return context != null && context.containsAliases(gvodAliases);
    }

    public static void registerSerializers() {
        NetMsg.setContext(context);
        SerializerAdapter.setContext(context);

        registerNetworkMsg();
        registerOthers();
    }

    private static void registerNetworkMsg() {
        try {
            context.registerSerializer(NetConnection.Request.class, new NetConnectionSerializer.Request());
            context.registerSerializer(NetConnection.Response.class, new NetConnectionSerializer.Response());
            context.registerSerializer(NetConnection.Update.class, new NetConnectionSerializer.Update());
            context.registerSerializer(NetConnection.Close.class, new NetConnectionSerializer.Close());
            context.registerSerializer(NetDownload.HashRequest.class, new NetDownloadSerializer.HashRequest());
            context.registerSerializer(NetDownload.HashResponse.class, new NetDownloadSerializer.HashResponse());
            context.registerSerializer(NetDownload.DataRequest.class, new NetDownloadSerializer.DataRequest());
            context.registerSerializer(NetDownload.DataResponse.class, new NetDownloadSerializer.DataResponse());

            context.registerSerializer(NetBootstrapGlobal.Request.class, new NetBootstrapGlobalSerializer.Request());
            context.registerSerializer(NetBootstrapGlobal.Response.class, new NetBootstrapGlobalSerializer.Response());
            context.registerSerializer(NetAddOverlay.Request.class, new NetAddOverlaySerializer.Request());
            context.registerSerializer(NetAddOverlay.Response.class, new NetAddOverlaySerializer.Response());
            context.registerSerializer(NetJoinOverlay.Request.class, new NetJoinOverlaySerializer.Request());
            context.registerSerializer(NetJoinOverlay.Response.class, new NetJoinOverlaySerializer.Response());
            context.registerSerializer(NetOverlaySample.Request.class, new NetOverlaySampleSerializer.Request());
            context.registerSerializer(NetOverlaySample.Response.class, new NetOverlaySampleSerializer.Response());
            context.registerSerializer(NetHeartbeat.OneWay.class, new NetHeartbeatSerializer.OneWay());

            context.multiplexAlias(MsgAliases.GVOD_NET_ONEWAY.toString(), NetConnection.Update.class, (byte) 0x01);
            context.multiplexAlias(MsgAliases.GVOD_NET_ONEWAY.toString(), NetConnection.Close.class, (byte) 0x02);
            context.multiplexAlias(MsgAliases.GVOD_NET_ONEWAY.toString(), NetHeartbeat.OneWay.class, (byte) 0x03);

            context.multiplexAlias(MsgAliases.GVOD_NET_REQUEST.toString(), NetConnection.Request.class, (byte) 0x01);
            context.multiplexAlias(MsgAliases.GVOD_NET_REQUEST.toString(), NetDownload.HashRequest.class, (byte) 0x02);
            context.multiplexAlias(MsgAliases.GVOD_NET_REQUEST.toString(), NetDownload.DataRequest.class, (byte) 0x03);
            context.multiplexAlias(MsgAliases.GVOD_NET_REQUEST.toString(), NetBootstrapGlobal.Request.class, (byte) 0x04);
            context.multiplexAlias(MsgAliases.GVOD_NET_REQUEST.toString(), NetAddOverlay.Request.class, (byte) 0x05);
            context.multiplexAlias(MsgAliases.GVOD_NET_REQUEST.toString(), NetJoinOverlay.Request.class, (byte) 0x06);
            context.multiplexAlias(MsgAliases.GVOD_NET_REQUEST.toString(), NetOverlaySample.Request.class, (byte) 0x07);

            context.multiplexAlias(MsgAliases.GVOD_NET_RESPONSE.toString(), NetConnection.Response.class, (byte) 0x01);
            context.multiplexAlias(MsgAliases.GVOD_NET_RESPONSE.toString(), NetDownload.HashResponse.class, (byte) 0x02);
            context.multiplexAlias(MsgAliases.GVOD_NET_RESPONSE.toString(), NetDownload.DataResponse.class, (byte) 0x03);
            context.multiplexAlias(MsgAliases.GVOD_NET_RESPONSE.toString(), NetBootstrapGlobal.Response.class, (byte) 0x04);
            context.multiplexAlias(MsgAliases.GVOD_NET_RESPONSE.toString(), NetAddOverlay.Response.class, (byte) 0x05);
            context.multiplexAlias(MsgAliases.GVOD_NET_RESPONSE.toString(), NetJoinOverlay.Response.class, (byte) 0x06);
            context.multiplexAlias(MsgAliases.GVOD_NET_RESPONSE.toString(), NetOverlaySample.Response.class, (byte) 0x07);
        } catch (SerializationContext.DuplicateException ex) {
            throw new RuntimeException(ex);
        } catch (SerializationContext.MissingException ex) {
            throw new RuntimeException(ex);
        }

    }

    private static void registerOthers() {
        try {
            context.registerSerializer(Connection.Request.class, new ConnectionSerializer.Request());
            context.registerSerializer(Connection.Response.class, new ConnectionSerializer.Response());
            context.registerSerializer(Connection.Update.class, new ConnectionSerializer.Update());
            context.registerSerializer(Connection.Close.class, new ConnectionSerializer.Close());
            context.registerSerializer(Download.HashRequest.class, new DownloadSerializer.HashRequest());
            context.registerSerializer(Download.HashResponse.class, new DownloadSerializer.HashResponse());
            context.registerSerializer(Download.DataRequest.class, new DownloadSerializer.DataRequest());
            context.registerSerializer(Download.DataResponse.class, new DownloadSerializer.DataResponse());

            context.registerSerializer(BootstrapGlobal.Request.class, new BootstrapGlobalSerializer.Request());
            context.registerSerializer(BootstrapGlobal.Response.class, new BootstrapGlobalSerializer.Response());
            context.registerSerializer(AddOverlay.Request.class, new AddOverlaySerializer.Request());
            context.registerSerializer(AddOverlay.Response.class, new AddOverlaySerializer.Response());
            context.registerSerializer(JoinOverlay.Request.class, new JoinOverlaySerializer.Request());
            context.registerSerializer(JoinOverlay.Response.class, new JoinOverlaySerializer.Response());
            context.registerSerializer(OverlaySample.Request.class, new OverlaySampleSerializer.Request());
            context.registerSerializer(OverlaySample.Response.class, new OverlaySampleSerializer.Response());
            context.registerSerializer(Heartbeat.OneWay.class, new HeartbeatSerializer.OneWay());

            //utils
            context.registerSerializer(FileMetadata.class, new FileMetadataSerializer());
            context.registerSerializer(ReqStatus.class, new ReqStatusSerializer());
            context.registerSerializer(UUID.class, new UUIDSerializer());
            context.registerSerializer(VodAddress.class, new VodAddressSerializer());
            context.registerSerializer(VodDescriptor.class, new VodDescriptorSerializer());
            context.registerSerializer(OverlayHeaderField.class, new OverlayHeaderFieldSerializer());

            //aliased HeaderFields
            context.registerAlias(OtherAliases.HEADER_FIELD.aliasedClass, OtherAliases.HEADER_FIELD.toString(), (byte) 0x00);
            context.multiplexAlias(OtherAliases.HEADER_FIELD.toString(), OverlayHeaderField.class, (byte) 0x01);
        } catch (SerializationContext.DuplicateException ex) {
            throw new RuntimeException(ex);
        } catch (SerializationContext.MissingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static enum MsgAliases {

        GVOD_NET_REQUEST(DirectMsgNetty.Request.class), GVOD_NET_RESPONSE(DirectMsgNetty.Response.class), GVOD_NET_ONEWAY(DirectMsgNetty.Oneway.class);
        public final Class aliasedClass;

        MsgAliases(Class aliasedClass) {
            this.aliasedClass = aliasedClass;
        }
    }

    public static enum OtherAliases {

        HEADER_FIELD(HeaderField.class);

        public final Class aliasedClass;

        OtherAliases(Class aliasedClass) {
            this.aliasedClass = aliasedClass;
        }
    }
}
