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
package phasereditor.ui.properties;

import static phasereditor.ui.IEditorSharedImages.IMG_BULLET_COLLAPSE;
import static phasereditor.ui.IEditorSharedImages.IMG_BULLET_EXPAND;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class FormPropertyPage extends Page implements IPropertySheetPage {

	Composite _sectionsContainer;
	private ScrolledComposite _scrolledCompo;

	public FormPropertyPage() {
		super();
	}

	protected abstract Object getDefaultModel();

	@SuppressWarnings({ "unused" })
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {

		var models = ((IStructuredSelection) selection).toArray();

		if (models.length == 0) {
			var defaultModel = getDefaultModel();
			if (defaultModel != null) {
				models = new Object[] { defaultModel };
			}
		}

		// create all candidate sections
		var allSections = new ArrayList<FormPropertySection>();
		{
			var clsMap = new HashMap<Object, FormPropertySection>();
			for (var model : models) {
				var objSections = createSections(model);

				for (var section : objSections) {
					if (!clsMap.containsKey(section.getClass())) {
						clsMap.put(section.getClass(), section);
						allSections.add(section);
					}
				}
			}
		}

		// pick only unique sections

		var uniqueSections = new ArrayList<FormPropertySection>();
		var sectionMap = new HashMap<>();

		for (var section : allSections) {
			var accept = true;
			
			if (!section.supportThisNumberOfModels(models.length)) {
				accept = false;
			} else {
				for (var model : models) {
					if (!section.canEdit(model)) {
						accept = false;
						break;
					}
				}
			}
			
			if (accept) {
				uniqueSections.add(section);
				sectionMap.put(section.getClass(), section);
			}
		}

		// hide all rows with irrelevant sections

		for (var control : _sectionsContainer.getChildren()) {
			var row = (RowComp) control;

			var newSection = sectionMap.get(row.getSection().getClass());

			if (newSection == null) {
				var gd = (GridData) row.getLayoutData();
				gd.heightHint = 0;
				gd.grabExcessVerticalSpace = false;
				gd.verticalAlignment = SWT.TOP;
				row.setVisible(false);
			}
		}

		// create the missing rows, or update current rows

		for (var section : uniqueSections) {
			var createNew = true;

			for (var control : _sectionsContainer.getChildren()) {
				var row = (RowComp) control;

				var oldSection = row.getSection();

				if (oldSection.getClass() == section.getClass()) {
					row.setVisible(true);
					var gd = (GridData) row.getLayoutData();
					gd.heightHint = SWT.DEFAULT;

					if (section.isFillSpace()) {
						gd.grabExcessVerticalSpace = true;
						gd.verticalAlignment = SWT.FILL;
					}

					oldSection.setModels(models);
					oldSection.update_UI_from_Model();
					createNew = false;
					break;
				}
			}

			if (createNew) {
				section.setModels(models);
				new RowComp(_sectionsContainer, section);
			}

		}

		_sectionsContainer.layout();

		updateScrolledComposite();
	}

	public class RowComp extends Composite {

		private FormPropertySection _section;
		boolean _collapsed = false;

		public RowComp(Composite parent, FormPropertySection section) {
			super(parent, SWT.NONE);

			_section = section;

			{
				var gl = new GridLayout(1, false);
				gl.marginWidth = 0;
				gl.marginHeight = 0;
				setLayout(gl);
				setLayoutData(createControlGridData());
			}

			var sectionId = getSectionId(section);

			var collapsed = _collapsedSectionsIds.contains(sectionId);

			var header = new Composite(this, SWT.NONE);
			{
				header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				var gl = new GridLayout(3, false);
				gl.marginWidth = 0;
				gl.marginHeight = 0;
				header.setLayout(gl);
				header.setData("org.eclipse.e4.ui.css.CssClassName", "FormSectionHeader");
			}

			var collapseBtn = new Label(header, SWT.NONE);
			collapseBtn.setImage(EditorSharedImages.getImage(collapsed ? IMG_BULLET_EXPAND : IMG_BULLET_COLLAPSE));

			var title = new Label(header, SWT.NONE);
			title.setText(section.getName());
			title.setFont(SWTResourceManager.getBoldFont(title.getFont()));
			title.setData("org.eclipse.e4.ui.css.CssClassName", "FormSectionTitle");

			var control = section.createContent(this);
			control.setLayoutData(createControlGridData());
			control.setData("org.eclipse.e4.ui.css.CssClassName", "FormSectionBody");

			var toolbarManager = new ToolBarManager();
			section.fillToolbar(toolbarManager);
			var toolbar = toolbarManager.createControl(header);
			{
				toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

				if (toolbar.getItemCount() == 0) {
					((GridData) header.getLayoutData()).verticalIndent = 5;
				}
			}

			section.update_UI_from_Model();

			if (collapsed) {
				((GridData) control.getLayoutData()).heightHint = 0;
				control.setVisible(false);
			}

			var sep = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
			sep.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			var expandListener = new MouseAdapter() {

				@Override
				public void mouseUp(MouseEvent e) {
					_collapsed = !_collapsed;

					var img = EditorSharedImages.getImage(_collapsed ? IEditorSharedImages.IMG_BULLET_EXPAND
							: IEditorSharedImages.IMG_BULLET_COLLAPSE);

					collapseBtn.setImage(img);

					control.setVisible(!_collapsed);

					{
						var gd = createControlGridData();
						gd.heightHint = _collapsed ? 0 : SWT.DEFAULT;
						control.setLayoutData(gd);
					}

					control.requestLayout();

					if (_collapsed) {
						_collapsedSectionsIds.add(sectionId);
					} else {
						_collapsedSectionsIds.remove(sectionId);
					}

					updateScrolledComposite();

				}
			};

			title.addMouseListener(expandListener);
			collapseBtn.addMouseListener(expandListener);
		}

		private String getSectionId(FormPropertySection section) {
			return section.getClass().getSimpleName();
		}

		public FormPropertySection getSection() {
			return _section;
		}

		GridData createControlGridData() {
			var expands = _section.isFillSpace() && !_collapsed;
			return new GridData(SWT.FILL, expands ? SWT.FILL : SWT.TOP, true, expands);
		}

	}

	static Set<String> _collapsedSectionsIds = new HashSet<>();

	protected abstract List<FormPropertySection> createSections(Object obj);

	@Override
	public void createControl(Composite parent) {

		parent.setBackgroundMode(SWT.INHERIT_FORCE);

		_scrolledCompo = new ScrolledComposite(parent, SWT.V_SCROLL);
		_sectionsContainer = new Composite(_scrolledCompo, SWT.NONE);
		_sectionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);
		var layout = new GridLayout(1, false);
		layout.verticalSpacing = 0;
		_sectionsContainer.setLayout(layout);

		_scrolledCompo.setContent(_sectionsContainer);
		_scrolledCompo.setExpandVertical(true);
		_scrolledCompo.setExpandHorizontal(true);
		_scrolledCompo.addControlListener(ControlListener.controlResizedAdapter(e -> {
			updateScrolledComposite();
		}));
	}

	void updateScrolledComposite() {
		Rectangle r = _scrolledCompo.getClientArea();
		_scrolledCompo.setMinSize(_sectionsContainer.computeSize(r.width, SWT.DEFAULT));
	}

	@Override
	public Control getControl() {
		return _scrolledCompo;
	}

	@Override
	public void setFocus() {
		_scrolledCompo.setFocus();
	}
}
