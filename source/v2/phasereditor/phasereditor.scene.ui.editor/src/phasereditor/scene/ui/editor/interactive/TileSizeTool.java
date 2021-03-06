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
package phasereditor.scene.ui.editor.interactive;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;

import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.SwtRM;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class TileSizeTool extends InteractiveTool {

	private static final int BOX = 14;
	private int _globalX;
	private int _globalY;
	private int _initialGlobalY;
	private int _initialGlobalX;
	private boolean _changeX;
	private boolean _changeY;

	public TileSizeTool(SceneEditor editor, boolean changeX, boolean changeY) {
		super(editor);

		_changeX = changeX;
		_changeY = changeY;
	}

	@Override
	protected boolean canEdit(ObjectModel model) {
		return model instanceof TileSpriteModel;
	}

	@Override
	public void render(GC gc) {
		var renderer = getRenderer();

		var globalX = 0;
		var globalY = 0;
		var globalAngle = 0f;

		for (var model : getModels()) {
			var modelX = 0;
			var modelY = 0;

			var width = TileSpriteComponent.get_width(model);
			var height = TileSpriteComponent.get_height(model);

			globalAngle += renderer.globalAngle(model);

			float[] xy;

			if (_changeX && _changeY) {
				xy = renderer.localToScene(model, modelX + width, modelY + height);
			} else if (_changeX) {
				xy = renderer.localToScene(model, modelX + width, modelY + height / 2);
			} else {
				xy = renderer.localToScene(model, modelX + width / 2, modelY + height);
			}

			globalX += (int) xy[0];
			globalY += (int) xy[1];

		}

		var size = getModels().size();

		globalX = globalX / size;
		globalY = globalY / size;

		globalAngle = globalAngle / size;

		_globalX = globalX;
		_globalY = globalY;

		// paint

		if (doPaint()) {
			var color = SwtRM
					.getColor(_changeX && _changeY ? SWT.COLOR_YELLOW : (_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN));

			var darkColor = _hightlights ? color
					: SwtRM.getColor(SWT.COLOR_DARK_YELLOW);

			drawRect(gc, globalX, globalY, globalAngle + (_changeY ? 90 : 0), BOX, color, darkColor);
		}

	}

	@Override
	public boolean contains(int sceneX, int sceneY) {

		if (_dragging) {
			return true;
		}

		var contains = PhaserEditorUI.distance(sceneX, sceneY, _globalX, _globalY) <= BOX;

		_hightlights = contains;

		return contains;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (_dragging && contains(e.x, e.y)) {

			for (var model : getModels()) {

				var initialLocalXY = (float[]) model.get("initial-local-xy");
				var localXY = getRenderer().sceneToLocal(model, e.x, e.y);

				var dx = localXY[0] - initialLocalXY[0];
				var dy = localXY[1] - initialLocalXY[1];

				var initialWidth = (float) model.get("initial-width");
				var initialHeight = (float) model.get("initial-height");

				var width = initialWidth + dx;
				var height = initialHeight + dy;

				{
					// snap
					var sceneModel = getEditor().getSceneModel();
					width = sceneModel.snapValueX(width);
					height = sceneModel.snapValueY(height);
				}

				if (width <= 0 || height <= 0) {
					continue;
				}

				if (_changeX) {
					TileSpriteComponent.set_width(model, width);
				}

				if (_changeY) {
					TileSpriteComponent.set_height(model, height);
				}

				GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);
			}

//			getEditor().updatePropertyPagesContentWithSelection();
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (contains(e.x, e.y)) {

			_dragging = true;

			_initialGlobalX = _globalX;
			_initialGlobalY = _globalY;

			for (var model : getModels()) {
				model.put("initial-width", TileSpriteComponent.get_width(model));
				model.put("initial-height", TileSpriteComponent.get_height(model));

				var xy = getRenderer().sceneToLocal(model, _initialGlobalX, _initialGlobalY);
				model.put("initial-local-xy", xy);

			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (_dragging) {

			var editor = getEditor();

			getModels().forEach(model -> {

				model.put("final-width", TileSpriteComponent.get_width(model));
				model.put("final-height", TileSpriteComponent.get_height(model));

				TileSpriteComponent.set_width(model, (float) model.get("initial-width"));
				TileSpriteComponent.set_height(model, (float) model.get("initial-height"));

			});

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			getModels().forEach(model -> {

				TileSpriteComponent.set_width(model, (float) model.get("final-width"));
				TileSpriteComponent.set_height(model, (float) model.get("final-height"));

			});

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set tile position.", true));

			editor.setDirty(true);

			if (editor.getOutline() != null) {
				editor.refreshOutline_basedOnId();
			}

			getScene().redraw();

		}
		_dragging = false;
	}

}
