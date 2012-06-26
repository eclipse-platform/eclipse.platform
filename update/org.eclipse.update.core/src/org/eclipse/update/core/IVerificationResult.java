/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

/**
 * Verification result. An object implementing this interface represents
 * a token passed between the update framework and the verifier and verification
 * listener. The verifier uses the token to capture the result of the file
 * verification. It is then passed to the verification listener
 * to optionally present this information to the user (in an 
 * implementation-specific way). The verification listener in turn
 * uses this token to capture the desired response.
 * <p>
 * Clients may implement this interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IVerificationResult {

	/**
	 * Indicates the file type is recognized but the file is not signed
	 * 
	 * @see #TYPE_ENTRY_UNRECOGNIZED
	 * @since 2.0
	 */
	public static final int TYPE_ENTRY_NOT_SIGNED = 1;

	/**
	 * Indicates the file has been modified since it was signed
	 * 
	 * @since 2.0
	 */
	public static final int TYPE_ENTRY_CORRUPTED = 2;

	/**
	 * Indicates the file is signed by a known signer
	 * 
	 * @since 2.0
	 */
	public static final int TYPE_ENTRY_SIGNED_RECOGNIZED = 3;

	/**
	 * Indicates the file is signed but the signer is not known
	 * 
	 * @since 2.0
	 */
	public static final int TYPE_ENTRY_SIGNED_UNRECOGNIZED = 5;

	/**
	 * Error occurred during verification
	 * 
	 * @since 2.0
	 */
	public static final int UNKNOWN_ERROR = 6;

	/**
	 * Verification was cancelled
	 * 
	 * @since 2.0
	 */
	public static final int VERIFICATION_CANCELLED = 7;

	/**
	 * Could not perform verification due to unrecognized file
	 * 
	 * @see #TYPE_ENTRY_NOT_SIGNED
	 * @since 2.0
	 */
	public static final int TYPE_ENTRY_UNRECOGNIZED = 8;

	/**
	 * Returns the content reference that is the target of the verification.
	 * 
	 * @return content reference
	 * @since 2.0
	 */
	public ContentReference getContentReference();

	/**
	 * Returns the feature the referenced file is part of.
	 * 
	 * @return feature
	 * @since 2.0
	 */
	public IFeature getFeature();

	/**
	 * Returns the verification code.
	 * 
	 * @return verification code, as defined in this interface.
	 * @since 2.0
	 */
	public int getVerificationCode();

	/**
	 * Returns any exception caught during verification
	 * 
	 * @return exception, or <code>null</code>.
	 * @since 2.0
	 */
	public Exception getVerificationException();

	/**
	 * Returns display text describing the result of the verification.
	 * 
	 * @return result text, or <code>null</code>.
	 * @since 2.0
	 */
	public String getText();

	/**
	 * Returns text describing the signer
	 * 
	 * @return signer information, or <code>null</code>.
	 * @since 2.0
	 */
	public String getSignerInfo();

	/**
	 * Returns text describing the authority that verified/ certified 
	 * the signer
	 * 
	 * @return verifier information, or <code>null</code>.
	 * @since 2.0
	 */
	public String getVerifierInfo();

	/**
	 * Indicates whether the referenced file is part of the overall feature
	 * definition, or one of its component plug-in or non-plug-in entries.
	 * 
	 * @return <code>true</code> if reference is a feature file, 
	 * <code>false</code> if reference is a plug-in or non-plug-in file
	 * @since 2.0
	 */
	public boolean isFeatureVerification();
	
	/**
	 * Indicates whether the signer and verifier info have already been accepted by the user
	 * during a previous verification of one of the file of the feature.
	 * 
	 * @return <code>true</code> if the result has already been accepted, <code>false</code>
	 * if the result has not yet been accepted by the user
	 * @since 2.0
	 */
	public boolean alreadySeen();
}
