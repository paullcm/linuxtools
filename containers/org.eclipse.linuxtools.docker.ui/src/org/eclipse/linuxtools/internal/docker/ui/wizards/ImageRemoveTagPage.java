/*******************************************************************************
 * Copyright (c) 2015, 2018 Red Hat.
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.linuxtools.internal.docker.ui.wizards;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.internal.docker.ui.SWTImagesFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ImageRemoveTagPage extends WizardPage {

	private final static String NAME = "ImageRemoveTag.name"; //$NON-NLS-1$
	private final static String TITLE = "ImageRemoveTag.title"; //$NON-NLS-1$
	private final static String DESC = "ImageRemoveTag.desc"; //$NON-NLS-1$
	private final static String REMOVE_TAG_LABEL = "ImageRemoveTagName.label"; //$NON-NLS-1$
	private final static String REMOVE_TAG_TOOLTIP = "ImageRemoveTagName.toolTip"; //$NON-NLS-1$

	private String selectedTag;
	private IDockerImage image;

	public ImageRemoveTagPage(IDockerImage image) {
		super(WizardMessages.getString(NAME));
		setDescription(WizardMessages.getString(DESC));
		setTitle(WizardMessages.getString(TITLE));
		setImageDescriptor(SWTImagesFactory.DESC_WIZARD);
		this.image = image;
	}

	public String getTag() {
		return selectedTag;
	}

	@Override
	public void createControl(Composite parent) {
		parent.setLayout(new GridLayout());
		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6)
				.applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1)
				.grab(true, false).applyTo(container);

		final Label repoLabel = new Label(container, SWT.NULL);
		repoLabel.setText(WizardMessages.getString(REMOVE_TAG_LABEL));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(repoLabel);
		final Combo tagCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		tagCombo.setToolTipText(WizardMessages.getString(REMOVE_TAG_TOOLTIP));
		tagCombo.addSelectionListener(SelectionListener
				.widgetSelectedAdapter(e -> selectedTag = tagCombo.getText()));
		// Set up combo with repoTags that can be removed
		final List<String> repoTags = image.repoTags();
		tagCombo.setItems(repoTags.toArray(new String[0]));
		tagCombo.select(0);
		selectedTag = tagCombo.getItem(0);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(tagCombo);

		setControl(container);
	}

}
