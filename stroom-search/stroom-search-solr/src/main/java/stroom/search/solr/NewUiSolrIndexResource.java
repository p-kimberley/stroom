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

package stroom.search.solr;

import stroom.docref.DocRef;
import stroom.importexport.api.DocumentData;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.importexport.shared.Base64EncodedDocumentData;
import stroom.importexport.shared.ImportState;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(tags = "Solr Indices (New UI)")
@Path(NewUiSolrIndexResource.BASE_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NewUiSolrIndexResource implements RestResource {

    public static final String BASE_RESOURCE_PATH = "/solr/index" + ResourcePaths.V1;

    private final SolrIndexStore solrIndexStore;

    @Inject
    public NewUiSolrIndexResource(final SolrIndexStore solrIndexStore) {
        this.solrIndexStore = solrIndexStore;
    }

    @GET
    @Path("/list")
    @Timed
    @ApiOperation("Submit a request for a list of doc refs held by this service")
    public Set<DocRef> listDocuments() {
        return solrIndexStore.listDocuments();
    }

    @POST
    @Path("/import")
    @Timed
    @ApiOperation("Submit an import request")
    public DocRef importDocument(@ApiParam("DocumentData") final Base64EncodedDocumentData base64EncodedDocumentData) {
        final DocumentData documentData = DocumentData.fromBase64EncodedDocumentData(base64EncodedDocumentData);
        final ImportState importState = new ImportState(documentData.getDocRef(), documentData.getDocRef().getName());
        final ImportExportActionHandler.ImpexDetails result = solrIndexStore.importDocument(documentData.getDocRef(),
                documentData.getDataMap(),
                importState,
                ImportMode.IGNORE_CONFIRMATION);
        if (result != null) {
            return result.getDocRef();
        } else {
            return null;
        }
    }

    @POST
    @Path("/export")
    @Timed
    @ApiOperation("Submit an export request")
    public Base64EncodedDocumentData exportDocument(@ApiParam("DocRef") final DocRef docRef) {
        final Map<String, byte[]> map = solrIndexStore.exportDocument(docRef, true, new ArrayList<>());
        return DocumentData.toBase64EncodedDocumentData(new DocumentData(docRef, map));
    }
}
