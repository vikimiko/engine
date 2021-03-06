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
package io.lumeer.remote.rest;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class ViewServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";
   private Query query;
   private static final String PERSPECTIVE = "postit";
   private static final Object CONFIG = "configuration object";

   private static final Set<Role> USER_ROLES = View.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(Role.READ);
   private Permission userPermission;
   private Permission groupPermission;

   private User user;

   private static final String CODE2 = "TVIEW2";

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String VIEWS_PATH = "/" + PATH_CONTEXT + "/rest/" + "organizations/" + ORGANIZATION_CODE + "/projects/" + PROJECT_CODE + "/views";
   private static final String VIEWS_URL = SERVER_URL + VIEWS_PATH;
   private static final String PERMISSIONS_URL = VIEWS_URL + "/" + CODE + "/permissions";
   private static final String VIEWS_COLLECTIONS_URL = VIEWS_URL + "/all/collections";

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      userPermission = Permission.buildWithRoles(this.user.getId(), USER_ROLES);
      groupPermission = Permission.buildWithRoles(GROUP, GROUP_ROLES);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      projectDao.setOrganization(storedOrganization);

      Project project = new Project();
      project.setCode(PROJECT_CODE);
      project.setPermissions(new Permissions());
      Project storedProject = projectDao.createProject(project);

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(this.user.getId(), Project.ROLES);
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      storedProject = projectDao.updateProject(storedProject.getId(), storedProject);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      viewDao.setProject(storedProject);

      Collection collection = collectionFacade.createCollection(
            new Collection("abc", "abc random", ICON, COLOR, projectPermissions));
      collectionFacade.updateUserPermissions(collection.getId(), Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
      query = new Query(new QueryStem(collection.getId()));
   }

   private View prepareView(String code) {
      return new View(code, NAME, ICON, COLOR, null, null, query, PERSPECTIVE, CONFIG, this.user.getId());
   }

   private View createView(String code) {
      View view = prepareView(code);
      view.getPermissions().updateUserPermissions(userPermission);
      view.getPermissions().updateGroupPermissions(groupPermission);
      return viewDao.createView(view);
   }

   @Test
   public void testCreateView() {
      View view = prepareView(CODE);
      Entity entity = Entity.json(view);

      Response response = client.target(VIEWS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      assertThat(returnedView).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(query);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateView() {
      createView(CODE);

      View updatedView = prepareView(CODE2);
      Entity entity = Entity.json(updatedView);

      Response response = client.target(VIEWS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(query);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();

      View storedView = viewDao.getViewByCode(CODE2);
      assertThat(storedView).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedView.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedView.getName()).isEqualTo(NAME);
      assertions.assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedView.getQuery()).isEqualTo(query);
      assertions.assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(storedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   public void testDeleteView() {
      createView(CODE);

      Response response = client.target(VIEWS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(VIEWS_URL).build());

      assertThatThrownBy(() -> viewDao.getViewByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetViewByCode() {
      createView(CODE);

      Response response = client.target(VIEWS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(query);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   public void testGetViewWithAuthorRights() {
      final String USER = "aaaaa4444400000000111112"; // non-existing author
      Permission permission = new Permission(USER, new HashSet<>(Arrays.asList(Role.WRITE.toString())));
      Collection collection = collectionFacade.createCollection(
            new Collection("cdefg", "abcefg random", ICON, COLOR, new Permissions(new HashSet<>(Arrays.asList(permission)), Collections.emptySet())));
      collectionFacade.updateUserPermissions(collection.getId(), Permission.buildWithRoles(USER, Collections.singleton(Role.WRITE)));

      View view = prepareView(CODE + "3");
      view.setQuery(new Query(new QueryStem(collection.getId())));
      view.getPermissions().updateUserPermissions(userPermission);
      view.getPermissions().updateGroupPermissions(groupPermission);
      view.setAuthorId(USER);
      view.getPermissions().updateUserPermissions(permission);
      viewDao.createView(view);

      Response response = client.target(VIEWS_URL).path(CODE + "3")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getAuthorRights()).containsOnly(new HashMap.SimpleEntry<>(collection.getId(), new HashSet<>(Arrays.asList(Role.WRITE))));
      assertions.assertAll();
   }

   @Test
   public void testGetAllViews() {
      createView(CODE);
      createView(CODE2);

      Response response = client.target(VIEWS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<View> views = response.readEntity(new GenericType<List<View>>() {
      });
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);

      Permissions permissions1 = views.get(0).getPermissions();
      assertThat(permissions1.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions1).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions1.getGroupPermissions()).containsOnly(groupPermission);

      Permissions permissions2 = views.get(1).getPermissions();
      assertThat(permissions2.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions2).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions2.getGroupPermissions()).containsOnly(groupPermission);
   }

   @Test
   public void testGetViewPermissions() {
      createView(CODE);

      Response response = client.target(PERMISSIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(Permissions.class);
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      createView(CODE);

      Permission[] userPermission = { Permission.buildWithRoles(this.user.getId(), new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ))) };
      Entity entity = Entity.json(userPermission);

      Response response = client.target(PERMISSIONS_URL).path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission[0]);

      Permissions storedPermissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission[0]);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      createView(CODE);

      Response response = client.target(PERMISSIONS_URL).path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createView(CODE);

      Permission[] groupPermission = { Permission.buildWithRoles(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ))) };
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(PERMISSIONS_URL).path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission[0]);

      Permissions storedPermissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission[0]);
   }

   @Test
   public void testRemoveGroupPermission() {
      createView(CODE);

      Response response = client.target(PERMISSIONS_URL).path("groups").path(GROUP)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   @Test
   public void testGetViewsCollections() {
      final String NON_EXISTING_USER = "aaaaa4444400000000111114"; // non-existing user
      final String VIEW_CODE = "MY_COOL_CODE";
      final String COLLECTION_NAME = "kolekce1";
      final String COLLECTION_ICON = "fa-eye";
      final String COLLECTION_COLOR = "#abcdea";

      // create collection under a different user
      Permissions collectionPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(NON_EXISTING_USER, Collection.ROLES);
      collectionPermissions.updateUserPermissions(userPermission);

      Collection collection = collectionFacade.createCollection(
            new Collection("", COLLECTION_NAME, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions));
      collectionFacade.updateUserPermissions(collection.getId(), Permission.buildWithRoles(this.user.getId(), Collections.emptySet()));

      // create a view under a different user
      View view = createView(VIEW_CODE);
      view.setAuthorId(NON_EXISTING_USER);
      view.setQuery(new Query(new QueryStem(collection.getId())));
      view.getPermissions().clearUserPermissions();
      view.getPermissions().updateUserPermissions(Permission.buildWithRoles(NON_EXISTING_USER, View.ROLES), Permission.buildWithRoles(this.user.getId(), Collections.emptySet()));
      viewDao.updateView(view.getId(), view);

      // share the view and make sure we can see it now
      Permissions viewPermissions = new Permissions();
      viewPermissions.updateUserPermissions(Permission.buildWithRoles(NON_EXISTING_USER, View.ROLES));
      viewPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
      view.setPermissions(viewPermissions);
      viewDao.updateView(view.getId(), view);

      Response response = client.target(VIEWS_COLLECTIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      Set<Collection> collections = response.readEntity(new GenericType<Set<Collection>>() {
      });

      assertThat(collections).hasSize(1).hasOnlyOneElementSatisfying(c ->
            assertThat(c.getId()).isEqualTo(collection.getId()));
   }

}
