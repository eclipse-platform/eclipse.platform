package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.util.HashMap;
import java.util.Map;

import org.eclipse.update.core.model.ImportModel;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Convenience implementation of a plug-in dependency.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IImport
 * @see org.eclipse.update.core.model.ImportModel
 * @since 2.0
 */
public class Import extends ImportModel implements IImport {


	/**
	 * Returns an identifier of the dependent plug-in.
	 * @see IImport#getIdentifier()
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		return new VersionedIdentifier(getPluginIdentifier(), getPluginVersion());
	}

	/**
	 * Returns the matching rule for the dependency.
	 * @see IImport#getRule()
	 */
	public int getRule() {
		return UpdateManagerUtils.getMatchingRule(getMatchingRuleName());
	}

}