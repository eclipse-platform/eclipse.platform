/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ui.internal;

import static org.eclipse.tips.ui.internal.DefaultTipManager.getImage;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.core.internal.LogUtil;
import org.eclipse.tips.core.internal.TipManager;
import org.eclipse.tips.ui.internal.util.ImageUtil;

@SuppressWarnings("restriction")
public class Slider extends Composite {

	private static final int RESIZE_RELOAD_DELAY = 100;
	private final Composite fScroller;
	private TipProvider fSelectedProvider;
	private final int fSpacing = 5;
	private int fSliderIndex = 0;
	private final List<ProviderSelectionListener> fListeners = new ArrayList<>();
	private TipManager fTipManager;
	private final Button fLeftButton;
	private final Button fRightButton;
	private Composite fSelectedProviderButton;
	private final HashMap<String, Image> fProviderImageCache = new HashMap<>();
	private int fIconSize = 48;
	private PropertyChangeListener fPropertyChangeListener;
	private final int fLeftRightButtonWidth;
	private long fLastResizeEventTime;
	private boolean fResizeRequestPending;
	private final ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources(), this);

	/**
	 * Constructor for the Slider widget.
	 *
	 * @param parent the parent
	 * @param style  the SWT style bits
	 */
	public Slider(Composite parent, int style) {
		super(parent, style);

		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		setLayout(layout);

		fLeftButton = new Button(this, SWT.FLAT);
		fLeftRightButtonWidth = fIconSize / 2 + 8;
		setLeftRightButtonGridData(fLeftButton, fLeftRightButtonWidth);
		fLeftButton.setImage(getImage("icons/" + fIconSize + "/aleft.png", resourceManager)); //$NON-NLS-1$ //$NON-NLS-2$
		fLeftButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> scrollLeft()));

		fScroller = new Composite(this, SWT.DOUBLE_BUFFERED);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		layoutData.heightHint = fIconSize + 4;
		fScroller.setLayoutData(layoutData);

		fRightButton = new Button(this, SWT.FLAT);
		setLeftRightButtonGridData(fRightButton, fLeftRightButtonWidth);
		fRightButton.setImage(getImage("icons/" + fIconSize + "/aright.png", resourceManager)); //$NON-NLS-1$ //$NON-NLS-2$
		fRightButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> scrollRight()));

		setupDisposeListener();
		setupProviderListener();
		setupResizeListener();
	}

	// Recalculation of slider is expensive. Make sure it is done only once when
	// resizing.
	private void setupResizeListener() {
		addListener(SWT.Resize, event -> {
			fLastResizeEventTime = System.currentTimeMillis();
			if (!fResizeRequestPending) {
				fResizeRequestPending = true;
				submitResizeExecution();
			}
		});
	}

	private void submitResizeExecution() {
		getDisplay().timerExec(RESIZE_RELOAD_DELAY / 2, () -> {
			long currentTime = System.currentTimeMillis();
			if (currentTime - fLastResizeEventTime > RESIZE_RELOAD_DELAY) {
				load();
				fResizeRequestPending = false;
			}
			if (fResizeRequestPending) {
				submitResizeExecution();
			}
		});
	}

	private void setLeftRightButtonGridData(Button pButton, int pWidth) {
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gridData.widthHint = pWidth;
		gridData.heightHint = fIconSize;
		pButton.setLayoutData(gridData);
	}

	private void setupDisposeListener() {
		addListener(SWT.Dispose, event -> {
			fTipManager.getChangeSupport().removePropertyChangeListener(fPropertyChangeListener);
			fProviderImageCache.values().forEach(img -> {
				if (!img.isDisposed()) {
					img.dispose();
				}
			});
		});
	}

	private void setupProviderListener() {
		fPropertyChangeListener = provider -> {
			if (!isDisposed()) {
				getDisplay().asyncExec(this::load);
			}
		};
	}

	/**
	 * Loads or reloads the provider list. If you want to update the read count of
	 * the current button or all buttons then check the {@link #updateButton()} and
	 * {@link #updateButtons()} methods.
	 */
	public void load() {
		if (isDisposed() || fScroller.isDisposed() || fTipManager == null) {
			return;
		}
		Arrays.stream(fScroller.getChildren()).filter(control -> !control.isDisposed()).forEach(Control::dispose);
		List<TipProvider> providers = fTipManager.getProviders();
		int spaceCount = Math.floorDiv(fScroller.getBounds().width, (fIconSize + fSpacing));
		int providerCount = providers.size();

		if (fSliderIndex > 0 && spaceCount >= providerCount) {
			fSliderIndex = 0;
		}

		if (spaceCount >= providerCount) {
			if (fRightButton.isEnabled()) {
				enableLeftRightButtons(false);
			}
		} else {
			if (!fRightButton.isEnabled()) {
				enableLeftRightButtons(true);
			}
		}

		int emptyPixelsLeft = Math.floorMod(fScroller.getBounds().width, (fIconSize + fSpacing));
		int newSpacing = fSpacing + (emptyPixelsLeft / (spaceCount + 1));
		for (int i = 0; i < Math.min(providerCount - fSliderIndex, spaceCount); i++) {
			TipProvider provider = providers.get(i + fSliderIndex);
			if (fSelectedProvider == null && !provider.getTips().isEmpty()) {
				fSelectedProvider = provider;
				notifyListeners(fSelectedProvider);
			}
			createProviderButton(providers.get(i + fSliderIndex), newSpacing, i);
		}
		setBackground(fScroller.getBackground());
	}

	private void enableLeftRightButtons(boolean enable) {
		fRightButton.setEnabled(enable);
		fRightButton.setVisible(enable);
		setLeftRightButtonGridData(fRightButton, enable ? fLeftRightButtonWidth : 0);
		fLeftButton.setEnabled(enable);
		fLeftButton.setVisible(enable);
		setLeftRightButtonGridData(fLeftButton, enable ? fLeftRightButtonWidth : 0);
		fRightButton.requestLayout();
		fLeftButton.requestLayout();
	}

	private Composite createProviderButton(TipProvider provider, int spacing, int index) {
		Composite button = new Composite(fScroller, SWT.DOUBLE_BUFFERED);
		button.setToolTipText(provider.getDescription());
		button.setBackground(fScroller.getBackground());
		button.setSize(fIconSize + 4, fIconSize + 4);
		button.setLocation((index * (fIconSize + spacing) + spacing - fSpacing), 2);
		button.addPaintListener(e -> {
			if (fSelectedProvider == provider) {
				fSelectedProviderButton = button;
			}
			paintButton(e.gc, button, provider);
		});
		button.addListener(SWT.MouseEnter, event -> button.redraw());
		button.addListener(SWT.MouseExit, event -> button.redraw());
		button.addListener(SWT.MouseUp, event -> {
			if (fSelectedProvider == provider) {
				return;
			}
			fSelectedProvider = provider;
			if (fSelectedProviderButton != null && !fSelectedProviderButton.isDisposed()) {
				fSelectedProviderButton.redraw();
			}
			fSelectedProviderButton = button;
			fSelectedProviderButton.redraw();
			notifyListeners(provider);
		});
		if (fSelectedProvider == provider) {
			fSelectedProviderButton = button;
		}
		return button;
	}

	/**
	 * Updates the read count of the currently selected button.
	 */
	public void updateButton() {
		if (fSelectedProviderButton != null && !fSelectedProviderButton.isDisposed()) {
			getDisplay().asyncExec(() -> fSelectedProviderButton.redraw());
		}
	}

	/**
	 * Calls redraw on all buttons to update the badge counter.
	 */
	public void updateButtons() {
		if (!isDisposed()) {
			getDisplay().asyncExec(() -> {
				if (!fScroller.isDisposed()) {
					for (Control control : fScroller.getChildren()) {
						if (control instanceof Composite && !control.isDisposed()) {
							control.redraw();
						}
					}
				}
			});
		}
	}

	/**
	 * Sets the {@link TipManager}.
	 *
	 * @param tipManager the {@link TipManager}
	 * @return this
	 */
	public Slider setTipManager(TipManager tipManager) {
		fTipManager = tipManager;
		fTipManager.getChangeSupport().addPropertyChangeListener(TipProvider.PROP_READY, fPropertyChangeListener);
		fIconSize = 48;
		load();
		return this;
	}

	private void notifyListeners(TipProvider provider) {
		fListeners.forEach(listener -> {
			try {
				listener.selected(provider);
			} catch (Exception e) {
				fTipManager.log(LogUtil.error(getClass(), e));
			}
		});
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(fIconSize * 3, fIconSize + fSpacing + fSpacing);
	}

	protected void scrollRight() {
		if (fSliderIndex < fTipManager.getProviders().size() - 1) {
			fSliderIndex++;
			load();
		}
	}

	protected void scrollLeft() {
		if (fSliderIndex > 0) {
			fSliderIndex--;
			load();
		}
	}

	/**
	 * @return the current selected {@link TipProvider}.
	 */
	public TipProvider getTipProvider() {
		return fSelectedProvider;
	}

	/**
	 * Sets the passed provider as the selected provider in the slider.
	 *
	 * @param provider the new provider for the slider.
	 *
	 * @return this
	 */
	public Slider setTipProvider(TipProvider provider) {
		fSelectedProvider = provider;
		updateButtons();
		return this;
	}

	/**
	 * Adds the listener to the list of listeners to be called when an event with a
	 * {@link TipProvider} occurs.
	 *
	 * @param listener the {@link ProviderSelectionListener}
	 * @return this
	 */
	public Slider addTipProviderListener(ProviderSelectionListener listener) {
		fListeners.add(listener);
		return this;
	}

	/**
	 * Removes the listener from the list.
	 *
	 * @param listener the {@link ProviderSelectionListener}
	 * @return this
	 */
	public Slider removeTipProviderListener(ProviderSelectionListener listener) {
		fListeners.remove(listener);
		return this;
	}

	private void paintButton(GC gc, Composite providerButton, TipProvider provider) {
		gc.setAdvanced(true);
		if (!gc.getAdvanced()) {
			throw new RuntimeException(Messages.Slider_13);
		}
		if (provider.equals(fSelectedProvider)) {
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
			gc.drawRectangle(0, 0, fIconSize + 3, fIconSize + 3);
		} else {
			gc.setForeground(fLeftButton.getForeground());
			gc.setBackground(fLeftButton.getBackground());
			boolean mouseIn = getDisplay().getCursorControl() == providerButton;
			if (mouseIn) {
				gc.drawRectangle(0, 0, fIconSize + 3, fIconSize + 3);
			} else {
				gc.setBackground(fScroller.getBackground());
			}
		}
		gc.fillRectangle(2, 2, fIconSize, fIconSize);
		Image overlay = getUnreadOverlay(provider);
		gc.drawImage(overlay, 2, 2);
	}

	private TipImage selectProviderImage(TipProvider provider) {
		return provider.getImage();
	}

	private Image getProviderImage(TipProvider provider, TipImage image) {
		return fProviderImageCache.computeIfAbsent(provider.getID(), id -> {
			try {
				return new Image(getDisplay(), ImageUtil.decodeToImage(image.getBase64Image()));
			} catch (Exception e) {
				fTipManager.log(LogUtil.error(getClass(), e));
				return null;
			}
		});
	}

	private Image getUnreadOverlay(TipProvider provider) {
		Image providerImage = getProviderImage(provider, selectProviderImage(provider));
		if (provider.getTips().isEmpty()) {
			return providerImage;
		}
		int tipCount = provider.getTips().size();
		int backgroundColor = fTipManager.mustServeReadTips() ? SWT.COLOR_DARK_GREEN : SWT.COLOR_RED;
		ImageDescriptor numberOverlays = new CircleNumberDescriptor(tipCount, backgroundColor);
		ImageDescriptor overlay = new DecorationOverlayIcon(providerImage, numberOverlays, IDecoration.TOP_RIGHT);
		return resourceManager.get(overlay);
	}

	private static final class CircleNumberDescriptor extends ImageDescriptor {
		private final int number;
		private final int backgroundColor;

		CircleNumberDescriptor(int number, int backgroundColor) {
			this.number = number;
			this.backgroundColor = backgroundColor;
		}

		@Override
		public ImageData getImageData(int zoom) {
			Display display = Display.getCurrent();
			Font font = display.getSystemFont();
			Font boldFont = FontDescriptor.createFrom(font).withStyle(SWT.BOLD).createFont(display);

			String text = Integer.toString(number);
			Point textExtent = getTextSize(text, boldFont);

			int imageHeight = textExtent.y + 5;
			int imageWidth = number > 9 ? textExtent.x + 8 : imageHeight;

			/* Create a 24 bit image data with alpha channel */
			PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
			ImageData transparentBackground = new ImageData(imageWidth, imageHeight, 24, palette);
			transparentBackground.alpha = 0;

			Image image = new Image(display, transparentBackground);
			GC gc = new GC(image);
			gc.setAdvanced(true);

			gc.setBackground(display.getSystemColor(backgroundColor));
			gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.setFont(boldFont);
			gc.setTextAntialias(SWT.ON);
			gc.fillOval(0, 0, imageWidth, imageHeight);
			int textX = number > 9 ? 4 : (imageHeight - textExtent.x + 1) / 2;
			gc.drawText(text, textX, 2, true);
			gc.dispose();
			boldFont.dispose();
			ImageData data = image.getImageData(zoom);
			image.dispose();
			return data;
		}

		@Override
		public int hashCode() {
			return Objects.hash(number, backgroundColor);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			return (obj instanceof CircleNumberDescriptor other) //
					&& number == other.number && backgroundColor == other.backgroundColor;
		}
	}

	private static Point getTextSize(String text, Font font) {
		GC gc2 = new GC(font.getDevice());
		gc2.setFont(font);
		Point textExtent = gc2.textExtent(text);
		gc2.dispose();
		return textExtent;
	}
}