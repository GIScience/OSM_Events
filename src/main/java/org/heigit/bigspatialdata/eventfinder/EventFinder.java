package org.heigit.bigspatialdata.eventfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBJdbc;
import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.heigit.bigspatialdata.oshdb.util.time.OSHDBTimestamps;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

public class EventFinder {

  // this should be replaced with an iteration over grid cells at the highest 
  public static void main(String[] args) throws Exception {
    String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    String propertiesPath = rootPath + "oshdb.properties";

    Properties oshdbProperties = new Properties();
    oshdbProperties.load(new FileInputStream(propertiesPath));

    // OSHDBH2 oshdb = (new OSHDBH2("C:\\Research Projects\\MapWars\\Case Study Results\\Katmandu\\nepal_20180201_z12_keytables.oshdb")).multithreading(true); // Nepal
    // OSHDBBoundingBox bb = new OSHDBBoundingBox(85.2880, 85.3383, 27.6675, 27.7378); // Katmandu
    // OSHDBBoundingBox bb = new OSHDBBoundingBox(85.2377, 85.2880, 27.5947, 27.6675); //near katmandu
    OSHDBH2 oshdb = (new OSHDBH2(oshdbProperties.getProperty("oshdb")))
        .multithreading(true)
        .inMemory(true);
    OSHDBJdbc keytables = oshdb;
//    OSHDBDatabase oshdb = new OSHDBIgnite(EventFinder.class.getResource("/ohsome-ignite-dev.xml")
//        .getFile());
//    oshdb.prefix("global_v4");
//    Connection conn = DriverManager.getConnection(
//        "jdbc:postgresql://10.11.12.21:5432/keytables-global_v4", "ohsome", args[0]);
//    OSHDBJdbc keytables = new OSHDBJdbc(conn);
    //OSHDBBoundingBox bb = new OSHDBBoundingBox(34.72, 34.85, 32.03, 32.14); // TLV
    // OSHDBBoundingBox bb = new OSHDBBoundingBox(34.2, 34.6, 31.2, 31.6); // Gaza
    OSHDBBoundingBox bb = new OSHDBBoundingBox(8.57733, 49.373558, 8.774741, 49.45832); //HD
    //OSHDBBoundingBox bb = new OSHDBBoundingBox(-180, -90, 180, 90); // Global

    Map<Integer, ArrayList<MappingEvent>> events = get_event(bb, oshdb, keytables);
    oshdb.close();

    //write output
    File file = new File("target/MappingEvents.csv");

//Create the file
    if (file.createNewFile()) {
      System.out.println("File is created!");
    } else {
      System.out.println("File already exists.");
    }

//Write Content
    FileWriter writer = new FileWriter(file);
    writer.write(
        "ID;GeomNr.;EventNr.;Timestamp;Users;Contributions;Change;MaxContributions;EditedEntitities;AverageGeomChanges;AverageTagChanges;Pvalue;Coeffs;TypeCounts\n");

    int[] id = {0};
    events.forEach((Integer geom, ArrayList<MappingEvent> ev) -> {
      if (ev.isEmpty()) {
        return;
      }
      System.out.println("");
      System.out.println("");
      System.out.println("Events for geom Nr. " + geom);
      System.out.println("");
      int[] eventnr = {1};
      ev.forEach((MappingEvent e) -> {
        try {
          writer.write(
              id[0] + ";"
              + geom + ";"
              + eventnr[0] + ";"
              + e.getTimestap().toDate() + ";"
              + e.getUser_counts().size() + ";"
              + e.get_contributions() + ";"
              + e.getDeltakontrib() + ";"
              + e.getMaxCont() + ";"
              + e.get_entity_edits().size() + ";"
              + e.get_geom_change_average() + ";"
              + e.get_tag_change_average() + ";"
              + e.get_pvalue() + ";"
              + Arrays.toString(e.getCoeffs()) + ";"
              + e.get_type_counts().toString() + ";"
              + "\n"
          );
        } catch (IOException ex) {
          Logger.getLogger(EventFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(
            e.getTimestap().toDate() + ";"
            + e.getUser_counts().size() + ";"
            + e.get_contributions() + ";"
            + Collections.max(e.getUser_counts().values()) + ";"
            + e.getChange() + ";"
            + e.get_type_counts().values() + ";"
            + e.getMaxCont() + ";"
            + Arrays.toString(e.getCoeffs())
        );
        eventnr[0] += 1;
        id[0] += 1;
      });
    });
    writer.close();
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
  public static Map<Integer, ArrayList<MappingEvent>> get_event(OSHDBBoundingBox bb,
      OSHDBDatabase oshdb, OSHDBJdbc keytables)
      throws Exception {
    // saves objects of type Mapping_Event which stores the month of the event, the number of active mappers, number of contributions, and maximal number of contributions by one user
    final Map<Integer, ArrayList<MappingEvent>> out = new HashMap<>();

    // collect contributions by month
    SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> result = OSMContributionView
        .on(oshdb)
        .keytables(keytables)
        .areaOfInterest(bb)
        //Relations make this VERY slow!!! We could include multipolygons seperately. Also: Do we have duplicate edites
        .osmType(OSMType.NODE, OSMType.WAY)
        .timestamps("2004-01-01", "2019-02-01", OSHDBTimestamps.Interval.MONTHLY)
        .aggregateByGeometry(EventFinder.getPolygons())
        .aggregateByTimestamp(contribution ->
            new OSHDBTimestamp(
                DateUtils.round(contribution.getTimestamp().toDate(), Calendar.MONTH)
            )
        )
        .map(new MapFunk())
        .reduce(new NewMapMonth(), new MonthCombiner());

    //devide result into resulty per geometry
    SortedMap<Integer, SortedMap<OSHDBTimestamp, MappingMonth>> nest = OSHDBCombinedIndex.nest(
        result);

    //TODO many loops following. Can we simplify?
    //iterate
    nest.forEach((Integer geom, SortedMap<OSHDBTimestamp, MappingMonth> geomContributions) -> {
      ArrayList<MappingEvent> list = new ArrayList<>();

      // remove entries before first contribution
      while (geomContributions.get(geomContributions.firstKey())
          .get_contributions() == 0) {
        geomContributions.remove(geomContributions.firstKey());
        if (geomContributions.isEmpty()) {
          return;
        }
      }

      // remove entries after last contribution
      while (geomContributions.get(geomContributions.lastKey()).get_contributions() == 0) {
        geomContributions.remove(geomContributions.lastKey());
      }


      /* for (OSHDBTimestamp t:contributions.keySet()) {
    System.out.println(t.formatIsoDateTime()+": "+contributions.get(t));
    } */
      // create accumulative data
      SortedMap<OSHDBTimestamp, Integer> acc_result = new TreeMap<OSHDBTimestamp, Integer>();
      Integer conts = 0;
      for (Entry<OSHDBTimestamp, MappingMonth> entry : geomContributions.entrySet()) {
        conts = conts + entry.getValue().get_contributions();
        acc_result.put(entry.getKey(), conts);
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
      double[] coeffs;
      try {
        coeffs = fitter.fit(points);
      } catch (ConvergenceException ex) {
        ex.printStackTrace();
        return;
      }

      // compute errors
      HashMap<OSHDBTimestamp, Double> errors = new HashMap<OSHDBTimestamp, Double>();
      for (Entry<OSHDBTimestamp, MappingMonth> entry : geomContributions.entrySet()) {
        Double value = coeffs[0] / (1.0 + coeffs[1] * Math.exp(-coeffs[2] * (i - coeffs[3])));
        errors.put(entry.getKey(), acc_result.get(entry.getKey()) - value);
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
      for (i = 1; i < geomContributions.keySet().size(); i++) {
        Double value = errors.get(geomContributions.keySet().toArray()[i]) - errors.get(
            geomContributions.keySet()
                .toArray()[i - 1]);
        lagged_errors.put((OSHDBTimestamp) geomContributions.keySet().toArray()[i], value);
      }

      // compute mean and standard deviation for lagged errors
      Double mean = 0.;
      for (Double err : lagged_errors.values()) {
        mean = mean + err;
      }
      mean = mean / lagged_errors.size();
      double std = 0.;
      for (double num : lagged_errors.values()) {
        std += Math.pow(num - mean, 2);
      }
      std = Math.sqrt(std / (lagged_errors.size() - 1.));

      Iterator<Entry<OSHDBTimestamp, MappingMonth>> iterator1 = geomContributions.entrySet()
          .iterator();
      iterator1.next();

      i = 1;
      while (iterator1.hasNext()) {
        Entry<OSHDBTimestamp, MappingMonth> next = iterator1.next();
        // identify events
        OSHDBTimestamp m_lag = (OSHDBTimestamp) geomContributions.keySet().toArray()[i - 1];
        Double error = (lagged_errors.get(next.getKey()) - mean) / std; // normalized error
        if (error > 1.644854) { // if error is positively significant at 95% - create event
          HashMap<Long, int[]> entity_edits = next.getValue().get_entity_edits();
          int sum_tags = 0;
          int sum_geom = 0;
          for (long k: entity_edits.keySet()) {
        	  sum_geom = sum_geom + entity_edits.get(k)[0];
        	  sum_tags = sum_tags + entity_edits.get(k)[1];
          }
          MappingEvent e = new MappingEvent(next.getKey(), next.getValue(),
              next.getValue().getUser_counts().size(),
              acc_result.get(next.getKey()) - acc_result.get(m_lag),
              ((float) (acc_result.get(next.getKey()) - (float) acc_result.get(m_lag))
              / (float) acc_result.get(m_lag)),
              Collections.max(next.getValue().getUser_counts().values()),
              coeffs,
              next.getValue().get_type_counts(),
              (float) sum_geom / entity_edits.size(),
              (float) sum_tags / entity_edits.size(),
              error);
          list.add(e);
        }
        i++;
      }
      out.put(geom, list); // add to list of events
    });

    return out;
  }

  private static Map<Integer, Polygon> getPolygons() throws IOException {
    //read geometries
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream is = classloader.getResourceAsStream("grid_20000_id.geojson");
    String json = IOUtils.toString(is);
    Map<Integer, Polygon> geometries = new HashMap<>();
    FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(json);
    GeometryFactory gf = new GeometryFactory();
    GeoJSONReader gjr = new GeoJSONReader();

    for (Feature f : featureCollection.getFeatures()) {
      geometries.put((Integer) f.getProperties().get("id"),
          gf.createPolygon(gjr.read(f.getGeometry()).getCoordinates()));
    }
    return geometries;
  }

}
