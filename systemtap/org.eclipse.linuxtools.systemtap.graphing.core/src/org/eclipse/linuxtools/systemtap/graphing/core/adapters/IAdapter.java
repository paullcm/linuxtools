/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Jeff Briggs, Henry Hughes, Ryan Morse
 *******************************************************************************/

package org.eclipse.linuxtools.systemtap.graphing.core.adapters;

public interface IAdapter {
    Number getYSeriesMax(int series, int start, int end);
    Number getSeriesMax(int series, int start, int end);

    String[] getLabels();
    int getRecordCount();
    int getSeriesCount();
    Object[][] getData();
    Object[][] getData(int start, int end);
}
