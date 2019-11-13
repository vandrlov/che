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

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment.PodData;

/**
 * Mount configured self-signed certificate for git provider as file in each workspace machines if
 * configured.
 *
 * @author Vitalii Parfonov
 */
@Singleton
public class VcsSslCertificateProvisioner
    implements ConfigurationProvisioner<KubernetesEnvironment> {
  static final String CHE_GIT_SELF_SIGNED_CERT_CONFIG_MAP_SUFFIX = "-che-git-self-signed-cert";
  static final String CHE_GIT_SELF_SIGNED_VOLUME = "che-git-self-signed-cert";
  static final String CERT_MOUNT_PATH = "/etc/che/git/cert/";
  static final String CA_CERT_FILE = "ca.crt";

  private static final String HTTPS = "https://";

  @Inject(optional = true)
  @Named("che.git.certs_path")
  private String certsPath;

  private Map<String, String> certs = new HashMap<>();

  public VcsSslCertificateProvisioner() {}

  @VisibleForTesting
  VcsSslCertificateProvisioner(String certsPathFolder) {
    this.certsPath = certsPathFolder;
  }

  /** @return true only if */
  public boolean isConfigured() {
    return !isNullOrEmpty(certsPath);
  }

  private List<Path> readFiles() {
    if (Strings.isNullOrEmpty(certsPath)) {
      return Collections.EMPTY_LIST;
    }
    try {
      return Files.walk(Paths.get(certsPath))
          .filter(Files::isRegularFile)
          .collect(Collectors.toList());
    } catch (IOException e) {
      return Collections.EMPTY_LIST;
    }
  }

  private void readCerts() {
    if (!certs.isEmpty()) {
      return;
    }
    List<Path> paths = readFiles();
    for (Path path : paths) {
      try {
        byte[] encoded = Files.readAllBytes(path);
        certs.put(path.getFileName().toString(), new String(encoded));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getHostConfigs() {
    StringBuilder config = new StringBuilder();
    List<Path> hosts = readFiles();
    for (Path host : hosts) {
      config
          .append("[http \"")
          .append(HTTPS)
          .append(host.getFileName().toString())
          .append("*\"]")
          .append('\n')
          .append('\t')
          .append("sslCAInfo = ")
          .append(CERT_MOUNT_PATH + host.getFileName().toString())
          .append('\n');
    }
    return config.toString();
  }

  @Override
  public void provision(KubernetesEnvironment k8sEnv, RuntimeIdentity identity)
      throws InfrastructureException {
    if (!isConfigured()) {
      return;
    }
    readCerts();
    String selfSignedCertConfigMapName =
        identity.getWorkspaceId() + CHE_GIT_SELF_SIGNED_CERT_CONFIG_MAP_SUFFIX;
    k8sEnv
        .getConfigMaps()
        .put(
            selfSignedCertConfigMapName,
            new ConfigMapBuilder()
                .withNewMetadata()
                .withName(selfSignedCertConfigMapName)
                .endMetadata()
                .withData(certs)
                .build());

    for (PodData pod : k8sEnv.getPodsData().values()) {
      Optional<Volume> certVolume =
          pod.getSpec()
              .getVolumes()
              .stream()
              .filter(v -> v.getName().equals(CHE_GIT_SELF_SIGNED_VOLUME))
              .findAny();

      if (!certVolume.isPresent()) {
        pod.getSpec().getVolumes().add(buildCertVolume(selfSignedCertConfigMapName));
      }

      for (Container container : pod.getSpec().getInitContainers()) {
        provisionCertVolumeMountIfNeeded(container);
      }
      for (Container container : pod.getSpec().getContainers()) {
        provisionCertVolumeMountIfNeeded(container);
      }
    }
  }

  private void provisionCertVolumeMountIfNeeded(Container container) {
    Optional<VolumeMount> certVolumeMount =
        container
            .getVolumeMounts()
            .stream()
            .filter(vm -> vm.getName().equals(CHE_GIT_SELF_SIGNED_VOLUME))
            .findAny();
    if (!certVolumeMount.isPresent()) {
      container.getVolumeMounts().add(buildCertVolumeMount());
    }
  }

  private VolumeMount buildCertVolumeMount() {
    return new VolumeMountBuilder()
        .withName(CHE_GIT_SELF_SIGNED_VOLUME)
        .withNewReadOnly(true)
        .withMountPath(CERT_MOUNT_PATH)
        .build();
  }

  private Volume buildCertVolume(String configMapName) {
    return new VolumeBuilder()
        .withName(CHE_GIT_SELF_SIGNED_VOLUME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build())
        .build();
  }
}
