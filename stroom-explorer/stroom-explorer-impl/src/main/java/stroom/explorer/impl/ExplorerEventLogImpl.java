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
 *
 */

package stroom.explorer.impl;

import event.logging.BaseObject;
import event.logging.CopyMove;
import event.logging.CopyMoveOutcome;
import event.logging.Criteria.DataSources;
import event.logging.Event;
import event.logging.MultiObject;
import event.logging.Object;
import event.logging.ObjectOutcome;
import event.logging.Search;
import event.logging.util.EventLoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.explorer.shared.BulkActionResult;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.FindExplorerNodeCriteria;
import stroom.explorer.shared.PermissionInheritance;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.docref.DocRef;
import stroom.security.api.Security;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

class ExplorerEventLogImpl implements ExplorerEventLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExplorerEventLogImpl.class);

    private final StroomEventLoggingService eventLoggingService;
    private final Security security;

    @Inject
    ExplorerEventLogImpl(final StroomEventLoggingService eventLoggingService,
                         final Security security) {
        this.eventLoggingService = eventLoggingService;
        this.security = security;
    }

    @Override
    public void create(final String type, final String uuid, final String name, final DocRef folder, final PermissionInheritance permissionInheritance, final Exception e) {
        security.insecure(() -> {
            try {
                final Event event = createAction("Create", "Creating", type, name, permissionInheritance);
                final ObjectOutcome objectOutcome = new ObjectOutcome();
                event.getEventDetail().setCreate(objectOutcome);

                final Object object = new Object();
                object.setType(type);
                object.setId(uuid);
                object.setName(name);

                objectOutcome.getObjects().add(object);
                objectOutcome.setOutcome(EventLoggingUtil.createOutcome(e));
                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error("Unable to create event!", e2);
            }
        });
    }

    @Override
    public void copy(final List<DocRef> docRefs,
                     final DocRef folder,
                     final PermissionInheritance permissionInheritance,
                     final BulkActionResult bulkActionResult,
                     final Exception e) {
        security.insecure(() -> docRefs.forEach(docRef -> {
            try {
                final Event event = createAction("Copy", "Copying", docRef, permissionInheritance);
                final CopyMove copy = new CopyMove();
                event.getEventDetail().setCopy(copy);

                if (docRef != null) {
                    final MultiObject source = new MultiObject();
                    copy.setSource(source);
                    source.getObjects().add(createBaseObject(docRef));
                }

                if (folder != null) {
                    final MultiObject destination = new MultiObject();
                    copy.setDestination(destination);
                    destination.getObjects().add(createBaseObject(folder));
                }

                final String message = getMeesage(docRef, bulkActionResult, e);
                if (message != null) {
                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
                    outcome.setSuccess(Boolean.FALSE);
                    outcome.setDescription(message);
                    copy.setOutcome(outcome);
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error("Unable to copy event!", e2);
            }
        }));
    }

//    @Override
//    public void copy(final DocRef document, final DocRef folder, final PermissionInheritance permissionInheritance, final Exception e) {
//        security.insecure(() -> {
//            try {
//                final Event event = createAction("Copy", "Copying", document, permissionInheritance);
//                final CopyMove copy = new CopyMove();
//                event.getEventDetail().setCopy(copy);
//
//                if (document != null) {
//                    final MultiObject source = new MultiObject();
//                    copy.setSource(source);
//                    source.getObjects().add(createBaseObject(document));
//                }
//
//                if (folder != null) {
//                    final MultiObject destination = new MultiObject();
//                    copy.setDestination(destination);
//                    destination.getObjects().add(createBaseObject(folder));
//                }
//
//                if (e != null && e.getMessage() != null) {
//                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
//                    outcome.setSuccess(Boolean.FALSE);
//                    outcome.setDescription(e.getMessage());
//                    copy.setOutcome(outcome);
//                }
//
//                eventLoggingService.log(event);
//            } catch (final RuntimeException e2) {
//                LOGGER.error("Unable to copy event!", e2);
//            }
//        });
//    }


    @Override
    public void move(final List<DocRef> docRefs, final DocRef folder, final PermissionInheritance permissionInheritance, final BulkActionResult bulkActionResult, final Exception e) {
        security.insecure(() -> docRefs.forEach(docRef -> {
            try {
                final Event event = createAction("Move", "Moving", docRef, permissionInheritance);
                final CopyMove move = new CopyMove();
                event.getEventDetail().setMove(move);

                if (docRef != null) {
                    final MultiObject source = new MultiObject();
                    move.setSource(source);
                    source.getObjects().add(createBaseObject(docRef));
                }

                if (folder != null) {
                    final MultiObject destination = new MultiObject();
                    move.setDestination(destination);
                    destination.getObjects().add(createBaseObject(folder));
                }

                final String message = getMeesage(docRef, bulkActionResult, e);
                if (message != null) {
                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
                    outcome.setSuccess(Boolean.FALSE);
                    outcome.setDescription(message);
                    move.setOutcome(outcome);
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error("Unable to move event!", e2);
            }
        }));
    }

//    @Override
//    public void move(final DocRef document, final DocRef folder, final PermissionInheritance permissionInheritance, final Exception e) {
//        security.insecure(() -> {
//            try {
//                final Event event = createAction("Move", "Moving", document, permissionInheritance);
//                final CopyMove move = new CopyMove();
//                event.getEventDetail().setMove(move);
//
//                if (document != null) {
//                    final MultiObject source = new MultiObject();
//                    move.setSource(source);
//                    source.getObjects().add(createBaseObject(document));
//                }
//
//                if (folder != null) {
//                    final MultiObject destination = new MultiObject();
//                    move.setDestination(destination);
//                    destination.getObjects().add(createBaseObject(folder));
//                }
//
//                if (e != null && e.getMessage() != null) {
//                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
//                    outcome.setSuccess(Boolean.FALSE);
//                    outcome.setDescription(e.getMessage());
//                    move.setOutcome(outcome);
//                }
//
//                eventLoggingService.log(event);
//            } catch (final RuntimeException e2) {
//                LOGGER.error("Unable to move event!", e2);
//            }
//        });
//    }

    private String getMeesage(final DocRef docRef, final BulkActionResult bulkActionResult, final Exception e) {
        String message = null;
        if (bulkActionResult != null && bulkActionResult.getMessages() != null) {
            message = bulkActionResult.getMessages().get(docRef);
        }
        if (message == null && e != null && e.getMessage() != null) {
            message = e.getMessage();
        }
        return message;
    }

    @Override
    public void rename(final DocRef document, final String name, final Exception e) {
        security.insecure(() -> {
            try {
                final Event event = createAction("Rename", "Renaming", document, null);
                final CopyMove move = new CopyMove();
                event.getEventDetail().setMove(move);

                if (document != null) {
                    final MultiObject source = new MultiObject();
                    move.setSource(source);
                    source.getObjects().add(createBaseObject(document));
                }

                if (name != null) {
                    final DocRef newDoc = new DocRef(document.getType(), document.getUuid(), name);
                    final MultiObject destination = new MultiObject();
                    move.setDestination(destination);
                    destination.getObjects().add(createBaseObject(newDoc));
                }

                if (e != null && e.getMessage() != null) {
                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
                    outcome.setSuccess(Boolean.FALSE);
                    outcome.setDescription(e.getMessage());
                    move.setOutcome(outcome);
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error("Unable to move event!", e2);
            }
        });
    }

    @Override
    public void delete(final List<DocRef> docRefs, final BulkActionResult bulkActionResult, final Exception e) {
        security.insecure(() -> docRefs.forEach(docRef -> {
            try {
                final Event event = createAction("Delete", "Deleting", docRef, null);
                final ObjectOutcome objectOutcome = new ObjectOutcome();
                event.getEventDetail().setDelete(objectOutcome);
                objectOutcome.getObjects().add(createBaseObject(docRef));

                final String message = getMeesage(docRef, bulkActionResult, e);
                if (message != null) {
                    objectOutcome.setOutcome(EventLoggingUtil.createOutcome(Boolean.FALSE, message));
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error("Unable to delete event!", e2);
            }
        }));
    }

//    @Override
//    public void delete(final DocRef document, final Exception e) {
//        security.insecure(() -> {
//            try {
//                final Event event = createAction("Delete", "Deleting", document, null);
//                final ObjectOutcome objectOutcome = new ObjectOutcome();
//                event.getEventDetail().setDelete(objectOutcome);
//                objectOutcome.getObjects().add(createBaseObject(document));
//                objectOutcome.setOutcome(EventLoggingUtil.createOutcome(e));
//                eventLoggingService.log(event);
//            } catch (final RuntimeException e2) {
//                LOGGER.error("Unable to delete event!", e2);
//            }
//        });
//    }


    @Override
    public void info(final DocRef document, final Exception ex) {
        security.insecure(() -> {
            try {
                final Event event = createAction("Info", "Get document info", document, null);
                final ObjectOutcome objectOutcome = new ObjectOutcome();
                event.getEventDetail().setView(objectOutcome);

                if (document != null) {
                    final BaseObject baseObject = createBaseObject(document);
                    objectOutcome.getObjects().add(baseObject);
                    objectOutcome.setOutcome(EventLoggingUtil.createOutcome(ex));
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error("Unable to get object info event!", e2);
            }
        });
    }

    @Override
    public void find(final FindExplorerNodeCriteria criteria, final Exception e) {
        security.insecure(() -> {
            try {
                final DataSources dataSources = new DataSources();
                dataSources.getDataSource().add("Explorer");

                final Search search = new Search();
                search.setDataSources(dataSources);
                // TODO : @66 expand query once new UI is complete
//                search.setQuery(getQuery(expression));
                search.setOutcome(EventLoggingUtil.createOutcome(e));

                final Event event = eventLoggingService.createAction("Find Explorer Nodes", "Finding explorer nodes");
                event.getEventDetail().setSearch(search);
//                event.getEventDetail().setPurpose(getPurpose(event.getEventDetail().getPurpose(), queryInfo));

                eventLoggingService.log(event);
            } catch (final RuntimeException e2) {
                LOGGER.error(e.getMessage(), e2);
            }
        });
    }

    @Override
    public void fetchDocRefs(final Set<DocRef> docRefs, final Exception ex) {
        // TODO : @66 implement
    }

    @Override
    public void fetchPermissions(final List<ExplorerNode> explorerNodeList, final Exception ex) {
        // TODO : @66 implement
    }

    private Event createAction(final String typeId,
                               final String description,
                               final DocRef docRef,
                               final PermissionInheritance permissionInheritance) {
        String desc = description;
        if (docRef != null) {
            desc = description + " " + docRef.getType() + " \"" + docRef.getName() + "\" uuid="
                    + docRef.getUuid();
        }
        desc += getPermissionString(permissionInheritance);
        return eventLoggingService.createAction(typeId, desc);
    }

    private Event createAction(final String typeId,
                               final String description,
                               final String objectType,
                               final String objectName,
                               final PermissionInheritance permissionInheritance) {
        String desc = description + " " + objectType + " \"" + objectName + "\"";
        desc += getPermissionString(permissionInheritance);
        return eventLoggingService.createAction(typeId, desc);
    }

    private String getPermissionString(final PermissionInheritance permissionInheritance) {
        if (permissionInheritance != null) {
            switch (permissionInheritance) {
                case NONE:
                    return " with no permissions";
                case SOURCE:
                    return " with source permissions";
                case DESTINATION:
                    return " with destination permissions";
                case COMBINED:
                    return " with combined permissions";
            }
        }
        return "";
    }

    private BaseObject createBaseObject(final DocRef docRef) {
        final Object object = new Object();
        object.setType(docRef.getType());
        object.setId(docRef.getUuid());
        object.setName(docRef.getName());
        return object;
    }
}
