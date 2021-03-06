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
package phasereditor.assetpack.ui.editor;

import static java.util.stream.Collectors.toList;
import static phasereditor.ui.IEditorSharedImages.IMG_ADD;
import static phasereditor.ui.PhaserEditorUI.isZoomEvent;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import phasereditor.animation.ui.AnimationsCellRender;
import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SceneFileAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AudioSpriteAssetCellRenderer;
import phasereditor.assetpack.ui.BitmapFontAssetCellRenderer;
import phasereditor.assetpack.ui.preview.AtlasAssetFramesProvider;
import phasereditor.assetpack.ui.preview.MultiAtlasAssetFrameProvider;
import phasereditor.audio.ui.AudioCellRenderer;
import phasereditor.scene.ui.SceneUI;
import phasereditor.ui.BaseCanvas;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FrameCanvasUtils;
import phasereditor.ui.FrameCellRenderer;
import phasereditor.ui.FrameGridCellRenderer;
import phasereditor.ui.HandModeUtils;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.IconCellRenderer;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.LoadingCellRenderer;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.ScrollUtils;
import phasereditor.ui.SwtRM;

/**
 * @author arian
 *
 */
public class PackEditorCanvas extends BaseCanvas implements PaintListener, MouseMoveListener {

	private static final int MIN_ROW_HEIGHT = 48;
	private AssetPackModel _model;
	private int _imageSize;
	private Set<Object> _collapsed;
	private Map<Rectangle, Object> _collapseIconBoundsMap;
	private Font _boldFont;
	private List<AssetRenderInfo> _renderInfoList;
	private FrameCanvasUtils _utils;
	private AssetPackEditor _editor;
	private MyScrollUtils _scrollUtils;
	private boolean _loadingImagesInBackground;
	private HandModeUtils _handModeUtils;

	public PackEditorCanvas(AssetPackEditor editor, Composite parent, int style) {
		super(parent, style | SWT.V_SCROLL);

		addPaintListener(this);
		addListener(SWT.MouseVerticalWheel, this::mouseScrolled);
		addMouseMoveListener(this);

		_utils = new MyFrameUtils();
		_utils.setFilterInputWhenSetSelection(false);

		_scrollUtils = new MyScrollUtils();

		_handModeUtils = new HandModeUtils(this);

		_editor = editor;
		_imageSize = 96;
		_collapsed = new HashSet<>();
		_collapseIconBoundsMap = new HashMap<>();
		_boldFont = SwtRM.getBoldFont(getFont());

		_renderInfoList = new ArrayList<>();

		_loadingImagesInBackground = true;
	}

	class MyScrollUtils extends ScrollUtils {

		public MyScrollUtils() {
			super(PackEditorCanvas.this);
		}

		@Override
		public Rectangle computeScrollArea() {
			return PackEditorCanvas.this.computeScrollArea();
		}
	}

	class MyFrameUtils extends FrameCanvasUtils {

		public MyFrameUtils() {
			super(PackEditorCanvas.this, false);
		}

		@Override
		public int getFramesCount() {
			return _renderInfoList.size();
		}

		@Override
		public Rectangle getSelectionFrameArea(int index) {
			return _renderInfoList.get(index).bounds;
		}

		@Override
		public Point viewToModel(int x, int y) {
			return new Point(x, y - _scrollUtils.getOrigin().y);
		}

		@Override
		public Point modelToView(int x, int y) {
			return new Point(x, y + _scrollUtils.getOrigin().y);
		}

		@Override
		public Object getFrameObject(int index) {
			return _renderInfoList.get(index).asset;
		}

		@Override
		public ImageProxy get_DND_Image(int index) {
			return null;
		}

		@Override
		public void mouseUp(MouseEvent e) {

			if (_model != null && _model.getSections().isEmpty()) {
				_editor.launchAddSectionDialog();
				return;
			}

			var hit = false;

			var modelPointer = _utils.viewToModel(e.x, e.y);

			for (var action : _actions) {
				if (action.getBounds().contains(_modelPointer)) {
					action.run();
					hit = true;
					break;
				}
			}

			{
				for (var entry : _collapseIconBoundsMap.entrySet()) {
					if (entry.getKey().contains(modelPointer)) {
						var obj = entry.getValue();
						if (_collapsed.contains(obj)) {
							_collapsed.remove(obj);
						} else {
							_collapsed.add(obj);
						}
						hit = true;
					}
				}
			}

			if (hit) {
				updateScroll();
			} else {
				super.mouseUp(e);
			}
		}
	}

	public void updateScroll() {
		_scrollUtils.updateScroll();
	}

	private static int ROW_HEIGHT = 30;
	private static int ASSET_SPACING_X = 10;
	private static int ASSET_SPACING_Y = 30;
	private static int MARGIN_X = 30;
	private static int ASSETS_MARGIN_X = 240;

	private List<IconAction> _actions;

	private class IconAction {
		private Image _image;
		private Runnable _run;
		private int _x;
		private int _y;
		private Rectangle _bounds;

		public IconAction(String icon, Runnable run, int x, int y) {
			_image = EditorSharedImages.getImage(icon);
			_x = x;
			_y = y;
			_bounds = new Rectangle(x, y, 16, 16);
			_run = run;
		}

		public void run() {
			_run.run();
		}

		public Rectangle getBounds() {
			return _bounds;
		}

		public void paint(GC gc, boolean hover) {
			if (hover) {
				PhaserEditorUI.paintIconHoverBackground(gc, PackEditorCanvas.this, 16, _bounds);
			}
			gc.drawImage(_image, _x, _y);
		}
	}

	static class AssetRenderInfo {
		public AssetModel asset;
		public Rectangle bounds;
	}

	@Override
	public void paintControl(PaintEvent event) {

		_collapseIconBoundsMap = new HashMap<>();

		var renderInfoList = new ArrayList<AssetRenderInfo>();
		var actions = new ArrayList<IconAction>();

		var gc = event.gc;
		var clientArea = getClientArea();

		try {

			if (_model == null) {
				return;
			}

			if (_model.getSections().isEmpty()) {
				var str = "Click to add a Section";
				var size = gc.textExtent(str);
				var b = getClientArea();
				gc.drawText(str, b.width / 2 - size.x / 2, b.height / 2 - size.y / 2, true);
				return;
			}

			prepareGC(gc);

			gc.setAlpha(5);
			gc.setBackground(getForeground());
			gc.fillRectangle(0, 0, ASSETS_MARGIN_X - 20, clientArea.height);
			gc.setAlpha(10);
			gc.drawLine(ASSETS_MARGIN_X - 20, 0, ASSETS_MARGIN_X - 20, clientArea.height);
			gc.setAlpha(255);

			{
				Transform tx = new Transform(getDisplay());
				tx.translate(0, _scrollUtils.getOrigin().y);
				gc.setTransform(tx);
				tx.dispose();
			}

			var font = gc.getFont();

			var x = MARGIN_X;
			var y = 10;

			// paint objects

			for (var section : _model.getSections()) {

				x = MARGIN_X;

				{
					var collapsed = isCollapsed(section);

					gc.setFont(_boldFont);

					gc.drawText(section.getKey(), x + 20, y, true);

					renderCollapseIcon(section, gc, collapsed, x, y);

					gc.drawImage(AssetLabelProvider.GLOBAL_16.getImage(section), x, y);

					gc.setFont(font);

					var action = new IconAction(IMG_ADD, () -> {

						var manager = _editor.createAddAssetMenu(section);
						var menu = manager.createContextMenu(PackEditorCanvas.this);
						menu.setVisible(true);
						// _editor.openAddAssetButtonDialog(section, null);
					}, ASSETS_MARGIN_X - 40, y);

					action.paint(gc, action.getBounds().contains(_modelPointer));
					actions.add(action);

					y += ROW_HEIGHT;

					if (isCollapsed(section)) {
						continue;
					}
				}

				var types = new ArrayList<>(List.of(AssetType.values()));

				types.sort((a, b) -> {
					var a1 = sortValue(section, a);
					var b1 = sortValue(section, b);
					return Long.compare(a1, b1);
				});

				for (var type : types) {
					var group = section.getGroup(type);
					var assets = group.getAssets();

					if (assets.isEmpty()) {
						continue;
					}

					{
						var collapsed = isCollapsed(group);

						var title = type.getCapitalName();
						var size = gc.stringExtent(title);

						var y2 = y + ROW_HEIGHT / 2 - size.y / 2 - 3;

						gc.drawText(title, x + 20, y2, true);

						var count = section.getGroup(type).getAssets().size();

						gc.setAlpha(100);
						gc.drawText(" (" + count + ")", x + size.x + 20, y2, true);
						gc.setAlpha(255);

						renderCollapseIcon(group, gc, collapsed, x, y2);
						gc.drawImage(AssetLabelProvider.GLOBAL_16.getImage(type), x, y + 3);

						var action = new IconAction(IMG_ADD, () -> {

							_editor.openAddAssetDialog(section, type);

						}, ASSETS_MARGIN_X - 40, y + 5);

						action.paint(gc, action.getBounds().contains(_modelPointer));
						actions.add(action);

						if (collapsed) {
							y += ROW_HEIGHT;
							continue;
						}
					}

					{

						int assetX = ASSETS_MARGIN_X;
						int assetY = y;
						int bottom = y;

						var last = assets.isEmpty() ? null : assets.get(assets.size() - 1);
						for (var asset : assets) {

							Rectangle bounds;

							if (isFullRowAsset(asset)) {
								bounds = new Rectangle(assetX, assetY, clientArea.width - assetX - 10, _imageSize);
							} else {
								bounds = new Rectangle(assetX, assetY, _imageSize, _imageSize);
							}

							{
								var info = new AssetRenderInfo();
								info.asset = asset;
								info.bounds = bounds;
								renderInfoList.add(info);
							}

							bottom = Math.max(bottom, bounds.y + bounds.height);

							// gc.setAlpha(20);
							// gc.setBackground(Colors.color(0, 0, 0));
							// gc.fillRectangle(bounds);
							// gc.setAlpha(255);

							if (_utils.isSelected(asset)) {
								gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
								gc.fillRectangle(bounds);
							}

							var renderer = getAssetRenderer(asset);

							if (renderer != null) {
								try {
									renderer.render(this, gc, bounds.x, bounds.y, bounds.width, bounds.height);
								} catch (Exception e2) {
									e2.printStackTrace();
								}
							}

							if (_utils.getOverObject() == asset) {
								gc.drawRectangle(bounds);
							} else {
								gc.setAlpha(30);
								gc.drawRectangle(bounds);
								gc.setAlpha(255);
							}

							var key = asset.getKey();

							var key2 = key;

							for (int i = key.length(); i > 0; i--) {
								key2 = key.substring(0, i);
								var size = gc.textExtent(key2);
								if (size.x < bounds.width) {
									break;
								}
							}

							if (key2.length() < key.length()) {
								if (key2.length() > 2) {
									key2 = key2.substring(0, key2.length() - 2) + "..";
								}
							}

							gc.drawText(key2, assetX, assetY + _imageSize + 5, true);

							assetX += bounds.width + ASSET_SPACING_X;

							if (asset != last) {
								if (assetX + _imageSize > clientArea.width - 5) {
									assetX = ASSETS_MARGIN_X;
									assetY += _imageSize + ASSET_SPACING_Y;
								}
							}
						} // end of assets loop

						y = bottom + ASSET_SPACING_Y;
					} // end of not collapsed types
					y += 10;
				}
			}
		} finally {
			_renderInfoList = renderInfoList;
			_actions = actions;
		}
	}

	public Rectangle computeScrollArea() {
		var gc = new GC(this);
		var e = getClientArea();

		try {

			if (_model == null) {
				return new Rectangle(0, 0, 0, 0);
			}

			var y = 10;

			for (var section : _model.getSections()) {

				y += ROW_HEIGHT;

				if (isCollapsed(section)) {
					continue;
				}

				var types = new ArrayList<>(List.of(AssetType.values()));

				types.sort((a, b) -> {
					var a1 = sortValue(section, a);
					var b1 = sortValue(section, b);
					return Long.compare(a1, b1);
				});

				for (var type : types) {
					var group = section.getGroup(type);
					var assets = group.getAssets();

					if (assets.isEmpty()) {
						continue;
					}

					if (isCollapsed(group)) {
						y += ROW_HEIGHT;
						continue;
					}

					int assetX = ASSETS_MARGIN_X;
					int assetY = y;
					int bottom = y;

					var last = assets.isEmpty() ? null : assets.get(assets.size() - 1);
					for (var asset : assets) {

						Rectangle bounds;

						if (isFullRowAsset(asset)) {
							bounds = new Rectangle(assetX, assetY, e.width - assetX - 10, _imageSize);
						} else {
							bounds = new Rectangle(assetX, assetY, _imageSize, _imageSize);
						}

						bottom = Math.max(bottom, bounds.y + bounds.height);

						assetX += bounds.width + ASSET_SPACING_X;

						if (asset != last) {
							if (assetX + _imageSize > e.width - 5) {
								assetX = ASSETS_MARGIN_X;
								assetY += _imageSize + ASSET_SPACING_Y;
							}
						}
					} // end of assets loop

					y = bottom + ASSET_SPACING_Y;

					y += 10;
				}
			}

			return new Rectangle(0, y, e.width, y);
		} finally {
			gc.dispose();
		}
	}

	private boolean isCollapsed(Object obj) {
		return _collapsed.contains(obj);
	}

	private static int sortValue(AssetSectionModel section, AssetType type) {
		var assets = section.getGroup(type).getAssets();
		var v = AssetType.values().length - type.ordinal();
		if (assets.size() > 0) {
			v += 1000;
		}
		return -v;
	}

	private static boolean isFullRowAsset(AssetModel asset) {
		return asset instanceof AnimationsAssetModel || asset instanceof AudioAssetModel;
	}

	private void renderCollapseIcon(Object obj, GC gc, boolean collapsed, int x, int y) {
		var path = collapsed ? IEditorSharedImages.IMG_BULLET_EXPAND : IEditorSharedImages.IMG_BULLET_COLLAPSE;
		var icon = EditorSharedImages.getImage(path);
		gc.drawImage(icon, x - 20, y);
		var bounds = new Rectangle(0, y - 5, ASSETS_MARGIN_X - 45, 16 + 10);
		_collapseIconBoundsMap.put(bounds, obj);
		// gc.drawRectangle(bounds);
	}

	private ICanvasCellRenderer getAssetRenderer(AssetModel asset) {

		if (_loadingImagesInBackground) {
			return new LoadingCellRenderer();
		}

		if (asset instanceof ImageAssetModel) {
			var asset2 = (ImageAssetModel) asset;
			return new FrameCellRenderer(asset2.getUrlFile(), asset2.getFrame().getFrameData());
		} else if (asset instanceof SpritesheetAssetModel) {
			var asset2 = (SpritesheetAssetModel) asset;
			var file = asset2.getUrlFile();
			return new FrameCellRenderer(file, null);
		} else if (asset instanceof AtlasAssetModel) {
			var asset2 = (AtlasAssetModel) asset;
			return new FrameGridCellRenderer(new AtlasAssetFramesProvider(asset2));
		} else if (asset instanceof MultiAtlasAssetModel) {
			var asset2 = (MultiAtlasAssetModel) asset;
			return new FrameGridCellRenderer(new MultiAtlasAssetFrameProvider(asset2));
		} else if (asset instanceof AnimationsAssetModel) {
			var asset2 = (AnimationsAssetModel) asset;
			return new AnimationsCellRender(asset2.getAnimationsModel(), 5);
		} else if (asset.getClass() == AudioAssetModel.class) {
			var asset2 = (AudioAssetModel) asset;
			for (var url : asset2.getUrls()) {
				var audioFile = asset2.getFileFromUrl(url);
				if (audioFile != null) {
					return new AudioCellRenderer(audioFile, 5);
				}
			}
			return null;
		} else if (asset instanceof AudioSpriteAssetModel) {
			return new AudioSpriteAssetCellRenderer((AudioSpriteAssetModel) asset, 5);
		} else if (asset instanceof BitmapFontAssetModel) {
			return new BitmapFontAssetCellRenderer((BitmapFontAssetModel) asset);
		} else if (asset instanceof SceneFileAssetModel) {
			var renderer = getSceneScreenshot((SceneFileAssetModel) asset);
			if (renderer != null) {
				return renderer;
			}
		}

		return new IconCellRenderer(AssetLabelProvider.GLOBAL_64.getImage(asset));
	}

	private static FrameCellRenderer getSceneScreenshot(SceneFileAssetModel asset) {
		var screenFile = SceneUI.getSceneScreenshotFile(asset);

		if (screenFile != null) {
			return new FrameCellRenderer(screenFile, null);
		}

		return null;
	}

	public AssetPackModel getModel() {
		return _model;
	}

	public void setModel(AssetPackModel model) {
		_model = model;

		loadImagesInBackground();
	}

	private void loadImagesInBackground() {

		var frames = _model.getAssets().stream()

				.flatMap(a -> a.getSubElements().stream())

				.filter(a -> a instanceof IAssetFrameModel)

				.filter(a -> !(a instanceof SpritesheetAssetModel.FrameModel))

				.map(a -> (IAssetFrameModel) a)

				.collect(toList());

		var job = new Job("Loading Pack Editor images") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading Pack Editor images", frames.size());

				for (var frame : frames) {

					getDisplay().syncExec(() -> {
						var image = AssetPackUI.getImageProxy(frame);

						if (image != null) {
							image.getImage();
						}
					});

					try {
						Thread.sleep(2);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					monitor.worked(1);
				}

				_loadingImagesInBackground = false;

				swtRun(() -> {
					redraw();
				});

				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	public FrameCanvasUtils getUtils() {
		return _utils;
	}

	public void mouseScrolled(Event e) {
		if (!isZoomEvent(_handModeUtils, e)) {
			return;
		}

		e.doit = false;

		var before = _imageSize;

		var f = e.count < 0 ? 0.8 : 1.2;

		_imageSize = (int) (_imageSize * f);

		if (_imageSize < MIN_ROW_HEIGHT) {
			_imageSize = MIN_ROW_HEIGHT;
		}

		if (_imageSize != before) {
			updateScroll();
		}
	}

	public void reveal(AssetModel asset) {
		_collapsed.remove(asset.getSection());
		_collapsed.remove(asset.getGroup());

		swtRun(() -> {
			for (var info : _renderInfoList) {
				if (info.asset == asset) {
					_scrollUtils.scrollTo(info.bounds.y);
					return;
				}
			}
		});

	}

	private Point _modelPointer = new Point(-10_000, -10_000);

	@Override
	public void mouseMove(MouseEvent e) {
		_modelPointer = _utils.viewToModel(e.x, e.y);
		redraw();
	}
}
