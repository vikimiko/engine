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
package io.lumeer.core;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.engine.api.event.UpdateServiceLimits;

import java.util.Optional;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class WorkspaceKeeper implements SelectedWorkspace {

   private String organizationCode;
   private String projectCode;

   @Inject
   private WorkspaceCache workspaceCache;

   @Inject
   private Event<UpdateServiceLimits> updateServiceLimitsEvent;

   @Override
   public Optional<Organization> getOrganization() {
      if (organizationCode == null) {
         return Optional.empty();
      }
      return Optional.of(workspaceCache.getOrganization(organizationCode));
   }

   @Override
   public Optional<Project> getProject() {
      if (projectCode == null) {
         return Optional.empty();
      }
      return Optional.of(workspaceCache.getProject(projectCode));
   }

   public void setOrganization(String organizationCode) {
      this.organizationCode = organizationCode;
   }

   public void setProject(String projectCode) {
      this.projectCode = projectCode;
   }

   public void setWorkspace(String organizationCode, String projectCode) {
      setOrganization(organizationCode);
      setProject(projectCode);
   }

   public void setServiceLimits(final Organization organization, final ServiceLimits serviceLimits) {
      if (organization != null) {
         workspaceCache.setServiceLimits(organization.getCode(), serviceLimits);
         if (updateServiceLimitsEvent != null) {
            updateServiceLimitsEvent.fire(new UpdateServiceLimits(organization, serviceLimits));
         }
      }
   }

   public ServiceLimits getServiceLimits(final Organization organization) {
      if (organization != null) {
         return workspaceCache.getServiceLimits(organization.getCode());
      }

      return null;
   }

   public void clearServiceLimits(final Organization organization) {
      if (organization != null) {
         workspaceCache.removeServiceLimits(organization.getCode());
      }
   }
}
