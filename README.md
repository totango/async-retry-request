# AsyncRetryRequest

This async request is useful for scenarios where we need a response from one of several resources
in the shortest possible time but don't want to query more than one of them if we don't need to.
First one of the resources is queried, if a response is not received before a given timeout then
the second resource is queried as well. Once both resources have been queried we wait for the
first response from either of them.

### Example usage for querying multiple Elasticsearch shards

More details on this used case [here](https://labs.totango.com/taming-elasticsearch-hiccups-with-async-retries-9729fbf6e192)

First we wrap a call to the Elasticsearch Rest API with a class implementing AsyncRequest :

```java
package com.totango.concurrency.example;

import java.util.concurrent.CountDownLatch;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.totango.concurrency.AsyncRequest;
import com.totango.concurrency.AsyncResponse;
import com.totango.data.datatypes.DocumentType;
import com.totango.service.elasticsearch.metrics.ElasticsearchMetricsClient;

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

```

Then we call AsyncRetryRequest with two of these wrapped calls. Each one with a different shard preference:

```java
package com.totango.concurrency.example;

import com.totango.concurrency.AsyncRetryRequest;
import com.totango.service.elasticsearch.index.client.response.RestResponseParser;
import com.totango.service.elasticsearch.index.client.response.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import java.io.IOException;

public class SearchClient {
  private final ElasticsearchRestConnection connection;

  public SearchClient(ElasticsearchRestConnection connection) {
    this.connection = connection;
  }

  public SearchResponse search(final SearchSourceBuilder source, final String shard1EndPoint,
      final String shard2EndPoint) throws IOException {

    final AsyncSearchRequest initial = new AsyncSearchRequest(connection, source, shard1EndPoint);
    final AsyncSearchRequest fallback = new AsyncSearchRequest(connection, source, shard2EndPoint);

    final AsyncRetryRequest<Response> asyncRetryRequest = new AsyncRetryRequest<>(initial, fallback, 800L, 5000L);

    final Response resp = asyncRetryRequest.get();
    return RestResponseParser.searchResponse(resp);
  }
}

```

### Tests

See the unit tests for more details on usage
