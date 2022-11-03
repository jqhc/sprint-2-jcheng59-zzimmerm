package handlers;

import static handlers.weather.WeatherHandler.getAPIResponse;
import static handlers.weather.WeatherHandler.getForecast;
import static handlers.weather.WeatherHandler.getLocationResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.squareup.moshi.Moshi;
import handlers.weather.WeatherHandler.NWSHourlyForecast;
import handlers.weather.WeatherHandler.NWSHourlyProperties;
import handlers.weather.WeatherHandler.NWSPeriod;
import handlers.weather.WeatherHandler.NWSPoints;
import handlers.weather.WeatherHandler.NWSPointsProperties;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import org.eclipse.jetty.server.HttpTransport;
import org.junit.jupiter.api.Test;

public class TestWeather {
  private static final Moshi moshi = new Moshi.Builder().build();

  /** Tests for getAPIResponse */

  // tests that getAPIResponse() provides the correct error message for an invalid URI.
  @Test
  public void getAPIResponseInvalidURI()
      throws URISyntaxException, IOException, InterruptedException {
    HttpResponse<String> httpResponse = getAPIResponse("https://api.weather.gov/blablabla");

    NWSErrorResponse response = moshi.adapter(NWSErrorResponse.class).fromJson(httpResponse.body());

    assertEquals(404, response.status());
    assertEquals("'/blablabla' is not a valid resource path", response.detail());
  }

  // tests that getAPIResponse() throws an error on improperly formatted URIs.
  @Test
  public void getAPIResponseBadFormatURI() {
    assertThrows(URISyntaxException.class, () -> getAPIResponse("\n"));
    assertThrows(URISyntaxException.class, () -> getAPIResponse(" "));
  }

  // tests that getAPIResponse() provides the correct API data for a valid URI.
  @Test
  public void getAPIResponseSuccess() throws URISyntaxException, IOException, InterruptedException {
    HttpResponse<String> httpResponse1 =
        getAPIResponse("https://api.weather.gov/points/41.8252,-71.4189"); // Providence

    NWSPoints response1 = moshi.adapter(NWSPoints.class).fromJson(httpResponse1.body());

    assertEquals(
        "https://api.weather.gov/gridpoints/BOX/64,64/forecast/hourly",
        response1.properties().forecastHourly());

    HttpResponse<String> httpResponse2 =
        getAPIResponse("https://api.weather.gov/points/34.0522,-118.2437"); // Los Angeles

    NWSPoints response2 = moshi.adapter(NWSPoints.class).fromJson(httpResponse2.body());

    assertEquals(
        "https://api.weather.gov/gridpoints/LOX/154,44/forecast/hourly",
        response2.properties().forecastHourly());
  }

  /** Tests for getForecast() */
//  @Test
//  public void getForecastCorrectTemperature() throws Exception {
//    // defining a local class so we can create a nested helper method.
//    class TestHelper {
//      static void testForURI(String uri, float lat, float lon) throws Exception {
//        NWSPoints nwsPoints = new NWSPoints(new NWSPointsProperties(uri));
//
//        // checking temperature independently to compare
//        HttpResponse<String> weatherResponse = getAPIResponse(uri);
//        NWSHourlyForecast nwsHourlyForecast =
//            moshi.adapter(NWSHourlyForecast.class).fromJson(weatherResponse.body());
//        int temperature = nwsHourlyForecast.properties().periods()[0].temperature();
//
//        Object response = getForecast(nwsPoints, lat, lon);
//
//        assertEquals(
//            "{\"result\":\"success\",\"lat\":"
//                + lat
//                + ",\"lon\":"
//                + lon
//                + ",\""
//                + "temperature\":"
//                + temperature
//                + ",\"message\":\"The temperature at "
//                + lat
//                + ", "
//                + lon
//                + " is "
//                + temperature
//                + ".\"}",
//            response);
//      }
//    }
//
//    TestHelper.testForURI(
//        "https://api.weather.gov/gridpoints/SLC/100,174/forecast/hourly",
//        40.7587f,
//        -111.8762f); // Salt Lake City
//    TestHelper.testForURI(
//        "https://api.weather.gov/gridpoints/PQR/112,103/forecast/hourly",
//        45.5231f,
//        -122.6765f); // Portland, OR
//    TestHelper.testForURI(
//        "https://api.weather.gov/gridpoints/SEW/124,67/forecast/hourly",
//        47.608f,
//        -122.3352f); // Seattle
//  }

  // tests that getForecast correctly returns the temperature of the single period
  // in NWSHourlyProperties.
  @Test
  public void testGetForecastMocksSinglePeriod() throws Exception {
    NWSHourlyForecast hourlyForecast = new NWSHourlyForecast(
      new NWSHourlyProperties(
        new NWSPeriod[]{new NWSPeriod(60)}));

    assertEquals("{\"result\":\"success\",\"lat\":1.0,\"lon\":1.0,\"temperature\":20,"
        + "\"message\":\"The temperature at 1.0, 1.0 is 60.\"}",
      getForecast(hourlyForecast, 1.0f, 1.0f));

  }

  // tests that getForecast correctly returns the first period of NWSHourlyProperties.
  @Test
  public void testGetForecastMocksMultiplePeriods() throws Exception {
    NWSHourlyForecast hourlyForecast = new NWSHourlyForecast(
      new NWSHourlyProperties(
        new NWSPeriod[]{new NWSPeriod(20),
                        new NWSPeriod(10),
                        new NWSPeriod(30),
                        new NWSPeriod(40)}));

    assertEquals("{\"result\":\"success\",\"lat\":1.0,\"lon\":1.0,\"temperature\":20,"
      + "\"message\":\"The temperature at 1.0, 1.0 is 20.\"}",
      getForecast(hourlyForecast, 1.0f, 1.0f));

  }

  // checks that getForecast throws an ArrayIndexOutOfBoundsException when trying to access
  // a NWSHourlyProperties with an empty properties array.
  @Test
  public void testGetForecastMocksNoPeriods() throws Exception {
    NWSHourlyForecast hourlyForecast = new NWSHourlyForecast(
      new NWSHourlyProperties(
        new NWSPeriod[]{}));

    assertThrows(ArrayIndexOutOfBoundsException.class, () -> getForecast(hourlyForecast, 1.0f, 1.0f));
  }

  /** Tests for getLocationResponse() */

  // tests that getLocationResponse() gives an error message for invalid latitude and longitude
  // coordinates.
  @Test
  public void getLocationResponseInvalidCoords() throws Exception {
    // defining a local class so we can create a nested helper method.
    class TestHelper {
      static void testForLatLon(float lat, float lon) throws Exception {
        HttpResponse<String> httpResponse =
            getAPIResponse("https://api.weather.gov/points/" + lat + "," + lon);

        Object response = getLocationResponse(httpResponse, lat, lon);

        assertEquals(
            "{\"result\":\"error_bad_request\",\"lat\":"
                + lat
                + ",\"lon\":"
                + lon
                + ",\""
                + "message\":\"Invalid latitude and longitude coordinates. Latitude ranges from -90 to 90 "
                + "and longitude ranges from -180 to 180.\"}",
            response);
      }
    }

    TestHelper.testForLatLon(50, 12093);
    TestHelper.testForLatLon(12385, 30);
    TestHelper.testForLatLon(1000, 1000);
  }

  // tests that getLocationResponse() gives an error message for coordinates outside of the US.
  @Test
  public void getLocationResponseOutsideUSCoords() throws Exception {
    // defining a local class so we can create a nested helper method.
    class TestHelper {
      static void testForLatLon(float lat, float lon) throws Exception {
        HttpResponse<String> httpResponse =
            getAPIResponse("https://api.weather.gov/points/" + lat + "," + lon);

        Object response = getLocationResponse(httpResponse, lat, lon);

        assertEquals(
            "{\"result\":\"error_datasource\",\"lat\":"
                + lat
                + ",\"lon\":"
                + lon
                + ","
                + "\"message\":\"The National Weather Service could not provide weather information for "
                + "these coordinates. Try a location in the United States.\"}",
            response);
      }
    }

    TestHelper.testForLatLon(31.2244f, 121.4692f); // Shanghai
    TestHelper.testForLatLon(48.8647f, 2.349f); // Paris
    TestHelper.testForLatLon(-1.2864f, 36.8172f); // Nairobi
  }

  // tests that getLocationResponse() returns the correct temperature for valid locations in the US.
  @Test
  public void getLocationResponseSuccess() throws Exception {
    // defining a local class so we can create a nested helper method.
    class TestHelper {
      static void testForLatLon(float lat, float lon) throws Exception {
        HttpResponse<String> httpResponse =
            getAPIResponse("https://api.weather.gov/points/" + lat + "," + lon);

        // checking temperature independently to compare
        NWSPoints nwsPoints = moshi.adapter(NWSPoints.class).fromJson(httpResponse.body());
        HttpResponse<String> weatherResponse =
            getAPIResponse(nwsPoints.properties().forecastHourly());
        NWSHourlyForecast nwsHourlyForecast =
            moshi.adapter(NWSHourlyForecast.class).fromJson(weatherResponse.body());
        int temperature = nwsHourlyForecast.properties().periods()[0].temperature();

        Object response = getLocationResponse(httpResponse, lat, lon);

        assertEquals(
            "{\"result\":\"success\",\"lat\":"
                + lat
                + ",\"lon\":"
                + lon
                + ",\""
                + "temperature\":"
                + temperature
                + ",\"message\":\"The temperature at "
                + lat
                + ", "
                + lon
                + " is "
                + temperature
                + ".\"}",
            response);
      }
    }

    TestHelper.testForLatLon(41.8252f, -71.4189f); // Providence
    TestHelper.testForLatLon(40.7306f, -73.9352f); // New York
    TestHelper.testForLatLon(25.7616f, -80.1917f); // Miami
  }

  public record NWSErrorResponse(int status, String detail) {}
}
