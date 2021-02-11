package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableSupplier;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class NewMapMonth implements SerializableSupplier<MappingMonth> {

  @Override
  public MappingMonth get() {
    return new MappingMonth(0, new HashMap<Integer, Integer>(),
        new HashMap<ContributionType, Integer>(), new EditCountEnum(0, 0), 
        0, 0);
  }

}
