/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.magma.math;

import javax.annotation.Nonnull;

import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.opal.search.StatsIndexManager;
import org.obiba.opal.search.es.ElasticSearchProvider;
import org.obiba.opal.search.service.OpalSearchService;

import com.google.common.base.Preconditions;

public class AbstractSummaryResource {

  protected final OpalSearchService opalSearchService;

  protected final StatsIndexManager statsIndexManager;

  protected final ElasticSearchProvider esProvider;

  @Nonnull
  private final ValueTable valueTable;

  @Nonnull
  private final Variable variable;

  protected AbstractSummaryResource(OpalSearchService opalSearchService, StatsIndexManager statsIndexManager,
      ElasticSearchProvider esProvider, @Nonnull ValueTable valueTable, @Nonnull Variable variable) {
    Preconditions.checkNotNull(valueTable);
    Preconditions.checkNotNull(variable);

    this.opalSearchService = opalSearchService;
    this.statsIndexManager = statsIndexManager;
    this.esProvider = esProvider;
    this.valueTable = valueTable;
    this.variable = variable;
  }

  @Nonnull
  public ValueTable getValueTable() {
    return valueTable;
  }

  @Nonnull
  public Variable getVariable() {
    return variable;
  }

  protected boolean canQueryEsIndex() {
    return opalSearchService.isEnabled() && opalSearchService.isRunning() && statsIndexManager.isReady() &&
        statsIndexManager.isIndexUpToDate(valueTable);
  }
}
