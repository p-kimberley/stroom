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

package stroom.security.shared;



import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DocumentPermissions {
    private static final long serialVersionUID = 5230917643321418827L;

    private String docRefUuid;
    private Map<String, Set<String>> userPermissions;

    public DocumentPermissions() {
        // Default constructor necessary for GWT serialisation.
    }

    public DocumentPermissions(final String docRefUuid,
                               final Map<String, Set<String>> userPermissions) {
        this.docRefUuid = docRefUuid;
        this.userPermissions = userPermissions;
    }

    public String getDocRefUuid() {
        return docRefUuid;
    }

    public void setDocRefUuid(String docRefUuid) {
        this.docRefUuid = docRefUuid;
    }

    public Map<String, Set<String>> getUserPermissions() {
        return userPermissions;
    }

    public Set<String> getPermissionsForUser(final String userUuid) {
        final Set<String> permissions = userPermissions.get(userUuid);
        if (permissions != null) {
            return permissions;
        }
        return Collections.emptySet();
    }
}
