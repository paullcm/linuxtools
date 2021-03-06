/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation.
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

package org.eclipse.linuxtools.internal.systemtap.ui.consolelog.actions;

import org.eclipse.linuxtools.systemtap.ui.consolelog.internal.ConsoleLogPlugin;
import org.eclipse.linuxtools.systemtap.ui.consolelog.internal.Localization;
import org.eclipse.linuxtools.systemtap.ui.consolelog.structures.ScriptConsole;
import org.eclipse.linuxtools.systemtap.ui.consolelog.structures.ScriptConsole.ScriptConsoleObserver;
import org.eclipse.ui.PlatformUI;



/**
 * A class that handles stopping the <code>ScriptConsole</code>.
 * @author Ryan Morse
 * @since 2.0
 */
public class StopScriptAction extends ConsoleAction implements ScriptConsoleObserver {

    /**
     * This is the main method of the class. It handles stopping the
     * currently active <code>ScriptConsole</code>.
     */
    @Override
    public void run() {
        PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
		    if(null != console){
		        console.stop();
		    }
		});
    }

    /**
     * Creates the action for the given console.
     * @param fConsole The console which will be stoppable.
     * @since 2.0
     */
    public StopScriptAction(ScriptConsole fConsole) {
        super(fConsole,
                ConsoleLogPlugin.getDefault().getBundle().getEntry("icons/actions/stop_script.gif"), //$NON-NLS-1$
                Localization.getString("action.stopScript.name"), //$NON-NLS-1$
                Localization.getString("action.stopScript.desc")); //$NON-NLS-1$
        console.addScriptConsoleObserver(this);
    }

    /**
     * @since 2.0
     */
    @Override
    public void runningStateChanged(boolean started, boolean stopped) {
        setEnabled(!stopped);
    }

}
