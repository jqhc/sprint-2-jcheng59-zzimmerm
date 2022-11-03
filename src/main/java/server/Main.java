package server;

import handlers.BadEndpointHandler;
import handlers.csv.CSVGetHandler;
import handlers.csv.CSVLoadHandler;
import handlers.csv.CSVStatsHandler;
import handlers.weather.WeatherHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import server.storage.Storage;

/** Starts the server with getCSV, loadCSV, and weather functionality */
public class Main {
  public static void main(String[] args) {

    Server server = new Server(3232);

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

    // adding bad endpoint
    server.addDatasource("*", new BadEndpointHandler());

    // start server
    server.start();
  }
}
