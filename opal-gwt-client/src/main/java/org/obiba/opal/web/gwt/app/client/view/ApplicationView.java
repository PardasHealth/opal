/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.view;

import org.obiba.opal.web.gwt.app.client.place.Places;
import org.obiba.opal.web.gwt.app.client.presenter.ApplicationPresenter;
import org.obiba.opal.web.gwt.app.client.presenter.ApplicationUiHandlers;
import org.obiba.opal.web.gwt.app.client.ui.CloseableList;
import org.obiba.opal.web.gwt.app.client.ui.HasUrl;
import org.obiba.opal.web.gwt.app.client.ui.ListItem;
import org.obiba.opal.web.gwt.app.client.ui.SuggestListBox;
import org.obiba.opal.web.gwt.app.client.ui.VariableSearchListItem;
import org.obiba.opal.web.gwt.app.client.ui.VariableSuggestOracle;
import org.obiba.opal.web.gwt.rest.client.authorization.HasAuthorization;
import org.obiba.opal.web.gwt.rest.client.authorization.WidgetAuthorizer;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

/**
 *
 */
public class ApplicationView extends ViewWithUiHandlers<ApplicationUiHandlers> implements ApplicationPresenter.Display {

  interface Binder extends UiBinder<Widget, ApplicationView> {}

  @UiField
  NavLink dashboardItem;

  @UiField
  NavLink administrationItem;

  @UiField
  NavLink profileItem;

  @UiField
  Dropdown username;

  @UiField
  Label version;

  @UiField
  NavLink projectsItem;

  @UiField
  Panel notification;

  @UiField
  Panel workbench;

  @UiField
  Frame frame;

  @UiField(provided = true)
  SuggestListBox search;

  private final VariableSuggestOracle oracle;

  @Inject
  public ApplicationView(EventBus eventBus, Binder uiBinder) {
    oracle = new VariableSuggestOracle(eventBus);
    search = new SuggestListBox(oracle);
    initWidget(uiBinder.createAndBindUi(this));

    dashboardItem.setHref("#" + Places.DASHBOARD);
    projectsItem.setHref("#" + Places.PROJECTS);
    administrationItem.setHref("#" + Places.ADMINISTRATION);
    profileItem.setHref("#" + Places.PROFILE);

    initSearchWidget();
  }

  private void initSearchWidget() {
    search.addItemRemovedHandler(new CloseableList.ItemRemovedHandler() {
      @Override
      public void onItemRemoved(ListItem item) {
        VariableSearchListItem.ItemType type = ((VariableSearchListItem) item).getType();
        if(VariableSearchListItem.ItemType.DATASOURCE == type) {
          oracle.setDatasource(null);
        } else if(VariableSearchListItem.ItemType.TABLE == type) {
          oracle.setTable(null);
        }
      }
    });

    search.setUpdaterCallback(new Typeahead.UpdaterCallback() {
      @Override
      public String onSelection(SuggestOracle.Suggestion selectedSuggestion) {
        // Reset suggestBox text to user input text
        String originalQuery = oracle.getOriginalQuery();
        // Forward selection event
        getUiHandlers().onSelection((VariableSuggestOracle.VariableSuggestion)selectedSuggestion);
        return originalQuery;
      }
    });
  }

  @Override
  public void setInSlot(Object slot, IsWidget content) {
    if(ApplicationPresenter.WORKBENCH == slot) {
      workbench.clear();
      workbench.add(content.asWidget());
    } else {
      notification.clear();
      notification.add(content);
    }
  }

  @Override
  public HasUrl getDownloader() {
    return new HasUrl() {

      @Override
      public void setUrl(String url) {
        frame.setUrl(url);
      }
    };
  }

  @Override
  public HasAuthorization getAdministrationAuthorizer() {
    return new WidgetAuthorizer(administrationItem);
  }

  @Override
  public void setUsername(String username) {
    this.username.setText(username);
  }

  @Override
  public void setVersion(String version) {
    this.version.setText(version);
  }

  @Override
  public void addSearchItem(String text, VariableSearchListItem.ItemType type) {
    String qText = quoteIfContainsSpace(text);
    if(VariableSearchListItem.ItemType.DATASOURCE == type) {
      oracle.setDatasource(qText);
    }
    if(VariableSearchListItem.ItemType.TABLE == type) {
      oracle.setTable(qText);
    }
    search.addItem(qText, type);
  }

  private String quoteIfContainsSpace(String s) {
    return s.contains(" ") ? "\"" + s + "\"" : s;
  }

  @Override
  public void clearSearch() {
    search.clear();
  }

  @UiHandler("quitItem")
  void onQuit(ClickEvent event) {
    getUiHandlers().onQuit();
  }

}
