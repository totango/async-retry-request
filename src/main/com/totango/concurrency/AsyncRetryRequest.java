package com.totango.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This async request is useful for scenarios where we need a response from one of several resources
 * in the shortest possible time but don't want to query more than one of them if we don't need to.
 * First one of the resources is queried, if a response is not received before a given timeout then
 * the second resource is queried as well. Once both resources have been queried we wait for the
 * first response from either of them.
 *
 * @author Aharon Levine
 * @since 2017-09-27
 */
public class AsyncRetryRequest<T> {
  private final AsyncRequest<T> initialRun;
  private final AsyncRequest<T> fallbackRun;
  private final long millisBeforeRetry;
  private final long timeoutMillis;

  public AsyncRetryRequest(final AsyncRequest<T> initialRun, final AsyncRequest<T> fallbackRun,
      final long millisBeforeRetry, long timeoutMillis) {
    this.initialRun = initialRun;
    this.fallbackRun = fallbackRun;
    this.millisBeforeRetry = millisBeforeRetry;
    this.timeoutMillis = timeoutMillis - millisBeforeRetry;
  }

  /**
   * Waits for the latch to be opened either by the initial task until retry-timeout is reached or
   * by either task after retry-timeout is past.
   */
  public T get() {
    final CountDownLatch firstToFinishLatch = new CountDownLatch(1);
    final AsyncResponse<T> response = new AsyncResponse<>();
    initialRun.run(firstToFinishLatch, response);

    try {
      final boolean initialRunFinished =
          firstToFinishLatch.await(millisBeforeRetry, TimeUnit.MILLISECONDS);

      if (initialRunFinished) {
        return handleInitialFinished(response);
      }

      fallbackRun.run(firstToFinishLatch, response);

      final boolean somethingFinished =
          firstToFinishLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);

      initialRun.abort();
      fallbackRun.abort();

      if (somethingFinished) {
        return response.retrieve();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    throw new RuntimeException("Both Initial and Fallback requests timed out");
  }

  private T handleInitialFinished(final AsyncResponse<T> initialResponse)
      throws InterruptedException {
    if (initialResponse.received()) {
      return initialResponse.retrieve();
    }
    final CountDownLatch fallBackLatch = new CountDownLatch(1);
    final AsyncResponse<T> fallbackResponse = new AsyncResponse<>();

    fallbackRun.run(fallBackLatch, fallbackResponse);

    final boolean finished = fallBackLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
    if (finished) {
      return fallbackResponse.retrieve();
    }
    throw new RuntimeException("Fallback request timed out after Initial request failed");
  }
}
