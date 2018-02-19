/*
 * Copyright 2016 Crown Copyright
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

package stroom.ruleset.server;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import stroom.dictionary.server.DictionaryStore;
import stroom.feed.MetaMap;
import stroom.feed.StroomStatusCode;
import stroom.feed.StroomStreamException;
import stroom.pipeline.server.errorhandler.ProcessException;
import stroom.pipeline.server.factory.ConfigurableElement;
import stroom.pipeline.server.factory.PipelineProperty;
import stroom.pipeline.server.factory.PipelinePropertyDocRef;
import stroom.pipeline.server.factory.Processor;
import stroom.pipeline.server.reader.AbstractInputElement;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.pipeline.state.MetaDataHolder;
import stroom.query.api.v2.DocRef;
import stroom.ruleset.shared.DataReceiptAction;
import stroom.ruleset.shared.RuleSet;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@Scope("prototype")
@ConfigurableElement(type = "MetaDataFilterInput", category = Category.READER, roles = {
        PipelineElementType.ROLE_HAS_TARGETS, PipelineElementType.ROLE_READER}, icon = ElementIcons.STREAM)
public class MetaDataFilterInputElement extends AbstractInputElement {
    private final RuleSetService ruleSetService;
    private final DictionaryStore dictionaryStore;
    private final MetaDataHolder metaDataHolder;

    private DocRef ruleSet;
    private boolean drop;

    @Inject
    public MetaDataFilterInputElement(final RuleSetService ruleSetService,
                                      final DictionaryStore dictionaryStore,
                                      final MetaDataHolder metaDataHolder) {
        this.ruleSetService = ruleSetService;
        this.dictionaryStore = dictionaryStore;
        this.metaDataHolder = metaDataHolder;
    }

    @Override
    public void startProcessing() {
        try {
            if (ruleSet != null) {
                final DataReceiptPolicyChecker dataReceiptPolicyChecker = new DataReceiptPolicyChecker(ruleSetService, dictionaryStore, ruleSet.getUuid());

                final MetaMap metaMap = metaDataHolder.getMetaData();
                // We need to examine the meta map and ensure we aren't dropping or rejecting this data.
                final DataReceiptAction dataReceiptAction = dataReceiptPolicyChecker.check(metaMap);

                if (DataReceiptAction.REJECT.equals(dataReceiptAction)) {
                    throw new StroomStreamException(StroomStatusCode.RECEIPT_POLICY_SET_TO_REJECT_DATA);

                }

                drop = DataReceiptAction.DROP.equals(dataReceiptAction);
            }
        } catch (final IOException e) {
            throw new ProcessException(e.getMessage(), e);
        }

        super.startProcessing();
    }

    @Override
    public List<Processor> createProcessors() {
        // If we are going to drop the data then return a processor that will do nothing with the provided data.
        if (drop) {
            final Processor doNothingProcessor = () -> {
            };
            return Collections.singletonList(doNothingProcessor);
        }

        return super.createProcessors();
    }

    @PipelineProperty(description = "The rule set to use.")
    @PipelinePropertyDocRef(types = RuleSet.DOCUMENT_TYPE)
    public void setRuleSet(final DocRef ruleSet) {
        this.ruleSet = ruleSet;
    }
}
