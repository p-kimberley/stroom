/*
 * Copyright 2026 Crown Copyright
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

package stroom.pipeline.stepping;

import stroom.meta.api.MetaService;
import stroom.meta.shared.FindMetaCriteria;
import stroom.meta.shared.Meta;
import stroom.pipeline.factory.PipelineDataHolderFactory;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.shared.stepping.PipelineStepRequest;
import stroom.pipeline.shared.stepping.StepType;
import stroom.pipeline.stepping.capture.ReprocessDriver;
import stroom.pipeline.stepping.capture.StreamCaptureDriver;
import stroom.pipeline.stepping.fingerprint.ElementFingerprinter;
import stroom.pipeline.stepping.read.SessionStepResolver;
import stroom.pipeline.stepping.read.SteppableElements;
import stroom.pipeline.stepping.session.SteppingSession;
import stroom.pipeline.stepping.session.SteppingSessionRegistry;
import stroom.pipeline.stepping.store.StepDataStoreManager;
import stroom.pipeline.stepping.store.SteppingConfig;
import stroom.security.api.SecurityContext;
import stroom.security.api.UserIdentity;
import stroom.security.shared.AppPermission;
import stroom.task.api.ExecutorProvider;
import stroom.task.api.TaskContextFactory;
import stroom.task.api.TaskManager;
import stroom.util.shared.ResultPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// A stepping session made for a step that fails is ended: its id never reaches the client, so nothing else
/// could end it, and it would hold its capture until it went idle.
class TestSteppingService {

    private final SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    private final UserIdentity user = Mockito.mock(UserIdentity.class);
    private final MetaService metaService = Mockito.mock(MetaService.class);
    private final SessionStepResolver resolver = Mockito.mock(SessionStepResolver.class);
    private final SteppingSessionRegistry registry = Mockito.mock(SteppingSessionRegistry.class);
    private final SteppingSession session = Mockito.mock(SteppingSession.class);
    private SteppingService service;

    @BeforeEach
    void setUp() {
        Mockito.when(securityContext.secureResult(Mockito.any(AppPermission.class), Mockito.<Supplier<Object>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(1).get());
        Mockito.when(securityContext.useAsReadResult(Mockito.<Supplier<Object>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        Mockito.when(securityContext.getUserIdentity()).thenReturn(user);
        Mockito.when(metaService.find(Mockito.any(FindMetaCriteria.class)))
                .thenReturn(new ResultPage<>(List.of(Meta.builder().id(5L).build())));
        Mockito.when(registry.getOrCreate(Mockito.eq(user), Mockito.any(), Mockito.eq(List.of(5L)), Mockito.any()))
                .thenReturn(session);
        Mockito.when(resolver.resolve(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong()))
                .thenThrow(new IllegalStateException("The pipeline could not be built"));
        service = new SteppingService(Mockito.mock(TaskContextFactory.class),
                () -> Mockito.mock(StreamCaptureDriver.class), () -> Mockito.mock(ReprocessDriver.class),
                Mockito.mock(ExecutorProvider.class), metaService, securityContext, Mockito.mock(TaskManager.class),
                Mockito.mock(StepDataStoreManager.class),
                Mockito.mock(PipelineDataHolderFactory.class, Mockito.RETURNS_DEEP_STUBS),
                Mockito.mock(ElementFingerprinter.class), Mockito.mock(SavedElementCode.class), SteppingConfig::new,
                resolver, registry, Mockito.mock(SteppableElements.class));
    }

    @Test
    void testASessionMadeForAStepThatFailsIsEnded() {
        // Seen in review: the agent's tools never learnt such a session's id, so they couldn't end it.
        Mockito.when(session.getSessionId()).thenReturn("made-for-this-step");

        assertThatThrownBy(() -> service.step(request(null))).hasMessage("The pipeline could not be built");

        Mockito.verify(registry).terminate(user, "made-for-this-step");
    }

    @Test
    void testTheClientsOwnSessionIsKeptWhenAStepFails() {
        // A session the client already holds may step again: a failed step doesn't end it.
        Mockito.when(session.getSessionId()).thenReturn("the-clients");

        assertThatThrownBy(() -> service.step(request("the-clients"))).isInstanceOf(IllegalStateException.class);

        Mockito.verify(registry, Mockito.never()).terminate(Mockito.any(), Mockito.any());
    }

    private static PipelineStepRequest request(final String sessionUuid) {
        return PipelineStepRequest.builder()
                .sessionUuid(sessionUuid)
                .criteria(new FindMetaCriteria())
                .stepType(StepType.FIRST)
                .pipelineDoc(PipelineDoc.builder().uuid("pipeline").name("Pipeline").build())
                .stepSize(1)
                .timeout(100L)
                .build();
    }
}
