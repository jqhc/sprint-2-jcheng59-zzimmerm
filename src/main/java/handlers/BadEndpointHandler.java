package handlers;

import com.squareup.moshi.Moshi;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * A class for handling invalid endpoints to the API server.
 */
public class BadEndpointHandler implements Route {

  /**
   * Handles invalid requests.
   * @param request
   * @param response
   * @return a serialized response describing that the endpoint is not valid.
   * @throws Exception
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    return new BadEndpointResponse(request.uri()).serialize();
  }

  // error response describing that the endpoint is invalid.
  public record BadEndpointResponse(String result, String message) {
    public BadEndpointResponse(String endpoint) {
      this("error_bad_json", "'" + endpoint + "' is not a valid endpoint.");
    }

    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(BadEndpointResponse.class).toJson(this);
    }
  }
}
