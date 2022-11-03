package handlers.weather;

import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import spark.Request;
import spark.Response;
import spark.Route;

public class WeatherHandler implements Route {

  /**
   * Handles a weather request. Checks that input is valid and passes input to returnResponse
   *
   * @param request
   * @param response
   * @return the result from passing the coordinates to returnReponse
   * @throws Exception
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    String lat = request.queryParams("lat");
    String lon = request.queryParams("lon");

    // if fields not provided, show error message
    if (lat == null || lon == null) {
      return new NoLatLonResponse().serialize();
    }

    // checks that both lat and lon are valid floats
    float latFloat;
    float lonFloat;
    try {
      latFloat = Float.parseFloat(lat);
      lonFloat = Float.parseFloat(lon);
    } catch (NumberFormatException e) {
      return new LatLonNotFloatsResponse(lat, lon).serialize();
    }

    // getting response for point
    HttpResponse<String> locationResponse =
        getAPIResponse("https://api.weather.gov/points/" + lat + "," + lon);

    int statusCode = locationResponse.statusCode();

    if (statusCode == 400) { // invalid latitude and longitude coordinates
      return new InvalidCoordsResponse(latFloat, lonFloat).serialize();

    } else if (statusCode == 404) { // NWS doesn't have data for specified coordinates
      return new NWSNoDataForCoordsResponse(latFloat, lonFloat).serialize();

    } else if (statusCode == 200) {
      return getLocationResponse(locationResponse, latFloat, lonFloat);

    } else { // unknown error (likely code 500)
      return new UnspecifiedNWSErrorResponse(latFloat, lonFloat).serialize();
  }
  }

  public static void main(String[] args) throws Exception {
    HttpResponse<String> locationResponse =
      getAPIResponse("https://api.weather.gov/points/20,-80");

    int statusCode = locationResponse.statusCode();
    System.out.println(statusCode);

    // parse body into NWSPoints with moshi
    Moshi moshi = new Moshi.Builder().build();
    NWSPoints nwsPoints = moshi.adapter(NWSPoints.class).fromJson(locationResponse.body());

    // get temperature from hourly forecast
    HttpResponse<String> weatherResponse = getAPIResponse(nwsPoints.properties().forecastHourly());
  }

  /**
   * Gets the location information for inputted coordinates. Passes data to getForecast
   *
   * @param httpResponse the HTTP response from NWS's API
   * @param lat
   * @param lon
   * @return Error responses for error codes 400 and 404. Otherwise, result from getForecast
   * @throws Exception
   */
  public static String getLocationResponse(HttpResponse<String> httpResponse, float lat, float lon)
      throws Exception {
    // parse body into NWSPoints with moshi
    Moshi moshi = new Moshi.Builder().build();
    NWSPoints nwsPoints = moshi.adapter(NWSPoints.class).fromJson(httpResponse.body());

    if (nwsPoints.properties().forecastHourly() == null) {
      return new UnspecifiedNWSErrorResponse(lat, lon).serialize();
    }

    // get temperature from hourly forecast
    HttpResponse<String> weatherResponse = getAPIResponse(nwsPoints.properties().forecastHourly());

    int statusCode = weatherResponse.statusCode();
    if (statusCode == 500) {
      return new UnspecifiedNWSErrorResponse(lat, lon).serialize();
    } else if (statusCode == 404) {
      return new MarineCoordsResponse(lat, lon).serialize();
    }
    // parse body into NWSHourlyForecast with moshi
    NWSHourlyForecast nwsHourlyForecast =
      moshi.adapter(NWSHourlyForecast.class).fromJson(weatherResponse.body());

    return getForecast(nwsHourlyForecast, lat, lon);
  }

  /**
   * Gets the next hour's temperature from NWS location data
   *
   * @param nwsHourlyForecast
   * @param lat
   * @param lon
   * @return The next hour's temperature from the hourly forecast
   * @throws Exception
   */
  public static String getForecast(NWSHourlyForecast nwsHourlyForecast, float lat, float lon)
    throws Exception {
    // return the temperature
    int temperature = nwsHourlyForecast.properties().periods()[0].temperature();
    return new SuccessWeatherResponse(lat, lon, temperature).serialize();
  }

  public static HttpResponse<String> getAPIResponse(String uri)
      throws URISyntaxException, IOException, InterruptedException {
    // creating request
    HttpRequest httpRequest = HttpRequest.newBuilder().uri(new URI(uri)).GET().build();

    // sending request and getting response
    return HttpClient.newBuilder().build().send(httpRequest, BodyHandlers.ofString());
  }

  public record NWSPoints(NWSPointsProperties properties) {}

  public record NWSPointsProperties(String forecastHourly) {}

  public record NWSHourlyForecast(NWSHourlyProperties properties) {}

  public record NWSHourlyProperties(NWSPeriod[] periods) {}

  public record NWSPeriod(int temperature) {}

  // responses
  // response for successful weather retrieval
  public record SuccessWeatherResponse(
      String result, float lat, float lon, int temperature, String message) {
    public SuccessWeatherResponse(float lat, float lon, int temperature) {
      this(
          "success",
          lat,
          lon,
          temperature,
          "The temperature at " + lat + ", " + lon + " is " + temperature + "\u00B0F.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(SuccessWeatherResponse.class).toJson(this);
    }
  }

  // error response for unknown NWS errors (usually code 500)
  public record UnspecifiedNWSErrorResponse(String result, float lat, float lon, String message) {
    public UnspecifiedNWSErrorResponse(float lat, float lon) {
      this(
          "error_datasource",
          lat,
          lon,
          "An unknown error occurred with NWS. Please try again later.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(UnspecifiedNWSErrorResponse.class).toJson(this);
    }
  }

  // error response for coordinates outside the US
  public record NWSNoDataForCoordsResponse(String result, float lat, float lon, String message) {
    public NWSNoDataForCoordsResponse(float lat, float lon) {
      this(
          "error_datasource",
          lat,
          lon,
          "The National Weather Service could not provide"
              + " weather information for these coordinates. Try a location in the United States.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(NWSNoDataForCoordsResponse.class).toJson(this);
    }
  }

  // error response for invalid latitude and longitude coordinates
  public record MarineCoordsResponse(String result, float lat, float lon, String message) {
    public MarineCoordsResponse(float lat, float lon) {
      this(
        "error_bad_request",
        lat,
        lon,
        "The National Weather Service does not provide weather information for marine "
          + "coordinates. Please choose land coordinates.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(MarineCoordsResponse.class).toJson(this);
    }
  }

  // error response for invalid latitude and longitude coordinates
  public record InvalidCoordsResponse(String result, float lat, float lon, String message) {
    public InvalidCoordsResponse(float lat, float lon) {
      this(
          "error_bad_request",
          lat,
          lon,
          "Invalid latitude and longitude "
              + "coordinates. Latitude ranges from -90 to 90 and longitude ranges from -180 to 180.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(InvalidCoordsResponse.class).toJson(this);
    }
  }

  // error response for when latitude and longitude cannot be parsed into floats
  public record LatLonNotFloatsResponse(String result, String lat, String lon, String message) {
    public LatLonNotFloatsResponse(String lat, String lon) {
      this("error_bad_request", lat, lon, "Latitude and longitude values " + "should be numbers.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(LatLonNotFloatsResponse.class).toJson(this);
    }
  }

  // error response for when latitude and/or longitude parameters are not provided
  public record NoLatLonResponse(String result, String message) {
    public NoLatLonResponse() {
      this(
          "error_bad_request",
          "No latitude and/or longitude coordinates provided.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(NoLatLonResponse.class).toJson(this);
    }
  }
}
