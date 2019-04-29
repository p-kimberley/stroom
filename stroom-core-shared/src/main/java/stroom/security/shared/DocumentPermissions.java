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

import stroom.docref.SharedObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DocumentPermissions implements SharedObject {
    private static final long serialVersionUID = 5230917643321418827L;

    private String docUuid;
    private Map<String, Set<String>> permissions = new HashMap<>();

    public DocumentPermissions() {
        // Default constructor necessary for GWT serialisation.
    }

    public DocumentPermissions(final String docUuid, final Map<String, Set<String>> permissions) {
        this.docUuid = docUuid;
        this.permissions = permissions;
    }

    public String getDocUuid() {
        return docUuid;
    }

    public void setDocUuid(String docUuid) {
        this.docUuid = docUuid;
    }

    public Map<String, Set<String>> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Set<String>> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getPermissionsForUser(final String userUuid) {
        return permissions.getOrDefault(userUuid, Collections.emptySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentPermissions that = (DocumentPermissions) o;
        return Objects.equals(docUuid, that.docUuid) &&
                Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docUuid, permissions);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DocumentPermissions{");
        sb.append("docUuid='").append(docUuid).append('\'');
        sb.append(", permissions='").append(permissions).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private final DocumentPermissions instance;
        private final Map<String, Set<String>> permissions = new HashMap<>();

        public Builder(final DocumentPermissions instance) {
            this.instance = instance;
        }

        public Builder() {
            this(new DocumentPermissions());
        }

        public Builder docUuid(final String value) {
            instance.setDocUuid(value);
            return this;
        }

        public Builder permission(final String userUuid, final String permission) {
            permissions.computeIfAbsent(userUuid, k -> new HashSet<>()).add(permission);
            return this;
        }

        public DocumentPermissions build() {
            instance.setPermissions(permissions);
            return instance;
        }
    }
}
