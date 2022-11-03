package handlers.csv;

import csv.creator.CreatorFromRow;
import java.util.List;

/**
 * A class that takes CSV rows as Lists of Strings, and returns them, unaltered.
 */
public class RawCreator implements CreatorFromRow<List<String>> {

  /**
   * Given a row as a List of Strings, returns it.
   * @param row a row of a CSV, represented as a List of Strings
   * @return row, unchanged.
   */
  @Override
  public List<String> create(List<String> row) {
    return row;
  }
}
