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
package se.sics.gvod.system.video.hashMngr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import se.sics.gvod.system.video.storage.FilePieceTracker;
import se.sics.gvod.system.video.storage.Storage;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HashBuilder {

    private final HashMngrConfig config;
    private final Storage file;

    public HashBuilder(HashMngrConfig config, Storage file) {
        this.config = config;
        this.file = file;
    }

    public void run() throws HBException {
        try {

            File hashFile = new File(config.libDir + File.pathSeparator + config.videoName);
            if (hashFile.exists()) {
                hashFile.delete();
            }
            hashFile.createNewFile();

            OutputStream fw =  new FileOutputStream(hashFile.getPath());
            MessageDigest md = MessageDigest.getInstance(config.hashAlg);
            
            for (int i = 0; i < file.nrPieces(); i++) {
                fw.write(md.digest(file.readPiece(i)));
            }
            fw.flush();
            fw.close();
        } catch (IOException ex) {
            throw new HBException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new HBException(ex);
        } catch (FilePieceTracker.PieceNotReadyException ex) {
            throw new HBException(ex);
        } catch (FilePieceTracker.OutOfBoundsException ex) {
            throw new HBException(ex);
        }
    }

    public static class HBException extends Exception {

        public HBException(Throwable cause) {
            super(cause);
        }
    }
}
