// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.canvas.ui.editors.properties;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.canvas.core.BaseObjectModel;

/**
 * @author arian
 *
 */
public class Editor_BaseObjectSection extends CanvasPropertySection {

	private Label _editorNameLabel;
	private Text _editorNameText;

	public Editor_BaseObjectSection(CanvasPropertiesPage page) {
		super("Editor", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof BaseObjectModel;
	}

	@Override
	public Control createContent(Composite parent) {

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		_editorNameLabel = new Label(comp, SWT.NONE);
		_editorNameLabel.setText("Var Name");

		_editorNameText = new Text(comp, SWT.BORDER);
		_editorNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		update_UI_from_Model();

		return comp;
	}

	private void update_UI_from_Model() {
		_editorNameText.setText(
				flatStringValues(Arrays.stream(getModels()).map(model -> ((BaseObjectModel) model).getEditorName())));
	}

}
