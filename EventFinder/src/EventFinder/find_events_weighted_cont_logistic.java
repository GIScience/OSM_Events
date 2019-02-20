package EventFinder;

// import java.io.File;
// import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDB_H2;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.objects.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.api.objects.OSHDBTimestamps;
import org.heigit.bigspatialdata.oshdb.api.objects.OSMContribution;
import org.heigit.bigspatialdata.oshdb.util.BoundingBox;
import org.heigit.bigspatialdata.oshdb.util.ContributionType;

import com.vividsolutions.jts.geom.Coordinate;


/*
 * This procedure identifies large scale events within osm data using the oshdb api
 * I define here events as large contributions in relation to the current development of the data base (i.e. relatively).
 * The procedures assumes that the accumulative number contribution actions (meaning the individual actions that make a contribution, e.g. deleting or adding a coordinate or a tag) follows 
 * an s-shaped (logistic) curve over time.
 * Accordingly, the procedure counts the accumulative number of actions for each month and fits a logistic curve of the type: a/(1+b*exp(-k*(t-u))), where t is a temporal index, with the data.
 * Differences between observed values and estimations ('errors') are calculated. To eliminate temporal dependency, the procedure uses lagged errors (lagged error at time t=
 * error at time t - error at time t-1).
 * The procedure normalizes the lagged errors and identifies significantly positive values at 95% confidence level as events.
 * For each event, the procedure records information regarding its date, number of active users, number of actions, maximal number of actions by a single user, relative change in the database size, 
 * and number of contributions by type.
 */

public class find_events_weighted_cont_logistic {
	public static ArrayList<Mapping_Event> get_event(BoundingBox bb, OSHDB_H2 oshdb, OSHDB_H2 oshdbKeytables) throws Exception {
		// saves objects of type Mapping_Event which stores the month of the event, the number of active mappers, number of contributions, and maximal number of contributions by one user
		ArrayList<Mapping_Event> out = new ArrayList<Mapping_Event>();
	    
		// collect contributions by month
		SortedMap<OSHDBTimestamp, List<OSMContribution>> result = OSMContributionView.on(oshdb).keytables(oshdbKeytables)
				.areaOfInterest(bb)
				.timestamps("2004-01-01", "2019-02-01", OSHDBTimestamps.Interval.MONTHLY)
				.aggregateByTimestamp()
				.collect();
		
		// remove entries before first contribution
		while (result.get(result.firstKey()).size() == 0) {
			result.remove(result.firstKey());
		}
		
		// remove entries after last contribution
		while (result.get(result.lastKey()).size() == 0) {
			result.remove(result.lastKey());
		}
		
		// count contribution actions by month
		HashMap<OSHDBTimestamp, Integer> contributions = new HashMap<OSHDBTimestamp, Integer>();
		for (OSHDBTimestamp t:result.keySet()) {
			int count = 0;
			for (OSMContribution c:result.get(t)) {
				if (c.getContributionTypes().contains(ContributionType.DELETION)) { 
					count = count + 1; // a deletion is considered as one action
				} else if (c.getContributionTypes().contains(ContributionType.CREATION)) {
					count = count + c.getGeometryAfter().getCoordinates().length + c.getEntityAfter().getTags().length; // each addition of a coordinate and of a tag is considered an action
				} else {
					// for modifications - each addition and deletion of coordinate or tag is considered an action
					List<Coordinate> c_aft = Arrays.asList(c.getGeometryAfter().getCoordinates());
					List<Coordinate> c_bef = Arrays.asList(c.getGeometryBefore().getCoordinates());
					if (!c_aft.equals(c_bef)) {
						int coord_additions = 0;
						for (Coordinate coord:c_aft) {
							if (!c_bef.contains(coord)) {
								coord_additions += 1;
							}
						}
					
						int coord_dels = 0;
						for (Coordinate coord:c_bef) {
							if (!c_aft.contains(coord)) {
								coord_dels += 1;
							}
						}
						
						count = count + coord_additions +  coord_dels;
					}
					
					List<Integer> t_aft = new ArrayList<Integer>();
					List<Integer> t_bef = new ArrayList<Integer>();
					for (int i:c.getEntityAfter().getTags()) {
						t_aft.add(i);
					}
					for (int i:c.getEntityBefore().getTags()) {
						t_bef.add(i);
					}
					if (!t_aft.equals(t_bef)) {
						int tags_adds = 0;
						for (Integer tag:t_aft) {
							if (!t_bef.contains(tag)) {
								tags_adds += 1;
							}
						}
						
						int tags_dels = 0;
						for (Integer tag:t_bef) {
							if (!t_aft.contains(tag)) {
								tags_dels += 1;
							}
						}
						
						count = count + tags_dels + tags_adds;
					}
				}
			}
			contributions.put(t, count);
		}
		
		
	    
	    
		/* for (OSHDBTimestamp t:contributions.keySet()) {
			System.out.println(t.formatIsoDateTime()+": "+contributions.get(t));
		} */
	    
	    // create accumulative data
	    ArrayList<OSHDBTimestamp> t = new ArrayList<OSHDBTimestamp>();
	    SortedMap<OSHDBTimestamp, Integer> acc_result = new TreeMap<OSHDBTimestamp, Integer>();
	    Integer conts = 0;
	    for (OSHDBTimestamp tt:result.keySet()) {
	    	conts = conts + contributions.get(tt);
	    	acc_result.put(tt, conts);
	    	t.add(tt);
	    }
	    
	    // create data for curve fitting
	    ArrayList<WeightedObservedPoint> points = new ArrayList<WeightedObservedPoint>();
	    int i = 0;
	    Iterator<Integer> values = acc_result.values().iterator();
	    while (values.hasNext()) {
		    float v = (float) values.next();
		    WeightedObservedPoint point = new WeightedObservedPoint(1.0, i, v);
		    points.add(point);
		    i++;
	    }
	    
	    // fit curve
	    MyFuncFitter fitter = new MyFuncFitter();
	    double[] coeffs = fitter.fit(points);
	    
	    
	    // compute errors
	    HashMap<OSHDBTimestamp, Double> errors = new HashMap<OSHDBTimestamp, Double>();
	    for (i=0; i<points.size(); i++) {
	    	Double value = coeffs[0] / (1.0 + coeffs[1] * Math.exp(-coeffs[2] * (i - coeffs[3])));
	    	errors.put(t.get(i), acc_result.get(t.get(i)) - value);
	    }
	    
	    /* PrintWriter pw = new PrintWriter(new File("C:\\temp\\near_kat_contributions.csv"));
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Number,Dev\n");
        for (OSHDBTimestamp tt:result.keySet()) {
        	sb.append(tt+","+acc_result.get(tt)+","+ests.get(tt)+"\n");
        }
	    pw.write(sb.toString());
	    pw.close(); */
	    
	    // get lagged errors
	    HashMap<OSHDBTimestamp, Double> lagged_errors = new HashMap<OSHDBTimestamp, Double>();
	    for (i=1; i<result.keySet().size(); i++) {
	    	Double value = errors.get(result.keySet().toArray()[i]) - errors.get(result.keySet().toArray()[i-1]);
	    	lagged_errors.put((OSHDBTimestamp) result.keySet().toArray()[i], value);
	    }
	    
	    // compute mean and standard deviation for lagged errors
	    Double mean = 0.;
	    for (Double err:lagged_errors.values()) {
	    	mean = mean + err;
	    }
	    mean = mean / lagged_errors.size();
	    double std = 0.;
	    for (double num: lagged_errors.values()) {
	    	std += Math.pow(num-mean, 2);
	    }
	    std = Math.sqrt(std/(lagged_errors.size()-1.));
	    
	    // identify events
	    for (i=1; i<result.keySet().size(); i++) {
	    	OSHDBTimestamp m = (OSHDBTimestamp) result.keySet().toArray()[i];
	    	OSHDBTimestamp m_lag = (OSHDBTimestamp) result.keySet().toArray()[i-1];
	    	Double error = (lagged_errors.get(m)-mean)/std; // normalized error
	    	if (error > 1.644854) { // if error is positively significant at 95% - create event
	    		HashMap<Integer, Integer> users_conts = new HashMap<Integer, Integer>();
	    		HashMap<ContributionType, Integer> type_counts = new HashMap<ContributionType, Integer>();
	    		type_counts.put(ContributionType.CREATION, 0);
	    		type_counts.put(ContributionType.DELETION, 0);
	    		type_counts.put(ContributionType.GEOMETRY_CHANGE, 0);
	    		type_counts.put(ContributionType.MEMBERLIST_CHANGE, 0);
	    		type_counts.put(ContributionType.TAG_CHANGE, 0);
	    		
	    		for (OSMContribution c:result.get(m)) {
	    			Integer user = c.getContributorUserId();
	    			if (!users_conts.containsKey(user)) {
	    				users_conts.put(user, 1);
	    			} else {
	    				users_conts.put(user, users_conts.get(user)+1);
	    			}
	    			
	    			if (c.getContributionTypes().contains(ContributionType.CREATION)) {
	    				type_counts.put(ContributionType.CREATION, type_counts.get(ContributionType.CREATION) + 1);
	    			}
	    			if (c.getContributionTypes().contains(ContributionType.DELETION)) {
	    				type_counts.put(ContributionType.DELETION, type_counts.get(ContributionType.DELETION) + 1);
	    			}
	    			if (c.getContributionTypes().contains(ContributionType.GEOMETRY_CHANGE)) {
	    				type_counts.put(ContributionType.GEOMETRY_CHANGE, type_counts.get(ContributionType.GEOMETRY_CHANGE) + 1);
	    			}
	    			if (c.getContributionTypes().contains(ContributionType.MEMBERLIST_CHANGE)) {
	    				type_counts.put(ContributionType.MEMBERLIST_CHANGE, type_counts.get(ContributionType.MEMBERLIST_CHANGE) + 1);
	    			}
	    			if (c.getContributionTypes().contains(ContributionType.TAG_CHANGE)) {
	    				type_counts.put(ContributionType.TAG_CHANGE, type_counts.get(ContributionType.TAG_CHANGE) + 1);
	    			}
	    		}
	    		Mapping_Event e = new Mapping_Event(m, users_conts.keySet().size(), acc_result.get(m)-acc_result.get(m_lag), Collections.max(users_conts.values()),
	    				 ((float) (acc_result.get(m)- (float) acc_result.get(m_lag))/(float) acc_result.get(m_lag)), type_counts);
	    		out.add(e); // add to list of events
	    	}
	    }
	    return out;
	  }
	
	// this should be replaced with an iteration over grid cells at the highest 
	public static void main(String[] args) throws Exception {
		OSHDB_H2 oshdbKeytables = new OSHDB_H2("C:\\Research Projects\\MapWars\\keytables");
		// OSHDB_H2 oshdb = (new OSHDB_H2("C:\\Research Projects\\MapWars\\Case Study Results\\Katmandu\\nepal_20180201_z12_keytables.oshdb")).multithreading(true); // Nepal
		// BoundingBox bb = new BoundingBox(85.2880, 85.3383, 27.6675, 27.7378); // Katmandu
		// BoundingBox bb = new BoundingBox(85.2377, 85.2880, 27.5947, 27.6675); //near katmandu
		OSHDB_H2 oshdb = (new OSHDB_H2("C:\\Research Projects\\MapWars\\israel-and-palestine.oshdb")).multithreading(true);
		BoundingBox bb = new BoundingBox(34.72, 34.85, 32.03, 32.14); // TLV
		// BoundingBox bb = new BoundingBox(34.2, 34.6, 31.2, 31.6); // Gaza
		ArrayList<Mapping_Event> events = get_event(bb, oshdb, oshdbKeytables);
		for (Mapping_Event e:events) {
			System.out.println(e.get_t().formatIsoDateTime()+","+e.get_users()+","+e.get_contributions()+","+e.get_max_contribution()+","+e.get_change()+","+e.get_type_counts().values());
		}
	}
}
