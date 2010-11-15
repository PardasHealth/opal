/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.inject.client;

import org.obiba.opal.web.gwt.app.client.unit.presenter.AddKeyPairDialogPresenter;
import org.obiba.opal.web.gwt.app.client.unit.presenter.FunctionalUnitDetailsPresenter;
import org.obiba.opal.web.gwt.app.client.unit.presenter.FunctionalUnitListPresenter;
import org.obiba.opal.web.gwt.app.client.unit.presenter.FunctionalUnitPresenter;
import org.obiba.opal.web.gwt.app.client.unit.presenter.FunctionalUnitUpdateDialogPresenter;
import org.obiba.opal.web.gwt.app.client.unit.view.AddKeyPairDialogView;
import org.obiba.opal.web.gwt.app.client.unit.view.FunctionalUnitDetailsView;
import org.obiba.opal.web.gwt.app.client.unit.view.FunctionalUnitListView;
import org.obiba.opal.web.gwt.app.client.unit.view.FunctionalUnitUpdateDialogView;
import org.obiba.opal.web.gwt.app.client.unit.view.FunctionalUnitView;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

public class UnitsModule extends AbstractGinModule {

  @Override
  protected void configure() {
    bind(FunctionalUnitListPresenter.Display.class).to(FunctionalUnitListView.class).in(Singleton.class);
    bind(FunctionalUnitDetailsPresenter.Display.class).to(FunctionalUnitDetailsView.class).in(Singleton.class);
    bind(FunctionalUnitPresenter.Display.class).to(FunctionalUnitView.class).in(Singleton.class);
    bind(FunctionalUnitUpdateDialogPresenter.Display.class).to(FunctionalUnitUpdateDialogView.class).in(Singleton.class);
    bind(AddKeyPairDialogPresenter.Display.class).to(AddKeyPairDialogView.class).in(Singleton.class);
  }

}
