/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.workbench.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;

/**
 *
 */
public class Table<T> extends CellTable<T> {

  private static final int DEFAULT_PAGESIZE = 15;

  public interface TableResources extends CellTable.Resources {
    @Source("org/obiba/opal/web/gwt/app/public/css/opal-CellTable.css")
    CellTable.Style cellTableStyle();
  }

  public Table() {
    super(DEFAULT_PAGESIZE, GWT.<TableResources> create(TableResources.class));
  }

  /**
   * @param pageSize
   */
  public Table(int pageSize) {
    super(pageSize, GWT.<TableResources> create(TableResources.class));
  }

}
