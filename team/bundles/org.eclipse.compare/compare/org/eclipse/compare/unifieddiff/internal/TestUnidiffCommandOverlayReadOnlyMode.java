package org.eclipse.compare.unifieddiff.internal;

import org.eclipse.compare.unifieddiff.UnifiedDiffMode;

//TODO (tm) used for manual testing which will later be removed
public class TestUnidiffCommandOverlayReadOnlyMode extends TestUnidiffCommand {

	@Override
	protected UnifiedDiffMode getMode() {
		return UnifiedDiffMode.OVERLAY_READ_ONLY_MODE;
	}

}
