/*
 * Copyright 2018 Crown Copyright
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

package stroom.activity.impl;

import stroom.activity.api.ActivityService;
import stroom.activity.shared.Activity;
import stroom.activity.shared.FindActivityCriteria;
import stroom.security.api.Security;
import stroom.util.AuditUtil;
import stroom.util.shared.BaseResultList;
import stroom.util.shared.EntityServiceException;

import javax.inject.Inject;

public class ActivityServiceImpl implements ActivityService {
    private final Security security;
    private final ActivityDao dao;

    @Inject
    public ActivityServiceImpl(final Security security,
                               final ActivityDao dao) {
        this.security = security;
        this.dao = dao;
    }

    @Override
    public Activity create() {
        return security.secureResult(() -> {
            final String userId = security.getUserId();

            final Activity activity = new Activity();
            activity.setUserId(userId);

            AuditUtil.stamp(userId, activity);

            return dao.create(activity);
        });
    }

    @Override
    public Activity fetch(final int id) {
        return security.secureResult(() -> {
            final Activity result = dao.fetch(id).orElseThrow(() ->
                    new EntityServiceException("Activity not found with id=" + id));
            if (!result.getUserId().equals(security.getUserId())) {
                throw new EntityServiceException("Attempt to read another persons activity");
            }

            return dao.fetch(id).orElse(null);
        });
    }

    @Override
    public Activity update(final Activity activity) {
        return security.secureResult(() -> {
            if (!security.getUserId().equals(activity.getUserId())) {
                throw new EntityServiceException("Attempt to update another persons activity");
            }

            AuditUtil.stamp(security.getUserId(), activity);
            return dao.update(activity);
        });
    }

    @Override
    public boolean delete(final int id) {
        return security.secureResult(() -> dao.delete(id));
    }

    @Override
    public BaseResultList<Activity> find(final FindActivityCriteria criteria) {
        return security.secureResult(() -> {
            criteria.setUserId(security.getUserId());
            return dao.find(criteria);
        });
    }
}
