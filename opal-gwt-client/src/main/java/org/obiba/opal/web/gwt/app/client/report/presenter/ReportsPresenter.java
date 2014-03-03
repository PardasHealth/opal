/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.report.presenter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.obiba.opal.web.gwt.app.client.event.ConfirmationEvent;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.presenter.ModalProvider;
import org.obiba.opal.web.gwt.app.client.report.event.ReportTemplateCreatedEvent;
import org.obiba.opal.web.gwt.app.client.report.event.ReportTemplateDeletedEvent;
import org.obiba.opal.web.gwt.app.client.report.event.ReportTemplateSelectedEvent;
import org.obiba.opal.web.gwt.rest.client.ResourceAuthorizationRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;
import org.obiba.opal.web.gwt.rest.client.UriBuilders;
import org.obiba.opal.web.gwt.rest.client.authorization.HasAuthorization;
import org.obiba.opal.web.model.client.opal.ReportTemplateDto;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Response;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;

public class ReportsPresenter extends PresenterWidget<ReportsPresenter.Display> implements ReportsUiHandlers {

  ReportTemplateDetailsPresenter reportTemplateDetailsPresenter;

  ModalProvider<ReportTemplateUpdateModalPresenter> reportTemplateUpdateModalPresenterProvider;

  private ReportTemplateDto reportTemplate;

  private Runnable actionRequiringConfirmation;

  private String project;

  @Inject
  public ReportsPresenter(Display display, EventBus eventBus,
      ReportTemplateDetailsPresenter reportTemplateDetailsPresenter,
      ModalProvider<ReportTemplateUpdateModalPresenter> reportTemplateUpdateModalPresenterProvider) {
    super(eventBus, display);
    this.reportTemplateDetailsPresenter = reportTemplateDetailsPresenter;
    this.reportTemplateUpdateModalPresenterProvider = reportTemplateUpdateModalPresenterProvider.setContainer(this);
    getView().setUiHandlers(this);
  }

  @Override
  public void onAdd() {
    ReportTemplateUpdateModalPresenter presenter = reportTemplateUpdateModalPresenterProvider.get();
    presenter.setDialogMode(ReportTemplateUpdateModalPresenter.Mode.CREATE);
    presenter.setProject(project);
  }

  @Override
  public void onSelection(ReportTemplateDto template) {
    fireEvent(new ReportTemplateSelectedEvent(template));
  }

  @Override
  protected void onBind() {
    setInSlot(null, reportTemplateDetailsPresenter);
    addHandlers();
  }

  public void showProject(String projectName) {
    project = projectName;
    refreshReportTemplates(null);
  }

  @Override
  protected void onReveal() {
    refreshReportTemplates(null);
  }

  //
  // Private methods
  //

  private void addHandlers() {
    addRegisteredHandler(ReportTemplateSelectedEvent.getType(), new ReportTemplateSelectedEvent.Handler() {

      @Override
      public void onReportTemplateSelected(ReportTemplateSelectedEvent event) {
        reportTemplate = event.getReportTemplate();
        getView().setCurrentReportTemplate(reportTemplate);
      }
    });
    addRegisteredHandler(ConfirmationEvent.getType(), new ConfirmationEvent.Handler() {
      @Override
      public void onConfirmation(ConfirmationEvent event) {
        if(event.getSource() == actionRequiringConfirmation && event.isConfirmed()) {
          actionRequiringConfirmation.run();
          actionRequiringConfirmation = null;
        }
      }
    });
    addRegisteredHandler(ReportTemplateCreatedEvent.getType(), new ReportTemplateCreatedHandler());
    addRegisteredHandler(ReportTemplateDeletedEvent.getType(), new ReportTemplateDeletedHandler());
  }

  private void refreshReportTemplates(ReportTemplateDto templateToSelect) {
    String uri;
    uri = project == null
        ? UriBuilders.REPORT_TEMPLATES.create().build()
        : UriBuilders.PROJECT_REPORT_TEMPLATES.create().build(project);

    ResourceRequestBuilderFactory.<JsArray<ReportTemplateDto>>newBuilder() //
        .forResource(uri) //
        .withCallback(new ReportTemplatesResourceCallback(templateToSelect)) //
        .withCallback(ResponseCodeCallback.NO_OP, Response.SC_FORBIDDEN) //
        .get().send();
  }

  private class ReportTemplateCreatedHandler implements ReportTemplateCreatedEvent.Handler {

    @Override
    public void onReportTemplateCreated(ReportTemplateCreatedEvent event) {
      refreshReportTemplates(event.getReportTemplate());
    }
  }

  private class ReportTemplateDeletedHandler implements ReportTemplateDeletedEvent.Handler {

    @Override
    public void onReportTemplateDeleted(ReportTemplateDeletedEvent event) {
      refreshReportTemplates(null);
    }
  }

  private class ReportTemplatesResourceCallback implements ResourceCallback<JsArray<ReportTemplateDto>> {

    private final ReportTemplateDto templateToSelect;

    ReportTemplatesResourceCallback(ReportTemplateDto templateToSelect) {
      this.templateToSelect = templateToSelect;
    }

    @Override
    public void onResource(Response response, JsArray<ReportTemplateDto> templates) {
      JsArray<ReportTemplateDto> sortedTemplates = sortReportTemplates(JsArrays.toSafeArray(templates));

      getView().setReportTemplates(sortedTemplates);

      int length = sortedTemplates.length();
      if(length == 0) {
        onSelection(null);
      } else if(templateToSelect != null) {
        onSelection(templateToSelect);
      } else if(length > 0) {
        onSelection(sortedTemplates.get(0));
      }
    }

    private JsArray<ReportTemplateDto> sortReportTemplates(JsArray<ReportTemplateDto> templates) {
      List<ReportTemplateDto> templateList = JsArrays.toList(templates);

      Collections.sort(templateList, new Comparator<ReportTemplateDto>() {

        @Override
        public int compare(ReportTemplateDto first, ReportTemplateDto second) {
          return first.getName().compareTo(second.getName());
        }
      });

      @SuppressWarnings("unchecked")
      JsArray<ReportTemplateDto> sortedTemplates = (JsArray<ReportTemplateDto>) JsArray.createArray();
      for(ReportTemplateDto template : templateList) {
        if(project == null || project.equals(template.getProject())) sortedTemplates.push(template);
      }

      return sortedTemplates;
    }
  }

  protected void authorize() {
    // create report templates
    ResourceAuthorizationRequestBuilderFactory.newBuilder() //
        .forResource("/report-templates") //
        .authorize(getView().getAddReportTemplateAuthorizer()) //
        .post().send();

  }

  public interface Display extends View, HasUiHandlers<ReportsUiHandlers> {
    HasAuthorization getAddReportTemplateAuthorizer();

    void setCurrentReportTemplate(ReportTemplateDto reportTemplateDto);

    void setReportTemplates(JsArray<ReportTemplateDto> templates);
  }
}
