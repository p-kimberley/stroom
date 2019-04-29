package stroom.explorer.impl;

import stroom.explorer.shared.FetchExplorerNodeAction;
import stroom.explorer.shared.FetchExplorerNodeResult;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


class FetchExplorerNodeHandler
        extends AbstractTaskHandler<FetchExplorerNodeAction, FetchExplorerNodeResult> {
    private final ExplorerServiceImpl explorerService;
    private final ExplorerEventLog explorerEventLog;

    @Inject
    FetchExplorerNodeHandler(final ExplorerServiceImpl explorerService,
                             final ExplorerEventLog explorerEventLog) {
        this.explorerService = explorerService;
        this.explorerEventLog = explorerEventLog;
    }

    @Override
    public FetchExplorerNodeResult exec(final FetchExplorerNodeAction action) {
        FetchExplorerNodeResult result;
        try {
            result = explorerService.getData(action.getCriteria());
            explorerEventLog.find(action.getCriteria(), null);
        } catch (final RuntimeException e) {
            explorerEventLog.find(action.getCriteria(), e);
            throw e;
        }
        return result;
    }
}
