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
package io.lumeer.core.cache;

import io.lumeer.api.model.User;
import io.lumeer.core.model.SimpleUser;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;
import io.lumeer.storage.api.dao.UserDao;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class UserCache {

   @Inject
   private CacheFactory cacheFactory;

   @Inject
   private UserDao userDao;

   private Cache<User> userCache;

   @PostConstruct
   public void initCache() {
      userCache = cacheFactory.getCache();
   }

   public User getUser(String username) {
      return userCache.computeIfAbsent(username, this::getOrCreateUser);
   }

   private User getOrCreateUser(String username) {
      Optional<User> userOptional = userDao.getUserByUsername(username);
      if (userOptional.isPresent()) {
         return userOptional.get();
      }

      User user = new SimpleUser(username);
      return userDao.createUser(user); // TODO remove this for production
   }

   public void updateUser(String username, User user) {
      userCache.set(username, user);
   }

   public void remoteUser(String username) {
      userCache.remove(username);
   }

   public void clear() {
      userCache.clear();
   }

}