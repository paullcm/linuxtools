/*******************************************************************************
 * Copyright (c) 2013, 2018 Red Hat Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.perf;

/**
 * Interface for generic perf data.
 */
public interface IPerfData {

    /**
     * Get string representation of the data.
     * @return String perf data
     */
    String getPerfData();

    /**
     * Get title for this data.
     * @return title for perf data
     */
    String getTitle();
}
