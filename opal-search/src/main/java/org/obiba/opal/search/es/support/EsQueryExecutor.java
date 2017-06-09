/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.search.es.support;

import com.google.common.base.Strings;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.http.HttpRequest;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.obiba.opal.search.es.ElasticSearchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class EsQueryExecutor {

  private static final Logger log = LoggerFactory.getLogger(EsQueryExecutor.class);

  private final ElasticSearchProvider elasticSearchProvider;

  private String searchPath;

  public EsQueryExecutor(ElasticSearchProvider elasticSearchProvider) {
    this.elasticSearchProvider = elasticSearchProvider;
  }

  public EsQueryExecutor setSearchPath(String path) {
    searchPath = path;
    return this;
  }

  public JSONObject executePost(JSONObject jsonBody) throws JSONException {
    return execute(jsonBody, RestRequest.Method.POST);
  }

  private JSONObject execute(JSONObject jsonBody, RestRequest.Method httpMethod) throws JSONException {
    return executeQuery(jsonBody, new EsRestRequest(jsonBody.toString(), searchPath).setHttpMethod(httpMethod));
  }

  //
  // Private members
  //

  private JSONObject executeQuery(JSONObject jsonBody, RestRequest esRestRequest) throws JSONException {
    if (log.isDebugEnabled()) log.debug("Request: " + searchPath + " => " + jsonBody.toString(2));
    Assert.notNull(jsonBody, "Query json body is null!");

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<byte[]> ref = new AtomicReference<>();

    elasticSearchProvider.getRest().dispatchRequest(esRestRequest, new RestChannel(esRestRequest, true) {

      @Override
      public void sendResponse(RestResponse response) {

        try {
          ref.set(convert(response));
        } catch(IOException e) {
          // Not gonna happen
        } finally {
          latch.countDown();
        }
      }

    });

    try {

      latch.await();
      byte[] content = ref.get();

      JSONObject response = new JSONObject(new String(content));
      if (log.isDebugEnabled()) log.debug("Response: " + response.toString(2));
      return response;

    } catch(InterruptedException e) {
      throw new RuntimeException(e);
    }

  }

  private byte[] convert(RestResponse response) throws IOException {
    byte[] entity;
    if(response.content() instanceof Releasable) {
      entity = new byte[response.content().length()];
      System.arraycopy(response.content().toBytes(), 0, entity, 0, response.content().length());
    } else {
      entity = response.content().toBytes();
    }
    return entity;
  }

  private static class EsRestRequest extends HttpRequest {

    private final String body;

    private final Map<String, String> params;

    private final String esUri;

    private Method httpMethod = Method.GET;

    private final Map<String, String> headers = ImmutableMap.of("Content-Type", "application/json");

    EsRestRequest(String body, String path) {
      this(body, path, new HashMap<String, String>());
    }

    EsRestRequest(String body, String path, Map<String, String> params) {
      this.body = body;
      this.params = params;

      StringBuilder pathBuilder = new StringBuilder("/");

      if(!Strings.isNullOrEmpty(path)) {
        pathBuilder.append(path).append("/");
      }

      esUri = pathBuilder.append("_search").toString();
    }

    public EsRestRequest setHttpMethod(Method method) {
      httpMethod = method;
      return this;
    }

    @Override
    public Method method() {
      return httpMethod;
    }

    @Override
    public String uri() {
      return esUri;
    }

    @Override
    public String rawPath() {
      int pathEndPos = esUri.indexOf('?');
      return pathEndPos < 0 ? esUri : esUri.substring(0, pathEndPos);
    }

    @Override
    public boolean hasContent() {
      return body != null && !body.isEmpty();
    }

    @Override
    public BytesReference content() {
      return new BytesArray(body);
    }

    @Override
    public String header(String name) {
      return headers.get(name);
    }

    @Override
    public Iterable<Map.Entry<String, String>> headers() {
      return headers.entrySet();
    }

    @Override
    public boolean hasParam(String key) {
      return params.containsKey(key);
    }

    @Override
    public String param(String key) {
      return params.get(key);
    }

    @Override
    public Map<String, String> params() {
      return params;
    }

    @Override
    public String param(String key, String defaultValue) {
      return hasParam(key) ? param(key) : defaultValue;
    }

  }
}
