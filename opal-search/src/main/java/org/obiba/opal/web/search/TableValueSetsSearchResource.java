/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.NoSuchDatasourceException;
import org.obiba.magma.NoSuchValueSetException;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.js.views.JavascriptClause;
import org.obiba.magma.support.VariableEntityBean;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.model.Search;
import org.obiba.opal.web.search.support.QuerySearchJsonBuilder;
import org.obiba.opal.web.search.support.VariableEntityValueSetDtoFunction;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Component
@Transactional
@Scope("request")
@Path("/datasource/{ds}/table/{table}/valueSets/_search")
@Api(value = "/datasource/{ds}/table/{table}/valueSets/_search",
    description = "Executes a query on an Elastic Search values index")
public class TableValueSetsSearchResource extends AbstractVariablesSearchResource {

//  private static final Logger log = LoggerFactory.getLogger(TableVariablesSearchResource.class);

  @PathParam("ds")
  private String datasource;

  @PathParam("table")
  private String table;

  @SuppressWarnings("PMD.ExcessiveParameterList")
  @GET
  @POST
  @ApiOperation("Returns a list of valueSets corresponding to specified query")
  public Response search(@Context UriInfo uriInfo, @QueryParam("query") String query,
      @QueryParam("offset") @DefaultValue("0") int offset, @QueryParam("limit") @DefaultValue("10") int limit,
      @QueryParam("select") String select) {

    try {
      if(!canQueryEsIndex()) return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
      if(!valuesIndexManager.hasIndex(getValueTable())) return Response.status(Response.Status.NOT_FOUND).build();
      QuerySearchJsonBuilder jsonBuiler = //
          buildQuerySearch(query, offset, limit, null, null, null);
      JSONObject jsonResponse = executeQuery(jsonBuiler.build());

      Search.ValueSetsResultDto.Builder dtoResponseBuilder = getvalueSetsDtoBuilder(uriInfo, offset, limit, select,
          jsonResponse);

      // filter entities
      return Response.ok().entity(dtoResponseBuilder.build()).build();
    } catch(NoSuchValueSetException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch(NoSuchDatasourceException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch(Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  private Search.ValueSetsResultDto.Builder getvalueSetsDtoBuilder(UriInfo uriInfo, int offset, int limit,
      String select, JSONObject jsonResponse) throws JSONException {
    Search.ValueSetsResultDto.Builder dtoResponseBuilder = Search.ValueSetsResultDto.newBuilder();
    JSONObject jsonHits = jsonResponse.getJSONObject("hits");

    dtoResponseBuilder.setTotalHits(jsonHits.getInt("total"));
    Collection<VariableEntity> entities = new ArrayList<VariableEntity>();
    String entityType = getValueTable().getEntityType();

    JSONArray hits = jsonHits.getJSONArray("hits");
    for(int i = 0; i < hits.length(); i++) {
      JSONObject jsonHit = hits.getJSONObject(i);
      entities.add(new VariableEntityBean(entityType, jsonHit.getString("_id")));
    }

    String path = uriInfo.getPath();
    path = path.substring(0, path.indexOf("/_search"));
    dtoResponseBuilder.setValueSets(getValueSetsDto(path, select, entities, offset, limit));
    return dtoResponseBuilder;
  }

  //
  // Protected methods
  //

  @Override
  protected String getSearchPath() {
    return valuesIndexManager.getIndex(getValueTable()).getRequestPath();
  }

  //
  // Private methods
  //

  private boolean canQueryEsIndex() {
    return searchServiceAvailable() && valuesIndexManager.isIndexUpToDate(getValueTable());
  }

  private ValueTable getValueTable() {
    return MagmaEngine.get().getDatasource(datasource).getValueTable(table);
  }

  private Magma.ValueSetsDto getValueSetsDto(String uriInfoPath, String select,
      Iterable<VariableEntity> variableEntities, int offset, int limit) {
    Iterable<Variable> variables = filterVariables(select, offset, limit);

    Magma.ValueSetsDto.Builder builder = Magma.ValueSetsDto.newBuilder().setEntityType(getValueTable().getEntityType());

    builder.addAllVariables(Iterables.transform(variables, new Function<Variable, String>() {

      @Override
      public String apply(Variable from) {
        return from.getName();
      }

    }));

    ImmutableList.Builder<Magma.ValueSetsDto.ValueSetDto> valueSetDtoBuilder = ImmutableList.builder();
    Iterable<Magma.ValueSetsDto.ValueSetDto> transform = Iterables.transform(variableEntities,
        new VariableEntityValueSetDtoFunction(getValueTable(), variables, uriInfoPath, false));

    for(Magma.ValueSetsDto.ValueSetDto dto : transform) {
      valueSetDtoBuilder.add(dto);
    }

    builder.addAllValueSets(valueSetDtoBuilder.build());

    return builder.build();
  }

  protected Iterable<Variable> filterVariables(String script, Integer offset, @Nullable Integer limit) {
    List<Variable> filteredVariables = null;

    if(StringUtils.isEmpty(script)) {
      filteredVariables = Lists.newArrayList(getValueTable().getVariables());
    } else {
      JavascriptClause jsClause = new JavascriptClause(script);
      jsClause.initialise();

      filteredVariables = new ArrayList<Variable>();
      for(Variable variable : getValueTable().getVariables()) {
        if(jsClause.select(variable)) {
          filteredVariables.add(variable);
        }
      }
    }

    int fromIndex = offset < filteredVariables.size() ? offset : filteredVariables.size();
    int toIndex = limit != null && limit >= 0 //
        ? Math.min(fromIndex + limit, filteredVariables.size()) //
        : filteredVariables.size();

    return filteredVariables.subList(fromIndex, toIndex);
  }

}