package org.heigit.bigspatialdata.eventfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableFunction;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.locationtech.jts.geom.Coordinate;

public class MapFunk implements SerializableFunction<OSMContribution, MappingMonth> {

  @Override
  public MappingMonth apply(OSMContribution c) {
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
    HashMap<Integer, Integer> users_conts = new HashMap<Integer, Integer>();
    HashMap<ContributionType, Integer> type_counts = new HashMap<ContributionType, Integer>();
    type_counts.put(ContributionType.CREATION, 0);
    type_counts.put(ContributionType.DELETION, 0);
    type_counts.put(ContributionType.GEOMETRY_CHANGE, 0);
    type_counts.put(ContributionType.TAG_CHANGE, 0);
    Integer user = c.getContributorUserId();
    if (!users_conts.containsKey(user)) {
      users_conts.put(user, 1);
    } else {
      users_conts.put(user, users_conts.get(user) + 1);
    }

    if (c.getContributionTypes().contains(ContributionType.CREATION)) {
      type_counts.put(ContributionType.CREATION,
          type_counts.get(ContributionType.CREATION) + 1);
    }
    if (c.getContributionTypes().contains(ContributionType.DELETION)) {
      type_counts.put(ContributionType.DELETION,
          type_counts.get(ContributionType.DELETION) + 1);
    }
    if (c.getContributionTypes().contains(ContributionType.GEOMETRY_CHANGE)) {
      type_counts.put(ContributionType.GEOMETRY_CHANGE, type_counts.get(
          ContributionType.GEOMETRY_CHANGE) + 1);
    }
    if (c.getContributionTypes().contains(ContributionType.TAG_CHANGE)) {
      type_counts.put(ContributionType.TAG_CHANGE, type_counts.get(
          ContributionType.TAG_CHANGE) + 1);
    }

    MappingMonth result = new MappingMonth(count, users_conts, type_counts);
    return result;
  }

}
