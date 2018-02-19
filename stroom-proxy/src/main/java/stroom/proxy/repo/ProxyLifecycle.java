package stroom.proxy.repo;

import io.dropwizard.lifecycle.Managed;

import javax.inject.Inject;

public class ProxyLifecycle implements Managed {
    private final ProxyRepositoryManager proxyRepositoryManager;

    @Inject
    public ProxyLifecycle(final ProxyRepositoryManager proxyRepositoryManager) {
        this.proxyRepositoryManager = proxyRepositoryManager;
    }

    @Override
    public void start() throws Exception {
        proxyRepositoryManager.start();
    }

    @Override
    public void stop() throws Exception {
        proxyRepositoryManager.stop();
    }
}
