package com.totango.concurrency;

/**
 * Holds a value which can be set exactly once from several threads.
 *
 * @author Aharon Levine
 * @since 2017-09-27
 */
public class AsyncResponse<T> {
  private T val;
  private Exception exception;

  /**
   * @return true if this is the first try to set the response or false if it already exists in
   *         which case the operation has no effect.
   */
  public synchronized boolean setFirst(final T val) {
    if (this.val == null) {
      this.val = val;
      return true;
    }
    return false;
  }

  /**
   * An async method might throw an exception instead of returning a response.
   */
  public synchronized void setException(final Exception e) {
    this.exception = e;
  }

  public synchronized T retrieve() {
    if (this.val == null && exception != null) {
      throw new RuntimeException(exception);
    }
    return val;
  }

  public synchronized boolean received() {
    return val != null;
  }
}
