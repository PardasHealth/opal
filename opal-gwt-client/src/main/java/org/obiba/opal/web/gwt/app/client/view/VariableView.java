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

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.presenter.VariablePresenter;
import org.obiba.opal.web.model.client.AttributeDto;
import org.obiba.opal.web.model.client.CategoryDto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.ListView.Delegate;

/**
 *
 */
public class VariableView extends Composite implements VariablePresenter.Display {

  @UiTemplate("VariableView.ui.xml")
  interface VariableViewUiBinder extends UiBinder<Widget, VariableView> {
  }

  private static final String DEFAULT_LOCALE_NAME = "default";

  private static final String LABEL_ATTRIBUTE_NAME = "label";

  private static VariableViewUiBinder uiBinder = GWT.create(VariableViewUiBinder.class);

  private static Translations translations = GWT.create(Translations.class);

  //
  // Instance Variables
  //

  @UiField
  Label variableName;

  @UiField
  Label entityType;

  @UiField
  Label valueType;

  @UiField
  Label mimeType;

  @UiField
  Label unit;

  @UiField
  Label repeatable;

  @UiField
  Label occurrenceGroup;

  @UiField
  Label categoryTableTitle;

  @UiField
  CellTable<CategoryDto> categoryTable;

  SimplePager<CategoryDto> categoryTablePager;

  @UiField
  Label attributeTableTitle;

  @UiField
  CellTable<AttributeDto> attributeTable;

  SimplePager<AttributeDto> attributeTablePager;

  //
  // Constructors
  //

  public VariableView() {
    initWidget(uiBinder.createAndBindUi(this));

    initCategoryTable();
    initAttributeTable();
  }

  //
  // VariablePresenter.Display Methods
  //

  @SuppressWarnings("unchecked")
  public void renderCategoryRows(JsArray<CategoryDto> rows) {
    final JsArray<CategoryDto> categoryRows = (rows != null) ? rows : (JsArray<CategoryDto>) JsArray.createArray();

    categoryTableTitle.setText(translations.categoriesLabel() + " (" + categoryRows.length() + ")");

    categoryTable.setDelegate(new Delegate<CategoryDto>() {

      @Override
      public void onRangeChanged(ListView<CategoryDto> listView) {
        int start = listView.getRange().getStart();
        int length = listView.getRange().getLength();
        listView.setData(start, length, JsArrays.toList(categoryRows, start, length));
      }
    });

    categoryTablePager.firstPage();
    categoryTable.setData(0, categoryTable.getPageSize(), JsArrays.toList(categoryRows, 0, categoryTable.getPageSize()));
    categoryTable.setDataSize(categoryRows.length(), true);
  }

  @SuppressWarnings("unchecked")
  public void renderAttributeRows(final JsArray<AttributeDto> rows) {
    final JsArray<AttributeDto> attributeRows = (rows != null) ? rows : (JsArray<AttributeDto>) JsArray.createArray();

    attributeTableTitle.setText(translations.attributesLabel() + " (" + attributeRows.length() + ")");

    attributeTable.setDelegate(new Delegate<AttributeDto>() {

      @Override
      public void onRangeChanged(ListView<AttributeDto> listView) {
        int start = listView.getRange().getStart();
        int length = listView.getRange().getLength();
        listView.setData(start, length, JsArrays.toList(attributeRows, start, length));
      }
    });

    attributeTablePager.firstPage();
    attributeTable.setData(0, attributeTable.getPageSize(), JsArrays.toList(attributeRows, 0, attributeTable.getPageSize()));
    attributeTable.setDataSize(attributeRows.length(), true);
  }

  public Widget asWidget() {
    return this;
  }

  public void startProcessing() {
  }

  public void stopProcessing() {
  }

  //
  // Methods
  //

  public HasText getVariableNameLabel() {
    return variableName;
  }

  public HasText getEntityTypeLabel() {
    return entityType;
  }

  public HasText getValueTypeLabel() {
    return valueType;
  }

  public HasText getMimeTypeLabel() {
    return mimeType;
  }

  public HasText getUnitLabel() {
    return unit;
  }

  public HasText getRepeatableLabel() {
    return repeatable;
  }

  public HasText getOccurrenceGroupLabel() {
    return occurrenceGroup;
  }

  private void initCategoryTable() {
    categoryTableTitle.setText(translations.categoriesLabel());
    categoryTable.setSelectionEnabled(false);
    categoryTable.setSelectionModel(new SingleSelectionModel<CategoryDto>());

    addCategoryTableColumns();

    // Add a pager.
    categoryTable.setPageSize(50);
    categoryTablePager = new SimplePager<CategoryDto>(categoryTable);
    categoryTable.setPager(categoryTablePager);
    ((VerticalPanel) categoryTable.getParent()).insert(categoryTablePager, 0);
  }

  private void addCategoryTableColumns() {
    categoryTable.addColumn(new TextColumn<CategoryDto>() {
      @Override
      public String getValue(CategoryDto object) {
        return object.hasCode() ? object.getCode() : "";
      }
    }, translations.codeLabel());

    categoryTable.addColumn(new TextColumn<CategoryDto>() {
      @Override
      public String getValue(CategoryDto object) {
        return object.getName();
      }
    }, translations.nameLabel());

    categoryTable.addColumn(new TextColumn<CategoryDto>() {
      @Override
      public String getValue(CategoryDto object) {
        return getCategoryLabel(object);
      }
    }, translations.labelLabel());

    categoryTable.addColumn(new TextColumn<CategoryDto>() {
      @Override
      public String getValue(CategoryDto object) {
        return object.getIsMissing() ? translations.yesLabel() : translations.noLabel();
      }
    }, translations.missingLabel());
  }

  private String getCategoryLabel(CategoryDto categoryDto) {
    String categoryLabel = "";

    JsArray<AttributeDto> attributes = categoryDto.getAttributesArray();
    for(int i = 0; i < attributes.length(); i++) {
      AttributeDto attribute = attributes.get(i);
      if(attribute.getName().equals(LABEL_ATTRIBUTE_NAME) && attribute.getLocale() != null && attribute.getLocale().equals(getCurrentLanguage())) {
        return attribute.getValue();
      }
    }

    return categoryLabel;
  }

  private String getCurrentLanguage() {
    String currentLocaleName = com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().getLocaleName();
    if(currentLocaleName.equals(DEFAULT_LOCALE_NAME)) {
      // No locale has been specified so the current locale is "default". Return English as the current language.
      return "en";
    }
    int separatorIndex = currentLocaleName.indexOf('_');

    return (separatorIndex != -1) ? currentLocaleName.substring(0, separatorIndex) : currentLocaleName;
  }

  private void initAttributeTable() {
    attributeTableTitle.setText(translations.attributesLabel());
    attributeTable.setSelectionEnabled(false);
    attributeTable.setSelectionModel(new SingleSelectionModel<AttributeDto>());

    addAttributeTableColumns();

    // Add a pager.
    attributeTable.setPageSize(50);
    attributeTablePager = new SimplePager<AttributeDto>(attributeTable);
    attributeTable.setPager(attributeTablePager);
    ((VerticalPanel) attributeTable.getParent()).insert(attributeTablePager, 0);
  }

  private void addAttributeTableColumns() {
    attributeTable.addColumn(new TextColumn<AttributeDto>() {
      @Override
      public String getValue(AttributeDto object) {
        return object.getName();
      }
    }, translations.nameLabel());

    attributeTable.addColumn(new TextColumn<AttributeDto>() {
      @Override
      public String getValue(AttributeDto object) {
        return object.hasLocale() ? object.getLocale() : "";
      }
    }, translations.languageLabel());

    attributeTable.addColumn(new TextColumn<AttributeDto>() {
      @Override
      public String getValue(AttributeDto object) {
        return object.getValue();
      }
    }, translations.valueLabel());
  }
}
