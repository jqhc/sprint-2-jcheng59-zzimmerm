package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squareup.moshi.Moshi;
import handlers.BadEndpointHandler;
import handlers.BadEndpointHandler.BadEndpointResponse;
import handlers.csv.CSVGetHandler;
import handlers.csv.CSVLoadHandler;
import handlers.csv.CSVStatsHandler;
import handlers.weather.WeatherHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.storage.Storage;

public class TestServer {
  private static Server server;
  private static final Moshi moshi = new Moshi.Builder().build();

  /**
   * Helper function for sending an HTTP request to an API, and returning a deserialized request
   *
   * @param uri the URI that the request should be sent to.
   * @param type the class that the response should be deserialized into (using Moshi).
   * @param <T> see above.
   * @return an object of type T which stores the deserialized information from the HTTP response.
   */
  private static <T> T getDeserializedResponse(String uri, Class<T> type)
      throws URISyntaxException, IOException, InterruptedException {
    // creating request
    HttpRequest httpRequest = HttpRequest.newBuilder().uri(new URI(uri)).GET().build();

    // sending request and getting response
    HttpResponse<String> httpResponse =
        HttpClient.newBuilder().build().send(httpRequest, BodyHandlers.ofString());

    // deserializing response
    return moshi.adapter(type).fromJson(httpResponse.body());
  }

  // starts up the server at the port 3232.
  @BeforeAll
  public static void startServer() {
    server = new Server(3232);
  }

  // sets up the loadcsv, getcsv, weather, and bad endpoints of the server, then starts it.
  @BeforeEach
  public void setup() {
    // adding CSV functionality
    CSVLoadHandler csvLoadHandler = new CSVLoadHandler();
    CSVGetHandler csvGetHandler = new CSVGetHandler();
    CSVStatsHandler csvStatsHandler = new CSVStatsHandler();

    Storage<List<List<String>>> csvStorage = new Storage();

    server.addStorageDatasources(
      Map.of("loadcsv", csvLoadHandler, "getcsv", csvGetHandler, "statscsv", csvStatsHandler),
      csvStorage);

    // adding weather functionality
    server.addDatasource("weather", new WeatherHandler());

    // adding bad endpoint handling
    server.addDatasource("*", new BadEndpointHandler());

    // start server
    server.start();
  }

  // closes and resets the server after each test.
  @AfterEach
  public void teardown() {
    server.stop();
  }

  // Tests that the server gives an error message when an invalid endpoint is accessed.
  @Test
  public void testBadEndpoint() throws URISyntaxException, IOException, InterruptedException {
    // defining a local class so we can create a nested helper method.
    class TestHelper {
      static void testForFilepath(String filepath)
          throws URISyntaxException, IOException, InterruptedException {
        BadEndpointResponse response =
            getDeserializedResponse("http://localhost:3232/" + filepath, BadEndpointResponse.class);

        assertEquals("error_bad_json", response.result());
        assertEquals("'/" + filepath + "' is not a valid endpoint.", response.message());
      }
    }

    TestHelper.testForFilepath("");
    TestHelper.testForFilepath("3232");
    TestHelper.testForFilepath("1902310239");
    TestHelper.testForFilepath("laodcsv");
  }

  /** TESTS FOR LOADCSV */

  // Tests that loadcsv gives an error message when a filepath isn't provided
  @Test
  public void testLoadCSVNoFilepath() throws URISyntaxException, IOException, InterruptedException {
    LoadCSVErrorResponse response =
        getDeserializedResponse("http://localhost:3232/loadcsv", LoadCSVErrorResponse.class);

    assertEquals("error_bad_request", response.result());
    assertEquals(
        "No filepath provided. Specify filepath by adding '?filepath=[insert filepath]' "
            + "to the end of the URL.",
        response.message());
  }

  // Tests that loadcsv gives an error message for an invalid filepath
  @Test
  public void testLoadCSVInvalidFilepath()
      throws URISyntaxException, IOException, InterruptedException {
    // defining a local class so we can create a nested helper method.
    class TestHelper {
      static void testForFilepath(String filepath)
          throws URISyntaxException, IOException, InterruptedException {
        LoadCSVErrorResponse response =
            getDeserializedResponse(
                "http://localhost:3232/loadcsv?filepath=" + filepath, LoadCSVErrorResponse.class);

        assertEquals("error_datasource", response.result());
        assertEquals(filepath, response.filepath());
        assertEquals("File '" + filepath + "' not found.", response.message());
      }
    }

    TestHelper.testForFilepath("");
    TestHelper.testForFilepath("alskdjfodasldkf");
    TestHelper.testForFilepath("bla/bla/bla.csv");
    TestHelper.testForFilepath("testData/bla.csv");
  }

  // Tests that loadcsv works for valid filepaths
  @Test
  public void testLoadCSVSuccess() throws URISyntaxException, IOException, InterruptedException {
    // defining a local class so we can create a nested helper method.
    class TestHelper {
      static void testForFilepath(String filepath)
          throws URISyntaxException, IOException, InterruptedException {
        LoadCSVSuccessResponse response =
            getDeserializedResponse(
                "http://localhost:3232/loadcsv?filepath=" + filepath, LoadCSVSuccessResponse.class);

        assertEquals("success", response.result());
        assertEquals(filepath, response.filepath());
        assertEquals(
            "Successfully loaded '"
                + filepath
                + "'! Access the endpoint "
                + "'getcsv' to get the contents of the file.",
            response.message());
      }
    }

    TestHelper.testForFilepath("testData/abc.csv");
    TestHelper.testForFilepath("testData/fruits-long.csv");
    TestHelper.testForFilepath("testData/numbers.csv");
  }

  /** TESTS FOR GETCSV */

  // Tests that getcsv gives an error response when a file hasn't been loaded
  @Test
  public void testGetCSVNotLoaded() throws Exception {
    GetCSVErrorResponse response =
        getDeserializedResponse("http://localhost:3232/getcsv", GetCSVErrorResponse.class);

    assertEquals("error_datasource", response.result());
    assertEquals(
        "You haven't loaded a file yet! To do so, access the endpoint 'loadcsv' "
            + "and provide a filepath as a parameter.",
        response.message());
  }

  // Testing getcsv on an empty CSV file
  @Test
  public void testGetCSVBlank() throws URISyntaxException, IOException, InterruptedException {
    getDeserializedResponse(
        "http://localhost:3232/loadcsv?filepath=testdata/empty.csv", LoadCSVSuccessResponse.class);

    GetCSVSuccessResponse response =
        getDeserializedResponse("http://localhost:3232/getcsv", GetCSVSuccessResponse.class);

    assertEquals("success", response.result());
    assertEquals(new ArrayList<String>(), response.data());
    assertEquals("Successfully retrieved CSV.", response.message());
  }

  // tests getcsv on a basic csv of repeated strings.
  @Test
  public void testGetCSVBasicCSV() throws URISyntaxException, IOException, InterruptedException {
    getDeserializedResponse(
        "http://localhost:3232/loadcsv?filepath=testdata/abc.csv", LoadCSVSuccessResponse.class);

    GetCSVSuccessResponse response =
        getDeserializedResponse("http://localhost:3232/getcsv", GetCSVSuccessResponse.class);

    assertEquals("success", response.result());

    ArrayList<String> row = new ArrayList(Arrays.asList("abc", "abc", "abc", "abc"));
    ArrayList<ArrayList<String>> csv = new ArrayList();
    csv.add(row);
    csv.add(row);
    csv.add(row);
    assertEquals(csv, response.data());
    assertEquals("Successfully retrieved CSV.", response.message());
  }

  // tests getcsv on a basic csv of numbers
  @Test
  public void testGetCSVNumverCSV() throws URISyntaxException, IOException, InterruptedException {
    getDeserializedResponse(
        "http://localhost:3232/loadcsv?filepath=testdata/numbers.csv",
        LoadCSVSuccessResponse.class);

    GetCSVSuccessResponse response =
        getDeserializedResponse("http://localhost:3232/getcsv", GetCSVSuccessResponse.class);

    assertEquals("success", response.result());

    ArrayList<ArrayList<String>> csv =
        new ArrayList(
            List.of(
                Arrays.asList("12", "34", "858", "2482"),
                Arrays.asList("239", "283", "1848", "585"),
                Arrays.asList("1828", "48", "84", "282"),
                Arrays.asList("818", "490", "4058", "3823"),
                Arrays.asList("0942", "1823", "848", "238")));
    assertEquals(csv, response.data());
    assertEquals("Successfully retrieved CSV.", response.message());
  }

  @Test
  public void testGetCSVLongCSV() throws URISyntaxException, IOException, InterruptedException {
    getDeserializedResponse(
        "http://localhost:3232/loadcsv?filepath=testdata/fruits-long.csv",
        LoadCSVSuccessResponse.class);

    GetCSVSuccessResponse response =
        getDeserializedResponse("http://localhost:3232/getcsv", GetCSVSuccessResponse.class);

    assertEquals("success", response.result());

    ArrayList<ArrayList<String>> csv =
        new ArrayList(
            List.of(
                Arrays.asList("apple", "orange", "banana", "pear", "grape"),
                Arrays.asList(
                    "grapefruit", "cherry", "blood orange", "kumquat", "african cherry orange"),
                Arrays.asList("lemon", "mango", "durian", "honeydew", "fig"),
                Arrays.asList("strawberry", "blueberry", "dragonfruit", "cantaloupe", "gooseberry"),
                Arrays.asList("blueberry", "kiwi", "jackfruit", "goji berry", "plum"),
                Arrays.asList("watermelon", "coconut", "date", "apricot", "lime")));

    assertEquals(csv, response.data());
    assertEquals("Successfully retrieved CSV.", response.message());
  }

  /** TESTS FOR WEATHER */
  @Test
  public void testValidWeather() throws URISyntaxException, IOException, InterruptedException {
    WeatherSuccessResponse response =
        getDeserializedResponse(
            "http://localhost:3232/weather?lat=41.8268&lon=-71.4029", WeatherSuccessResponse.class);

    assertEquals("success", response.result());
    assertEquals(41.8268, response.lat(), 0.01);
    assertEquals(-71.4029, response.lon(), 0.01);
    assertTrue(response.temperature() > -50);
    assertTrue(response.temperature() < 150);
    assertEquals(
        "The temperature at 41.8268, -71.4029 is " + response.temperature() + ".",
        response.message());
  }

  @Test
  public void testNoCoordinates() throws URISyntaxException, IOException, InterruptedException {
    WeatherErrorResponse response =
        getDeserializedResponse("http://localhost:3232/weather", WeatherErrorResponse.class);

    assertEquals("error_bad_request", response.result());
    assertEquals(
        "No latitude or longitude coordinate provided. "
            + "Specify by adding '?lat=[latitude]&lon=[longitude]' to the end of the URL.",
        response.message());
  }

  @Test
  public void testNonUSCoordinates() throws URISyntaxException, IOException, InterruptedException {
    WeatherWithLatLonErrorResponse response =
        getDeserializedResponse(
            "http://localhost:3232/weather?lat=51.5072&lon=0.1276",
            WeatherWithLatLonErrorResponse.class);

    assertEquals("error_datasource", response.result());
    assertEquals(
        "The National Weather Service could not provide weather information "
            + "for these coordinates. Try a location in the United States.",
        response.message());
  }

  @Test
  public void testInvalidCoordinates()
      throws URISyntaxException, IOException, InterruptedException {
    WeatherCoordsNotFloats response =
        getDeserializedResponse(
            "http://localhost:3232/weather?lat=abc&lon=def", WeatherCoordsNotFloats.class);

    assertEquals("error_bad_request", response.result());
    assertEquals("Latitude and longitude values should be numbers.", response.message());
  }

  public record LoadCSVErrorResponse(String result, String filepath, String message) {}

  public record LoadCSVSuccessResponse(String result, String filepath, String message) {}

  public record GetCSVErrorResponse(String result, String message) {}

  public record GetCSVSuccessResponse(String result, List<List<String>> data, String message) {}

  public record WeatherErrorResponse(String result, String message) {}

  public record WeatherWithLatLonErrorResponse(
      String result, float lat, float lon, String message) {}

  public record WeatherSuccessResponse(
      String result, float lat, float lon, int temperature, String message) {}

  public record WeatherCoordsNotFloats(String result, String lat, String lon, String message) {}
}
