package server;

import static spark.Spark.after;

import java.util.ArrayList;
import java.util.Map;
import server.handler.HandlerWithStorage;
import server.storage.Storage;
import spark.Route;
import spark.Spark;

/** A class representing the server. */
public class Server {
  private final int portID;
  private ArrayList<String> endpoints;

  public Server(int portID) {
    this.endpoints = new ArrayList<>();
    this.portID = portID;
    Spark.port(portID);

    after(
        (request, response) -> {
          response.header("Access-Control-Allow-Origin", "*");
          response.header("Access-Control-Allow-Methods", "*");
        });
  }

  /**
   * Adds multiple storage-holding datasources to the endpoints of the server,
   * each sharing one storage.
   *
   * @param handlers a map from strings (the endpoint names) to the handler
   *                 objects for each string.
   * @param storage a storage object for communication between the handlers
   * @param <S>
   */
  public <S> void addStorageDatasources(Map<String, HandlerWithStorage<S>> handlers, Storage<S> storage) {
    // set unified storage for all the handlers
    for (HandlerWithStorage<S> handler : handlers.values()) {
      handler.setStorage(storage);
    }
    // creates load and get endpoints
    for (String endpointName : handlers.keySet()) {
      Spark.get(endpointName, handlers.get(endpointName));
      this.endpoints.add(endpointName);
    }
  }

  /**
   * Adds multiple datasources to the endpoints of the server
   *
   * @param handlers a map from strings (the endpoint names) to the handler
   *                 objects for each string.
   */
  public void addDatasources(Map<String, Route> handlers) {
    for (String endpointName : handlers.keySet()) {
      Spark.get(endpointName, handlers.get(endpointName));
      this.endpoints.add(endpointName);
    }
  }

  /**
   * Adds a datasource to the endpoints of the server
   *
   * @param endpointName
   * @param handler
   */
  public void addDatasource(String endpointName, Route handler) {
    Spark.get(endpointName, handler);
    this.endpoints.add(endpointName);
  }

  /** Starts the server */
  public void start() {
    Spark.init();
    Spark.awaitInitialization();

    System.out.println("Server started.");
  }

  /** Ends the server */
  public void stop() {
    for (String endpoint : this.endpoints) {
      Spark.unmap(endpoint);
      Spark.stop();
      Spark.awaitStop();
    }
  }
}
