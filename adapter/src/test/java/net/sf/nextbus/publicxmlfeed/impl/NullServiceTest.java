/*******************************************************************************
 * Copyright (C) 2011,2012 by James R. Doyle
 *
 * This file is part of the NextBus® Livefeed Java Adapter (nblf4j). See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership and licensing.
 *
 * nblf4j is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 *
 * nblf4j is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with UJMP; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Usage of the NextBus Web Service and its data is subject to separate
 * Terms and Conditions of Use (License) available at:
 * 
 *      http://www.nextbus.com/xmlFeedDocs/NextBusXMLFeed.pdf
 * 
 * 
 * NextBus® is a registered trademark of Webtech Wireless Inc.
 *
 ******************************************************************************/
package net.sf.nextbus.publicxmlfeed.impl;
import net.sf.nextbus.publicxmlfeed.impl.Service;
import net.sf.nextbus.publicxmlfeed.domain.PredictionGroup;
import net.sf.nextbus.publicxmlfeed.domain.Geolocation;
import net.sf.nextbus.publicxmlfeed.domain.Route;
import net.sf.nextbus.publicxmlfeed.domain.Agency;
import net.sf.nextbus.publicxmlfeed.domain.RouteConfiguration;
import net.sf.nextbus.publicxmlfeed.domain.Stop;
import net.sf.nextbus.publicxmlfeed.domain.Schedule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import java.util.*;

/**
 * Tests the DomainFactory + XSD Mappings with a Stub Network Protocol implementation.
 * 
 * @author jrd
 */
public class NullServiceTest {
    
    Service svc;
    
    @Before
    public void setup() throws Exception {
         svc = new Service(new NullRpcImpl());
    }
    
    @Test
    public void testAgencies() throws Exception {
        List<Agency> agencies = svc.getAgencies();
        Assert.assertNotNull(agencies);
        Assert.assertTrue(agencies.size()>0);
    }
    
    @Test
    public void testRouteList() throws Exception {
        Agency mbta = new Agency("mbta","","","","");
        List<Route> routes = svc.getRoutes(mbta);
        System.out.println(routes);
    }
    
    @Test
    public void testRouteConfig() throws Exception {
        Agency mbta = new Agency("mbta","","","","");
        Route r = new Route(mbta,"a","b","c","");
        RouteConfiguration rc = svc.getRouteConfiguration(r);
        //stops=87, directions=2, paths=20,
        Assert.assertTrue(rc.getStops().size() == 87);
        Assert.assertTrue(rc.getDirections().size() == 2);
        Assert.assertTrue(rc.getPaths().size() == 20);
        System.out.println(rc);
    }
    
    @Test
    public void testPrediction() throws Exception {
        Agency mbta = new Agency("mbta", "Test Agency", "Test Agency", "Test Agency", "Test Agency");
        Route r = new Route(mbta, "route1", "Test Route", "Test Route", "Test Route");
        Stop s = new Stop(r, "1936", "1936", "1936", "1936", new Geolocation(0.0, 0.0), "");
        PredictionGroup pg = svc.getPredictions(s);
        Assert.assertTrue(pg.getDirections().size() == 1);
        Assert.assertTrue(pg.getAvailablePredictions() == 5);
        System.out.println(pg);
    }
    
    @Test
    public void testMultiPrediction() throws Exception {
        Agency mbta = new Agency("mbta", "Test Agency", "Test Agency", "Test Agency", "Test Agency");
        Route r = new Route(mbta, "route1", "Test Route", "Test Route", "Test Route");
        Stop s1 = new Stop(r, "8310", "8310", "8310", "8310", new Geolocation(0.0, 0.0), "");
         Stop s2 = new Stop(r, "5271", "5271", "5271", "5271", new Geolocation(0.0, 0.0), "");
         Stop s3 = new Stop(r, "5271_ar", "5271_ar", "5271_ar", "5271_ar", new Geolocation(0.0, 0.0), "");
         List<Stop> stops =    Arrays.asList(new Stop[] { s1, s2, s3});
       
               
        List<PredictionGroup> pgs = svc.getPredictions(stops);
        Assert.assertTrue(pgs.size() == stops.size()); // should be a predictions block for each request parameter
        
        boolean gotStop1 = false, gotStop2 = false, gotStop3 = false;
        
        for (PredictionGroup pg : pgs) {
            if (pg.getStop().equals(s1)) {
                // Stop 8310 should have Two Directions and Two Messages
                Assert.assertTrue(pg.getDirections().size() == 2);
                Assert.assertTrue(pg.getMessages().size() == 2);
                gotStop1=true;
                System.out.println(">>>> 1");
            }
             if (pg.getStop().equals(s2)) {
                // Stop 5271 should have 1Directions and Zero Messages
                Assert.assertTrue(pg.getDirections().size() == 1);
                Assert.assertTrue(pg.getMessages().size() == 0);
                gotStop2=true;
                 System.out.println(">>>> 2");
            }
             if (pg.getStop().equals(s3)) {
                // Stop 5271_ar should have 1Directions and Zero Messages
                Assert.assertTrue(pg.getDirections().size() == 1);
                Assert.assertTrue(pg.getMessages().size() == 0);
                gotStop3=true;
                 System.out.println(">>>> 3");
            }  
        }
    }
    
    @Test
    public void testSchedules() {
        Agency mbta = new Agency("mbta", "Test Agency", "Test Agency", "Test Agency", "Test Agency");
        Route r = new Route(mbta, "route1", "Test Route", "Test Route", "Test Route");
        List<Schedule> s = svc.getSchedule(r);
        Assert.assertNotNull(s);
        Assert.assertTrue(s.size()>0);
        
    }
    
    
}
