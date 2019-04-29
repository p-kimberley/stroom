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

package stroom.cache.impl;

import stroom.cache.shared.FindCacheInfoCriteria;
import stroom.cluster.task.api.ClusterDispatchAsyncHelper;
import stroom.cluster.task.api.TargetType;
import stroom.security.api.Security;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;

import javax.inject.Inject;

class CacheClearService {
    private final ClusterDispatchAsyncHelper dispatchHelper;
    private final Security security;
    private final SecurityContext securityContext;

    @Inject
    CacheClearService(final ClusterDispatchAsyncHelper dispatchHelper,
                      final Security security,
                      final SecurityContext securityContext) {
        this.dispatchHelper = dispatchHelper;
        this.security = security;
        this.securityContext = securityContext;
    }

    void clear(final String cacheName,
               final String nodeName) {
        security.secure(PermissionNames.MANAGE_CACHE_PERMISSION, () -> {
            final FindCacheInfoCriteria criteria = new FindCacheInfoCriteria();
            criteria.getName().setString(cacheName);

            final CacheClearClusterTask clusterTask = new CacheClearClusterTask(
                    securityContext.getUserToken(),
                    "Clear cache",
                    criteria);

            if (nodeName != null) {
                dispatchHelper.execAsync(clusterTask, nodeName);
            } else {
                dispatchHelper.execAsync(clusterTask, TargetType.ACTIVE);
            }
        });
    }
}
