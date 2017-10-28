package com.totango.concurrency;

import java.util.concurrent.CountDownLatch;

/**
 * An asynchronous request which opens the given latch when it returns finishes.
 *
 * @author Aharon Levine
 * @since 2017-09-27
 */
public interface AsyncRequest<T> {

  /**
   * @param latch should be opened when the request returns
   * @param response the response object from the request should be set here
   */
  void run(CountDownLatch latch, AsyncResponse<T> response);

  /**
   * If possible this will be used to stop the request if it has already been received via another
   * thread. Note that this method might be called on all AsyncRequest objects at the end of the
   * request whether finished or not.
   */
  void abort();
}
