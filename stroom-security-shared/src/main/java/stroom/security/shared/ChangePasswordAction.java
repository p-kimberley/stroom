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

package stroom.security.shared;

import stroom.entity.shared.Action;

public class ChangePasswordAction extends Action<UserAndPermissions> {
    private static final long serialVersionUID = -6740095230475597845L;

    private UserRef userRef;
    private String oldPassword;
    private String newPassword;

    public ChangePasswordAction() {
    }

    public ChangePasswordAction(final UserRef userRef, String oldPassword, String newPassword) {
        this.userRef = userRef;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public UserRef getUserRef() {
        return userRef;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    @Override
    public String getTaskName() {
        return "Change Password";
    }
}
