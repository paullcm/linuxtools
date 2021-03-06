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

package org.eclipse.linuxtools.internal.systemtap.ui.ide.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {

    //environmentvariables
    public static enum P_ENV {
        LD_LIBRARY_PATH {
            @Override
            public String toPrefKey() {
                return "EnvLdLibraryPath"; //$NON-NLS-1$
            }
        }, PATH {
            @Override
            public String toPrefKey() {
                return "EnvPath"; //$NON-NLS-1$
            }
        }, SYSTEMTAP_TAPSET {
            @Override
            public String toPrefKey() {
                return "EnvSystemtapTapset"; //$NON-NLS-1$
            }
        }, SYSTEMTAP_RUNTIME {
            @Override
            public String toPrefKey() {
                return "EnvSystemtapRuntime"; //$NON-NLS-1$
            }
        };
        public abstract String toPrefKey();
        public String toEnvKey() {
            return toString();
        }
        public String createKeyValString(String value) {
            return toEnvKey() + '=' + value.trim();
        }
    }

}
