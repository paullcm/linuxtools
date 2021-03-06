/*******************************************************************************
 * Copyright (c) 2014, 2018 Red Hat.
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

import static org.eclipse.linuxtools.internal.docker.ui.launch.IRunDockerImageLaunchConfigurationConstants.MB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerHostConfig;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerPortBinding;
import org.eclipse.linuxtools.internal.docker.core.DockerContainerConfig;
import org.eclipse.linuxtools.internal.docker.core.DockerContainerConfig.Builder;
import org.eclipse.linuxtools.internal.docker.core.DockerHostConfig;
import org.eclipse.linuxtools.internal.docker.core.DockerPortBinding;
import org.eclipse.linuxtools.internal.docker.ui.launch.IRunDockerImageLaunchConfigurationConstants;
import org.eclipse.linuxtools.internal.docker.ui.launch.LaunchConfigurationUtils;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageRunSelectionModel.ContainerLinkModel;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageRunSelectionModel.ExposedPortModel;

/**
 * Wizard to 'docker run' a given {@link IDockerImage}.
 */
public class ImageRun extends Wizard {

	private final ImageRunSelectionPage imageRunSelectionPage;
	private final ImageRunResourceVolumesVariablesPage imageRunResourceVolumesPage;
	private final ImageRunNetworkPage imageRunNetworkPage;

	/**
	 * Constructor when an {@link IDockerConnection} has been selected to run an
	 * {@link IDockerImage}.
	 * 
	 * @param connection
	 *            the {@link IDockerConnection} pointing to a specific Docker
	 *            daemon/host.
	 * @throws DockerException
	 */
	public ImageRun(final IDockerConnection connection) throws DockerException {
		super();
		setWindowTitle(WizardMessages.getString("ImageRun.title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		this.imageRunSelectionPage = new ImageRunSelectionPage(connection);
		this.imageRunResourceVolumesPage = new ImageRunResourceVolumesVariablesPage(
				connection);
		this.imageRunNetworkPage = new ImageRunNetworkPage(connection);
	}

	/**
	 * Full constructor with a selected {@link IDockerImage} to run.
	 * 
	 * @param selectedImage
	 *            the {@link IDockerImage} to use to fill the wizard pages
	 * @throws DockerException
	 * @throws CoreException
	 */
	public ImageRun(final IDockerImage selectedImage)
			throws DockerException, CoreException {
		setWindowTitle(WizardMessages.getString("ImageRun.title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		// attempt to find the last "Image Run" launch configuration for this
		// image
		final ILaunchConfiguration lastLaunchConfiguration = LaunchConfigurationUtils
				.getLaunchConfigurationByImageName(
						LaunchConfigurationUtils.getLaunchConfigType(
								IRunDockerImageLaunchConfigurationConstants.CONFIG_TYPE_ID),
						selectedImage.repoTags().get(0));
		this.imageRunSelectionPage = new ImageRunSelectionPage(selectedImage,
				lastLaunchConfiguration);
		this.imageRunResourceVolumesPage = new ImageRunResourceVolumesVariablesPage(
				selectedImage, lastLaunchConfiguration);
		this.imageRunNetworkPage = new ImageRunNetworkPage(selectedImage,
				lastLaunchConfiguration);
	}

	@Override
	public void addPages() {
		addPage(imageRunSelectionPage);
		addPage(imageRunResourceVolumesPage);
		addPage(imageRunNetworkPage);
	}

	@Override
	public IWizardPage getNextPage(final IWizardPage page) {
		if (page.equals(imageRunSelectionPage)) {
			imageRunResourceVolumesPage.getModel().setSelectedImage(
					imageRunSelectionPage.getModel().getSelectedImage());
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	public String getDockerContainerName() {
		return this.imageRunSelectionPage.getModel().getContainerName();
	}

	public boolean removeWhenExits() {
		return this.imageRunSelectionPage.getModel().isRemoveWhenExits();
	}

	public IDockerHostConfig getDockerHostConfig() {
		final ImageRunSelectionModel selectionModel = this.imageRunSelectionPage
				.getModel();
		final ImageRunResourceVolumesVariablesModel resourcesModel = this.imageRunResourceVolumesPage
				.getModel();
		final ImageRunNetworkModel networkModel = this.imageRunNetworkPage
				.getModel();

		final DockerHostConfig.Builder hostConfigBuilder = new DockerHostConfig.Builder();
		if (selectionModel.isPublishAllPorts()) {
			hostConfigBuilder.publishAllPorts(true);
		} else {
			final Map<String, List<IDockerPortBinding>> portBindings = new HashMap<>();
			for (ExposedPortModel exposedPort : selectionModel.getExposedPorts()) {
				// only selected Ports in the CheckboxTableViewer are exposed.
				if (!selectionModel.getSelectedPorts().contains(exposedPort)) {
					continue;
				}
				final DockerPortBinding portBinding = new DockerPortBinding(exposedPort.getHostAddress(),
						exposedPort.getHostPort());
				portBindings.put(
						exposedPort.getContainerPort()
								+ "/" + exposedPort.getPortType(), //$NON-NLS-1$
						Arrays.<IDockerPortBinding> asList(portBinding));
			}
			hostConfigBuilder.portBindings(portBindings);
		}
		// container links
		final List<String> links = new ArrayList<>();
		for (ContainerLinkModel link : selectionModel.getLinks()) {
			links.add(link.getContainerName() + ':' + link.getContainerAlias());
		}
		hostConfigBuilder.links(links);

		// data volumes
		final List<String> volumesFrom = new ArrayList<>();
		final List<String> binds = new ArrayList<>();
		for (DataVolumeModel dataVolume : resourcesModel.getDataVolumes()) {
			// only data volumes selected in the CheckBoxTableViewer are
			// included.
			if (!resourcesModel.getSelectedDataVolumes().contains(dataVolume)) {
				continue;
			}

			switch (dataVolume.getMountType()) {
			case HOST_FILE_SYSTEM:
				String bind = LaunchConfigurationUtils.convertToUnixPath(dataVolume.getHostPathMount()) + ':'
						+ dataVolume.getContainerPath() + ":Z"; //$NON-NLS-1$ //$NON-NLS-2$
				if (dataVolume.isReadOnly()) {
					bind += ",ro"; //$NON-NLS-1$
				}
				binds.add(bind);
				break;
			case CONTAINER:
				volumesFrom.add(dataVolume.getContainerMount());
				break;
			default:
				break;

			}
		}
		hostConfigBuilder.binds(binds);
		hostConfigBuilder.volumesFrom(volumesFrom);
		hostConfigBuilder.privileged(selectionModel.isPrivileged());
		// if user has asked for basic security, use a readonly rootfs,
		// make /tmp and /run use tmpfs, and drop all capabilities
		if (selectionModel.isBasicSecurity()) {
			hostConfigBuilder.readonlyRootfs(true);
			Map<String, String> tmpfsValues = new HashMap<>();
			tmpfsValues.put("/run", "rw,exec"); //$NON-NLS-1$ //$NON-NLS-2$
			tmpfsValues.put("/tmp", "rw,exec"); //$NON-NLS-1$ //$NON-NLS-2$
			hostConfigBuilder.tmpfs(tmpfsValues);
			List<String> capDropList = new ArrayList<>();
			capDropList.add("all"); //$NON-NLS-1$
			hostConfigBuilder.capDrop(capDropList);
		}
		if (selectionModel.isUnconfined()) {
			List<String> seccomp = new ArrayList<>();
			seccomp.add("seccomp:unconfined"); //$NON-NLS-1$
			hostConfigBuilder.securityOpt(seccomp);
		}
		String networkMode = networkModel.getNetworkModeString();
		// if network mode is not default, set it in host config
		if (networkMode != null
				&& !networkMode.equals(ImageRunNetworkModel.DEFAULT_MODE))
			hostConfigBuilder.networkMode(networkMode);
		// memory constraints (in bytes)
		if (resourcesModel.isEnableResourceLimitations()) {
			hostConfigBuilder.memory(resourcesModel.getMemoryLimit() * MB);
			hostConfigBuilder.cpuShares(resourcesModel.getCpuShareWeight());
		}
		return hostConfigBuilder.build();
	}

	public DockerContainerConfig getDockerContainerConfig() {
		final ImageRunSelectionModel selectionModel = this.imageRunSelectionPage
				.getModel();
		final ImageRunResourceVolumesVariablesModel resourcesModel = this.imageRunResourceVolumesPage
				.getModel();

		final Builder config = new DockerContainerConfig.Builder()
				.cmd(selectionModel.getCommand())
				.entryPoint(selectionModel.getEntrypoint())
				.image(selectionModel.getSelectedImageName())
				.tty(selectionModel.isAllocatePseudoTTY())
				.openStdin(selectionModel.isInteractiveMode());
		if (resourcesModel.isEnableResourceLimitations()) {
			// memory limit must be converted from MB to bytes
			config.memory(resourcesModel.getMemoryLimit() * MB);
			config.cpuShares(resourcesModel.getCpuShareWeight());
		}
		// environment variables
		final List<String> environmentVariables = new ArrayList<>();
		for (EnvironmentVariableModel var : resourcesModel.getEnvironmentVariables()) {
			environmentVariables.add(var.getName() + "=" + var.getValue()); //$NON-NLS-1$
		}
		config.env(environmentVariables);

		// container labels
		final Map<String, String> labelVariables = new HashMap<>();
		for (LabelVariableModel var : resourcesModel.getLabelVariables()) {
			labelVariables.put(var.getName(), var.getValue()); // $NON-NLS-1$
		}
		config.labels(labelVariables);

		if (!selectionModel.isPublishAllPorts()) {
			final Set<String> exposedPorts = new HashSet<>();
			for (ExposedPortModel exposedPort : selectionModel.getExposedPorts()) {
				// only selected Ports in the CheckboxTableViewer are exposed.
				if (!selectionModel.getSelectedPorts().contains(exposedPort)) {
					continue;
				}
				exposedPorts.add(exposedPort.getContainerPort()
						+ "/" + exposedPort.getPortType()); //$NON-NLS-1$
			}
			config.exposedPorts(exposedPorts);
		}
		return config.build();
	}


}
