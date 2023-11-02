package org.eclipse.e4.core.internal.tests.nls;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.nls.BaseMessageRegistry;
import org.eclipse.e4.core.services.nls.Translation;

import jakarta.inject.Inject;

@Creatable
public class BundleMessagesRegistry extends BaseMessageRegistry<BundleMessages> {

	@Override
	@Inject
	public void updateMessages(@Translation BundleMessages messages) {
		super.updateMessages(messages);
	}

}
