/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 430127
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.ui;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;
import org.eclipse.ui.externaltools.internal.ui.BuilderPropertyPage.ErrorConfig;


class BuilderLabelProvider extends LabelProvider {
		private static final String IMG_BUILDER = "icons/full/obj16/builder.png"; //$NON-NLS-1$;
		private static final String IMG_INVALID_BUILD_TOOL = "icons/full/obj16/invalid_build_tool.png"; //$NON-NLS-1$
		IDebugModelPresentation debugModelPresentation= DebugUITools.newDebugModelPresentation();

		private Image builderImage = ExternalToolsPlugin.getDefault().getImageDescriptor(IMG_BUILDER).createImage();
		private Image invalidBuildToolImage = ExternalToolsPlugin.getDefault().getImageDescriptor(IMG_INVALID_BUILD_TOOL).createImage();

		@Override
		public String getText(Object element) {
			if (element instanceof ICommand) {
				return getCommandText((ICommand) element);
			} else if (element instanceof ILaunchConfiguration || element instanceof ILaunchConfigurationType) {
				return getDebugModelText(element);
			} else if (element instanceof ErrorConfig) {
				return ExternalToolsUIMessages.BuilderPropertyPage_invalidBuildTool;
			}
			return super.getText(element);
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof ICommand) {
				return getCommandImage();
			} else if (element instanceof ILaunchConfiguration || element instanceof ILaunchConfigurationType) {
				return getDebugModelImage(element);
			} else if (element instanceof ErrorConfig) {
				return invalidBuildToolImage;
			}
			return super.getImage(element);
		}

		public String getCommandText(ICommand command) {
			String builderID = command.getBuilderName();
			return getBuilderName(builderID);
		}

		private String getBuilderName(String builderID) {
			// Get the human-readable name of the builder
			IExtension extension = Platform.getExtensionRegistry().getExtension(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_BUILDERS, builderID);
			String builderName;
			if (extension != null) {
				builderName = extension.getLabel();
			} else {
				builderName = NLS.bind(ExternalToolsUIMessages.BuilderPropertyPage_missingBuilder, new Object[] { builderID });
			}
			return builderName;
		}

		/**
		 * Returns the image for build commands.
		 *
		 * @return the build command image
		 */
		public Image getCommandImage() {
			return builderImage;
		}

		/**
		 * Returns a text label for the given object from a debug
		 * model presentation.
		 * @param element the element
		 * @return a text label from a debug model presentation
		 */
		public String getDebugModelText(Object element) {
			if (element instanceof ILaunchConfiguration) {
				try {
					String disabledBuilderName= ((ILaunchConfiguration) element).getAttribute(IExternalToolConstants.ATTR_DISABLED_BUILDER, (String)null);
					if (disabledBuilderName != null) {
						//really a disabled builder wrapped as a launch configuration
						return getBuilderName(disabledBuilderName);
					}
				} catch (CoreException e) {
				}
			}
			return debugModelPresentation.getText(element);
		}

		/**
		 * Returns an image for the given object from a debug
		 * model presentation.
		 * @param element the element
		 * @return an image from a debug model presentation
		 */
		public Image getDebugModelImage(Object element) {
			if (element instanceof ILaunchConfiguration) {
				try {
					String disabledBuilderName= ((ILaunchConfiguration) element).getAttribute(IExternalToolConstants.ATTR_DISABLED_BUILDER, (String)null);
					if (disabledBuilderName != null) {
						//really a disabled builder wrapped as a launch configuration
						return builderImage;
					}
				} catch (CoreException e) {
				}
			}
			return debugModelPresentation.getImage(element);
		}

	@Override
		public void dispose() {
			builderImage.dispose();
			invalidBuildToolImage.dispose();
		}
}
