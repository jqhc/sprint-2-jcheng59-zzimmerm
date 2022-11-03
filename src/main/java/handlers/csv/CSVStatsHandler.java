package handlers.csv;

import com.squareup.moshi.Moshi;
import handlers.csv.CSVGetHandler.FileNotLoadedResponse;
import java.util.List;
import server.handler.HandlerWithStorage;
import server.storage.FileNotLoadedException;
import server.storage.Storage;
import spark.Request;
import spark.Response;

public class CSVStatsHandler implements HandlerWithStorage<List<List<String>>> {

  Storage<List<List<String>>> storage;

  @Override
  public Object handle(Request request, Response response) throws Exception {
    List<List<String>> csv2d;
    try {
      csv2d = this.storage.getData();
    } catch (FileNotLoadedException e) {
      return new FileNotLoadedResponse().serialize();
    }

    int rows = csv2d.size();
    int cols = (rows == 0) ? 0 : csv2d.get(0).size();

    return new StatsFileResponse(rows, cols).serialize();
  }

  @Override
  public void setStorage(Storage<List<List<String>>> storage) {
    this.storage = storage;
  }

  // response for when a file is successfully retrieved
  public record StatsFileResponse(String result, int rows, int cols, String message) {
    public StatsFileResponse(int rows, int cols) {
      this("success", rows, cols, rows + " rows, " + cols + " columns.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(StatsFileResponse.class).toJson(this);
    }
  }
}
