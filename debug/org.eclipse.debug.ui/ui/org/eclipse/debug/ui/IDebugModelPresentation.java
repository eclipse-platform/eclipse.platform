/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.ui;


import org.eclipse.debug.core.model.IValue;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * A debug model presentation is responsible for providing labels, images, and
 * editors associated with debug elements in a specific debug model. Extensions
 * of type <code>org.eclipse.debug.ui.debugModelPresentations</code> implement
 * this interface. Generally, a debug model implementation will also provide a
 * debug model presentation extension to render and display its elements. A
 * debug model presentation is registered for a specific debug model, and is
 * responsible for the presentation elements defined/implemented by that model.
 * <p>
 * A debug model presentation extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a debug model presentation extension.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.debugModelPresentations"&gt;
 *   &lt;debugModelPresentation
 *      id="com.example.debugModelIdentifier"
 *      class="com.example.ExamplePresentation"
 *      detailsViewerConfiguration="com.example.ExampleSourceViewerConfiguration"&gt;
 *   &lt;/debugModelPresentation&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies the identifier of the debug model this
 * presentation is responsible for. Corresponds to the model identifier returned
 * from a debug element - see
 * <code>IDebugElement.getModelIndentifier</code></li>
 * <li><code>class</code> specifies the fully qualified name of the Java class
 * that implements this interface.</li>
 * <li><code>detailsViewerConfiguration</code> optionally specifies the fully
 * qualified name of the Java class that is an instance of
 * <code>org.eclipse.jface.text.source.SourceViewerConfiguration</code>. When
 * specified, the source viewer configuration will be used in the "details" area
 * of the variables and expressions view when displaying the details of an
 * element from the debug model associated with this debug model presentation.
 * When unspecified, a default configuration is used.</li>
 * </ul>
 * <p>
 * To allow for an extensible configuration, this interface defines a
 * <code>setAttribute</code> method. The debug UI plug-in defines one
 * presentation attribute:
 * <ul>
 * <li><code>DISPLAY_VARIABLE_TYPE_NAMES</code> - This is a boolean attribute
 * indicating whether variable elements should be rendered with the declared
 * type of a variable. For example, a Java debug model presentation would render
 * an integer as <code>"int x = 3"</code> when true, and <code>"x = 3"</code>
 * when false.</li>
 * </ul>
 *
 * <p>
 * Clients may define new presentation attributes. For example, a client may
 * wish to define a "hexadecimal" property to display numeric values in
 * hexadecimal. Implementations should honor the presentation attributes defined
 * by this interface where possible, but do not need to honor presentation
 * attributes defined by other clients. To access the debug model presentation
 * for a debug view, clients should use
 * <code>IDebugView#getPresentation(String)</code>.
 * </p>
 * <p>
 * Since 3.1, debug model presentations may optionally implement
 * <code>IColorProvider</code> and <code>IFontProvider</code> to override
 * default fonts and colors for debug elements.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see org.eclipse.debug.core.model.IDebugElement
 * @see org.eclipse.jface.viewers.ILabelProvider
 * @see org.eclipse.debug.ui.IDebugView
 */

public interface IDebugModelPresentation extends ILabelProvider, ISourcePresentation {
	/**
	 * Variable type names presentation property (value <code>"org.eclipse.debug.ui.displayVariableTypeNames"</code>).
	 * When <code>DISPLAY_VARIABLE_TYPE_NAMES</code> is set to <code>true</code>,
	 * this label provider should include the reference type of a variable  when rendering
	 * variables. When set to <code>false</code>, this label provider
	 * should not include the reference type of a variable when rendering
	 * variables.
	 * @see #setAttribute(String, Object)
	 */
	String DISPLAY_VARIABLE_TYPE_NAMES= IDebugUIConstants.PLUGIN_ID + ".displayVariableTypeNames"; //$NON-NLS-1$
	/**
	 * Sets a presentation attribute of this label provider. For example,
	 * see the presentation attribute <code>DISPLAY_VARIABLE_TYPE_NAMES</code>
	 * defined by this interface.
	 *
	 * @param attribute the presentation attribute identifier
	 * @param value the value of the attribute
	 */
	void setAttribute(String attribute, Object value);
	/**
	 * Returns an image for the element, or <code>null</code> if a default
	 * image should be used.
	 *
	 * @param element the debug model element
	 * @return an image for the element, or <code>null</code> if a default
	 *    image should be used
	 * @see ILabelProvider
	 */
	@Override Image getImage(Object element);
	/**
	 * Returns a label for the element, or <code>null</code> if a default
	 * label should be used.
	 *
	 * @param element the debug model element
	 * @return a label for the element, or <code>null</code> if a default
	 *    label should be used
	 * @see ILabelProvider
	 */
	@Override String getText(Object element);

	/**
	 * Computes a detailed description of the given value, reporting
	 * the result to the specified listener. This allows a presentation
	 * to provide extra details about a selected value in the variable detail
	 * portion of the variables view. Since this can be a long-running operation,
	 * the details are reported back to the specified listener asynchronously.
	 * If <code>null</code> is reported, the value's value string is displayed
	 * (<code>IValue.getValueString()</code>).
	 *
	 * @param value the value for which a detailed description
	 * 	is required
	 * @param listener the listener to report the details to
	 * 	asynchronously
	 * @since 2.0
	 */
	void computeDetail(IValue value, IValueDetailListener listener);

}
