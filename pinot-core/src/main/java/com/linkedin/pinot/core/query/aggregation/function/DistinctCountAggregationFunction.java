package com.linkedin.pinot.core.query.aggregation.function;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.linkedin.pinot.common.data.FieldSpec.DataType;
import com.linkedin.pinot.common.request.AggregationInfo;
import com.linkedin.pinot.core.common.BlockValIterator;
import com.linkedin.pinot.core.indexsegment.IndexSegment;
import com.linkedin.pinot.core.indexsegment.columnar.readers.ColumnarReader;
import com.linkedin.pinot.core.query.aggregation.AggregationFunction;
import com.linkedin.pinot.core.query.aggregation.CombineLevel;
import com.linkedin.pinot.core.query.utils.IntArray;


public class DistinctCountAggregationFunction implements AggregationFunction<IntOpenHashSet, Integer> {

  private String _distinctCountColumnName;

  public DistinctCountAggregationFunction() {

  }

  @Override
  public void init(AggregationInfo aggregationInfo) {
    _distinctCountColumnName = aggregationInfo.getAggregationParams().get("column");
  }

  @Override
  public IntOpenHashSet aggregate(IntArray docIds, int docIdCount, IndexSegment indexSegment) {
    ColumnarReader columnarReader = indexSegment.getColumnarReader(_distinctCountColumnName);
    IntOpenHashSet intOpenHashSet = new IntOpenHashSet();
    for (int i = 0; i < docIdCount; i++) {
      intOpenHashSet.add(columnarReader.getRawValue(i).hashCode());
    }
    return intOpenHashSet;
  }

  @Override
  public IntOpenHashSet aggregate(IntOpenHashSet currentResult, int docId, IndexSegment indexSegment) {
    ColumnarReader columnarReader = indexSegment.getColumnarReader(_distinctCountColumnName);
    if (currentResult == null) {
      currentResult = new IntOpenHashSet();
    }
    currentResult.add(columnarReader.getRawValue(docId).hashCode());
    return currentResult;
  }

  @Override
  public List<IntOpenHashSet> combine(List<IntOpenHashSet> aggregationResultList, CombineLevel combineLevel) {
    if ((aggregationResultList == null) || aggregationResultList.isEmpty()) {
      return null;
    }
    IntOpenHashSet intOpenHashSet = aggregationResultList.get(0);
    for (int i = 1; i < aggregationResultList.size(); ++i) {
      intOpenHashSet.addAll(aggregationResultList.get(i));
    }
    aggregationResultList.clear();
    aggregationResultList.add(intOpenHashSet);
    return aggregationResultList;
  }

  @Override
  public IntOpenHashSet combineTwoValues(IntOpenHashSet aggregationResult0, IntOpenHashSet aggregationResult1) {
    if (aggregationResult0 == null) {
      return aggregationResult1;
    }
    if (aggregationResult1 == null) {
      return aggregationResult0;
    }
    aggregationResult0.addAll(aggregationResult1);
    return aggregationResult0;
  }

  @Override
  public Integer reduce(List<IntOpenHashSet> combinedResultList) {
    if ((combinedResultList == null) || combinedResultList.isEmpty()) {
      return 0;
    }
    IntOpenHashSet reducedResult = combinedResultList.get(0);
    for (int i = 1; i < combinedResultList.size(); ++i) {
      reducedResult.addAll(combinedResultList.get(i));
    }
    return reducedResult.size();
  }

  @Override
  public JSONObject render(Integer finalAggregationResult) {
    try {
      return new JSONObject().put("distinctCount", finalAggregationResult.toString());
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DataType aggregateResultDataType() {
    return DataType.OBJECT;
  }

  @Override
  public String getFunctionName() {
    return "distinctCount_" + _distinctCountColumnName;
  }

  @Override
  public IntOpenHashSet aggregate(BlockValIterator[] blockValIterators) {
    IntOpenHashSet ret = new IntOpenHashSet();
    while (blockValIterators[0].hasNext()) {
      ret.add(blockValIterators[0].nextIntVal());
    }
    return ret;
  }

  @Override
  public String getColumn() {
    return _distinctCountColumnName;
  }

  @Override
  public String[] getColumns() {
    return new String[] { _distinctCountColumnName };
  }

}