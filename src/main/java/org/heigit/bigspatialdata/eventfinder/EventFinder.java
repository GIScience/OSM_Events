package org.heigit.bigspatialdata.eventfinder;

// import java.io.File;
// import java.io.PrintWriter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.time.OSHDBTimestamps;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

public class EventFinder {

  // this should be replaced with an iteration over grid cells at the highest 
  public static void main(String[] args) throws Exception {
    // OSHDBH2 oshdb = (new OSHDBH2("C:\\Research Projects\\MapWars\\Case Study Results\\Katmandu\\nepal_20180201_z12_keytables.oshdb")).multithreading(true); // Nepal
    // OSHDBBoundingBox bb = new OSHDBBoundingBox(85.2880, 85.3383, 27.6675, 27.7378); // Katmandu
    // OSHDBBoundingBox bb = new OSHDBBoundingBox(85.2377, 85.2880, 27.5947, 27.6675); //near katmandu
    OSHDBH2 oshdb = (new OSHDBH2("/data_ssd/heidelberg.oshdb")).multithreading(true).inMemory(true);
    //OSHDBBoundingBox bb = new OSHDBBoundingBox(34.72, 34.85, 32.03, 32.14); // TLV
    // OSHDBBoundingBox bb = new OSHDBBoundingBox(34.2, 34.6, 31.2, 31.6); // Gaza
    OSHDBBoundingBox bb = new OSHDBBoundingBox(-180, -90, 180, 90); // Global
    ArrayList<Mapping_Event> events = get_event(bb, oshdb);
    for (Mapping_Event e : events) {
      System.out.println(e.get_t().toDate() + "," + e.get_users() + "," + e
          .get_contributions() + "," + e.get_max_contribution() + "," + e.get_change() + "," + e
          .get_type_counts().values());
    }
  }

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
  public static ArrayList<Mapping_Event> get_event(OSHDBBoundingBox bb, OSHDBH2 oshdb)
      throws Exception {
    // saves objects of type Mapping_Event which stores the month of the event, the number of active mappers, number of contributions, and maximal number of contributions by one user
    ArrayList<Mapping_Event> out = new ArrayList<Mapping_Event>();
    //read geometries
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream is = classloader.getResourceAsStream("grid_20000.geojson");
    String json = IOUtils.toString(is);
    Map<Integer, Polygon> geometries = new HashMap<>();
    FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(json);
    GeometryFactory gf = new GeometryFactory();
    GeoJSONReader gjr = new GeoJSONReader();

    Integer featureId = 0;
    for (Feature f : featureCollection.getFeatures()) {
      geometries.put(featureId,
          gf.createPolygon(gjr.read(f.getGeometry()).getCoordinates()));
      featureId++;
    }

    // collect contributions by month
    SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, Number> result = OSMContributionView
        .on(oshdb)
        .areaOfInterest(bb)
        //Relations make this VERY slow!!! We could include multipolygons seperately. Also: Do we have duplicate edites
        .osmType(OSMType.NODE, OSMType.WAY)
        .timestamps("2004-01-01", "2019-02-01", OSHDBTimestamps.Interval.MONTHLY)
        .aggregateByGeometry(geometries)
        .aggregateByTimestamp(contribution ->
            new OSHDBTimestamp(
                DateUtils.round(contribution.getTimestamp().toDate(), Calendar.MONTH)
            )
        )
        .map(new MapFunk())
        .sum();

    //TODO: remove empty results again
//    // remove entries before first contribution
//    while (result.get(result.firstKey()).size() == 0) {
//      result.remove(result.firstKey());
//    }
//
//    // remove entries after last contribution
//    while (result.get(result.lastKey()).size() == 0) {
//      result.remove(result.lastKey());
//    }
    /* for (OSHDBTimestamp t:contributions.keySet()) {
    System.out.println(t.formatIsoDateTime()+": "+contributions.get(t));
    } */
    //devide result into resulty per geometry
    SortedMap<Integer, SortedMap<OSHDBTimestamp, Number>> nest = OSHDBCombinedIndex.nest(result);
    nest.forEach((Integer geom, SortedMap<OSHDBTimestamp, Number> geomContributions) -> {
      geomContributions.forEach((OSHDBTimestamp ts, Number nr) -> {
        if (nr.intValue() > 0) {
          System.out.println(
              "Geom nr. " + geom + " at timestamp " + ts + " had " + nr + " contributions"
          );
        }
      });
//      // create data for curve fitting
//      ArrayList<WeightedObservedPoint> points = new ArrayList<WeightedObservedPoint>();
//      int i = 0;
//      Iterator<Number> values = geomContributions.values().iterator();
//      while (values.hasNext()) {
//        //error here
//        float v = (float) values.next();
//        WeightedObservedPoint point = new WeightedObservedPoint(1.0, i, v);
//        points.add(point);
//        i++;
//      }
//
//      // fit curve
//      MyFuncFitter fitter = new MyFuncFitter();
//      double[] coeffs = fitter.fit(points);
//
//      // compute errors
//      HashMap<OSHDBTimestamp, Double> errors = new HashMap<OSHDBTimestamp, Double>();
//      Iterator<Entry<OSHDBTimestamp, Number>> iterator = geomContributions.entrySet().iterator();
//      int counter = 0;
//      while (iterator.hasNext()) {
//        Entry<OSHDBTimestamp, Number> next = iterator.next();
//        Double value = coeffs[0] / (1.0 + coeffs[1] * Math.exp(-coeffs[2] * (counter - coeffs[3])));
//        //TODO: is this convertet to double?
//        errors.put(next.getKey(), next.getValue().intValue() - value);
//      }
//
//      /* PrintWriter pw = new PrintWriter(new File("C:\\temp\\near_kat_contributions.csv"));
//        StringBuilder sb = new StringBuilder();
//        sb.append("Date,Number,Dev\n");
//        for (OSHDBTimestamp tt:result.keySet()) {
//        	sb.append(tt+","+acc_result.get(tt)+","+ests.get(tt)+"\n");
//        }
//	    pw.write(sb.toString());
//	    pw.close(); */
//      // get lagged errors
//      HashMap<OSHDBTimestamp, Double> lagged_errors = new HashMap<OSHDBTimestamp, Double>();
//      for (i = 1; i < result.keySet().size(); i++) {
//        Double value = errors.get(result.keySet().toArray()[i]) - errors.get(result.keySet()
//            .toArray()[i - 1]);
//        lagged_errors.put((OSHDBTimestamp) result.keySet().toArray()[i], value);
//      }
//
//      // compute mean and standard deviation for lagged errors
//      Double mean = 0.;
//      for (Double err : lagged_errors.values()) {
//        mean = mean + err;
//      }
//      mean = mean / lagged_errors.size();
//      double std = 0.;
//      for (double num : lagged_errors.values()) {
//        std += Math.pow(num - mean, 2);
//      }
//      std = Math.sqrt(std / (lagged_errors.size() - 1.));
//
//      //TODO: move also to server and return events instead of numbers. afterwards aggregate events and classify them
////      // identify events
////      for (i = 1; i < result.keySet().size(); i++) {
////        OSHDBTimestamp m = (OSHDBTimestamp) result.keySet().toArray()[i];
////        OSHDBTimestamp m_lag = (OSHDBTimestamp) result.keySet().toArray()[i - 1];
////        Double error = (lagged_errors.get(m) - mean) / std; // normalized error
////        if (error > 1.644854) { // if error is positively significant at 95% - create event
////          HashMap<Integer, Integer> users_conts = new HashMap<Integer, Integer>();
////          HashMap<ContributionType, Integer> type_counts = new HashMap<ContributionType, Integer>();
////          type_counts.put(ContributionType.CREATION, 0);
////          type_counts.put(ContributionType.DELETION, 0);
////          type_counts.put(ContributionType.GEOMETRY_CHANGE, 0);
////          type_counts.put(ContributionType.TAG_CHANGE, 0);
////
////          for (OSMContribution c : result.get(m)) {
////            Integer user = c.getContributorUserId();
////            if (!users_conts.containsKey(user)) {
////              users_conts.put(user, 1);
////            } else {
////              users_conts.put(user, users_conts.get(user) + 1);
////            }
////
////            if (c.getContributionTypes().contains(ContributionType.CREATION)) {
////              type_counts.put(ContributionType.CREATION,
////                  type_counts.get(ContributionType.CREATION) + 1);
////            }
////            if (c.getContributionTypes().contains(ContributionType.DELETION)) {
////              type_counts.put(ContributionType.DELETION,
////                  type_counts.get(ContributionType.DELETION) + 1);
////            }
////            if (c.getContributionTypes().contains(ContributionType.GEOMETRY_CHANGE)) {
////              type_counts.put(ContributionType.GEOMETRY_CHANGE, type_counts.get(
////                  ContributionType.GEOMETRY_CHANGE) + 1);
////            }
////            if (c.getContributionTypes().contains(ContributionType.TAG_CHANGE)) {
////              type_counts.put(ContributionType.TAG_CHANGE, type_counts.get(
////                  ContributionType.TAG_CHANGE) + 1);
////            }
////          }
////          Mapping_Event e = new Mapping_Event(m, users_conts.keySet().size(),
////              acc_result.get(m) - acc_result.get(m_lag), Collections.max(users_conts.values()),
////              ((float) (acc_result.get(m) - (float) acc_result.get(m_lag)) / (float) acc_result.get(
////              m_lag)), type_counts);
////          out.add(e); // add to list of events
////        }
////      }
////      return out;
    });

    return null;
  }

}
