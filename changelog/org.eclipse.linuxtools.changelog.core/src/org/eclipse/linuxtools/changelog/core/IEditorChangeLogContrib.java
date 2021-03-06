/*******************************************************************************
 * Copyright (c) 2006, 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Kyu Lee <klee@redhat.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.changelog.core;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;

public interface IEditorChangeLogContrib {

	/**
	 * Set default content type. GNU Changelog only has one type.
	 *
	 * @param sourceViewer
	 *            The source viewer to retrieve configured content type for.
	 *
	 * @return default content type.
	 */
	String[] getConfiguredContentTypes(ISourceViewer sourceViewer);

	/**
	 * Detects hyperlinks in GNU formatted changelogs.
	 *
	 * @param sourceViewer
	 *            The source viewer to retrieve hyperlinks for.
	 *
	 * @return link detector for GNU format.
	 */
	IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer);

	/**
	 * Hyperlink presenter (decorator).
	 *
	 * @param sourceViewer
	 *            The source viewer to operate on.
	 *
	 * @return default presenter.
	 */
	IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer);

	/**
	 * Highlights GNU format changelog syntaxes.
	 *
	 * @param sourceViewer
	 *            The source viewer to get presentation reconciler for.
	 *
	 * @return reconciler for GNU format changelog.
	 */
	IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer);

	/**
	 * Perform documentation setup. Use this to specify partitioning.
	 *
	 * @param document
	 *            to set up.
	 *
	 * @since 3.0.0
	 */
	void setup(IDocument document);
}
