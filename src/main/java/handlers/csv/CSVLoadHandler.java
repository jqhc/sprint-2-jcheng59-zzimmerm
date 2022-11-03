package handlers.csv;

import com.squareup.moshi.Moshi;
import csv.ParseBot;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import server.handler.HandlerWithStorage;
import server.storage.Storage;
import spark.Request;
import spark.Response;

public class CSVLoadHandler implements HandlerWithStorage<List<List<String>>> {

  Storage<List<List<String>>> storage;

  public CSVLoadHandler() {}

  /**
   * Handles request for loading a CSV file.
   *
   * @param request
   * @param response
   * @return Appropriate response for errors or success
   * @throws Exception
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    String filepath = request.queryParams("filepath");
    // if no filepath is provided, show error message
    if (filepath == null) {
      return new NoFilepathResponse().serialize();
    }

    // if file is not found, show error message
    FileReader reader;
    try {
      reader = new FileReader(filepath);
    } catch (FileNotFoundException e) {
      return new InvalidFilepathResponse(filepath).serialize();
    }

    ParseBot parser = new ParseBot(reader, new RawCreator(), false);
    List<List<String>> csv2d;
    // If the BufferedReader throws an IOException while reading the file,
    // show error message
    try {
      csv2d = parser.parse();
    } catch (IOException e) {
      return new ErrorReadingFileResponse(filepath).serialize();
    }

    this.storage.loadData(csv2d);
    return new SuccessReadingFileResponse(filepath).serialize();
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
  // response for successful loading of file
  public record SuccessReadingFileResponse(String result, String filepath, String message) {
    public SuccessReadingFileResponse(String filepath) {
      this(
          "success",
          filepath,
          "Successfully loaded '"
              + filepath
              + "'! Access the endpoint "
              + "'getcsv' to get the contents of the file.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(SuccessReadingFileResponse.class).toJson(this);
    }
  }

  // error response for when there is an error reading the file (from the BufferedReader)
  public record ErrorReadingFileResponse(String result, String filepath, String message) {
    public ErrorReadingFileResponse(String filepath) {
      this(
          "error_datasource",
          filepath,
          "There was an error reading '" + filepath + "'. Try again!");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(ErrorReadingFileResponse.class).toJson(this);
    }
  }

  // error response for when the specified file does not exist
  public record InvalidFilepathResponse(String result, String filepath, String message) {
    public InvalidFilepathResponse(String filepath) {
      this("error_datasource", filepath, "File '" + filepath + "' not found.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(InvalidFilepathResponse.class).toJson(this);
    }
  }

  // error response for when no filepath parameter is provided
  public record NoFilepathResponse(String result, String message) {
    public NoFilepathResponse() {
      this(
          "error_bad_request",
          "No filepath provided.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(NoFilepathResponse.class).toJson(this);
    }
  }
}
