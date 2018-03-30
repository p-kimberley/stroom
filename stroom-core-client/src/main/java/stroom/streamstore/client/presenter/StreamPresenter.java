/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.streamstore.client.presenter;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;
import stroom.alert.client.event.AlertEvent;
import stroom.alert.client.event.ConfirmEvent;
import stroom.core.client.LocationManager;
import stroom.data.client.event.DataSelectionEvent.DataSelectionHandler;
import stroom.data.client.event.HasDataSelectionHandlers;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.dispatch.client.ExportFileCompleteUtil;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.shared.BaseEntity;
import stroom.entity.shared.DocRefUtil;
import stroom.entity.shared.EntityServiceFindDeleteAction;
import stroom.entity.shared.IdSet;
import stroom.entity.shared.PageRequest;
import stroom.entity.shared.ResultList;
import stroom.entity.shared.SharedDocRef;
import stroom.entity.shared.Sort.Direction;
import stroom.feed.shared.Feed;
import stroom.pipeline.shared.PipelineEntity;
import stroom.pipeline.stepping.client.event.BeginPipelineSteppingEvent;
import stroom.process.client.presenter.ExpressionPresenter;
import stroom.query.api.v2.DocRef;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.security.client.ClientSecurityContext;
import stroom.security.shared.ApplicationPermissionNames;
import stroom.streamstore.shared.DownloadDataAction;
import stroom.streamstore.shared.ExpressionUtil;
import stroom.streamstore.shared.FindStreamAttributeMapCriteria;
import stroom.streamstore.shared.FindStreamCriteria;
import stroom.streamstore.shared.ReprocessDataInfo;
import stroom.streamstore.shared.Stream;
import stroom.streamstore.shared.StreamAttributeMap;
import stroom.streamstore.shared.StreamDataSource;
import stroom.streamstore.shared.StreamStatus;
import stroom.streamstore.shared.StreamType;
import stroom.streamtask.shared.ReprocessDataAction;
import stroom.streamtask.shared.StreamProcessor;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.DefaultPopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreamPresenter extends MyPresenterWidget<StreamPresenter.StreamView>
        implements HasDataSelectionHandlers<IdSet>, HasDocumentRead<BaseEntity>, BeginSteppingHandler {
    public static final String DATA = "DATA";
    public static final String STREAM_RELATION_LIST = "STREAM_RELATION_LIST";
    public static final String STREAM_LIST = "STREAM_LIST";

    private final LocationManager locationManager;
    private final StreamListPresenter streamListPresenter;
    private final StreamRelationListPresenter streamRelationListPresenter;
    private final DataPresenter dataPresenter;
    private final Provider<StreamUploadPresenter> streamUploadPresenter;
    private final Provider<ExpressionPresenter> streamListFilterPresenter;
    private final ClientDispatchAsync dispatcher;
    private final ButtonView streamListFilter;

    private FindStreamAttributeMapCriteria findStreamAttributeMapCriteria;
    private Feed feedCriteria;
    private ButtonView streamListUpload;
    private ButtonView streamListDownload;
    private ButtonView streamListDelete;
    private ButtonView streamListProcess;
    private ButtonView streamListUndelete;
    private ButtonView streamRelationListDownload;
    private ButtonView streamRelationListDelete;
    private ButtonView streamRelationListUndelete;
    private ButtonView streamRelationListProcess;

    @Inject
    public StreamPresenter(final EventBus eventBus, final StreamView view, final LocationManager locationManager,
                           final StreamListPresenter streamListPresenter,
                           final StreamRelationListPresenter streamRelationListPresenter, final DataPresenter dataPresenter,
                           final Provider<ExpressionPresenter> streamListFilterPresenter,
                           final Provider<StreamUploadPresenter> streamUploadPresenter,
                           final ClientDispatchAsync dispatcher, final ClientSecurityContext securityContext) {
        super(eventBus, view);
        this.locationManager = locationManager;
        this.streamListPresenter = streamListPresenter;
        this.streamRelationListPresenter = streamRelationListPresenter;
        this.streamListFilterPresenter = streamListFilterPresenter;
        this.streamUploadPresenter = streamUploadPresenter;
        this.dataPresenter = dataPresenter;
        this.dispatcher = dispatcher;

        setInSlot(STREAM_LIST, streamListPresenter);
        setInSlot(STREAM_RELATION_LIST, streamRelationListPresenter);
        setInSlot(DATA, dataPresenter);

        dataPresenter.setBeginSteppingHandler(this);

        // Process
        if (securityContext.hasAppPermission(ApplicationPermissionNames.MANAGE_PROCESSORS_PERMISSION)) {
            streamListProcess = streamListPresenter.add(SvgPresets.PROCESS);
            streamRelationListProcess = streamRelationListPresenter.add(SvgPresets.PROCESS);
        }

        // Delete, Undelete, DE-duplicate
        if (securityContext.hasAppPermission(ApplicationPermissionNames.DELETE_DATA_PERMISSION)) {
            streamListDelete = streamListPresenter.add(SvgPresets.DELETE);
            streamListDelete.setEnabled(false);
            streamRelationListDelete = streamRelationListPresenter.add(SvgPresets.DELETE);
            streamRelationListDelete.setEnabled(false);
            streamListUndelete = streamListPresenter.add(SvgPresets.UNDO);
            streamListUndelete.setTitle("Un-Delete");
            streamRelationListUndelete = streamRelationListPresenter.add(SvgPresets.UNDO);
            streamRelationListUndelete.setTitle("un-Delete");
        }

        // Download
        if (securityContext.hasAppPermission(ApplicationPermissionNames.EXPORT_DATA_PERMISSION)) {
            streamListDownload = streamListPresenter.add(SvgPresets.DOWNLOAD);
            streamRelationListDownload = streamRelationListPresenter.add(SvgPresets.DOWNLOAD);
        }

        // Upload
        if (securityContext.hasAppPermission(ApplicationPermissionNames.IMPORT_DATA_PERMISSION)) {
            streamListUpload = streamListPresenter.add(SvgPresets.UPLOAD);
        }

        // Filter
        streamListFilter = streamListPresenter.add(SvgPresets.FILTER);

        // Init the buttons
        setStreamListSelectableEnabled(null, StreamStatus.UNLOCKED);
        setStreamRelationListSelectableEnabled(null, StreamStatus.UNLOCKED);
    }

    public static FindStreamCriteria createFindStreamCriteria() {
        final FindStreamCriteria findStreamCriteria = new FindStreamCriteria();
        findStreamCriteria.obtainExpression();
        return findStreamCriteria;
    }

    private static Stream getStream(final AbstractStreamListPresenter streamListPresenter, final long id) {
        final ResultList<StreamAttributeMap> list = streamListPresenter.getResultList();
        if (list != null) {
            if (list.getValues() != null) {
                for (final StreamAttributeMap streamAttributeMap : list.getValues()) {
                    if (streamAttributeMap.getStream().getId() == id) {
                        return streamAttributeMap.getStream();
                    }
                }
            }
        }
        return null;
    }

    public static boolean isSelectedAllOfStatus(final StreamStatus filterStatus,
                                                final AbstractStreamListPresenter streamListPresenter, final IdSet selectedIdSet,
                                                final StreamStatus... statusArray) {
        final List<StreamStatus> statusList = Arrays.asList(statusArray);
        // Nothing Selected
        if (selectedIdSet == null || selectedIdSet.isMatchNothing()) {
            return false;
        }
        // Match All must be a status that we expect
        if (Boolean.TRUE.equals(selectedIdSet.getMatchAll())) {
            if (filterStatus == null) {
                return false;
            }
            return statusList.contains(filterStatus);
        }

        for (final Long id : selectedIdSet.getSet()) {
            final Stream stream = getStream(streamListPresenter, id);
            if (stream == null || !statusList.contains(stream.getStatus())) {
                return false;
            }
        }

        return true;

    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(streamListPresenter.getSelectionModel().addSelectionHandler(event -> {
            streamRelationListPresenter.setSelectedStream(streamListPresenter.getSelectedStream(), true,
                    !StreamStatus.UNLOCKED.equals(getSingleStatus(getCriteria())));
            showData();
        }));
        registerHandler(streamListPresenter.addDataSelectionHandler(event -> setStreamListSelectableEnabled(event.getSelectedItem(),
                getSingleStatus(findStreamAttributeMapCriteria))));
        registerHandler(streamRelationListPresenter.getSelectionModel().addSelectionHandler(event -> showData()));
        registerHandler(streamRelationListPresenter.addDataSelectionHandler(event -> setStreamRelationListSelectableEnabled(event.getSelectedItem(), getSingleStatus(findStreamAttributeMapCriteria))));

        registerHandler(streamListFilter.addClickHandler(event -> {
            final ExpressionPresenter presenter = streamListFilterPresenter.get();
            presenter.read(findStreamAttributeMapCriteria.obtainFindStreamCriteria().obtainExpression(), StreamDataSource.STREAM_STORE_DOC_REF, StreamDataSource.getExtendedFields());

            final PopupUiHandlers streamFilterPUH = new DefaultPopupUiHandlers() {
                @Override
                public void onHideRequest(final boolean autoClose, final boolean ok) {
                    if (ok) {
                        final ExpressionOperator expression = presenter.write();

                        if (!expression.equals(findStreamAttributeMapCriteria.obtainFindStreamCriteria().obtainExpression())) {
                            if (hasAdvancedCriteria(expression)) {
                                ConfirmEvent.fire(StreamPresenter.this,
                                        "You are setting advanced filters!  It is recommendend you constrain your filter (e.g. by 'Created') to avoid an expensive query.  "
                                                + "Are you sure you want to apply this advanced filter?",
                                        confirm -> {
                                            if (confirm) {
                                                applyCriteriaAndShow(presenter);
                                                HidePopupEvent.fire(StreamPresenter.this, presenter);
                                            } else {
                                                // Don't hide
                                            }
                                        });

                            } else {
                                applyCriteriaAndShow(presenter);
                                HidePopupEvent.fire(StreamPresenter.this, presenter);
                            }

                        } else {
                            // Nothing changed!
                            HidePopupEvent.fire(StreamPresenter.this, presenter);
                        }

                    } else {
                        HidePopupEvent.fire(StreamPresenter.this, presenter);
                    }
                }

                private void applyCriteriaAndShow(final ExpressionPresenter presenter) {
                    // Copy new filter settings back.
                    findStreamAttributeMapCriteria.obtainFindStreamCriteria().setExpression(presenter.write());
                    // Reset the page offset.
                    findStreamAttributeMapCriteria.obtainPageRequest().setOffset(0L);

                    // Init the buttons
                    final StreamStatus status = getSingleStatus(findStreamAttributeMapCriteria);
                    setStreamListSelectableEnabled(streamListPresenter.getSelectedEntityIdSet(), status);

                    // Clear the current selection and get a new list of streams.
                    streamListPresenter.getSelectionModel().clear();
                    streamListPresenter.refresh();
                }
            };

            final PopupSize popupSize = new PopupSize(800, 600, 400, 400, true);
            ShowPopupEvent.fire(StreamPresenter.this, presenter, PopupType.OK_CANCEL_DIALOG, popupSize,
                    "Filter Streams", streamFilterPUH);
        }));

        // Some button's may not exist due to permissions
        if (streamListUpload != null) {
            registerHandler(streamListUpload.addClickHandler(event -> streamUploadPresenter.get().show(StreamPresenter.this, DocRefUtil.create(feedCriteria))));
        }
        if (streamListDownload != null) {
            registerHandler(streamListDownload
                    .addClickHandler(new DownloadStreamClickHandler(this, streamListPresenter, true, dispatcher, locationManager)));
        }
        if (streamRelationListDownload != null) {
            registerHandler(streamRelationListDownload.addClickHandler(
                    new DownloadStreamClickHandler(this, streamRelationListPresenter, false, dispatcher, locationManager)));
        }
        // Delete
        if (streamListDelete != null) {
            registerHandler(streamListDelete
                    .addClickHandler(new DeleteStreamClickHandler(this, streamListPresenter, true, dispatcher)));
        }
        if (streamRelationListDelete != null) {
            registerHandler(streamRelationListDelete.addClickHandler(
                    new DeleteStreamClickHandler(this, streamRelationListPresenter, false, dispatcher)));
        }
        // UN-Delete
        if (streamListUndelete != null) {
            registerHandler(streamListUndelete
                    .addClickHandler(new DeleteStreamClickHandler(this, streamListPresenter, true, dispatcher)));
        }
        if (streamRelationListUndelete != null) {
            registerHandler(streamRelationListUndelete.addClickHandler(
                    new DeleteStreamClickHandler(this, streamRelationListPresenter, false, dispatcher)));
        }
        // Process
        if (streamListProcess != null) {
            registerHandler(streamListProcess
                    .addClickHandler(new ProcessStreamClickHandler(this, streamListPresenter, true, dispatcher)));
        }
        if (streamRelationListProcess != null) {
            registerHandler(streamRelationListProcess.addClickHandler(
                    new ProcessStreamClickHandler(this, streamRelationListPresenter, false, dispatcher)));
        }
    }

    public boolean hasAdvancedCriteria(final ExpressionOperator expression) {
        final StreamStatus status = getSingleStatus(expression);

        if (!StreamStatus.UNLOCKED.equals(status)) {
            return true;
        }

        final Set<String> statusPeriod = getTerms(expression, StreamDataSource.STATUS_TIME);
        return statusPeriod.size() > 0;
    }


    private static StreamStatus getSingleStatus(final FindStreamAttributeMapCriteria criteria) {
        if (criteria == null) {
            return null;
        }
        return getSingleStatus(criteria.getFindStreamCriteria());
    }

    private static StreamStatus getSingleStatus(final FindStreamCriteria criteria) {
        if (criteria == null) {
            return null;
        }
        return getSingleStatus(criteria.getExpression());
    }

    private static StreamStatus getSingleStatus(final ExpressionOperator expression) {
        final Set<StreamStatus> streamStatuses = getStatusSet(expression);
        if (streamStatuses.size() == 1) {
            return streamStatuses.iterator().next();
        }
        return null;
    }

    private static Set<StreamStatus> getStatusSet(final ExpressionOperator expression) {
        final Set<String> terms = getTerms(expression, StreamDataSource.STATUS);
        final Set<StreamStatus> streamStatuses = new HashSet<>();
        for (final String term : terms) {
            for (final StreamStatus streamStatus : StreamStatus.values()) {
                if (streamStatus.getDisplayValue().equals(term)) {
                    streamStatuses.add(streamStatus);
                }
            }
        }

        return streamStatuses;
    }

    private static Set<String> getTerms(final ExpressionOperator expression, final String field) {
        final Set<String> terms = new HashSet<>();
        if (expression != null) {
            getTerms(expression, field, terms);
        }
        return terms;
    }

    private static void getTerms(final ExpressionOperator expressionOperator, final String field, final Set<String> terms) {
        if (expressionOperator.enabled()) {
            for (final ExpressionItem item : expressionOperator.getChildren()) {
                if (item.enabled()) {
                    if (item instanceof ExpressionTerm) {
                        if (field.equals(((ExpressionTerm) item).getField())) {
                            terms.add(((ExpressionTerm) item).getValue());
                        }
                    } else if (item instanceof ExpressionOperator) {
                        getTerms((ExpressionOperator) item, field, terms);
                    }
                }
            }
        }
    }


    private void showData() {
        final Stream stream = getSelectedStream();
        if (stream == null) {
            dataPresenter.clear();
        } else {
            dataPresenter.fetchData(stream);
        }
    }

    public void refresh() {
        // Get a new list of streams.
        streamListPresenter.refresh();
    }

    public FindStreamCriteria getCriteria() {
        return findStreamAttributeMapCriteria.getFindStreamCriteria();
    }

    @Override
    public void read(final DocRef docRef, final BaseEntity entity) {
        if (entity instanceof Feed) {
            setFeedCriteria((Feed) entity);
        } else if (entity instanceof PipelineEntity) {
            setPipelineCriteria((PipelineEntity) entity);
        } else {
            setNullCriteria();
        }
    }

    private FindStreamAttributeMapCriteria createFindStreamAttributeMapCriteria() {
        final FindStreamAttributeMapCriteria criteria = new FindStreamAttributeMapCriteria();
        criteria.obtainFindStreamCriteria().obtainExpression();

        final PageRequest pageRequest = criteria.obtainPageRequest();
        pageRequest.setLength(PageRequest.DEFAULT_PAGE_SIZE);
        pageRequest.setOffset(0L);

        criteria.obtainFindStreamCriteria().getFetchSet().add(Feed.ENTITY_TYPE);
        criteria.obtainFindStreamCriteria().getFetchSet().add(PipelineEntity.ENTITY_TYPE);
        criteria.obtainFindStreamCriteria().getFetchSet().add(StreamProcessor.ENTITY_TYPE);
        criteria.obtainFindStreamCriteria().getFetchSet().add(StreamType.ENTITY_TYPE);
        criteria.obtainFindStreamCriteria().setSort(StreamDataSource.CREATE_TIME, Direction.DESCENDING, false);

        return criteria;
    }

    private void setFeedCriteria(final Feed feed) {
        feedCriteria = feed;
        showStreamListButtons(true);
        showStreamRelationListButtons(true);

        findStreamAttributeMapCriteria = createFindStreamAttributeMapCriteria();
        findStreamAttributeMapCriteria.obtainFindStreamCriteria().setExpression(ExpressionUtil.createFeedExpression(feed));

        initCriteria();
    }

    private void setPipelineCriteria(final PipelineEntity pipelineEntity) {
        showStreamListButtons(false);
        showStreamRelationListButtons(false);

        findStreamAttributeMapCriteria = createFindStreamAttributeMapCriteria();
        findStreamAttributeMapCriteria.obtainFindStreamCriteria().setExpression(ExpressionUtil.createPipelineExpression(pipelineEntity));

        initCriteria();
    }

    private void setNullCriteria() {
        showStreamListButtons(false);
        showStreamRelationListButtons(false);
        findStreamAttributeMapCriteria = createFindStreamAttributeMapCriteria();

        initCriteria();
    }

    private void initCriteria() {
        streamListPresenter.setCriteria(findStreamAttributeMapCriteria);
        streamRelationListPresenter.setCriteria(null);
    }

    private Stream getSelectedStream() {
        StreamAttributeMap selectedStream = streamListPresenter.getSelectedStream();
        if (streamRelationListPresenter.getSelectedStream() != null) {
            selectedStream = streamRelationListPresenter.getSelectedStream();
        }

        if (selectedStream != null) {
            return selectedStream.getStream();
        }

        return null;
    }

    public IdSet getSelectedEntityIdSet() {
        return streamListPresenter.getSelectedEntityIdSet();
    }

    @Override
    public HandlerRegistration addDataSelectionHandler(final DataSelectionHandler<IdSet> handler) {
        return streamListPresenter.addDataSelectionHandler(handler);
    }

    private void showStreamListButtons(final boolean visible) {
        if (streamListUpload != null) {
            streamListUpload.setVisible(visible);
        }
    }

    private void showStreamRelationListButtons(final boolean visible) {
    }

    public boolean isSomeSelected(final AbstractStreamListPresenter streamListPresenter,
                                  final IdSet selectedIdSet) {
        if (streamListPresenter.getResultList() == null || streamListPresenter.getResultList().size() == 0) {
            return false;
        }
        return selectedIdSet != null && (Boolean.TRUE.equals(selectedIdSet.getMatchAll()) || selectedIdSet.size() > 0);
    }

    public void setStreamListSelectableEnabled(final IdSet streamIdSet, final StreamStatus streamStatus) {
        final boolean someSelected = isSomeSelected(streamListPresenter, streamIdSet);

        if (streamListDownload != null) {
            streamListDownload.setEnabled(someSelected);
        }
        if (streamListDelete != null) {
            streamListDelete
                    .setEnabled(someSelected);
            // && isSelectedAllOfStatus(getCriteria().obtainStatusSet().getSingleItem(),
//                            streamListPresenter, streamIdSet, StreamStatus.LOCKED, StreamStatus.UNLOCKED));
        }
        if (streamListProcess != null) {
            streamListProcess.setEnabled(someSelected);
        }
        if (streamListUndelete != null) {
            // Hide if we are normal view (Unlocked streams)
            streamListUndelete.setVisible(!StreamStatus.UNLOCKED.equals(streamStatus));
            streamListUndelete
                    .setEnabled(someSelected && isSelectedAllOfStatus(getSingleStatus(getCriteria()),
                            streamListPresenter, streamIdSet, StreamStatus.DELETED));
        }

    }

    private void setStreamRelationListSelectableEnabled(final IdSet streamIdSet,
                                                        final StreamStatus streamStatus) {
        final boolean someSelected = isSomeSelected(streamRelationListPresenter, streamIdSet);

        if (streamRelationListDownload != null) {
            streamRelationListDownload.setEnabled(someSelected);
        }
        if (streamRelationListDelete != null) {
            streamRelationListDelete
                    .setEnabled(someSelected && isSelectedAllOfStatus(getSingleStatus(getCriteria()),
                            streamRelationListPresenter, streamIdSet, StreamStatus.LOCKED, StreamStatus.UNLOCKED));
        }
        if (streamRelationListUndelete != null) {
            // Hide if we are normal view (Unlocked streams)
            streamRelationListUndelete.setVisible(!StreamStatus.UNLOCKED.equals(streamStatus));
            streamRelationListUndelete
                    .setEnabled(someSelected && isSelectedAllOfStatus(getSingleStatus(getCriteria()),
                            streamRelationListPresenter, streamIdSet, StreamStatus.DELETED));
        }
        if (streamRelationListProcess != null) {
            streamRelationListProcess.setEnabled(someSelected);
        }
    }

    @Override
    public void beginStepping(final Long streamId, final StreamType childStreamType) {
        if (streamId != null) {
            // Try and get a pipeline id to use as a starting point for
            // stepping.
            SharedDocRef pipelineRef = null;

            // TODO : Fix by making entity id sets docref sets.
//            final EntityIdSet<PipelineEntity> entityIdSet = findStreamAttributeMapCriteria.obtainFindStreamCriteria()
//                    .getPipelineIdSet();
//            if (entityIdSet != null) {
//                if (entityIdSet.getSet().size() > 0) {
//                    pipelineRef = DocRef.create(entityIdSet.getSet().iterator().next());
//                }
//            }

            // We will assume that the stream list has a child stream selected.
            // This would be the case where a user chooses an event with errors
            // in the top screen and then chooses the raw stream in the middle
            // pane to step through.
            Long childStreamId = null;
            final StreamAttributeMap map = streamListPresenter.getSelectedStream();
            if (map != null && map.getStream() != null) {
                final Stream childStream = map.getStream();
                // If the top list has a raw stream selected or isn't a child of
                // the selected stream then this is't the child stream we are
                // looking for.
                if (childStream.getParentStreamId() != null && childStream.getParentStreamId().equals(streamId)) {
                    childStreamId = childStream.getId();
                }
            }

            BeginPipelineSteppingEvent.fire(this, streamId, 0L, childStreamId, childStreamType, pipelineRef);
        }
    }

    public void setClassificationUiHandlers(final ClassificationUiHandlers classificationUiHandlers) {
        dataPresenter.setClassificationUiHandlers(classificationUiHandlers);
    }

    public interface StreamView extends View {
    }

    private static abstract class AbstractStreamClickHandler implements ClickHandler, HasHandlers {
        private final StreamPresenter streamPresenter;
        private final AbstractStreamListPresenter streamListPresenter;
        private final boolean useCriteria;
        private final ClientDispatchAsync dispatcher;

        public AbstractStreamClickHandler(final StreamPresenter streamPresenter,
                                          final AbstractStreamListPresenter streamListPresenter, final boolean useCriteria,
                                          final ClientDispatchAsync dispatcher) {
            this.streamPresenter = streamPresenter;
            this.streamListPresenter = streamListPresenter;
            this.useCriteria = useCriteria;
            this.dispatcher = dispatcher;
        }

        protected AbstractStreamListPresenter getStreamListPresenter() {
            return streamListPresenter;
        }

        protected FindStreamCriteria createCriteria() {
            final IdSet idSet = streamListPresenter.getSelectedEntityIdSet();
            // First make sure there is some sort of selection, either
            // individual streams have been selected or all streams have been
            // selected.
            if (Boolean.TRUE.equals(idSet.getMatchAll()) || idSet.size() > 0) {
                // Only use match all if we are allowed to use criteria.
                if (useCriteria && Boolean.TRUE.equals(idSet.getMatchAll())) {
                    final FindStreamCriteria criteria = createFindStreamCriteria();
                    criteria.copyFrom(streamPresenter.getCriteria());
                    // Paging is NA
                    criteria.obtainPageRequest().setLength(null);
                    criteria.obtainPageRequest().setOffset(null);
                    return criteria;

                } else if (idSet.size() > 0) {
                    // If we aren't matching all then create a criteria that
                    // only includes the selected streams.
                    final FindStreamCriteria criteria = createFindStreamCriteria();
                    // Copy the current filter status
                    criteria.setExpression(ExpressionUtil.createStatusExpression(getSingleStatus(streamPresenter.getCriteria())));
                    criteria.obtainSelectedIdSet().addAll(idSet.getSet());
                    // Paging is NA
                    criteria.obtainPageRequest().setLength(null);
                    criteria.obtainPageRequest().setOffset(null);
                    return criteria;
                }
            }

            return null;
        }

        @Override
        public void onClick(final ClickEvent event) {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                final FindStreamCriteria criteria = createCriteria();
                if (criteria != null) {
                    performAction(criteria, dispatcher);
                }
            }
        }

        protected abstract void performAction(FindStreamCriteria criteria, ClientDispatchAsync dispatcher);

        @Override
        public void fireEvent(final GwtEvent<?> event) {
            streamPresenter.fireEvent(event);
        }

        protected void refreshList() {
            streamListPresenter.refresh();
        }
    }

    private static class DownloadStreamClickHandler extends AbstractStreamClickHandler {
        private final LocationManager locationManager;

        public DownloadStreamClickHandler(final StreamPresenter streamPresenter,
                                          final AbstractStreamListPresenter streamListPresenter, final boolean useCriteria,
                                          final ClientDispatchAsync dispatcher, final LocationManager locationManager) {
            super(streamPresenter, streamListPresenter, useCriteria, dispatcher);
            this.locationManager = locationManager;
        }

        @Override
        protected void performAction(final FindStreamCriteria criteria, final ClientDispatchAsync dispatcher) {
            dispatcher.exec(new DownloadDataAction(criteria)).onSuccess(result -> ExportFileCompleteUtil.onSuccess(locationManager, null, result));
        }
    }

    private static class DeleteStreamClickHandler extends AbstractStreamClickHandler {
        public DeleteStreamClickHandler(final StreamPresenter streamPresenter,
                                        final AbstractStreamListPresenter streamListPresenter, final boolean useCriteria,
                                        final ClientDispatchAsync dispatcher) {
            super(streamPresenter, streamListPresenter, useCriteria, dispatcher);
        }

        protected String getDeleteText(final FindStreamCriteria criteria, final boolean pastTense) {
            if (StreamStatus.DELETED.equals(getSingleStatus(criteria))) {
                return "Restore" + (pastTense ? "d" : "");
            } else {
                return "Delete" + (pastTense ? "d" : "");
            }
        }

        @Override
        protected void performAction(final FindStreamCriteria initialCriteria, final ClientDispatchAsync dispatcher) {
            final FindStreamCriteria deleteCriteria = new FindStreamCriteria();
            deleteCriteria.copyFrom(initialCriteria);

//            if (getSingleStatus(deleteCriteria) == null) {
//                for (final StreamStatus streamStatusToCheck : StreamStatus.values()) {
//                    if (isSelectedAllOfStatus(null, getStreamListPresenter(), deleteCriteria.getSelectedIdSet(),
//                            streamStatusToCheck)) {
//                        deleteCriteria.obtainStatusSet().setSingleItem(streamStatusToCheck);
//                    }
//                }
//            }
            if (getSingleStatus(deleteCriteria) == null) {
                AlertEvent.fireError(this, "Unable to action command on mixed status", null);
            } else {
                ConfirmEvent.fire(this,
                        "Are you sure you want to " + getDeleteText(deleteCriteria, false).toLowerCase() + " the selected items?",
                        confirm -> {
                            if (confirm) {
                                if (!deleteCriteria.getSelectedIdSet().isConstrained()) {
                                    ConfirmEvent.fireWarn(DeleteStreamClickHandler.this,
                                            "You have selected all items.  Are you sure you want to "
                                                    + getDeleteText(deleteCriteria, false).toLowerCase() + " all the selected items?",
                                            confirm1 -> {
                                                if (confirm1) {
                                                    doDelete(deleteCriteria, dispatcher);
                                                }
                                            });

                                } else {
                                    doDelete(deleteCriteria, dispatcher);
                                }
                            }
                        });
            }
        }

        void doDelete(final FindStreamCriteria criteria, final ClientDispatchAsync dispatcher) {
            dispatcher.exec(new EntityServiceFindDeleteAction<FindStreamCriteria, Stream>(criteria)).onSuccess(result -> {
                getStreamListPresenter().getSelectedEntityIdSet().clear();
                getStreamListPresenter().getSelectedEntityIdSet().setMatchAll(false);

                AlertEvent.fireInfo(DeleteStreamClickHandler.this,
                        getDeleteText(criteria, true) + " " + result + " record" + ((result.longValue() > 1) ? "s" : ""), () -> refreshList());
            });
        }
    }

    private static class ProcessStreamClickHandler extends AbstractStreamClickHandler {
        public ProcessStreamClickHandler(final StreamPresenter streamPresenter,
                                         final AbstractStreamListPresenter streamListPresenter, final boolean useCriteria,
                                         final ClientDispatchAsync dispatcher) {
            super(streamPresenter, streamListPresenter, useCriteria, dispatcher);
        }

        @Override
        protected void performAction(final FindStreamCriteria criteria, final ClientDispatchAsync dispatcher) {
            if (criteria != null) {
                ConfirmEvent.fire(this, "Are you sure you want to reprocess the selected items", confirm -> {
                    if (confirm) {
                        dispatcher.exec(new ReprocessDataAction(criteria)).onSuccess(result -> {
                            if (result != null && result.size() > 0) {
                                for (final ReprocessDataInfo info : result) {
                                    switch (info.getSeverity()) {
                                        case INFO:
                                            AlertEvent.fireInfo(ProcessStreamClickHandler.this, info.getMessage(),
                                                    info.getDetails(), null);
                                            break;
                                        case WARNING:
                                            AlertEvent.fireWarn(ProcessStreamClickHandler.this, info.getMessage(),
                                                    info.getDetails(), null);
                                            break;
                                        case ERROR:
                                            AlertEvent.fireError(ProcessStreamClickHandler.this, info.getMessage(),
                                                    info.getDetails(), null);
                                            break;
                                        case FATAL_ERROR:
                                            AlertEvent.fireError(ProcessStreamClickHandler.this, info.getMessage(),
                                                    info.getDetails(), null);
                                            break;
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }
    }
}
