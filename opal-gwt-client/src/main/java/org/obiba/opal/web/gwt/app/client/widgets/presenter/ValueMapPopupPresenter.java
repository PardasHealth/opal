/*******************************************************************************
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.widgets.presenter;

import java.util.Arrays;
import java.util.List;

import org.obiba.opal.web.gwt.app.client.fs.event.FileDownloadEvent;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.model.client.magma.TableDto;
import org.obiba.opal.web.model.client.magma.ValueSetsDto;
import org.obiba.opal.web.model.client.magma.VariableDto;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.PopupView;
import com.gwtplatform.mvp.client.PresenterWidget;

/**
 *
 */
public class ValueMapPopupPresenter extends PresenterWidget<ValueMapPopupPresenter.Display> {

  private TableDto table;

  private VariableDto variable;

  private String entityIdentifier;

  /**
   * @param eventBus
   * @param view
   * @param proxy
   */
  @Inject
  public ValueMapPopupPresenter(EventBus eventBus, Display view) {
    super(eventBus, view);
  }

  @Override
  protected void onBind() {
    super.onBind();
    addHandler();
  }

  public void initialize(TableDto table, VariableDto variable, String entityIdentifier, ValueSetsDto.ValueDto value, boolean modal) {
    this.table = table;
    this.variable = variable;
    this.entityIdentifier = entityIdentifier;
    getView().initialize(table, variable, entityIdentifier, value, modal);
  }

  //
  // Private methods
  //

  private void addHandler() {
    registerHandler(getView().getButton().addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        getView().hide();
      }
    }));
  }

  //
  // Inner classes and Interfaces
  //

  public interface Display extends PopupView {

    void initialize(TableDto table, VariableDto variable, String entityIdentifier, ValueSetsDto.ValueDto value, boolean modal);

    HasClickHandlers getButton();

  }

}
