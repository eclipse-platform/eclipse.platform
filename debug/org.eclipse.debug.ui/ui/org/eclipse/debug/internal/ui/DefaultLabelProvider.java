/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     QNX Software Systems - Mikhail Khodjaiants - Registers View (Bug 53640)
 *     Valentin Ciocoi - Bug 414994 Guard against out of bounds exception when handling escaped characters in DefaultLabelProvider
 *******************************************************************************/
package org.eclipse.debug.internal.ui;


import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IRegister;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.core.model.IWatchpoint;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.internal.ui.views.variables.IndexedVariablePartition;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class DefaultLabelProvider implements ILabelProvider {

	/**
	 * Maps image descriptors to images.
	 */
	private Map<ImageDescriptor, Image> fImages = new HashMap<>();

	/**
	 * @see ILabelProvider#getImage(Object)
	 */
	@Override
	public Image getImage(Object element) {
		String key= getImageKey(element);
		if (key == null && element instanceof ILaunch) {
			return null;
		}
		if (key == null && element instanceof IAdaptable) {
			IWorkbenchAdapter de= ((IAdaptable) element).getAdapter(IWorkbenchAdapter.class);
			if (de != null) {
				ImageDescriptor descriptor= de.getImageDescriptor(element);
				if( descriptor != null) {
					return getImage(descriptor);
				}
			}
			return null;
		}
		if(element instanceof LaunchShortcutExtension) {
			return getImage(((LaunchShortcutExtension)element).getImageDescriptor());
		}
		return DebugPluginImages.getImage(key);
	}

	/**
	 * Returns an image created from the given image descriptor or <code>null</code>.
	 * Caches and reuses images.
	 *
	 * @param descriptor image descriptor
	 * @return image or <code>null</code>
	 */
	private Image getImage(ImageDescriptor descriptor) {
		Image image = fImages.get(descriptor);
		if (image != null) {
			return image;
		}
		image = descriptor.createImage();
		if (image != null) {
			fImages.put(descriptor, image);
		}
		return image;
	}

	/**
	 * Returns the key (<code>String</code>) of the default image
	 * appropriate for the given element or <code>null</code>
	 * if no default image is defined.
	 */
	public String getImageKey(Object element) {
		if (element instanceof IDebugElement) {
			// Group elements into debug elements and non-debug elements
			// to reduce the number of instanceof checks performed
			if (element instanceof IRegister) {
				return IDebugUIConstants.IMG_OBJS_REGISTER;
			} else if (element instanceof IRegisterGroup) {
				return IDebugUIConstants.IMG_OBJS_REGISTER_GROUP;
			} else if (element instanceof IVariable || element instanceof IValue) {
				if (element instanceof IndexedVariablePartition) {
					return IInternalDebugUIConstants.IMG_OBJS_ARRAY_PARTITION;
				}
				return IDebugUIConstants.IMG_OBJS_VARIABLE;
			} else if (element instanceof IStackFrame) {
				if (((IStackFrame)element).getThread().isSuspended()) {
					return IDebugUIConstants.IMG_OBJS_STACKFRAME;
				}
				return IDebugUIConstants.IMG_OBJS_STACKFRAME_RUNNING;
			} else if (element instanceof IThread) {
				IThread thread = (IThread)element;
				if (thread.isSuspended()) {
					return IDebugUIConstants.IMG_OBJS_THREAD_SUSPENDED;
				} else if (thread.isTerminated()) {
					return IDebugUIConstants.IMG_OBJS_THREAD_TERMINATED;
				} else {
					return IDebugUIConstants.IMG_OBJS_THREAD_RUNNING;
				}
			} else if (element instanceof IDebugTarget) {
				IDebugTarget target= (IDebugTarget) element;
				if (target.isTerminated() || target.isDisconnected()) {
					return IDebugUIConstants.IMG_OBJS_DEBUG_TARGET_TERMINATED;
				} else if (target.isSuspended()) {
					return IDebugUIConstants.IMG_OBJS_DEBUG_TARGET_SUSPENDED;
				} else {
					return IDebugUIConstants.IMG_OBJS_DEBUG_TARGET;
				}
			} else if (element instanceof IExpression) {
				return IDebugUIConstants.IMG_OBJS_EXPRESSION;
			}
		} else {
			if (element instanceof IMarker) {
				return getMarkerImageKey((IMarker)element);
			} else if (element instanceof IBreakpoint) {
				return getBreakpointImageKey((IBreakpoint)element);
			} else if (element instanceof IProcess) {
				if (((IProcess) element).isTerminated()) {
					return IDebugUIConstants.IMG_OBJS_OS_PROCESS_TERMINATED;
				}
				return IDebugUIConstants.IMG_OBJS_OS_PROCESS;
			} else if (element instanceof ILaunch) {
				// determine the image from the launch config type
				ILaunch launch= (ILaunch)element;
				ILaunchConfiguration configuration = launch.getLaunchConfiguration();
				if (configuration != null) {
					try {
						return configuration.getType().getIdentifier();
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
						return null;
					}
				}
				// if no config, use the old "mode" way
				if (launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE)) {
					return IDebugUIConstants.IMG_OBJS_LAUNCH_DEBUG;
				} else if (launch.isTerminated()) {
					return IDebugUIConstants.IMG_OBJS_LAUNCH_RUN_TERMINATED;
				} else {
					return IDebugUIConstants.IMG_OBJS_LAUNCH_RUN;
				}
			} else if (element instanceof ILaunchConfigurationType) {
				return ((ILaunchConfigurationType)element).getIdentifier();
			} else if (element instanceof ILaunchConfiguration) {
				try {
					return ((ILaunchConfiguration)element).getType().getIdentifier();
				} catch (CoreException e) {
					DebugUIPlugin.log(e);
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * @see ILabelProvider#getText(Object)
	 */
	@Override
	public String getText(Object element) {
		StringBuilder label= new StringBuilder();
		try {
			// Group elements into debug elements and non-debug elements
			// to reduce the number of instanceof checks performed
			if (element instanceof IDebugElement) {
				if (element instanceof IStackFrame) {
					label.append(((IStackFrame)element).getName());
				} else if (element instanceof IndexedVariablePartition) {
					label.append(((IndexedVariablePartition)element).getName());
				} else if (element instanceof IVariable) {
					label.append(getVariableText((IVariable)element));
				} else if (element instanceof IThread) {
					label.append(((IThread)element).getName());
				} else if (element instanceof IDebugTarget) {
					label.append((((IDebugTarget)element).getName()));
				} else if (element instanceof IExpression) {
					label.append(getExpressionText((IExpression)element));
				} else if (element instanceof IRegisterGroup) {
					label.append(getRegisterGroupText((IRegisterGroup)element));
				} else if (element instanceof IValue) {
					label.append(((IValue)element).getValueString());
				}
			} else {
				if (element instanceof IMarker) {
					label.append(getMarkerText((IMarker) element));
				} else if (element instanceof IBreakpoint) {
					label.append(getBreakpointText((IBreakpoint)element));
				} else if (element instanceof IProcess) {
					label.append(((IProcess) element).getLabel());
				} else if (element instanceof ILaunch) {
					label.append(getLaunchText((ILaunch) element));
				} else if (element instanceof ILaunchConfiguration) {
					label.append(((ILaunchConfiguration)element).getName());
				} else if (element instanceof ILaunchConfigurationType) {
					label.append(((ILaunchConfigurationType)element).getName());
				} else if(element instanceof ILaunchDelegate) {
					ILaunchDelegate delegate = (ILaunchDelegate) element;
					String name = delegate.getName();
					if(name == null) {
						name = delegate.getContributorName();
					}
					label.append(name);
				} else if(element instanceof LaunchShortcutExtension) {
					label.append(((LaunchShortcutExtension)element).getLabel());
				} else if (element instanceof String) {
					label.append(element);
				} else {
					label.append(getAdapterLabel(element));
				}
			}
			if (element instanceof ITerminate) {
				if (((ITerminate) element).isTerminated()) {
					String terminatedMessage= null;
					if (element instanceof IProcess) {
						IProcess process = (IProcess)element;
						int exit = process.getExitValue();
						terminatedMessage = MessageFormat.format(DebugUIMessages.DefaultLabelProvider_16, new Object[] { Integer.toString(exit) });
					} else {
						terminatedMessage= DebugUIMessages.DefaultLabelProvider_1;
					}
					label.insert(0, terminatedMessage);
				}
			} else if (element instanceof IDisconnect) {
				if (((IDisconnect) element).isDisconnected()) {
					label.insert(0, DebugUIMessages.DefaultLabelProvider__disconnected__1);
				}
			}
		} catch (DebugException e) {
			DebugUIPlugin.log(e);
			label.append(DebugUIMessages.DefaultLabelProvider__unknown__1);
		}
		return label.toString();
	}

	/**
	 * Returns default label for a breakpoint.
	 *
	 * @param breakpoint
	 * @return default label for a breakpoint
	 */
	private String getBreakpointText(IBreakpoint breakpoint) {
		IResource resource = breakpoint.getMarker().getResource();
		StringBuilder label = new StringBuilder();
		if (resource != null) {
			label.append(resource.getName());
		}
		if (breakpoint instanceof ILineBreakpoint) {
			try {
				int lineNumber = ((ILineBreakpoint)breakpoint).getLineNumber();
				label.append(MessageFormat.format(DebugUIMessages.DefaultLabelProvider_17, new Object[] { Integer.toString(lineNumber) }));
			} catch (CoreException e) {
			}
		}
		return label.toString();
	}

	public String getAdapterLabel(Object object) {
		if (object instanceof IAdaptable) {
			IWorkbenchAdapter de= ((IAdaptable) object).getAdapter(IWorkbenchAdapter.class);
			if (de != null) {
				return de.getLabel(object);
			}
		}
		return DebugUIMessages.DefaultLabelProvider__unknown__1;
	}

	/**
	 * Used to render launch history items in the re-launch drop downs
	 */
	protected String getLaunchText(ILaunch launch) {
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		if (config == null || (!config.exists() && !config.isWorkingCopy())) {
			return DebugUIMessages.DefaultLabelProvider__unknown__1;
		}
		// new launch configuration
		StringBuilder buff= new StringBuilder(config.getName());
		buff.append(" ["); //$NON-NLS-1$
		try {
			buff.append(config.getType().getName());
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
		buff.append("]"); //$NON-NLS-1$
		return buff.toString();
	}

	protected String getExpressionText(IExpression expression) {
		if (expression instanceof IWatchExpression) {
			return getWatchExpressionText((IWatchExpression) expression);
		}
		StringBuilder buffer= new StringBuilder(expression.getExpressionText());
		String valueString= null;
		IValue value= expression.getValue();
		if (value != null) {
			try {
				valueString= value.getValueString();
			} catch (DebugException de) {
				DebugUIPlugin.log(de);
			}
		}
		if (valueString != null && valueString.length() > 0) {
			buffer.append("= "); //$NON-NLS-1$
			buffer.append(valueString);
		}
		return buffer.toString();
	}

	/**
	 * @param expression
	 * @return
	 */
	protected String getWatchExpressionText(IWatchExpression expression) {
		StringBuilder result= new StringBuilder();

		String snippet = expression.getExpressionText().trim();
		StringBuilder snippetBuffer = new StringBuilder();
		if (snippet.length() > 30){
			snippetBuffer.append(snippet.substring(0, 15));
			snippetBuffer.append(DebugUIMessages.DefaultLabelProvider_0);
			snippetBuffer.append(snippet.substring(snippet.length() - 15));
		} else {
			snippetBuffer.append(snippet);
		}
		snippet = snippetBuffer.toString().replace("[\n\r\t]+", " ");  //$NON-NLS-1$//$NON-NLS-2$

		result.append('"');
		result.append(snippet);
		result.append('"');

		if (expression.isPending()) {
			result.append(DebugUIMessages.DefaultLabelProvider_12);
		} else if (expression.hasErrors()) {
			result.append(DebugUIMessages.DefaultLabelProvider_13);
		} else {
			IValue value= expression.getValue();
			if (value != null) {
				String valueString= DebugUIPlugin.getModelPresentation().getText(value);
				if (valueString.length() > 0) {
					result.append(" = ").append(valueString); //$NON-NLS-1$
				}
			}
		}
		if (!expression.isEnabled()) {
			result.append(DebugUIMessages.DefaultLabelProvider_15);
		}
		return result.toString();
	}

	protected String getVariableText(IVariable variable) {
		StringBuilder buffer= new StringBuilder();
		try {
			IValue value = variable.getValue();
			buffer.append(variable.getName());
			buffer.append(" = "); //$NON-NLS-1$
			buffer.append(value.getValueString());
		} catch (DebugException de) {
			DebugUIPlugin.log(de);
		}
		return buffer.toString();
	}

	protected String getRegisterGroupText(IRegisterGroup registerGroup) {
		StringBuilder buffer= new StringBuilder();
		try {
			buffer.append(registerGroup.getName());
		} catch (DebugException de) {
			DebugUIPlugin.log(de);
		}
		return buffer.toString();
	}

	protected String getMarkerText(IMarker marker) {
		try {
			if (marker.exists() && marker.isSubtypeOf(IBreakpoint.BREAKPOINT_MARKER)) {
				return DebugUIMessages.DefaultLabelProvider_Breakpoint_1;
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}

	protected String getMarkerImageKey(IMarker marker) {
		try {
			IBreakpoint breakpoint= DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
			if (breakpoint != null && marker.exists()) {
				if (breakpoint.isEnabled()) {
					return IDebugUIConstants.IMG_OBJS_BREAKPOINT;
				}
				return IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED;
			}
		} catch (CoreException e) {
		}
		return null;
	}

	protected String getBreakpointImageKey(IBreakpoint breakpoint) {
		if (breakpoint != null && breakpoint.getMarker().exists()) {
			try {
				boolean enabled = breakpoint.isEnabled();
				if (breakpoint instanceof IWatchpoint) {
					IWatchpoint watchpoint = (IWatchpoint) breakpoint;
					if (watchpoint.isAccess()) {
						if (watchpoint.isModification()) {
							//access and modification
							if (enabled) {
								return IDebugUIConstants.IMG_OBJS_WATCHPOINT;
							}
							return IDebugUIConstants.IMG_OBJS_WATCHPOINT_DISABLED;
						}
						if (enabled) {
							return IDebugUIConstants.IMG_OBJS_ACCESS_WATCHPOINT;
						}
						return IDebugUIConstants.IMG_OBJS_ACCESS_WATCHPOINT_DISABLED;
					} else if (watchpoint.isModification()) {
						if (enabled) {
							return IDebugUIConstants.IMG_OBJS_MODIFICATION_WATCHPOINT;
						}
						return IDebugUIConstants.IMG_OBJS_MODIFICATION_WATCHPOINT_DISABLED;
					} else {
						//neither access nor modification
						return IDebugUIConstants.IMG_OBJS_WATCHPOINT_DISABLED;
					}
				}
				if (enabled) {
					return IDebugUIConstants.IMG_OBJS_BREAKPOINT;
				}
				return IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED;
			} catch (CoreException e) {
			}
		}
		return null;
	}

	/**
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	/**
	 * @see IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		for (Image image : fImages.values()) {
			image.dispose();
		}
		fImages.clear();
	}

	/**
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	/**
	 * Returns the given string with special chars in escaped sequences.
	 *
	 * @param label
	 * @return the given string with special chars in escaped sequences
	 * @since 3.3
	 */
	public static String escapeSpecialChars(String string) {
		if (string == null) {
			return null;
		}
		StringBuilder escaped = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
				case '\b':
					escaped.append("\\b"); //$NON-NLS-1$
					break;
				case '\f':
					escaped.append("\\f"); //$NON-NLS-1$
					break;
				case '\n':
					escaped.append("\\n"); //$NON-NLS-1$
					break;
				case '\r':
					escaped.append("\\r"); //$NON-NLS-1$
					break;
				case '\t':
					escaped.append("\\t"); //$NON-NLS-1$
					break;
				case '\\':
					escaped.append("\\\\"); //$NON-NLS-1$
					break;
				default:
					escaped.append(c);
					break;
			}
		}
		return escaped.toString();
	}

	/**
	 * Returns the string with escaped sequences replaced with single chars.
	 *
	 * @param string
	 * @return the string with escaped sequences replaced with single chars
	 * @since 3.3
	 */
	public static String encodeEsacpedChars(String string) {
		if (string == null) {
			return null;
		}
		StringBuilder encoded = new StringBuilder();
		if (string.length() == 1) {
			return string;
		}
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\\' && (i + 1 < string.length())) {
				switch (string.charAt(i+1)) {
					case 'b':
						c= '\b';
						i++;
						break;
					case 'f':
						c= '\f';
						i++;
						break;
					case 'n':
						c= '\n';
						i++;
						break;
					case 'r':
						c= '\r';
						i++;
						break;
					case 't':
						c= '\t';
						i++;
						break;
					case '\'':
						c= '\'';
						i++;
						break;
					case '\\':
						c= '\\';
						i++;
						break;
					default :
						break;
				}
			}
			encoded.append(c);
		}
		return encoded.toString();
	}
}

