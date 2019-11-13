/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.provision;

import static org.eclipse.che.workspace.infrastructure.kubernetes.provision.VcsSslCertificateProvisioner.CERT_MOUNT_PATH;
import static org.eclipse.che.workspace.infrastructure.kubernetes.provision.VcsSslCertificateProvisioner.CHE_GIT_SELF_SIGNED_CERT_CONFIG_MAP_SUFFIX;
import static org.eclipse.che.workspace.infrastructure.kubernetes.provision.VcsSslCertificateProvisioner.CHE_GIT_SELF_SIGNED_VOLUME;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

/**
 * Tests {@link VcsSslCertificateProvisioner}.
 *
 * @author Vitalii Parfonov
 */
@Listeners(MockitoTestNGListener.class)
public class VcsSslCertificateProvisionerTest {

  private static final String WORKSPACE_ID = "workspace123";
  private static final String EXPECTED_CERT_NAME =
      WORKSPACE_ID + CHE_GIT_SELF_SIGNED_CERT_CONFIG_MAP_SUFFIX;

  @Mock private RuntimeIdentity runtimeId;
  private VcsSslCertificateProvisioner provisioner;
  private KubernetesEnvironment k8sEnv;
  private String localhostCert;
  private String ipCert;
  private String certsPath;
  private File localhostFile;
  private File ipFile;

  @BeforeMethod
  public void setUp() throws IOException {
    when(runtimeId.getWorkspaceId()).thenReturn(WORKSPACE_ID);

    ClassLoader classLoader = getClass().getClassLoader();

    localhostFile = new File(classLoader.getResource("certs/localhost").getFile());
    localhostCert = Files.readFile(localhostFile);

    ipFile = new File(classLoader.getResource("certs/127.0.0.1:1234").getFile());
    ipCert = Files.readFile(ipFile);

    certsPath = ipFile.toPath().getParent().toAbsolutePath().toString();

    provisioner = new VcsSslCertificateProvisioner(certsPath);
    k8sEnv = KubernetesEnvironment.builder().build();
  }

  @Test
  public void shouldReturnFalseIfCertificateIsNotConfigured() {
    provisioner = new VcsSslCertificateProvisioner();
    assertFalse(provisioner.isConfigured());
  }

  @Test
  public void shouldReturnTrueIfCertificateIsConfigured() {
    provisioner = new VcsSslCertificateProvisioner("localhost");
    assertTrue(provisioner.isConfigured());
  }

  @Test
  public void shouldReturnCertPathFile() {
    String certPath = provisioner.getHostConfigs();
    assertEquals(
        certPath,
        "[http \"https://127.0.0.1:1234\"]\n\tsslCAInfo = "
            + CERT_MOUNT_PATH
            + ipFile.getName()
            + "\n"
            + "[http \"https://localhost\"]\n\tsslCAInfo = "
            + CERT_MOUNT_PATH
            + localhostFile.getName()
            + "\n");
  }

  @Test
  public void shouldAddConfigMapWithCertificateIntoEnvironment() throws Exception {
    provisioner.provision(k8sEnv, runtimeId);

    Map<String, ConfigMap> configMaps = k8sEnv.getConfigMaps();
    assertEquals(configMaps.size(), 1);

    ConfigMap configMap = configMaps.get(EXPECTED_CERT_NAME);
    assertNotNull(configMap);
    assertEquals(configMap.getMetadata().getName(), EXPECTED_CERT_NAME);
    assertEquals(configMap.getData().get("localhost").trim(), localhostCert.trim());
  }

  @Test
  public void shouldAddVolumeAndVolumeMountsToPodsAndContainersInEnvironment() throws Exception {
    k8sEnv.addPod(createPod("pod"));
    k8sEnv.addPod(createPod("pod2"));

    provisioner.provision(k8sEnv, runtimeId);

    for (Pod pod : k8sEnv.getPodsCopy().values()) {
      verifyVolumeIsPresent(pod);

      for (Container container : pod.getSpec().getInitContainers()) {
        verifyVolumeMountIsPresent(container);
      }

      for (Container container : pod.getSpec().getContainers()) {
        verifyVolumeMountIsPresent(container);
      }
    }
  }

  @Test
  public void
      shouldNotAddVolumeAndVolumeMountsToPodsAndContainersInEnvironmentIfCertIsNotConfigured()
          throws Exception {
    provisioner = new VcsSslCertificateProvisioner();
    k8sEnv.addPod(createPod("pod"));
    k8sEnv.addPod(createPod("pod2"));

    provisioner.provision(k8sEnv, runtimeId);

    for (Pod pod : k8sEnv.getPodsCopy().values()) {
      assertTrue(pod.getSpec().getVolumes().isEmpty());
      for (Container container : pod.getSpec().getContainers()) {
        assertTrue(container.getVolumeMounts().isEmpty());
      }
    }
  }

  private void verifyVolumeIsPresent(Pod pod) {
    List<Volume> podVolumes = pod.getSpec().getVolumes();
    assertEquals(podVolumes.size(), 1);
    Volume certVolume = podVolumes.get(0);
    assertEquals(certVolume.getName(), CHE_GIT_SELF_SIGNED_VOLUME);
    ConfigMapVolumeSource volumeConfigMap = certVolume.getConfigMap();
    assertNotNull(volumeConfigMap);
    assertEquals(volumeConfigMap.getName(), EXPECTED_CERT_NAME);
  }

  private void verifyVolumeMountIsPresent(Container container) {
    List<VolumeMount> volumeMounts = container.getVolumeMounts();
    assertEquals(volumeMounts.size(), 1);
    VolumeMount volumeMount = volumeMounts.get(0);
    assertEquals(volumeMount.getName(), CHE_GIT_SELF_SIGNED_VOLUME);
    assertTrue(volumeMount.getReadOnly());
    assertEquals(volumeMount.getMountPath(), CERT_MOUNT_PATH);
  }

  private Pod createPod(String podName) {
    return new PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .endMetadata()
        .withNewSpec()
        .withInitContainers(new ContainerBuilder().build())
        .withContainers(new ContainerBuilder().build(), new ContainerBuilder().build())
        .endSpec()
        .build();
  }
}
