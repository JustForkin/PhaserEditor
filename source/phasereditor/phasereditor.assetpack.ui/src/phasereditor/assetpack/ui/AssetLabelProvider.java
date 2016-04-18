// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.ScriptAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;
import phasereditor.audio.core.AudioCore;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageFileCache;
import phasereditor.ui.PhaserEditorUI;

public class AssetLabelProvider extends LabelProvider implements IEditorSharedImages {
	private final int _iconSize;
	private final Rectangle _iconRect;
	private WorkbenchLabelProvider _workbenchLabelProvider;
	private ImageFileCache _cache = new ImageFileCache();
	private Control _control;
	private final boolean _keepRatio;

	public AssetLabelProvider(int iconSize, boolean keepRatio) {
		_iconSize = iconSize;
		_keepRatio = keepRatio;
		_iconRect = new Rectangle(0, 0, _iconSize, _iconSize);
		_workbenchLabelProvider = new WorkbenchLabelProvider();
	}

	public AssetLabelProvider() {
		// TODO: we should query for the best screen/resolution icon.
		this(16, false);
	}

	public void setControl(Control control) {
		_control = control;
	}

	public Control getControl() {
		return _control;
	}

	public ImageFileCache getCache() {
		return _cache;
	}

	@Override
	public void dispose() {
		super.dispose();
		_cache.dispose();
	}

	public static Image getFileImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	}

	public static Image getFolderImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	}

	public static Image getElementImage() {
		return EditorSharedImages.getImage(IMG_ASSET_ELEM_KEY);
	}

	public static Image getKeyImage() {
		return EditorSharedImages.getImage(IMG_ASSET_KEY);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Image getImage(Object element) {
		if (element instanceof IResource) {
			return _workbenchLabelProvider.getImage(element);
		}

		{
			// the simple image assets

			IFile file = null;
			if (element instanceof ImageAssetModel) {
				file = ((ImageAssetModel) element).getUrlFile();
			}

			if (element instanceof AtlasAssetModel) {
				file = ((AtlasAssetModel) element).getTextureFile();
			}

			if (element instanceof BitmapFontAssetModel) {
				file = ((BitmapFontAssetModel) element).getTextureFile();
			}

			if (file != null) {
				try {
					Image orig = getFileImage(file);
					Image copy = scaleDownImage(orig, _keepRatio);
					_cache.addExtraImageToDispose(copy);
					return copy;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof AudioAssetModel) {
			AudioAssetModel asset = (AudioAssetModel) element;
			List<IFile> files = asset.getFilesFromUrls(asset.getUrls());
			Path wavesFile = null;
			for (IFile file : files) {
				wavesFile = AudioCore.getSoundWavesFile(file, false);
				if (wavesFile != null) {
					break;
				}
			}
			if (wavesFile != null) {
				Image orig = getFileImage(wavesFile);
				Image copy = new Image(Display.getCurrent(), _iconSize, _iconSize);
				GC gc = new GC(copy);
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
				gc.setXORMode(true);
				gc.drawImage(orig, 0, 0, orig.getBounds().width, orig.getBounds().height, 0, 0, _iconSize, _iconSize);
				gc.dispose();
				_cache.addExtraImageToDispose(copy);
				return copy;
			}
		}

		if (element instanceof SpritesheetAssetModel) {
			SpritesheetAssetModel asset = (SpritesheetAssetModel) element;
			IFile file = asset.getUrlFile();
			if (file != null) {
				try {
					Image img = getFileImage(file);
					if (img != null) {
						Rectangle b = img.getBounds();
						List<FrameData> frames = AssetPackUI.generateSpriteSheetRects(asset, b, b);
						if (frames.isEmpty()) {
							return img;
						}
						FrameData fd = frames.get(0);
						return buildFrameImage(img, fd);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof FrameItem) {
			FrameItem frame = (FrameItem) element;
			IFile file = ((AtlasAssetModel) frame.getAsset()).getTextureFile();
			if (file != null) {
				try {
					Image texture = getFileImage(file);
					FrameData fd = new FrameData();
					fd.src = new Rectangle(frame.getFrameX(), frame.getFrameY(), frame.getFrameW(), frame.getFrameH());
					return buildFrameImage(texture, fd);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
			SpritesheetAssetModel asset = (frame).getAsset();
			IFile file = asset.getUrlFile();
			if (file != null) {
				try {
					Image texture = getFileImage(file);
					FrameData fd = new FrameData();
					Rectangle b = frame.getBounds();
					fd.src = b;
					fd.dst = new Rectangle(0, 0, b.width, b.height);
					return buildFrameImage(texture, fd);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof ScriptAssetModel) {
			IFile file = ((ScriptAssetModel) element).getUrlFile();
			if (file != null) {
				return WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(file);
			}
		}

		if (element instanceof AssetSectionModel) {
			return EditorSharedImages.getImage(IMG_ASSET_FOLDER);
		}

		if (element instanceof AssetGroupModel) {
			return EditorSharedImages.getImage(IMG_ASSET_FOLDER);
		}

		if (element instanceof AssetModel) {
			return getKeyImage();
		}

		if (element instanceof AssetPackModel) {
			return EditorSharedImages.getImage(IMG_PACKAGE);
		}

		if (element instanceof IAssetElementModel) {
			return getElementImage();
		}

		return getFolderImage();
	}

	private Image scaleDownImage(Image orig, boolean keepRatio) {
		Rectangle src = orig.getBounds();
		Rectangle dst = _iconRect;
		Image img2 = new Image(Display.getCurrent(), dst.width, dst.height);
		GC gc = new GC(img2);
		scaleDraw(gc, orig, src, keepRatio);
		gc.dispose();
		_cache.addExtraImageToDispose(img2);
		return img2;
	}

	private void scaleDraw(GC gc, Image orig, Rectangle src, boolean keepRatio) {
		Rectangle dst = _iconRect;
		Rectangle zoom;
		if (_control != null) {
			Color c = _control.getBackground();
			gc.setBackground(c);
			gc.fillRectangle(_iconRect);
		}
		if (keepRatio && dst.width < src.width && dst.height < src.height) {
			zoom = PhaserEditorUI.computeImageZoom(src, dst);
		} else {
			zoom = dst;
		}
		gc.drawImage(orig, src.x, src.y, src.width, src.height, zoom.x, zoom.y, zoom.width, zoom.height);
	}

	private Image buildFrameImage(Image texture, FrameData fd) {
		Image frameImg = new Image(Display.getCurrent(), _iconSize, _iconSize);
		GC gc = new GC(frameImg);
		if (_control != null) {
			Color c = _control.getBackground();
			gc.setBackground(c);
			gc.fillRectangle(_iconRect);
		}
		if (_keepRatio) {
			Rectangle z = PhaserEditorUI.computeImageZoom(fd.src, _iconRect);
			gc.drawImage(texture, fd.src.x, fd.src.y, fd.src.width, fd.src.height, z.x, z.y, z.width, z.height);
		} else {
			gc.drawImage(texture, fd.src.x, fd.src.y, fd.src.width, fd.src.height, 0, 0, _iconSize, _iconSize);
		}
		gc.dispose();
		_cache.addExtraImageToDispose(frameImg);
		return frameImg;

	}

	private Image getFileImage(IFile file) {
		return _cache.getImage(file);
	}

	private Image getFileImage(Path file) {
		return _cache.getImage(file);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IResource) {
			return _workbenchLabelProvider.getText(element);
		}

		if (element instanceof AssetSectionModel) {
			return ((AssetSectionModel) element).getKey();
		}

		if (element instanceof AssetGroupModel) {
			return ((AssetGroupModel) element).getType().name();
		}

		if (element instanceof AssetModel) {
			AssetModel asset = (AssetModel) element;
			return asset.getKey();
		}

		if (element instanceof AssetPackModel) {
			AssetPackModel pack = (AssetPackModel) element;
			return pack.getFile().getProject().getName() + "/" + pack.getName();
		}

		if (element instanceof IAssetElementModel) {
			return ((IAssetElementModel) element).getName();
		}

		return super.getText(element);
	}
}