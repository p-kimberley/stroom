package stroom.explorer.impl;

import stroom.docref.DocRefInfo;
import stroom.explorer.api.ExplorerService;
import stroom.explorer.shared.ExplorerServiceInfoAction;
import stroom.explorer.shared.SharedDocRefInfo;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;

class ExplorerServiceInfoHandler extends AbstractTaskHandler<ExplorerServiceInfoAction, SharedDocRefInfo> {
    private final ExplorerServiceImpl explorerService;
    private final ExplorerEventLog explorerEventLog;

    @Inject
    ExplorerServiceInfoHandler(final ExplorerServiceImpl explorerService,
                               final ExplorerEventLog explorerEventLog) {
        this.explorerService = explorerService;
        this.explorerEventLog = explorerEventLog;
    }

    @Override
    public SharedDocRefInfo exec(final ExplorerServiceInfoAction task) {
        try {
            final DocRefInfo docRefInfo = explorerService.info(task.getDocRef());
            explorerEventLog.info(task.getDocRef(), null);

            return new SharedDocRefInfo.Builder()
                    .type(docRefInfo.getDocRef().getType())
                    .uuid(docRefInfo.getDocRef().getUuid())
                    .name(docRefInfo.getDocRef().getName())
                    .otherInfo(docRefInfo.getOtherInfo())
                    .createTime(docRefInfo.getCreateTime())
                    .createUser(docRefInfo.getCreateUser())
                    .updateTime(docRefInfo.getUpdateTime())
                    .updateUser(docRefInfo.getUpdateUser())
                    .build();

        } catch (final RuntimeException e) {
            explorerEventLog.info(task.getDocRef(), e);
            throw e;
        }
    }
}
