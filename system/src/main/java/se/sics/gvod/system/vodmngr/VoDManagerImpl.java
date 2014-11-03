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

package se.sics.gvod.system.vodmngr;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.manager.FileStatus;
import se.sics.gvod.manager.VoDManager;
import se.sics.gvod.system.vod.VoDPort;
import se.sics.gvod.system.vod.msg.DownloadVideo;
import se.sics.gvod.system.vod.msg.UploadVideo;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDManagerImpl extends ComponentDefinition implements VoDManager {
    private static final Logger log = LoggerFactory.getLogger(VoDManager.class);

    private final Positive<VoDPort> vodPort = requires(VoDPort.class);
    private final Positive<UtilityUpdatePort> utilityPort = requires(UtilityUpdatePort.class);
    
    private final VoDManagerConfig config;
    
    private final Map<String, FileStatus> videos;
    
    
    public VoDManagerImpl(VoDManagerInit init) {
        this.config = init.config;
        this.videos = new ConcurrentHashMap<String, FileStatus>();
    }
    
    public void loadLibrary() {
        log.info("loading library folder:{}", config.libDir);
        
        File dir = new File(config.libDir);
        if (!dir.isDirectory()) {
            throw new RuntimeException("bad library path");
        }
        
        FileFilter mp4Filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (!file.isFile()) {
                    return false;
                }
                return file.getName().endsWith(".mp4");
            }
        };
        
        for (File video : dir.listFiles(mp4Filter)) {
            log.debug("reading video: {} info", video.getName());
            videos.put(video.getName(), FileStatus.NONE);
        }
    }
    
     @Override
    public Map<String, FileStatus> getFiles() {
        return new HashMap<String, FileStatus>(videos);
    }
    
    @Override
    public boolean pendingUpload(String videoName) {
        FileStatus videoStatus = videos.get(videoName);
        if(videoStatus == null || !videoStatus.equals(FileStatus.NONE)) {
            return false;
        }
        videos.put(videoName, FileStatus.PENDING);
        return true;
    }
    
    @Override
    public boolean uploadVideo(String videoName, int overlayId) {
        FileStatus videoStatus = videos.get(videoName);
        if(videoStatus == null || !videoStatus.equals(FileStatus.PENDING)) {
            return false;
        }
        videos.put(videoName, FileStatus.UPLOADING);
        trigger(new UploadVideo.Request(videoName, overlayId), vodPort);
        return true;
    }

    @Override
    public boolean downloadVideo(String videoName, int overlayId) {
        FileStatus videoStatus = videos.get(videoName);
        if(videoStatus != null) {
            return false;
        }
        videos.put(videoName, FileStatus.DOWNLOADING);
        trigger(new DownloadVideo.Request(videoName, overlayId), vodPort);
        return true;
    }
    
    public static class VoDManagerInit extends Init<VoDManagerImpl> {
        public final VoDManagerConfig config;
        
        public VoDManagerInit(VoDManagerConfig config) {
            this.config = config;
        }
    }
}