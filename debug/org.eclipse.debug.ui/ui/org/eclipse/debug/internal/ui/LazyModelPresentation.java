/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.views.variables.IndexedVariablePartition;
import org.eclipse.debug.ui.IDebugEditorPresentation;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugModelPresentationExtension;
import org.eclipse.debug.ui.IInstructionPointerPresentation;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * A proxy to an IDebugModelPresentation extension. Instantiates the extension
 * when it is needed.
 */

public class LazyModelPresentation implements IDebugModelPresentation, IDebugEditorPresentation,
	IColorProvider, IFontProvider, IInstructionPointerPresentation, IDebugModelPresentationExtension {

	/**
	 * A temporary mapping of attribute ids to their values
	 * @see IDebugModelPresentation#setAttribute
	 */
	protected HashMap<String, Object> fAttributes = new HashMap<>(3);

	/**
	 * The config element that defines the extension
	 */
	protected IConfigurationElement fConfig = null;

	/**
	 * The actual presentation instance - null until called upon
	 */
	protected IDebugModelPresentation fPresentation = null;

	/**
	 * Temp holding for listeners - we do not add to presentation until
	 * it needs to be instantiated.
	 */
	protected ListenerList<ILabelProviderListener> fListeners = new ListenerList<>();

	/**
	 * Non-null when nested inside a delegating model presentation
	 */
	private DelegatingModelPresentation fOwner = null;


	@Override
	public void removeAnnotations(IEditorPart editorPart, IThread thread) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IDebugEditorPresentation) {
			((IDebugEditorPresentation)presentation).removeAnnotations(editorPart, thread);
		}
	}

	@Override
	public boolean addAnnotations(IEditorPart editorPart, IStackFrame frame) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IDebugEditorPresentation) {
			return ((IDebugEditorPresentation)presentation).addAnnotations(editorPart, frame);
		}
		return false;
	}

	/**
	 * Constructs a lazy presentation from the config element.
	 */
	public LazyModelPresentation(IConfigurationElement configElement) {
		fConfig = configElement;
	}

	/**
	 * Constructs a lazy presentation from the config element, owned by the specified
	 * delegating model presentation.
	 *
	 * @param parent owning presentation
	 * @param configElement XML configuration element
	 */
	public LazyModelPresentation(DelegatingModelPresentation parent, IConfigurationElement configElement) {
		this(configElement);
		fOwner = parent;
	}

	/**
	 * @see IDebugModelPresentation#getImage(Object)
	 */
	@Override
	public Image getImage(Object element) {
		initImageRegistry();
		Image image = getPresentation().getImage(element);
		if (image == null) {
			image = getDefaultImage(element);
		}
		if (image != null) {
			int flags= computeAdornmentFlags(element);
			if (flags > 0) {
				CompositeDebugImageDescriptor descriptor= new CompositeDebugImageDescriptor(image, flags);
				return DebugUIPlugin.getImageDescriptorRegistry().get(descriptor);
			}
		}
		return image;
	}

	/**
	 * Initializes the image registry
	 */
	private synchronized void initImageRegistry() {
		if (!DebugPluginImages.isInitialized()) {
			DebugUIPlugin.getDefault().getImageRegistry();
		}
	}

	/**
	 * Computes and return common adornment flags for the given element.
	 *
	 * @param element
	 * @return adornment flags defined in CompositeDebugImageDescriptor
	 */
	private int computeAdornmentFlags(Object element) {
		if (element instanceof IBreakpoint) {
			if (!DebugPlugin.getDefault().getBreakpointManager().isEnabled()) {
				return CompositeDebugImageDescriptor.SKIP_BREAKPOINT;
			}
		}
		return 0;
	}

	/**
	 * Returns a default text label for the debug element
	 */
	protected String getDefaultText(Object element) {
		return DebugUIPlugin.getDefaultLabelProvider().getText(element);
	}

	/**
	 * Returns a default image for the debug element
	 */
	protected Image getDefaultImage(Object element) {
		return DebugUIPlugin.getDefaultLabelProvider().getImage(element);
	}

	/**
	 * @see IDebugModelPresentation#getText(Object)
	 */
	@Override
	public String getText(Object element) {
		if (!(element instanceof IndexedVariablePartition)) {
			// Attempt to delegate
			String text = getPresentation().getText(element);
			if (text != null) {
				return text;
			}
		}
		// If no delegate returned a text label, use the default
		if (showVariableTypeNames()) {
			try {
				if (element instanceof IExpression) {
					StringBuilder buf = new StringBuilder();
					IValue value = ((IExpression)element).getValue();
					if (value != null) {
						String type = value.getReferenceTypeName();
						if (type != null && type.length() > 0) {
							buf.append(type);
							buf.append(' ');
						}
					}
					buf.append(getDefaultText(element));
					return buf.toString();
				} else if (element instanceof IVariable) {
					return new StringBuilder(((IVariable)element).getValue().getReferenceTypeName()).append(' ').append(getDefaultText(element)).toString();
				}
			} catch (DebugException de) {
				DebugUIPlugin.log(de);
			}
		}
		return getDefaultText(element);
	}

	/**
	 * Whether or not to show variable type names.
	 * This option is configured per model presentation.
	 * This allows this option to be set per view, for example.
	 */
	protected boolean showVariableTypeNames() {
		Boolean show = (Boolean) fAttributes.get(DISPLAY_VARIABLE_TYPE_NAMES);
		show = show == null ? Boolean.FALSE : show;
		return show.booleanValue();
	}

	/**
	 * @see IDebugModelPresentation#computeDetail(IValue, IValueDetailListener)
	 */
	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {
		getPresentation().computeDetail(value, listener);
	}

	/**
	 * @see ISourcePresentation#getEditorInput(Object)
	 */
	@Override
	public IEditorInput getEditorInput(Object element) {
		return getPresentation().getEditorInput(element);
	}

	/**
	 * @see ISourcePresentation#getEditorId(IEditorInput, Object)
	 */
	@Override
	public String getEditorId(IEditorInput input, Object inputObject) {
		return getPresentation().getEditorId(input, inputObject);
	}

	/**
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
		if (fPresentation != null) {
			getPresentation().addListener(listener);
		}
		fListeners.add(listener);
	}

	/**
	 * @see IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		if (fPresentation != null) {
			getPresentation().dispose();
		}
		fListeners = null;
	}

	/**
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		if (fPresentation != null) {
			return getPresentation().isLabelProperty(element, property);
		}
		return false;
	}

	/**
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
		if (fPresentation != null) {
			getPresentation().removeListener(listener);
		}
		ListenerList<ILabelProviderListener> listeners = fListeners;
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	/**
	 * Returns the real presentation, instantiating if required.
	 */
	protected IDebugModelPresentation getPresentation() {
		if (fPresentation == null) {
			synchronized (this) {
				if (fPresentation != null) {
					// In the case that the synchronization is enforced, the "blocked" thread
					// should return the presentation configured by the "owning" thread.
					return fPresentation;
				}
				try {
					IDebugModelPresentation tempPresentation= (IDebugModelPresentation) DebugUIPlugin.createExtension(fConfig, "class"); //$NON-NLS-1$
					// configure it
					if (fListeners != null) {
						for (ILabelProviderListener iLabelProviderListener : fListeners) {
							tempPresentation.addListener(iLabelProviderListener);
						}
					}
					for (Entry<String, Object> entry : fAttributes.entrySet()) {
						tempPresentation.setAttribute(entry.getKey(), entry.getValue());
					}
					// Only assign to the instance variable after it's been configured. Otherwise,
					// the synchronization is defeated (a thread could return the presentation before
					// it's been configured).
					fPresentation= tempPresentation;
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
				}
			}
		}
		return fPresentation;
	}

	/**
	 * @see IDebugModelPresentation#setAttribute(String, Object)
	 */
	@Override
	public void setAttribute(String id, Object value) {
		if (value == null) {
			return;
		}
		if (fPresentation != null) {
			getPresentation().setAttribute(id, value);
		}

		fAttributes.put(id, value);

		if (fOwner != null) {
			fOwner.basicSetAttribute(id, value);
		}
	}

	/**
	 * Returns the identifier of the debug model this
	 * presentation is registered for.
	 */
	public String getDebugModelIdentifier() {
		return fConfig.getAttribute("id"); //$NON-NLS-1$
	}

	/**
	 * Returns a new source viewer configuration for the details
	 * area of the variables view, or <code>null</code> if
	 * unspecified.
	 *
	 * @return source viewer configuration or <code>null</code>
	 * @exception CoreException if unable to create the specified
	 * 	source viewer configuration
	 */
	public SourceViewerConfiguration newDetailsViewerConfiguration() throws CoreException {
		String attr  = fConfig.getAttribute("detailsViewerConfiguration"); //$NON-NLS-1$
		if (attr != null) {
			return (SourceViewerConfiguration)fConfig.createExecutableExtension("detailsViewerConfiguration"); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Returns a copy of the attributes in this model presentation.
	 *
	 * @return a copy of the attributes in this model presentation
	 * @since 3.0
	 */
	public Map<String, Object> getAttributeMap() {
		return new HashMap<>(fAttributes);
	}

	/**
	 * Returns the raw attribute map
	 *
	 * @return the raw attribute map
	 */
	public Map<String, Object> getAttributes() {
		return fAttributes;
	}

	@Override
	public Color getForeground(Object element) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IColorProvider) {
			IColorProvider colorProvider = (IColorProvider) presentation;
			return colorProvider.getForeground(element);
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IColorProvider) {
			IColorProvider colorProvider = (IColorProvider) presentation;
			return colorProvider.getBackground(element);
		}
		return null;
	}

	@Override
	public Font getFont(Object element) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IFontProvider) {
			IFontProvider fontProvider = (IFontProvider) presentation;
			return fontProvider.getFont(element);
		}
		return null;
	}

	@Override
	public Annotation getInstructionPointerAnnotation(IEditorPart editorPart, IStackFrame frame) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IInstructionPointerPresentation) {
			IInstructionPointerPresentation pointerPresentation = (IInstructionPointerPresentation) presentation;
			return pointerPresentation.getInstructionPointerAnnotation(editorPart, frame);
		}
		return null;
	}

	@Override
	public String getInstructionPointerAnnotationType(IEditorPart editorPart, IStackFrame frame) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IInstructionPointerPresentation) {
			IInstructionPointerPresentation pointerPresentation = (IInstructionPointerPresentation) presentation;
			return pointerPresentation.getInstructionPointerAnnotationType(editorPart, frame);
		}
		return null;
	}

	@Override
	public Image getInstructionPointerImage(IEditorPart editorPart, IStackFrame frame) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IInstructionPointerPresentation) {
			IInstructionPointerPresentation pointerPresentation = (IInstructionPointerPresentation) presentation;
			return pointerPresentation.getInstructionPointerImage(editorPart, frame);
		}
		return null;
	}

	@Override
	public String getInstructionPointerText(IEditorPart editorPart, IStackFrame frame) {
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IInstructionPointerPresentation) {
			IInstructionPointerPresentation pointerPresentation = (IInstructionPointerPresentation) presentation;
			return pointerPresentation.getInstructionPointerText(editorPart, frame);
		}
		return null;
	}

	@Override
	public boolean requiresUIThread(Object element) {
		if (!DebugPluginImages.isInitialized()) {
			// need UI thread for breakpoint adornment and default images
			return true;
		}
		IDebugModelPresentation presentation = getPresentation();
		if (presentation instanceof IDebugModelPresentationExtension) {
			return ((IDebugModelPresentationExtension) presentation).requiresUIThread(element);
		}
		return false;
	}
}
