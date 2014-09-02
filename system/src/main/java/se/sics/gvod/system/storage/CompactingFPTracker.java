///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.gvod.system.storage;
//
//import java.util.Iterator;
//import java.util.Map;
//import java.util.TreeMap;
//import org.javatuples.Pair;
//import org.javatuples.Triplet;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class FilePieceTracker {
//
//    private final TreeMap<Integer, Integer> previousRanges;
//    private final TreeMap<Integer, Integer> readRanges;
//
//    private int readPos;
//    private Triplet<Integer, Integer, Boolean> readRange;
//    private boolean modifiedReadRange;
//    private boolean modifiedPreviousRanges;
//
//    public FilePieceTracker(int readPos, Triplet<Integer, Integer, Boolean> readRange) {
//        this.previousRanges = new TreeMap<Integer, Integer>();
//        this.readRanges = new TreeMap<Integer, Integer>();
//        this.readRange = readRange;
//        this.readPos = readPos;
//        if (readRange.getValue2()) {
//            this.readRanges.put(readRange.getValue0(), readRange.getValue1());
//        }
//        this.modifiedReadRange = false;
//        this.modifiedPreviousRanges = false;
//    }
//
//    public Triplet getReadRange() {
//        compact();
//        Map.Entry<Integer, Integer> firstReadR = readRanges.firstEntry();
//        if(readRange.getValue0() == firstReadR.getKey()) {
//            readRange = Triplet.with(firstReadR.getKey(), firstReadR.getValue(), true);
//        }
//        return readRange;
//    }
//    
//    public void addToRange(int pieceId) {
//        if(pieceId < readPos) {
//            modifiedPreviousRanges = true;
//            previousRanges.put(pieceId, pieceId);
//        } else {
//            modifiedReadRange = true;
//            readRanges.put(pieceId, pieceId);
//        }
//    }
//
//    public void changeReadPos(int readPos) {
//        compact();
//        TreeMap<Integer, Integer> move = new TreeMap<Integer, Integer>();
//        this.readPos = readPos;
//        for (Integer startRPos : readRanges.keySet()) {
//            int endRPos = readRanges.get(startRPos);
//            if (endRPos < readPos) {
//                move.put(startRPos, endRPos);
//            }
//        }
//        for (Map.Entry<Integer, Integer> e : move.entrySet()) {
//            readRanges.remove(e.getKey());
//            previousRanges.put(e.getKey(), e.getValue());
//        }
//        Map.Entry<Integer, Integer> readE = readRanges.firstEntry();
//        if (readE.getKey() <= readPos) {
//            readRange = Triplet.with(readE.getKey(), readE.getValue(), true);
//        } else {
//            readRange = Triplet.with(readPos, readPos, false);
//        }
//    }
//
//    public boolean addToRanges(Integer pieceId) {
//        if (readRange.getValue2()) {
//            if (readRange.getValue0() <= pieceId) {
//                if (pieceId <= readRange.getValue1()) {
//                    return false;
//                } else {
//                    boolean result = putInRanges(readRanges, pieceId);
//                    if(result) {
//                        Map.Entry<Integer, Integer> 
//                    }
//                }
//            } else if (readRange.getValue0() - 1 == pieceId) {
//                return startReadRange(pieceId);
//            } else {
//                return addToPreviousRanges(pieceId);
//            }
//        } else {
//            if (pieceId == readRange.getValue0()) {
//                return startReadRange(pieceId);
//            } else if (readRange.getValue0() < pieceId) {
//                return addToReadRanges(pieceId);
//            } else {
//                return addToPreviousRanges(pieceId);
//            }
//        }
//    }
//
//    private boolean startReadRange(int pieceId) {
//        Map.Entry<Integer, Integer> lastPreviousRange = previousRanges.lastEntry();
//        Map.Entry<Integer, Integer> firstReadRange = readRanges.firstEntry();
//        int startR = pieceId;
//        int endR = pieceId;
//        if (lastPreviousRange.getValue() == pieceId - 1) {
//            startR = lastPreviousRange.getKey();
//            previousRanges.remove(lastPreviousRange.getKey());
//        }
//        if (firstReadRange.getKey() == pieceId + 1) {
//            endR = firstReadRange.getValue();
//            readRanges.remove(firstReadRange.getKey());
//        }
//        readRange = Triplet.with(startR, endR, true);
//        readRanges.put(startR, endR);
//        return true;
//    }
//
//    private boolean putInRanges(TreeMap<Integer,Integer> ranges, Integer pieceId) {
//        Iterator<Integer> it = ranges.keySet().iterator();
//        Integer previousKey;
//        Integer key;
//
//        if (!it.hasNext()) {
//            //no read ranges
//            ranges.put(pieceId, pieceId);
//            return true;
//        }
//        previousKey = it.next();
//
//        while (it.hasNext()) {
//            key = it.next();
//            if (key > pieceId + 1) {
//                break;
//            } else if (key == pieceId + 1) {
//                int previousVal = ranges.get(previousKey);
//                int val = ranges.get(key);
//                if (previousVal == pieceId - 1) {
//                    ranges.remove(key);
//                    ranges.put(previousKey, val);
//                    return true;
//                } else {
//                    ranges.remove(key);
//                    ranges.put(pieceId, val);
//                    return true;
//                }
//            } else {
//                previousKey = key;
//                continue;
//            }
//        }
//        int previousVal = ranges.get(previousKey);
//        if(previousKey <= pieceId && pieceId <= previousVal) {
//            return false;
//        }
//        if(previousVal == pieceId - 1) {
//            ranges.put(previousKey, pieceId);
//            return true;
//        }
//        ranges.put(pieceId, pieceId);   
//        return true;
//    }
//    
//    private Pair<Boolean, Pair<Integer, Integer>> tryMerge(Map.Entry<Integer, Integer> firstRange, Map.Entry<Integer, Integer> secondRange, int pieceId) {
//        if(firstRange.getValue() != pieceId -1) {
//            return Pair.with(false, null);
//        }
//        if(secondRange.getKey() != pieceId + 1) {
//            return Pair.with(false, null);
//        }
//        return Pair.with(true, Pair.with(firstRange.getKey(), secondRange.getValue()));
//    }
//}
