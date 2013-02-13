/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.gwt.ace.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 */
public class AceEditor extends Widget implements HasText, HasEnabled, HasChangeHandlers {

  @SuppressWarnings("StaticNonFinalField")
  private static int nextId = 0;

  @SuppressWarnings("FieldCanBeLocal")
  private JavaScriptObject editor;

  private HandlerRegistration setTextHandlerRegistration;

  private HandlerRegistration setEnabledHandlerRegistration;

  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  public AceEditor() {
    setElement(DOM.createDiv());
    getElement().setId("ace-editor-" + nextId++);

    // Hack to avoid fire ChangeEvent on Ace change event because Ace API fire change events on SetValue() and for all internal changes.
    // So we fire ChangeEvent on key down
    addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        DomEvent.fireNativeEvent(Document.get().createChangeEvent(), AceEditor.this);
      }
    });
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    editor = createEditor(getElement().getId());
  }

  private static native JavaScriptObject createEditor(String elementId) /*-{
      var editor = $wnd.ace.edit(elementId);
      editor.setTheme("ace/theme/textmate");
      editor.getSession().setMode("ace/mode/javascript");
      editor.getSession().setTabSize(2);
      return editor;
  }-*/;

  @Override
  public final native String getText() /*-{
      return this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor.getValue();
  }-*/;

  @Override
  public void setText(final String text) {
    if(isAttached()) {
      setEditorValue(text);
      if(setTextHandlerRegistration != null) {
        setTextHandlerRegistration.removeHandler();
        setTextHandlerRegistration = null;
      }
    } else {
      setTextHandlerRegistration = addAttachHandler(new AttachEvent.Handler() {
        @Override
        public void onAttachOrDetach(AttachEvent event) {
          setText(text);
        }
      });
    }
  }

  @Override
  public HandlerRegistration addChangeHandler(ChangeHandler handler) {
    return addDomHandler(handler, ChangeEvent.getType());
  }

  private HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(handler, KeyUpEvent.getType());
  }

  @Override
  public final native boolean isEnabled() /*-{
      return this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor.getReadOnly();
  }-*/;

  @Override
  public void setEnabled(final boolean enabled) {
    if(isAttached()) {
      setNativeEnabled(enabled);
      if(setEnabledHandlerRegistration != null) {
        setEnabledHandlerRegistration.removeHandler();
        setEnabledHandlerRegistration = null;
      }
    } else {
      setEnabledHandlerRegistration = addAttachHandler(new AttachEvent.Handler() {
        @Override
        public void onAttachOrDetach(AttachEvent event) {
          setEnabled(enabled);
        }
      });
    }
  }

  public final native void setNativeEnabled(boolean enabled) /*-{
      this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor.setReadOnly(!enabled);
  }-*/;

  public final native void beautify() /*-{
      var value = this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor.getValue();
      console.log("before beautify: " + value);
      console.log("***************");
      value = $wnd.js_beautify(value, { 'indent_size': 2 });
      console.log("after beautify: " + value);
      this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor.setValue(value);
  }-*/;

  private native void setEditorValue(String value) /*-{
      this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor.setValue(value);
  }-*/;

  /**
   * Get selected text
   */
  public final native String getSelectedText() /*-{
      var editor = this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor;
      return editor.getSession().getTextRange(editor.getSelectionRange());
  }-*/;

  /**
   * Register a handler for change events generated by the editor.
   *
   * @param callback the change event handler
   */
  public final native void addAceEditorOnChangeHandler(AceEditorCallback callback) /*-{
      this.@org.obiba.opal.web.gwt.ace.client.AceEditor::editor.getSession().on("change", function (e) {
//          console.log(e);
          callback.@org.obiba.opal.web.gwt.ace.client.AceEditorCallback::invokeAceCallback(Lcom/google/gwt/core/client/JavaScriptObject;)(e);
      });
  }-*/;

}
