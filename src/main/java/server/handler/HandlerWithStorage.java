package server.handler;

import server.storage.Storage;
import spark.Route;

/**
 * An interface for a handler that has a storage object, implemented by both CSVGetHandler and
 * CSVLoadHandler
 *
 * @param <T>
 */
public interface HandlerWithStorage<T> extends Route {
  public void setStorage(Storage<T> storage);
}
