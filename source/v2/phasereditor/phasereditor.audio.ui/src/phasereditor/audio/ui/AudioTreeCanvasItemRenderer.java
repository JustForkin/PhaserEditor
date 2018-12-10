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
package phasereditor.audio.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import phasereditor.audio.core.AudioCore;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.FrameData;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class AudioTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	private Image _image;
	private String _label;
	private IFile _audioFile;

	public AudioTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);

		var data = _item.getData();

		_audioFile = getAudioFile(data);
		_label = getAudioLabel(data);
	}

	protected String getAudioLabel(Object data) {
		var file = getAudioFile(data);

		if (file == null) {
			return "";
		}

		return file.getName();
	}

	@SuppressWarnings("static-method")
	protected IFile getAudioFile(Object data) {
		return (IFile) data;
	}

	public String getLabel() {
		return _label;
	}

	public void setLabel(String label) {
		_label = label;
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var canvas = _item.getCanvas();

		if (_image == null) {
			var imgPath = AudioCore.getSoundWavesFile(_audioFile, false);
			_image = canvas.loadImage(imgPath.toFile());
		}

		var gc = e.gc;

		int rowHeight = computeRowHeight(canvas);

		int textHeight = gc.stringExtent("M").y;

		int textOffset = textHeight + 5;
		int imgHeight = rowHeight - textOffset - 10;

		if (imgHeight >= 16) {
			if (_image != null) {
				int w = e.width - x - 5;
				if (w > 0) {
					renderImage(gc, x, y, w, imgHeight);
				}
			}

			gc.setForeground(canvas.getForeground());
			gc.drawText(_label, x + 5, y + rowHeight - textOffset, true);
		}
	}

	public Image getImage() {
		return _image;
	}

	protected void renderImage(GC gc, int dstX, int dstY, int dstWidth, int dstHeight) {

		gc.drawImage(_image, 0, 0, _image.getBounds().width, _image.getBounds().height, dstX, dstY + 5, dstWidth,
				dstHeight);

		gc.setAlpha(100);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.fillGradientRectangle(dstX, dstY + 5, dstWidth, dstHeight, false);
		gc.setAlpha(255);
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {
		return canvas.getImageSize() + 32;
	}

	@Override
	public Image get_DND_Image() {
		return _image;
	}

	@Override
	public FrameData get_DND_Image_FrameData() {
		return FrameData.fromImage(_image);
	}

}