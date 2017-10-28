package com.totango.concurrency;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.util.concurrent.Uninterruptibles;

/**
 * @author aharon
 * @since 2017-09-27
 */
public class AsyncRetryRequestTest {

  @Test
  public void testResponseFromInitial() {
    TestRequest r1 = new TestRequest(1, 20);
    TestRequest r2 = new TestRequest(2, 20);

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 60, 300);
    int id = req.get();

    // Make sure fallback doesn't start later
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertFalse(r2.started);
    Assert.assertEquals(id, r1.id);

    Assert.assertFalse(r1.aborted);
    Assert.assertFalse(r2.aborted);
  }

  @Test
  public void testResponseFromFallback() {
    TestRequest r1 = new TestRequest(1, 60);
    TestRequest r2 = new TestRequest(2, 20);

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 20, 300);
    int id = req.get();

    // Make sure value isn't overwritten by initial
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, r2.id);

    Assert.assertTrue(r1.aborted);
    Assert.assertTrue(r2.aborted);
  }

  @Test
  public void testResponseFromInitialAfterFallbackStarted() {
    TestRequest r1 = new TestRequest(1, 30);
    TestRequest r2 = new TestRequest(2, 60);

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 10, 300);
    int id = req.get();

    // Make sure value isn't overwritten by fallback
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, r1.id);

    Assert.assertTrue(r1.aborted);
    Assert.assertTrue(r2.aborted);
  }

  @Test
  public void testResponseFromFallbackWithoutAbort() {
    TestRequest r1 = new TestRequest(1, 60, false, null);
    TestRequest r2 = new TestRequest(2, 20, false, null);

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 20, 300);
    int id = req.get();

    // Make sure value isn't overwritten by initial
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, r2.id);

    Assert.assertFalse(r1.aborted);
    Assert.assertFalse(r2.aborted);
  }

  @Test
  public void testResponseFromInitialAfterFallbackStartedWithoutAbort() {
    TestRequest r1 = new TestRequest(1, 30, false, null);
    TestRequest r2 = new TestRequest(2, 60, false, null);

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 10, 300);
    int id = req.get();

    // Make sure value isn't overwritten by fallback
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, r1.id);

    Assert.assertFalse(r1.aborted);
    Assert.assertFalse(r2.aborted);
  }

  /**
   * Unfortunate side-effect of eagerly opening the latch on exceptions (to avoid possible thread
   * starvation scenarios). If the Initial req passes its wait time and the Fallback fails on an
   * exception we loose the chance of possibly receiving a response from the Initial request.
   */
  @Test
  public void testResponseFromInitialLostAfterFallbackThrowsException() {
    TestRequest r1 = new TestRequest(1, 40);
    TestRequest r2 = new TestRequest(2, 10, true, new IOException());

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 10, 300);
    int id = 0;
    RuntimeException re = null;
    try {
      id = req.get();
    } catch (RuntimeException e) {
      re = e;
    }

    // Make sure value isn't overwritten by fallback
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, 0);
    Assert.assertNotNull(re);
    Assert.assertTrue(re.getCause() instanceof IOException);

    Assert.assertTrue(r1.aborted);
    Assert.assertTrue(r2.aborted);
  }

  @Test
  public void testResponseFromFallbackWithExceptionOnInitial() {
    TestRequest r1 = new TestRequest(1, 20, true, new IllegalArgumentException());
    TestRequest r2 = new TestRequest(2, 20);

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 40, 300);

    int id = req.get();

    // Make sure fallback doesn't start later
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, 2);

    Assert.assertFalse(r1.aborted);
    Assert.assertFalse(r2.aborted);
  }

  @Test
  public void testExceptionOnBoth() {
    TestRequest r1 = new TestRequest(1, 20, true, new IOException());
    TestRequest r2 = new TestRequest(2, 20, true, new IOException());

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 40, 300);
    int id = 0;
    RuntimeException re = null;
    try {
      id = req.get();
    } catch (RuntimeException e) {
      re = e;
    }

    // Make sure value isn't overwritten by initial
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, 0);
    Assert.assertNotNull(re);
    Assert.assertTrue(re.getCause() instanceof IOException);

    Assert.assertFalse(r1.aborted);
    Assert.assertFalse(r2.aborted);
  }

  @Test
  public void testTimeout() {
    TestRequest r1 = new TestRequest(1, 60);
    TestRequest r2 = new TestRequest(2, 60);

    AsyncRetryRequest<Integer> req = new AsyncRetryRequest<>(r1, r2, 20, 40);
    int id = 0;
    RuntimeException re = null;
    try {
      id = req.get();
    } catch (RuntimeException e) {
      re = e;
    }

    // Make sure value isn't overwritten by initial
    Uninterruptibles.sleepUninterruptibly(60, TimeUnit.MILLISECONDS);

    Assert.assertTrue(r1.started);
    Assert.assertTrue(r2.started);
    Assert.assertEquals(id, 0);
    Assert.assertNotNull(re);

    Assert.assertTrue(r1.aborted);
    Assert.assertTrue(r2.aborted);
  }

  private class TestRequest implements AsyncRequest<Integer> {
    private boolean started;
    private boolean aborted;

    private final int id;
    private final long millisPause;
    private Thread thread;
    private final boolean ableToAbort;
    private final Object lock = new Object();
    private final Exception exceptionToThrow;

    private TestRequest(int id, long millisPause, boolean ableToAbort, Exception exception) {
      this.id = id;
      this.millisPause = millisPause;
      this.ableToAbort = ableToAbort;
      this.exceptionToThrow = exception;
    }

    private TestRequest(int id, long millisPause) {
      this(id, millisPause, true, null);
    }

    @Override
    public void run(CountDownLatch latch, AsyncResponse<Integer> result) {
      started = true;
      synchronized (lock) {
        thread = new Thread(() -> {
          try {
            Thread.sleep(millisPause);
          } catch (InterruptedException e) {
            System.out.printf("bye bye");
          }
          if (exceptionToThrow != null) {
            result.setException(exceptionToThrow);
          } else {
            result.setFirst(id);
          }
          latch.countDown();
        });
      }
      thread.start();
    }

    @Override
    public void abort() {
      synchronized (lock) {
        if (ableToAbort && thread != null) {
          thread.interrupt();
          aborted = true;
        }
      }
    }

  }
}
