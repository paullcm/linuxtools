/*******************************************************************************
 * Copyright (c) 2007, 2018 Alphonse Van Assche.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alphonse Van Assche - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.rpmstubby;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.linuxtools.internal.rpmstubby.model.FeatureModel;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Generates the RPM specfile and the fetch script based on the feature and user
 * preferences.
 */
public class StubbyGenerator extends AbstractGenerator {

    private FeatureModel model;
    private IFile featureFile;

    /**
     * Creates the specfile and fetch script generator for the given packages.
     *
     * @param featureFile
     *            The feature.xml file to generate from.
     */
    public StubbyGenerator(IFile featureFile) {
        this.featureFile = featureFile;
        parse(featureFile);
        specfileName = model.getPackageName().toLowerCase() + ".spec";
        projectName = featureFile.getProject().getName();
    }

    private void parse(IFile featureFile) {
        DocumentBuilderFactory docfactory = DocumentBuilderFactory
                .newInstance();
        IPath featureDir = featureFile.getLocation().removeLastSegments(1);
        String featurePropertiesFile = featureDir.toOSString()
                + "/feature.properties";
        Properties featureProperties = new Properties();
        try {
            featureProperties.load(new FileInputStream(featurePropertiesFile));
        } catch (FileNotFoundException e) {
            // Do nothing if the feature.properties is not found
        } catch (IOException e) {
            StubbyLog.logError(e);
        }
        DocumentBuilder docbuilder;
        try {
            docbuilder = docfactory.newDocumentBuilder();
            Document docroot = docbuilder.parse(featureFile.getContents());
            model = new FeatureModel(docroot, featureProperties);

        } catch (ParserConfigurationException|SAXException|IOException|CoreException e) {
            StubbyLog.logError(e);
        }
    }

    /**
     * Generates a RPM specfile based on the parsed data from the pom file.
     *
     * @return The generated specfile.
     */
    @Override
    public String generateSpecfile() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Name:           " + model.getPackageName().toLowerCase()
                + "\n");
        buffer.append("Version:        " + model.getVersion() + "\n");
        buffer.append("Release:        1%{?dist}" + "\n");
        buffer.append("Summary:        " + model.getSummary() + "\n\n");
        buffer.append("License:        " + model.getLicense() + "\n");
        buffer.append("URL:            " + model.getURL() + "\n");
        buffer.append("Source0:        #FIXME\n");
        buffer.append("BuildArch:      noarch\n\n");
        generateRequires(buffer);
        buffer.append("%description\n" + model.getDescription() + "\n");
        generatePrepSection(buffer);
        generateBuildSection(buffer);
        generateInstallSection(buffer);
        generateFilesSections(buffer);
        generateChangelog(buffer);

        return buffer.toString();
    }

    private static void generateRequires(StringBuilder buffer) {
        buffer.append("BuildRequires:  maven-local\n");
        buffer.append("\n\n");
    }

    private static void generateInstallSection(StringBuilder buffer) {
        buffer.append("%install\n");
        buffer.append("%mvn_install \n");
        buffer.append("\n\n");
    }

    private void generateFilesSections(StringBuilder buffer) {
        buffer.append("%files -f .mfiles\n");
        String docsRoot = featureFile.getLocation().removeLastSegments(1)
                .lastSegment();
        String[] files = featureFile.getLocation().removeLastSegments(1)
                .toFile().list();
        for (String file : files) {
            if (file.matches("(epl-.*|license)\\.html")) {
                buffer.append("%doc " + docsRoot + "/" + file + "\n");
            }
        }
        buffer.append("\n\n");
    }

    private static void generatePrepSection(StringBuilder buffer) {
        buffer.append("\n%prep\n");
        buffer.append("%setup -q #You may need to update this according to your Source0\n");
        buffer.append("\n\n");
    }

    private static void generateBuildSection(StringBuilder buffer) {
        buffer.append("%build\n");
        buffer.append("%mvn_build -j\n ");
        buffer.append("\n\n");
    }

    /**
     * Returns the last meaningful part of the feature id before the feature
     * substring.
     *
     * @param packageName
     *            The feature id from which to extract the name.
     * @return The part of the feature id to be used for package name.
     */
    public String getPackageName(String packageName) {
        String[] packageItems = packageName.split("\\.");
        String name = packageItems[packageItems.length - 1];
        if (name.equalsIgnoreCase("feature")) {
            name = packageItems[packageItems.length - 2];
        }
        return "eclipse-" + name;
    }
}
