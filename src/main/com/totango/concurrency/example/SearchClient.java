package com.totango.concurrency.example;

import com.totango.concurrency.AsyncRetryRequest;
import com.totango.service.elasticsearch.index.client.response.RestResponseParser;
import com.totango.service.elasticsearch.index.client.response.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;

/**
 * @author aharon
 * @since 2017-05-18
 */
public class SearchClient {
  private final ElasticsearchRestConnection connection;

  public SearchClient(ElasticsearchRestConnection connection) {
    this.connection = connection;
  }

  public SearchResponse search(final SearchSourceBuilder source, final String endPoint1,
      final String endPoint2) throws IOException {

    final ShardPreference pref = new ShardPreference(preference);
    final AsyncSearchRequest initial = new AsyncSearchRequest(connection, source, endPoint1);
    final AsyncSearchRequest fallback = new AsyncSearchRequest(connection, source, endPoint2);

    final AsyncRetryRequest<Response> asyncRetryRequest =
        new AsyncRetryRequest<>(initial, fallback, 800L, 5000L);

    final Response resp = asyncRetryRequest.get();
    return RestResponseParser.searchResponse(resp);
  }
}
