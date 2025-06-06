/**********************************************************************
.
.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.ui.dtd.schema;

import org.eclipse.osgi.util.NLS;

public class AntDTDSchemaMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.dtd.schema.AntDTDSchemaMessages";//$NON-NLS-1$

	public static String Model_model_may_not_be_changed;
	public static String Model____UNKNOWN____2;
	public static String NfmParser_Ambiguous;
	public static String SchemaFactory_Doubly_defined;
	public static String SchemaFactory_Start_with_left_parenthesis;
	public static String SchemaFactory_Expecting_operator_or_right_parenthesis;
	public static String SchemaFactory_Unexpected_end;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AntDTDSchemaMessages.class);
	}
}