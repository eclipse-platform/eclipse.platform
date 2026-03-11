package org.eclipse.compare.unifieddiff.internal;

import org.eclipse.compare.unifieddiff.UnifiedDiff.UnifiedDiffMode;

//TODO (tm) used for manual testing which will later be removed
public class TestUnidiffCommandReplaceMode extends TestUnidiffCommand {

	@Override
	protected UnifiedDiffMode getMode() {
		return UnifiedDiffMode.REPLACE_MODE;
	}

}
