package org.heigit.bigspatialdata.eventfinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBIgnite;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBJdbc;
import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.heigit.bigspatialdata.oshdb.util.time.OSHDBTimestamps;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Polygonal;
import org.slf4j.LoggerFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

public class EventFinder {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventFinder.class);

  public static void main(String[] args) throws Exception {

    LOG.info("Start preparation");
    
    // read properties file
    String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    String propertiesPath = rootPath + "oshdb.properties";
    
    Properties oshdbProperties = new Properties();
    oshdbProperties.load(new FileInputStream(propertiesPath));

    // configure OSHDB connection
    OSHDBDatabase oshdb;
    OSHDBJdbc keytables;
    if (oshdbProperties.getProperty("type").contains("H2")) {
      oshdb = (new OSHDBH2(oshdbProperties.getProperty("oshdb")))
          .multithreading(true)
          .inMemory(false);
      keytables = (OSHDBJdbc) oshdb;
    } else {
      oshdb = new OSHDBIgnite(EventFinder.class.getResource("/ignite-dev-ohsome-client.xml")
          .getFile());
      oshdb.prefix("global_v5");
      Connection conn = DriverManager.getConnection(
          "jdbc:postgresql://10.11.12.21:5432/keytables-global_b", "ohsome", args[0]);
      keytables = new OSHDBJdbc(conn);
    }
    
    String months_file = oshdbProperties.getProperty("months_file");
    
    Boolean produce = Boolean.valueOf(oshdbProperties.getProperty("produce"));
    
    String[] split = oshdbProperties.getProperty("bbox").split(",");

    OSHDBBoundingBox bb = new OSHDBBoundingBox(
        Double.valueOf(split[0]),
        Double.valueOf(split[1]),
        Double.valueOf(split[2]),
        Double.valueOf(split[3]));
    
    // get geometries for grid polygons
    Map<Integer, Polygon> polygons = EventFinder.getPolygons();
    
    
    SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> queryDatabase
        = new TreeMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth>();
    
    String end_date = oshdbProperties.getProperty("end_date");
    
    // get data by grid and month - contribution counts by type, contributions per user
    queryDatabase = EventFinder.queryDatabase(bb, oshdb, keytables, polygons, "2004-01-01", end_date);
    FileActions.write_csv(months_file, queryDatabase, end_date);
    
    oshdb.close();
  }

  public static SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> queryDatabase(
      OSHDBBoundingBox bb,
      OSHDBDatabase oshdb,
      OSHDBJdbc keytables,
      Map<Integer, Polygon> polygons,
      String startMonth,
      String endMonth)
      throws IOException, Exception {

    LOG.info("Run Query");

    StopWatch createStarted = StopWatch.createStarted();
    // collect data by polygon and month":
    // number of contributions by type, number of edits per user, number of edit operations
    SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> result = OSMContributionView
        .on(oshdb)
        .keytables(keytables)
        .areaOfInterest(bb)
        //Relations are excluded because they hold only little extra information and make this process very slow!
        .osmType(OSMType.NODE, OSMType.WAY)
        .timestamps(startMonth, endMonth, OSHDBTimestamps.Interval.MONTHLY)
        .aggregateByGeometry(polygons)
        .aggregateByTimestamp(OSMContribution::getTimestamp)
        .map(new MapFunk())
        .reduce(new NewMapMonth(), new MonthCombiner());

    createStarted.stop();
    double toMinutes = (createStarted.getTime() / 1000.0) / 60.0;

    LOG.info("Query Finished, took " + toMinutes + " minutes");
    return result;
  }

  private static Map<Integer, Polygon> getPolygons() throws IOException {
    LOG.info("Read Polygons");
    //read geometries
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream is = classloader.getResourceAsStream("grid_20000_id.geojson");

    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    br.lines().forEach(line -> sb.append(line));
    String geoJson = sb.toString();

    Map<Integer, Polygon> geometries = new HashMap<>();
    FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(geoJson);
    GeometryFactory gf = new GeometryFactory();
    GeoJSONReader gjr = new GeoJSONReader();

    for (Feature f : featureCollection.getFeatures()) {
      geometries.put((Integer) f.getProperties().get("id"),
          gf.createPolygon(gjr.read(f.getGeometry()).getCoordinates()));
    }
    return geometries;
  }

}
