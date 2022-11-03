package handlers.csv;

import com.squareup.moshi.Moshi;
import java.util.List;
import server.handler.HandlerWithStorage;
import server.storage.FileNotLoadedException;
import server.storage.Storage;
import spark.Request;
import spark.Response;

public class CSVGetHandler implements HandlerWithStorage<List<List<String>>> {

  Storage<List<List<String>>> storage;

  public CSVGetHandler() {}

  /**
   * Handles requests for getting a CSV file. Gets data from storage object
   *
   * @param request
   * @param response
   * @return a response with the CSV contents
   * @throws Exception
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    List<List<String>> csv2d;
    try {
      csv2d = this.storage.getData();
      return new GetFileResponse(csv2d).serialize();
    } catch (FileNotLoadedException e) {
      return new FileNotLoadedResponse().serialize();
    }
  }

  /**
   * Setter for handler's storage object
   *
   * @param storage
   */
  @Override
  public void setStorage(Storage<List<List<String>>> storage) {
    this.storage = storage;
  }

  /*
  Response messages for errors or success
   */
  // response for when a file is successfully retrieved
  public record GetFileResponse(String result, List<List<String>> data, String message) {
    public GetFileResponse(List<List<String>> csv2d) {
      this("success", csv2d, "Successfully retrieved CSV.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(GetFileResponse.class).toJson(this);
    }
  }

  // error response for when a file has not been loaded yet
  public record FileNotLoadedResponse(String result, String message) {
    public FileNotLoadedResponse() {
      this(
          "error_datasource",
          "You haven't loaded a file yet!");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(FileNotLoadedResponse.class).toJson(this);
    }
  }
}
