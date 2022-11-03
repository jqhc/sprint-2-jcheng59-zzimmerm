package server.storage;

/**
 * An exception for Storage.getData, for when data hasn't been loaded to the Storage yet
 */
public class FileNotLoadedException extends Exception {
  public FileNotLoadedException() {}
}
