package org.eclipse.e4.core.internal.tests.nls;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.nls.BaseMessageRegistry;
import org.eclipse.e4.core.services.nls.Translation;

@Creatable
public class BundleMessagesRegistry extends BaseMessageRegistry<BundleMessages> {

	@Override
	@Inject
	public void updateMessages(@Translation BundleMessages messages) {
		super.updateMessages(messages);
	}

}
