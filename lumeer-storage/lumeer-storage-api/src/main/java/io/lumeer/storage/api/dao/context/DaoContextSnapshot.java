/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.storage.api.dao.context;

import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.CompanyContactDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;
import io.lumeer.storage.api.dao.UserNotificationDao;
import io.lumeer.storage.api.dao.ViewDao;

/**
 * Holds contextual information necessary to create any Dao object in the application.
 * This is useful in batch processing tasks and long running tasks that are executed out
 * of any CDI scope. In such cases, the injection does not work and we must create
 * a snapshot while inside of a CDI scope.
 * Please remember that nothing that gets injected into Dao objects will work. This includes
 * mainly invocation of CDI events that lead to creation of Push and User Notifications.
 * If it is desired to send these notifications, they must be created manually in the batch task.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface DaoContextSnapshot {
   OrganizationDao getOrganizationDao();
   ProjectDao getProjectDao();
   CollectionDao getCollectionDao();
   CompanyContactDao getCompanyContactDao();
   DataDao getDataDao();
   DocumentDao getDocumentDao();
   FavoriteItemDao getFavoriteItemDao();
   FeedbackDao getFeedbackDao();
   GroupDao getGroupDao();
   LinkInstanceDao getLinkInstanceDao();
   LinkTypeDao getLinkTypeDao();
   PaymentDao getPaymentDao();
   UserDao getUserDao();
   UserLoginDao getUserLoginDao();
   UserNotificationDao getUserNotificationDao();
   ViewDao getViewDao();
}
