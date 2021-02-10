package org.heigit.bigspatialdata.eventfinder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class FileActions {
	
	public static void write_csv(String filename, SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> results, String end_date) 
			throws Exception {
		try (FileWriter file = new FileWriter(filename)) {
		    file.write(end_date+"\n");
			file.write("GeomID,date,contributions,users,max_cont,geometryActions,tagActions,creations,deletions,tagChanges,GeometryChanges\n");
			for (OSHDBCombinedIndex<Integer, OSHDBTimestamp> m:results.keySet()) {
				String l = "";
				if (results.get(m).get_contributions() > 0) {
					l = m.getFirstIndex().toString() + ","
							+ m.getSecondIndex().toString() + ","
							+ results.get(m).get_contributions().toString() + ","
							+ results.get(m).getUser_counts().size() + ","
							+ Collections.max(results.get(m).getUser_counts().values()) + ","
							+ results.get(m).get_edit_counts().get_GEOM() + ","
							+ results.get(m).get_edit_counts().get_TAG() + ","
							+ results.get(m).get_type_counts().get(ContributionType.CREATION) + ","
							+ results.get(m).get_type_counts().get(ContributionType.DELETION).toString() + ","
							+ results.get(m).get_type_counts().get(ContributionType.TAG_CHANGE).toString() + ","
							+ results.get(m).get_type_counts().get(ContributionType.GEOMETRY_CHANGE).toString() + "\n";
				} else {
					l = m.getFirstIndex().toString() + ","
							+ m.getSecondIndex().toString() + ","
							+ "0,0,0,0,0,0,0,0,0\n";
				}
				file.write(l);
			}
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
