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

package stroom.pipeline;

import com.google.inject.persist.Transactional;
import stroom.entity.DocumentEntityServiceImpl;
import stroom.entity.StroomEntityManager;
import stroom.explorer.ExplorerActionHandler;
import stroom.explorer.shared.DocumentType;
import stroom.importexport.ImportExportActionHandler;
import stroom.importexport.ImportExportHelper;
import stroom.pipeline.shared.FindTextConverterCriteria;
import stroom.pipeline.shared.TextConverter;
import stroom.security.SecurityContext;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Transactional
public class TextConverterServiceImpl extends DocumentEntityServiceImpl<TextConverter, FindTextConverterCriteria>
        implements TextConverterService, ExplorerActionHandler, ImportExportActionHandler {

    @Inject
    TextConverterServiceImpl(final StroomEntityManager entityManager,
                             final ImportExportHelper importExportHelper,
                             final SecurityContext securityContext) {
        super(entityManager, importExportHelper, securityContext);
    }

    @Override
    public Class<TextConverter> getEntityClass() {
        return TextConverter.class;
    }

//    @Override
//    protected List<EntityReferenceQuery> getReferenceTableList() {
//        final boolean mySql = stroomDatabaseInfo.isMysql();
//        final ArrayList<EntityReferenceQuery> rtnList = new ArrayList<>();
//        if (mySql) {
//            rtnList.add(new EntityReferenceQuery(PipelineEntity.ENTITY_TYPE, PipelineEntity.TABLE_NAME,
//                    PipelineEntity.DATA + " regexp '<type>@TYPE@</type>[[:space:]]*<id>@ID@</id>'"));
//        } else {
//            // This won't work too well as we really need to match with a regex
//            // that we can only do in MySQL
//            rtnList.add(new EntityReferenceQuery(PipelineEntity.ENTITY_TYPE, PipelineEntity.TABLE_NAME,
//                    "(locate('<type>@TYPE@</type>', CAST(" + PipelineEntity.DATA
//                            + " AS LONGVARCHAR)) <> 0 AND locate('<id>@ID@</id>', CAST(" + PipelineEntity.DATA
//                            + " AS LONGVARCHAR)) <> 0)"));
//
//        }
//
//        return rtnList;
//    }

    @Override
    public FindTextConverterCriteria createCriteria() {
        return new FindTextConverterCriteria();
    }

    @Override
    public DocumentType getDocumentType() {
        return new DocumentType(4, TextConverter.ENTITY_TYPE, "Text Converter");
    }
}
