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

import stroom.explorer.shared.BulkActionResult;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.FindExplorerNodeCriteria;
import stroom.explorer.shared.PermissionInheritance;
import stroom.docref.DocRef;

import java.util.List;
import java.util.Set;

interface ExplorerEventLog {
    void create(String type, String uuid, String name, DocRef folder, PermissionInheritance permissionInheritance, Exception ex);

//    void copy(DocRef document, DocRef folder, PermissionInheritance permissionInheritance, Exception ex);

    void copy(List<DocRef> docRefs, DocRef folder, PermissionInheritance permissionInheritance, BulkActionResult bulkActionResult, Exception ex);

//    void move(DocRef document, DocRef folder, PermissionInheritance permissionInheritance, Exception ex);

    void move(List<DocRef> docRefs, DocRef folder, PermissionInheritance permissionInheritance, BulkActionResult bulkActionResult, Exception ex);

    void rename(DocRef document, String name, Exception ex);

//    void delete(DocRef document, Exception ex);

    void delete(List<DocRef> docRefs, BulkActionResult bulkActionResult, Exception ex);

    void info(DocRef document, Exception ex);

    void find(FindExplorerNodeCriteria criteria, Exception ex);

    void fetchPermissions(List<ExplorerNode> explorerNodeList, Exception ex);

    void fetchDocRefs(Set<DocRef> docRefs, Exception ex);
}
