package org.heigit.bigspatialdata.eventfinder;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableFunction;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class MapFunk implements SerializableFunction<OSMContribution, Integer> {

  @Override
  public Integer apply(OSMContribution c) {
    int count = 0;
    if (c.getContributionTypes().contains(ContributionType.DELETION)) {
      count = count + 1; // a deletion is considered as one action
    } else if (c.getContributionTypes().contains(ContributionType.CREATION)) {
      count += c.getGeometryAfter().getCoordinates().length;
      Iterator<OSHDBTag> tags = c.getEntityAfter().getTags().iterator();
      while (tags.hasNext()) {
        tags.next();
        count++;
      }// each addition of a coordinate and of a tag is considered an action
    } else {
      // for modifications - each addition and deletion of coordinate or tag is considered an action
      //TODO: do we need geometries? They are resourceintensive to create
      List<Coordinate> c_aft = Arrays.asList(c.getGeometryAfter().getCoordinates());
      List<Coordinate> c_bef = Arrays.asList(c.getGeometryBefore().getCoordinates());
      if (!c_aft.equals(c_bef)) {
        int coord_additions = 0;
        for (Coordinate coord : c_aft) {
          if (!c_bef.contains(coord)) {
            coord_additions += 1;
          }
        }

        int coord_dels = 0;
        for (Coordinate coord : c_bef) {
          if (!c_aft.contains(coord)) {
            coord_dels += 1;
          }
        }

        count = count + coord_additions + coord_dels;
      }

      List<OSHDBTag> t_aft = new ArrayList<>();
      List<OSHDBTag> t_bef = new ArrayList<>();
      for (OSHDBTag i : c.getEntityAfter().getTags()) {
        t_aft.add(i);
      }
      for (OSHDBTag i : c.getEntityBefore().getTags()) {
        t_bef.add(i);
      }
      if (!t_aft.equals(t_bef)) {
        int tags_adds = 0;
        for (OSHDBTag tag : t_aft) {
          if (!t_bef.contains(tag)) {
            tags_adds += 1;
          }
        }

        int tags_dels = 0;
        for (OSHDBTag tag : t_bef) {
          if (!t_aft.contains(tag)) {
            tags_dels += 1;
          }
        }

        count = count + tags_dels + tags_adds;
      }
    }
    return count;
  }

}
