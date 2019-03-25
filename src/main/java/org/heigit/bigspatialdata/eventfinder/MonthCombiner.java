package org.heigit.bigspatialdata.eventfinder;

import java.util.HashMap;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableBinaryOperator;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;

public class MonthCombiner implements SerializableBinaryOperator<MappingMonth> {

  @Override
  public MappingMonth apply(MappingMonth arg0, MappingMonth arg1) {
    MappingMonth result = (new NewMapMonth()).get();

    result.set_contributions(arg0.get_contributions() + arg1.get_contributions());

    HashMap<Integer, Integer> userCounts = new HashMap<>();
    userCounts.putAll(arg0.getUser_counts());
    arg1.getUser_counts().forEach(
        (Integer ct, Integer cou) ->
        userCounts.merge(ct, cou, (v1, v2) -> v1 + v2)
    );
    result.setUser_counts(userCounts);

    HashMap<Long, int[]> entity_edits = new HashMap();
    entity_edits.putAll(arg0.get_entity_edits());
    arg1.get_entity_edits().forEach(
    		(Long ct, int[] cu) ->
    		entity_edits.merge(ct, cu, (v1, v2) -> arg0.pairwise_sum(v1, v2))
    );
    result.set_entity_edits(entity_edits);
    
    HashMap<ContributionType, Integer> contribs = new HashMap<>();
    contribs.putAll(arg0.get_type_counts());
    arg1.get_type_counts().forEach(
        (ContributionType ct, Integer cou) ->
        contribs.merge(ct, cou, (v1, v2) -> v1 + v2)
    );
    result.set_types_count(contribs);

    return result;
  }

}
