/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.magma;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.jboss.resteasy.annotations.cache.Cache;
import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.ValueTableWriter.ValueSetWriter;
import org.obiba.magma.ValueType;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.js.JavascriptVariableBuilder;
import org.obiba.magma.js.JavascriptVariableValueSource;
import org.obiba.magma.lang.Closeables;
import org.obiba.magma.support.VariableEntityBean;
import org.obiba.opal.web.TimestampedResponses;
import org.obiba.opal.web.magma.support.InvalidRequestException;
import org.obiba.opal.web.model.Magma.TableDto;
import org.obiba.opal.web.model.Magma.ValueSetsDto;
import org.obiba.opal.web.model.Magma.VariableEntityDto;

public class TableResource extends AbstractValueTableResource {

  public TableResource(ValueTable valueTable, Set<Locale> locales) {
    super(valueTable, locales);
  }

  public TableResource(ValueTable valueTable) {
    this(valueTable, Collections.<Locale>emptySet());
  }

  @GET
  public TableDto get(@Context final UriInfo uriInfo) {
    TableDto.Builder builder = Dtos.asDto(getValueTable()).setLink(uriInfo.getPath(false));
    if(getValueTable().isView()) builder.setViewLink(uriInfo.getPath(false).replaceFirst("table", "view"));
    return builder.build();
  }

  @Path("/variables")
  public VariablesResource getVariables(@Context Request request) {
    TimestampedResponses.evaluate(request, getValueTable());
    return new VariablesResource(getValueTable(), getLocales());
  }

  /**
   * Get the entities, optionally filtered by a script.
   *
   * @param script script for filtering the entities
   * @return
   */
  @GET
  @Path("/entities")
  public Set<VariableEntityDto> getEntities(@QueryParam("script") String script) {
    Iterable<VariableEntity> entities = filterEntities(script);

    return ImmutableSet.copyOf(Iterables.transform(entities, new Function<VariableEntity, VariableEntityDto>() {
      @Override
      public VariableEntityDto apply(VariableEntity from) {
        return VariableEntityDto.newBuilder().setIdentifier(from.getIdentifier()).build();
      }
    }));
  }

  /**
   * Get the value set from the given entity identifier, for the variables filtered by the optional 'select' script.
   *
   * @param identifier
   * @param select     script for filtering the variables
   * @return
   */
  @Path("/valueSet/{identifier}")
  public ValueSetsResource getValueSet(@Context Request request, @PathParam("identifier") String identifier) {
    TimestampedResponses.evaluate(request, getValueTable());
    return new ValueSetsResource(getValueTable(),
        ImmutableList.<VariableEntity>of(new VariableEntityBean(this.getValueTable().getEntityType(), identifier)));
  }

  // This should be /valueSets, but its POST is already implemented in ValueSetsResource due to GET not allowing a body
  @POST
  @Path("/valueSet")
  public Response updateValueSet(ValueSetsDto valueSetsDto) {
    ValueTableWriter tableWriter = getDatasource()
        .createWriter(getValueTable().getName(), valueSetsDto.getEntityType());
    try {
      for(ValueSetsDto.ValueSetDto valueSetDto : valueSetsDto.getValueSetsList()) {
        VariableEntity entity = new VariableEntityBean(valueSetsDto.getEntityType(), valueSetDto.getIdentifier());
        ValueSetWriter writer = tableWriter.writeValueSet(entity);
        try {
          for(int i = 0; i < valueSetsDto.getVariablesCount(); i++) {
            Variable variable = getValueTable().getVariable(valueSetsDto.getVariables(i));
            writer.writeValue(variable,
                Dtos.fromDto(valueSetDto.getValues(i), variable.getValueType(), variable.isRepeatable()));
          }
        } finally {
          Closeables.closeQuietly(writer);
        }
      }
    } finally {
      Closeables.closeQuietly(tableWriter);
    }
    return Response.ok().build();
  }

  @Path("/valueSets")
  public ValueSetsResource getValueSets(@Context Request request) {
    TimestampedResponses.evaluate(request, getValueTable());
    return new ValueSetsResource(getValueTable());
  }

  @Path("/variable/{variable}")
  public VariableResource getVariable(@Context Request request, @PathParam("variable") String name) {
    TimestampedResponses.evaluate(request, getValueTable());
    return getVariableResource(getValueTable().getVariableValueSource(name));
  }

  @Path("/variable/_transient")
  public VariableResource getTransient(@QueryParam("valueType") @DefaultValue("text") String valueTypeName,
      @QueryParam("repeatable") @DefaultValue("false") Boolean repeatable, @QueryParam("script") String scriptQP,
      @QueryParam("category") List<String> categoriesQP, @FormParam("script") String scriptFP,
      @FormParam("category") List<String> categoriesFP) {
    String script = scriptQP;
    List<String> categories = categoriesQP;
    if(script == null || script.equals("")) {
      script = scriptFP;
    }
    if(script == null || script.equals("")) {
      script = "null";
    }
    if(categories == null || categories.isEmpty()) {
      categories = categoriesFP;
    }
    Variable transientVariable = buildTransientVariable(resolveValueType(valueTypeName), repeatable, script,
        categories == null ? ImmutableList.<String>of() : categories);
    JavascriptVariableValueSource jvvs = new JavascriptVariableValueSource(transientVariable, getValueTable());
    jvvs.initialise();
    return getVariableResource(jvvs);
  }

  @Path("/compare")
  public CompareResource getTableCompare() {
    return new CompareResource(getValueTable());
  }

  @Path("/locales")
  public LocalesResource getLocalesResource() {
    return super.getLocalesResource();
  }

  private ValueType resolveValueType(String valueTypeName) {
    ValueType valueType = null;
    try {
      valueType = ValueType.Factory.forName(valueTypeName);
    } catch(IllegalArgumentException ex) {
      throw new InvalidRequestException("IllegalParameterValue", "valueType", valueTypeName);
    }
    return valueType;
  }

  private VariableResource getVariableResource(VariableValueSource source) {
    return new VariableResource(this.getValueTable(), source);
  }

  private Variable buildTransientVariable(ValueType valueType, boolean repeatable, String script,
      List<String> categories) {
    Variable.Builder builder = new Variable.Builder("_transient", valueType, getValueTable().getEntityType())
        .extend(JavascriptVariableBuilder.class).setScript(script);

    if(repeatable) {
      builder.repeatable();
    }
    builder.addCategories(categories.toArray(new String[] {}));

    return builder.build();
  }

}
