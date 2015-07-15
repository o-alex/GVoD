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

package se.sics.gvod.system.storage;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class UtilTest {

    @Test
    public void test1() throws NoSuchAlgorithmException {
        Map<Integer, Integer> mp = new HashMap<Integer, Integer>(); 
        mp.put(1,1);
        mp.put(2,2);
        mp.entrySet().iterator().next();
        for(Map.Entry<Integer, Integer> e : mp.entrySet()) {
            System.out.println(e.getValue());
        }
        System.out.println("" + (byte)0x91);
         
    }
}
