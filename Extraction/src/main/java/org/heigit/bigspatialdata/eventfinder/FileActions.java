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
	
	public static QueryOutput read_csv(String filename) 
			throws NumberFormatException, ParseException {
		SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> results = 
				new TreeMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth>();
		
		BufferedReader reader = null;
		String start_date = "";
		try {
			reader = new BufferedReader(new FileReader(filename));
			start_date = reader.readLine().split(",")[0];
			String line = reader.readLine();
			
			while ((line = reader.readLine()) != null) {
				String[] entry = line.split(",");
				
				Date date = Date.from(LocalDateTime.parse(entry[1]).atOffset(ZoneOffset.of("-00:00")).toInstant());
				
				OSHDBCombinedIndex<Integer, OSHDBTimestamp> m = new OSHDBCombinedIndex<Integer, OSHDBTimestamp>(
						Integer.parseInt(entry[0]),
						new OSHDBTimestamp(date));
				
				HashMap<ContributionType, Integer> type_counts = new HashMap<ContributionType, Integer>();
				type_counts.put(ContributionType.CREATION, Integer.parseInt(entry[7]));
				type_counts.put(ContributionType.DELETION, Integer.parseInt(entry[8]));
				type_counts.put(ContributionType.TAG_CHANGE, Integer.parseInt(entry[9]));
				type_counts.put(ContributionType.GEOMETRY_CHANGE, Integer.parseInt(entry[10]));
				
				MappingMonth month = new MappingMonth(
						Integer.parseInt(entry[2]), 
						new HashMap<Integer, Integer>(), 
						type_counts, 
						new EditCountEnum(Integer.parseInt(entry[5]), Integer.parseInt(entry[6])), 
						Integer.parseInt(entry[3]), 
						Integer.parseInt(entry[4]));
				
				results.put(m, month);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
		}
		
		return new QueryOutput(results, start_date);
	}
	
	public static void append_csv(String filename, SortedMap<OSHDBCombinedIndex<Integer, OSHDBTimestamp>, MappingMonth> results,
			String end_date) throws FileNotFoundException {
		try {
			List<String> lines = Files.readAllLines(Paths.get(filename));
			lines.set(0, end_date);
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
							+ results.get(m).get_type_counts().get(ContributionType.GEOMETRY_CHANGE).toString();
				} else {
					l = m.getFirstIndex().toString() + ","
							+ m.getSecondIndex().toString() + ","
							+ "0,0,0,0,0,0,0,0,0";
				}
				lines.add(l);
			}
			Files.write(Paths.get(filename), lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
