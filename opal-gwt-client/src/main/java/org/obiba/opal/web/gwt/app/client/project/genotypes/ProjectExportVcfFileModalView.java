/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.app.client.project.genotypes;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import org.obiba.opal.web.gwt.app.client.fs.presenter.FileSelectionPresenter;
import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.ui.Chooser;
import org.obiba.opal.web.gwt.app.client.ui.Modal;
import org.obiba.opal.web.gwt.app.client.ui.ModalPopupViewWithUiHandlers;
import org.obiba.opal.web.gwt.app.client.ui.OpalSimplePanel;
import org.obiba.opal.web.model.client.magma.TableDto;

import javax.annotation.Nullable;

public class ProjectExportVcfFileModalView extends ModalPopupViewWithUiHandlers<ProjectExportVcfFileModalUiHandlers>
        implements ProjectExportVcfFileModalPresenter.Display {

  interface Binder extends UiBinder<Widget, ProjectExportVcfFileModalView> {}

  @UiField
  Modal dialog;

  @UiField
  Button cancelButton;

  @UiField
  OpalSimplePanel vcfFilePanel;

  @UiField
  ControlGroup fileGroup;

  @UiField
  ControlGroup participantsFilterGroup;

  @UiField
  Chooser participantsFilter;

  @UiField
  CheckBox includeCaseControls;

  @UiField
  Alert exportNVCF;

  @Inject
  public ProjectExportVcfFileModalView(EventBus eventBus, Binder binder, Translations translations) {
    super(eventBus);
    initWidget(binder.createAndBindUi(this));
    dialog.setTitle(translations.exportVcfModalTitle());
  }

  @Override
  public void onShow() {
  }

  @Override
  public void hideDialog() {
    dialog.hide();
  }

  @Override
  public void showExportNAlert(String message) {
    exportNVCF.setText(message);
  }

  @Override
  public String getParticipantsFilterTable() {
    return participantsFilter.getSelectedValue();
  }

  @Override
  public boolean hasCaseControls() {
    return includeCaseControls.getValue();
  }

  @UiHandler("cancelButton")
  public void onCancelButton(ClickEvent event) {
    dialog.hide();
  }

  @UiHandler("exportButton")
  public void exportButtonClick(ClickEvent event) {
    getUiHandlers().onExport();
  }

  @Override
  public void setParticipants(JsArray<TableDto> tables) {
    participantsFilter.clear();

    for (TableDto tableDto : JsArrays.toIterable(tables)) {
      // place full path in case same table name exists in another datasource
      participantsFilter.addItem(tableDto.getDatasourceName() + "." + tableDto.getName());
    }
  }

  @Override
  public void setFileSelectorWidgetDisplay(FileSelectionPresenter.Display display) {
    vcfFilePanel.setWidget(display.asWidget());
    display.setFieldWidth("20em");
  }

  @Override
  public void clearErrors() {
    dialog.clearAlert();
  }

  @Override
  public void showError(@Nullable FormField formField, String message) {
    ControlGroup group = null;
    if(formField != null) {
      switch(formField) {
        case DIRECTORY:
          group = fileGroup ;
          break;
      }
    }
    if(group == null) {
      dialog.addAlert(message, AlertType.ERROR);
    } else {
      dialog.addAlert(message, AlertType.ERROR, group);
    }
  }
}
