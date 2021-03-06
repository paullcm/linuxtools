/*******************************************************************************
 * Copyright (c) 2008, 2018 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Kent Sebastian <ksebasti@redhat.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.oprofile.ui.model;

import java.util.Arrays;

import org.eclipse.linuxtools.internal.oprofile.core.model.OpModelEvent;
import org.eclipse.linuxtools.internal.oprofile.core.model.OpModelSession;
import org.eclipse.linuxtools.internal.oprofile.ui.OprofileUiMessages;
import org.eclipse.linuxtools.internal.oprofile.ui.OprofileUiPlugin;
import org.eclipse.swt.graphics.Image;

/**
 * Children of events in the view -- sessions containing images/symbols
 *  for its parent event. Must have a child image. May also have dependent
 *  images, which are children of the Image in the data model, but are
 *  displayed as children of the session in the view.
 * @since 1.1
 */
public class UiModelSession implements IUiModelElement {
    private OpModelSession session;        //the node in the data model

    private UiModelEvent events[];

    //OProfile's default session name
    private static final String DEFAULT_SESSION_NAME = "current"; //$NON-NLS-1$

    /**
     * Constructor to the UiModelSession class
     * @param session Oprofile session node in the data model
     */
    public UiModelSession(OpModelSession session) {
        if (session != null) {
            this.session = session;
            refreshModel();
        }
    }

    private void refreshModel() {

        OpModelEvent dataModelEvents[] = session.getEvents();
        events = new UiModelEvent[dataModelEvents.length];
        for (int i = 0; i < dataModelEvents.length; i++) {
            events[i] = new UiModelEvent(this, dataModelEvents[i]);
        }

    }

    @Override
    public String toString() {
        return session.getName();
    }

    /**
     * Check if this is Oprofile's default session name
     * @return true whether this is Oprofile's default session, false otherwise
     */
    public boolean isDefaultSession() {
        return session.getName().equalsIgnoreCase(DEFAULT_SESSION_NAME);
    }

    /** IUiModelElement functions
     * Returns the text to display in the tree viewer as required by the label provider.
     * @return text Text string describing this element
     */
    @Override
    public String getLabelText() {
        if (session.getName().equals(DEFAULT_SESSION_NAME)){
            return OprofileUiMessages.getString("UiModelSession_current"); //$NON-NLS-1$
        }
        return toString();
    }

    /**
     * Returns the children of this element.
     * @return An array of child elements
     */
    @Override
    public IUiModelElement[] getChildren() {
        if (UiModelRoot.SortType.EVENT == UiModelRoot.getSortingType()) {
            Arrays.sort(events, UiModelSorting.getInstance());
            return events;
        }

        return events;
    }

    /**
     * Returns if the element has any children.
     * @return true if the element has children, false otherwise
     */
    @Override
    public boolean hasChildren() {
        return (events == null || events.length == 0 ? false : true);
    }

    /**
     * Returns the element's parent.
     * @return parent The parent element
     */
    @Override
    public IUiModelElement getParent() {
        return null;
    }

    /**
     * Returns the Image to display next to the text in the tree viewer.
     * @return an Image object of the icon
     */
    @Override
    public Image getLabelImage() {
        return OprofileUiPlugin.getImageDescriptor(OprofileUiPlugin.SESSION_ICON).createImage();
    }
}
