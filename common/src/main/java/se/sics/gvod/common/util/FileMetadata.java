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

package se.sics.gvod.common.util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class FileMetadata {
    public final int fileSize; 
    public final int pieceSize; 
    public final String hashAlg;
    public final int hashFileSize;
    
    public FileMetadata(int fileSize, int pieceSize, String hashAlg, int hashFileSize) {
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.hashAlg = hashAlg;
        this.hashFileSize = hashFileSize;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + this.fileSize;
        hash = 41 * hash + this.pieceSize;
        hash = 41 * hash + (this.hashAlg != null ? this.hashAlg.hashCode() : 0);
        hash = 41 * hash + this.hashFileSize;
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
        final FileMetadata other = (FileMetadata) obj;
        if (this.fileSize != other.fileSize) {
            return false;
        }
        if (this.pieceSize != other.pieceSize) {
            return false;
        }
        if ((this.hashAlg == null) ? (other.hashAlg != null) : !this.hashAlg.equals(other.hashAlg)) {
            return false;
        }
        if (this.hashFileSize != other.hashFileSize) {
            return false;
        }
        return true;
    }

}