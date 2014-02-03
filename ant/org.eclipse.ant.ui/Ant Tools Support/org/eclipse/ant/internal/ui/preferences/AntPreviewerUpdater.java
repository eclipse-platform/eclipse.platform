/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.preferences;

import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;
import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlFormatter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Handles changes for Ant preview viewers.
 * 
 * @since 3.0
 */
class AntPreviewerUpdater {
	
	private Color fForegroundColor= null;
	private Color fBackgroundColor= null;
	private Color fSelectionBackgroundColor= null;
	private Color fSelectionForegroundColor= null;
	
	
	/**
	 * Creates a source preview updater for the given viewer, configuration and preference store.
	 *
	 * @param viewer the viewer
	 * @param configuration the configuration
	 * @param preferenceStore the preference store
	 */
	public AntPreviewerUpdater(final SourceViewer viewer, final AntSourceViewerConfiguration configuration, final IPreferenceStore preferenceStore) {
		
		initializeViewerColors(viewer, preferenceStore);
		
		final IPropertyChangeListener fontChangeListener= new IPropertyChangeListener() {
			/*
			 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
			 */
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(JFaceResources.TEXT_FONT)) {
					Font font= JFaceResources.getFont(JFaceResources.TEXT_FONT);
					viewer.getTextWidget().setFont(font);
				}
			}
		};
		final IPropertyChangeListener propertyChangeListener= new IPropertyChangeListener() {
			/*
			 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
			 */
			public void propertyChange(PropertyChangeEvent event) {
				
				String property= event.getProperty();
					
				if (AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND.equals(property) || AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT.equals(property) ||
						AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND.equals(property) ||	AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property) ||
						AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND.equals(property) || AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT.equals(property) ||
						AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND.equals(property) ||	AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT.equals(property))
					{
						initializeViewerColors(viewer, preferenceStore);
				} 
				
				if (configuration.affectsTextPresentation(event)) {
					configuration.adaptToPreferenceChange(event);
					viewer.invalidateTextPresentation();
				}
				
				if (FormattingPreferences.affectsFormatting(event)) {
					format(viewer, preferenceStore);
				}
			}

			/**
			 * @param viewer
			 * @param preferenceStore
			 */
			private void format(final SourceViewer sourceViewer, final IPreferenceStore store) {
				String contents= sourceViewer.getDocument().get();
				FormattingPreferences prefs= new FormattingPreferences();
				prefs.setPreferenceStore(store);
				contents= XmlFormatter.format(contents, prefs);
				viewer.getDocument().set(contents);
			}
		};
		viewer.getTextWidget().addDisposeListener(new DisposeListener() {
			/*
			 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			 */
			public void widgetDisposed(DisposeEvent e) {
				preferenceStore.removePropertyChangeListener(propertyChangeListener);
				JFaceResources.getFontRegistry().removeListener(fontChangeListener);
			}
		});
		JFaceResources.getFontRegistry().addListener(fontChangeListener);
		preferenceStore.addPropertyChangeListener(propertyChangeListener);
	}
	
	/**
	 * Initializes the given viewer's colors.
	 * 
	 * @param viewer the viewer to be initialized
	 * @since 2.0
	 */
	protected void initializeViewerColors(ISourceViewer viewer, IPreferenceStore store) {
			
		StyledText styledText= viewer.getTextWidget();
		
		// ----------- foreground color --------------------
		Color color= store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
			? null
			: createColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, styledText.getDisplay());
		styledText.setForeground(color);
			
		if (fForegroundColor != null) {
			fForegroundColor.dispose();
		}
		
		fForegroundColor= color;
		
		// ---------- background color ----------------------
		color= store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
			? null
			: createColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, styledText.getDisplay());
		styledText.setBackground(color);
			
		if (fBackgroundColor != null) {
			fBackgroundColor.dispose();
		}
			
		fBackgroundColor= color;
		
		// ----------- selection foreground color --------------------
		color= store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT)
			? null
			: createColor(store, AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND, styledText.getDisplay());
		styledText.setSelectionForeground(color);
			
		if (fSelectionForegroundColor != null) {
			fSelectionForegroundColor.dispose();
		}
		
		fSelectionForegroundColor= color;
		
		// ---------- selection background color ----------------------
		color= store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT)
			? null
			: createColor(store, AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND, styledText.getDisplay());
		styledText.setSelectionBackground(color);
			
		if (fSelectionBackgroundColor != null) {
			fSelectionBackgroundColor.dispose();
		}
			
		fSelectionBackgroundColor= color;
	}
	
	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 * 
	 * @param store the store to read from
	 * @param key the key used for the lookup in the preference store
	 * @param display the display used create the color
	 * @return the created color according to the specification in the preference store
	 * @since 2.0
	 */
	private Color createColor(IPreferenceStore store, String key, Display display) {
	
		RGB rgb= null;		
		
		if (store.contains(key)) {
			
			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);
		
			if (rgb != null)
				return new Color(display, rgb);
		}
		
		return null;
	}

	public void dispose() {
		if (fForegroundColor != null) {
			fForegroundColor.dispose();
			fForegroundColor= null;
		}
		
		if (fBackgroundColor != null) {
			fBackgroundColor.dispose();
			fBackgroundColor= null;
		}
		
		if (fSelectionForegroundColor != null) {
			fSelectionForegroundColor.dispose();
			fSelectionForegroundColor= null;
		}
		
		if (fSelectionBackgroundColor != null) {
			fSelectionBackgroundColor.dispose();
			fSelectionBackgroundColor= null;
		}
	}
}
