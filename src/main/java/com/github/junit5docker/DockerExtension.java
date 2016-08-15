package com.github.junit5docker;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ContainerExtensionContext;

import java.util.HashMap;

public class DockerExtension implements BeforeAllCallback, AfterAllCallback {

    private final DockerClientAdapter dockerClient;

    private String containerID;

    public DockerExtension() {
        this(new DefaultDockerClient());
    }

    DockerExtension(DockerClientAdapter dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void beforeAll(ContainerExtensionContext containerExtensionContext) throws Exception {
        Docker dockerAnnotation = findDockerAnnotation(containerExtensionContext);
        PortBinding[] portBindings = createPortBindings(dockerAnnotation);
        HashMap<String, String> environmentMap = createEnvironmentMap(dockerAnnotation);
        String imageReference = findImageName(dockerAnnotation);
        containerID = dockerClient.startContainer(imageReference, environmentMap, portBindings);
    }

    private Docker findDockerAnnotation(ContainerExtensionContext containerExtensionContext) {
        Class<?> testClass = containerExtensionContext.getTestClass().get();
        return testClass.getAnnotation(Docker.class);
    }

    private String findImageName(Docker dockerAnnotation) {
        return dockerAnnotation.image();
    }

    private HashMap<String, String> createEnvironmentMap(Docker dockerAnnotation) {
        HashMap<String, String> environmentMap = new HashMap<>();
        Environment[] environments = dockerAnnotation.environments();
        for (Environment environment : environments) {
            environmentMap.put(environment.key(), environment.value());
        }
        return environmentMap;
    }

    private PortBinding[] createPortBindings(Docker dockerAnnotation) {
        Port[] ports = dockerAnnotation.ports();
        PortBinding[] portBindings = new PortBinding[ports.length];
        for (int i = 0; i < ports.length; i++) {
            Port port = ports[i];
            portBindings[i] = new PortBinding(port.exposed(), port.inner());
        }
        return portBindings;
    }

    @Override
    public void afterAll(ContainerExtensionContext containerExtensionContext) throws Exception {
        dockerClient.stopAndRemoveContainer(containerID);
    }
}