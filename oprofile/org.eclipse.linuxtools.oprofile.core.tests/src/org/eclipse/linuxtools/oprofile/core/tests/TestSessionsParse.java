/*******************************************************************************
 * Copyright (c) 2009, 2018 Red Hat, Inc.
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
package org.eclipse.linuxtools.oprofile.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.FileReader;
import java.util.ArrayList;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.linuxtools.internal.oprofile.core.model.OpModelEvent;
import org.eclipse.linuxtools.internal.oprofile.core.model.OpModelSession;
import org.eclipse.linuxtools.internal.oprofile.core.opxml.OprofileSAXHandler;
import org.eclipse.linuxtools.internal.oprofile.core.opxml.sessions.SessionsProcessor;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class TestSessionsParse {
    private static final String REL_PATH_TO_TEST_XML = "resources/test_sessions.xml"; //$NON-NLS-1$
    private static final String EVENT1_OUTPUT = "current\nEvent: BR_INST_EXEC\nEvent: UOPS_RETIRED\n"; //$NON-NLS-1$
    private static final String EVENT1_OUTPUT_WITHTAB = "current\n\tEvent: BR_INST_EXEC\n\tEvent: UOPS_RETIRED\n"; //$NON-NLS-1$
    private static final String EVENT2_OUTPUT = "saved\nEvent: CPU_CLK_UNHALTED\n"; //$NON-NLS-1$
    private static final String EVENT2_OUTPUT_WITHTAB = "saved\n\tEvent: CPU_CLK_UNHALTED\n"; //$NON-NLS-1$
    private static final String EVENT3_OUTPUT = "\"<>&'\nEvent: UOPS_RETIRED\n"; //$NON-NLS-1$
    private static final String EVENT3_OUTPUT_WITHTAB = "\"<>&'\n\tEvent: UOPS_RETIRED\n"; //$NON-NLS-1$

    private ArrayList<OpModelSession> eventList;

    @Before
    public void setUp() throws Exception {
        /* this code mostly taken from OpxmlRunner */
        XMLReader reader = null;
        eventList = new ArrayList<>();
        SessionsProcessor.SessionInfo sessioninfo = new SessionsProcessor.SessionInfo(eventList);
        OprofileSAXHandler handler = OprofileSAXHandler.getInstance(sessioninfo);

        // Create XMLReader
        SAXParserFactory factory = SAXParserFactory.newInstance();
        reader = factory.newSAXParser().getXMLReader();

        // Set content/error handlers
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        String filePath = FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(this.getClass()), new Path(REL_PATH_TO_TEST_XML), null)).getFile();
        reader.parse(new InputSource(new FileReader(filePath)));
    }

    @Test
    public void testParse() {
        assertEquals(3, eventList.size());
        OpModelEvent evt1 = eventList.get(0).getEvents()[0], evt2 = eventList.get(1).getEvents()[0], evt3 = eventList.get(2).getEvents()[0];

        assertEquals("BR_INST_EXEC", evt1.getName()); //$NON-NLS-1$
        assertEquals("CPU_CLK_UNHALTED", evt2.getName()); //$NON-NLS-1$
        assertEquals("UOPS_RETIRED", evt3.getName()); //$NON-NLS-1$


        OpModelSession evt1_ss_s1 = evt1.getSession();
        OpModelSession evt2_ss_s1 = evt2.getSession();
        OpModelSession evt3_ss_s1 = evt3.getSession();


        assertEquals("current", evt1_ss_s1.getName()); //$NON-NLS-1$
        assertEquals(true, evt1_ss_s1.isDefaultSession());
        assertNull(evt1.getImage());
        assertEquals(0, evt1.getCount());
        assertEquals(evt1, evt1_ss_s1.getEvents()[0]);

        assertEquals("saved", evt2_ss_s1.getName()); //$NON-NLS-1$
        assertEquals(false, evt2_ss_s1.isDefaultSession());
        assertNull(evt2.getImage());
        assertEquals(0, evt2.getCount());
        assertEquals(evt2, evt2_ss_s1.getEvents()[0]);

        assertEquals("\"<>&'", evt3_ss_s1.getName()); //$NON-NLS-1$
        assertEquals(false, evt3_ss_s1.isDefaultSession());
        assertNull(evt3.getImage());
        assertEquals(0, evt3.getCount());
        assertEquals(evt3, evt3_ss_s1.getEvents()[0]);
    }

    @Test
    public void testStringOutput() {
        assertEquals(EVENT1_OUTPUT, eventList.get(0).toString());
        assertEquals(EVENT1_OUTPUT_WITHTAB, eventList.get(0).toString("\t")); //$NON-NLS-1$
        assertEquals(EVENT2_OUTPUT, eventList.get(1).toString());
        assertEquals(EVENT2_OUTPUT_WITHTAB, eventList.get(1).toString("\t")); //$NON-NLS-1$
        assertEquals(EVENT3_OUTPUT, eventList.get(2).toString());
        assertEquals(EVENT3_OUTPUT_WITHTAB, eventList.get(2).toString("\t")); //$NON-NLS-1$
    }
}
