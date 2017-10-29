package com.totango.concurrency.example;

import java.util.concurrent.CountDownLatch;

import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.totango.concurrency.AsyncRequest;
import com.totango.concurrency.AsyncResponse;
import com.totango.data.datatypes.DocumentType;
import com.totango.service.elasticsearch.metrics.ElasticsearchMetricsClient;

/**
 * @author aharon
 * @since 2017-09-27
 */
public class AsyncSearchRequest implements AsyncRequest<Response> {

  private final ElasticsearchRestConnection connection;
  private final SearchSourceBuilder source;
  private final String endPoint;

  public AsyncSearchRequest(
      ElasticsearchRestConnection connection, SearchSourceBuilder source, String endPoint) {
    this.connection = connection;
    this.source = source;
    this.endPoint = endPoint;
  }

  @Override
  public void run(CountDownLatch latch, AsyncResponse<Response> asyncResponse) {

    connection.performAsyncRequest("GET", endPoint, source, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        final boolean success = asyncResponse.setFirst(response);
        latch.countDown();
        if (success)
          System.out.println("Yay! response received");
      }

      @Override
      public void onFailure(Exception exception) {
        asyncResponse.setException(exception);
        latch.countDown();
      }
    });
  }

  @Override
  public void abort() {}
}
