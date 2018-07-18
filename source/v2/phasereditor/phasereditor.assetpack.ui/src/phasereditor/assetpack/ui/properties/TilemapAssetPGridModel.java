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
package phasereditor.assetpack.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.ui.properties.PGridStringProperty;

/**
 * @author arian
 *
 */
public class TilemapAssetPGridModel extends BaseAssetPGridModel<TilemapAssetModel> {

	public TilemapAssetPGridModel(TilemapAssetModel asset) {
		super(asset);

		addSection("Tilemap",

				createKeyProperty(),

				new PGridStringProperty("url", "url", getAsset().getHelp("url")) {

					@Override
					public void setValue(String value, boolean notify) {
						getAsset().setUrl(value);
					}

					@Override
					public String getValue() {
						return getAsset().getUrl();
					}

					@Override
					public boolean isModified() {
						return true;
					}

					@Override
					public CellEditor createCellEditor(Composite parent, Object element) {
						return new TilemapUrlCellEditor(parent, getAsset());
					}
				}

		);
	}

}
