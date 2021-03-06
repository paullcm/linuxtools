/*******************************************************************************
 * Copyright (c) 2015, 2016 Red Hat Inc. and others.
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.man.help;

import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.osgi.framework.FrameworkUtil;

/**
 * Provider for help system table of contents.
 * 
 * @see ManualToc
 */
public class ManualTocProvider extends AbstractTocProvider {

	@Override
	public ITocContribution[] getTocContributions(String locale) {
		ITocContribution contribution = new ITocContribution() {
			@Override
			public String getId() {
				return getContributorId() + ".toc"; //$NON-NLS-1$
			}

			@Override
			public String getCategoryId() {
				return null;
			}

			@Override
			public boolean isPrimary() {
				return true;
			}

			@Override
			public IToc getToc() {
				return new ManualToc();
			}

			@Override
			public String getLocale() {
				return "en_US"; //$NON-NLS-1$
			}

			@Override
			public String[] getExtraDocuments() {
				return new String[0];
			}

			@Override
			public String getLinkTo() {
				return ""; //$NON-NLS-1$
			}

			@Override
			public String getContributorId() {
				return FrameworkUtil.getBundle(this.getClass())
						.getSymbolicName();
			}
		};
		return new ITocContribution[] { contribution };
	}
}
