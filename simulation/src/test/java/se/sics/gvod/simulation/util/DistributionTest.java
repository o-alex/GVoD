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

package se.sics.gvod.simulation.util;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DistributionTest {

    @Test
    public void testIntUDist1() {
        IntegerUniformDistribution dist = new IntegerUniformDistribution(1, 1, new Random());
        Assert.assertEquals((Integer)1, dist.draw());
    }
    
    @Test
    public void testIntUDist2() {
        int min = 100;
        int max = 1000;
        IntegerUniformDistribution dist = new IntegerUniformDistribution(min, max, new Random());
        for(int i = 0; i < 100000; i++) {
            int draw = dist.draw();
            Assert.assertTrue("" + draw, min <= draw && draw <= max);
        }
    }
}
