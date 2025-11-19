package org.opensingular.dbuserprovider.service;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@JBossLog
public class UserGroupSyncService {

    private final KeycloakSession session;

    public UserGroupSyncService(KeycloakSession session) {
        this.session = session;
    }

    public void assignGroupsToFederatedUser(RealmModel realm, UserModel federatedUser) {
        String roles = federatedUser.getFirstAttribute("roles");
        log.debugv("Syncing groups for federated user: username={0}, roles={1}", federatedUser.getUsername(), roles);

        Set<String> targetGroupNames = parseRolesAttribute(roles);
        syncUserGroups(realm, federatedUser, targetGroupNames);
    }

    private Set<String> parseRolesAttribute(String roles) {
        if (roles == null || roles.trim().isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(roleName -> !roleName.isEmpty())
                .collect(Collectors.toSet());
    }

    private void syncUserGroups(RealmModel realm, UserModel user, Set<String> targetGroupNames) {
        Set<String> groupsToAdd = new HashSet<>(targetGroupNames);
        Set<GroupModel> currentGroups = user.getGroupsStream().collect(Collectors.toSet());

        for (GroupModel currentGroup : currentGroups) {
            String currentGroupName = currentGroup.getName();

            if (targetGroupNames.contains(currentGroupName)) {
                groupsToAdd.remove(currentGroupName);
            } else {
                user.leaveGroup(currentGroup);
                log.debugv("Left group: group={0}, username={1}", currentGroupName, user.getUsername());
            }
        }

        for (String groupName : groupsToAdd) {
            GroupModel group = session.groups().getGroupByName(realm, null, groupName);

            if (group == null) {
                log.warnv("Group not found for role: role={0}, username={1}", groupName, user.getUsername());
                continue;
            }

            user.joinGroup(group);
            log.debugv("Joined group: group={0}, username={1}", groupName, user.getUsername());
        }
    }
}