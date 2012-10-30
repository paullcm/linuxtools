/*******************************************************************************
 * Copyright (c) 2009, 2010 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Patrick Tasse - Factored out events table
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.views.events;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomEventsTable;
import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomTxtTrace;
import org.eclipse.linuxtools.internal.tmf.ui.parsers.custom.CustomXmlTrace;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.core.signal.TmfExperimentDisposedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfExperimentSelectedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTimestampFormatUpdateSignal;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceType;
import org.eclipse.linuxtools.tmf.ui.viewers.events.TmfEventsTable;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;
import org.osgi.framework.Bundle;

/**
 * The generic TMF Events View.
 * <p>
 * Support for:
 * <ul>
 * <li>Traces larger than available memory
 * <li>Searching and Filtering
 * <li>Customized table viewers (per trace type)
 * </ul>
 *
 * TODO: Handle column selection, sort, ... generically (nothing less...)
 * TODO: Implement hide/display columns
 *
 * @version 1.0
 * @author Francois Chouinard
 * @author Patrick Tasse
 * @since 2.0
 */
public class TmfEventsView extends TmfView implements IResourceChangeListener, ISelectionProvider, ISelectionChangedListener {

    /** Event View's ID */
    public static final String ID = "org.eclipse.linuxtools.tmf.ui.views.events"; //$NON-NLS-1$

    private TmfExperiment fExperiment;
    private TmfEventsTable fEventsTable;
    private static final int DEFAULT_CACHE_SIZE = 100;
    private String fTitlePrefix;
    private Composite fParent;
    private ListenerList fSelectionChangedListeners = new ListenerList();

	// ------------------------------------------------------------------------
    // Constructor
	// ------------------------------------------------------------------------

    /**
     * Create an events view with a cache size
     * @param cacheSize not used
     */
    public TmfEventsView(int cacheSize) {
    	super("TmfEventsView"); //$NON-NLS-1$
    }

    /**
     * Default constructor, with a default cache size
     */
    public TmfEventsView() {
    	this(DEFAULT_CACHE_SIZE);
    }

	// ------------------------------------------------------------------------
    // ViewPart
	// ------------------------------------------------------------------------

	@Override
	public void createPartControl(Composite parent) {
        fParent = parent;

        fTitlePrefix = getTitle();

        // If an experiment is already selected, update the table
        TmfExperiment experiment = TmfExperiment.getCurrentExperiment();
        if (experiment != null) {
            experimentSelected(new TmfExperimentSelectedSignal(this, experiment));
        } else {
            fEventsTable = createEventsTable(parent);
            fEventsTable.addSelectionChangedListener(this);
        }
        // we need to wrap the ISelectionProvider interface in the view because
        // the events table can be replaced later while the selection changed listener
        // is only added once by the platform to the selection provider set here
        getSite().setSelectionProvider(this);
    }

    @Override
    public void dispose() {
        if (fEventsTable != null) {
            fEventsTable.dispose();
        }
        super.dispose();
    }

    /**
     * Get the events table for an experiment.
     * If all traces in the experiment are of the same type,
     * use the extension point specified event table
     * @param parent the parent Composite
     * @return an events table of the appropriate type
     */
    protected TmfEventsTable createEventsTable(Composite parent) {
        if (fExperiment == null) {
            return new TmfEventsTable(parent, DEFAULT_CACHE_SIZE);
        }
        int cacheSize = fExperiment.getCacheSize();
        String commonTraceType = null;
        try {
            for (ITmfTrace trace : fExperiment.getTraces()) {
                IResource resource = trace.getResource();
                if (resource == null) {
                    return new TmfEventsTable(parent, cacheSize);
                }
                String traceType = resource.getPersistentProperty(TmfCommonConstants.TRACETYPE);
                if ((commonTraceType != null) && !commonTraceType.equals(traceType)) {
                    return new TmfEventsTable(parent, cacheSize);
                }
                commonTraceType = traceType;
            }
            if (commonTraceType == null) {
                return new TmfEventsTable(parent, cacheSize);
            }
            if (commonTraceType.startsWith(CustomTxtTrace.class.getCanonicalName())) {
                return new CustomEventsTable(((CustomTxtTrace) fExperiment.getTraces()[0]).getDefinition(), parent, cacheSize);
            }
            if (commonTraceType.startsWith(CustomXmlTrace.class.getCanonicalName())) {
                return new CustomEventsTable(((CustomXmlTrace) fExperiment.getTraces()[0]).getDefinition(), parent, cacheSize);
            }
            for (IConfigurationElement ce : TmfTraceType.getTypeElements()) {
                if (ce.getAttribute(TmfTraceType.ID_ATTR).equals(commonTraceType)) {
                    IConfigurationElement[] eventsTableTypeCE = ce.getChildren(TmfTraceType.EVENTS_TABLE_TYPE_ELEM);
                    if (eventsTableTypeCE.length != 1) {
                        break;
                    }
                    String eventsTableType = eventsTableTypeCE[0].getAttribute(TmfTraceType.CLASS_ATTR);
                    if ((eventsTableType == null) || (eventsTableType.length() == 0)) {
                        break;
                    }
                    Bundle bundle = Platform.getBundle(ce.getContributor().getName());
                    Class<?> c = bundle.loadClass(eventsTableType);
                    Class<?>[] constructorArgs = new Class[] { Composite.class, int.class };
                    Constructor<?> constructor = c.getConstructor(constructorArgs);
                    Object[] args = new Object[] { parent, cacheSize };
                    return (TmfEventsTable) constructor.newInstance(args);
                }
            }
        } catch (CoreException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (InvalidRegistryObjectException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (SecurityException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (IllegalArgumentException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (ClassNotFoundException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (NoSuchMethodException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (InstantiationException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (IllegalAccessException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        } catch (InvocationTargetException e) {
            Activator.getDefault().logError("Error creating events table", e); //$NON-NLS-1$
        }
        return new TmfEventsTable(parent, cacheSize);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
	public void setFocus() {
        fEventsTable.setFocus();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class adapter) {
        if (IGotoMarker.class.equals(adapter)) {
            return fEventsTable;
        } else if (IPropertySheetPage.class.equals(adapter)) {
            // Override for unsorted property sheet page
            return new PropertySheetPage() {
                @Override
                public void createControl(Composite parent) {
                    super.createControl(parent);
                    setSorter(new PropertySheetSorter() {
                        @Override
                        public void sort(IPropertySheetEntry[] entries) {
                        }
                    });
                }
            };
        }
        return super.getAdapter(adapter);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
	@SuppressWarnings("nls")
	public String toString() {
    	return "[TmfEventsView]";
    }

    // ------------------------------------------------------------------------
    // ISelectionProvider
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    /**
     * @since 2.0
     */
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        fSelectionChangedListeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
     */
    /**
     * @since 2.0
     */
    @Override
    public ISelection getSelection() {
        if (fEventsTable == null) {
            return StructuredSelection.EMPTY;
        }
        return fEventsTable.getSelection();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    /**
     * @since 2.0
     */
    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        fSelectionChangedListeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
     */
    /**
     * @since 2.0
     */
    @Override
    public void setSelection(ISelection selection) {
        // not implemented
    }

    /**
     * Notifies any selection changed listeners that the viewer's selection has changed.
     * Only listeners registered at the time this method is called are notified.
     *
     * @param event a selection changed event
     *
     * @see ISelectionChangedListener#selectionChanged
     * @since 2.0
     */
    protected void fireSelectionChanged(final SelectionChangedEvent event) {
        Object[] listeners = fSelectionChangedListeners.getListeners();
        for (int i = 0; i < listeners.length; ++i) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            SafeRunnable.run(new SafeRunnable() {
                @Override
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
    }

    // ------------------------------------------------------------------------
    // ISelectionChangedListener
    // ------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    /**
     * @since 2.0
     */
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        fireSelectionChanged(event);
    }

    // ------------------------------------------------------------------------
    // Signal handlers
	// ------------------------------------------------------------------------

    /**
     * Handler for the experiment selected signal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void experimentSelected(TmfExperimentSelectedSignal signal) {
        // Update the trace reference
        TmfExperiment exp = signal.getExperiment();
        if (!exp.equals(fExperiment)) {
            fExperiment = exp;
            setPartName(fTitlePrefix + " - " + fExperiment.getName()); //$NON-NLS-1$
            if (fEventsTable != null) {
                fEventsTable.dispose();
            }
            fEventsTable = createEventsTable(fParent);
            fEventsTable.addSelectionChangedListener(this);
            fEventsTable.setTrace(fExperiment, false);
            fEventsTable.refreshBookmarks(fExperiment.getBookmarksFile());
            if (fExperiment.getBookmarksFile() != null) {
                ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
            }
            fParent.layout();
        }
    }

    /**
     * Handler for the experiment disposed signal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void experimentDisposed(TmfExperimentDisposedSignal signal) {
        // Clear the trace reference
        TmfExperiment experiment = signal.getExperiment();
        if (experiment.equals(fExperiment)) {
            fEventsTable.setTrace(null, false);

            Activator.getDefault().getWorkbench().getWorkbenchWindows()[0].getShell().getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    setPartName(fTitlePrefix);
                }
            });

            if ((fExperiment != null) && (fExperiment.getBookmarksFile() != null)) {
                ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
            }
            fExperiment = null;
        }
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
	    if ((fExperiment == null) || (fExperiment.getBookmarksFile() == null)) {
	        return;
	    }

        for (final IMarkerDelta delta : event.findMarkerDeltas(IMarker.BOOKMARK, false)) {
            if (delta.getResource().equals(fExperiment.getBookmarksFile())) {
                if (delta.getKind() == IResourceDelta.REMOVED) {
                    final IMarker bookmark = delta.getMarker();
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            fEventsTable.removeBookmark(bookmark);
                        }
                    });
                } else if (delta.getKind() == IResourceDelta.CHANGED) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            fEventsTable.getTable().refresh();
                        }
                    });
                }
            }
        }
    }

    /**
     * Update the display to use the updated timestamp format
     *
     * @param signal the incoming signal
     * @since 2.0
     */
    @TmfSignalHandler
    public void timestampFormatUpdated(TmfTimestampFormatUpdateSignal signal) {
        fEventsTable.refresh();
    }

}
