package server.storage;

/**
 * A class accessed by CSVGetHandler and CSVLoadHandler that stores loaded CSV data
 *
 * @param <T>
 */
public class Storage<T> {
  T data;

  public T getData() throws FileNotLoadedException {
    if (data == null) {
      throw new FileNotLoadedException();
    }
    return this.data;
  }

  public void loadData(T data) {
    this.data = data;
  }
}
