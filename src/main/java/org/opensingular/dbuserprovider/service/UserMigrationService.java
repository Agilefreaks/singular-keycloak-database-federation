package org.opensingular.dbuserprovider.service;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * Service responsible for migrating federated users to local Keycloak storage.
 * This happens on first successful login from the external database.
 */
@JBossLog
public class UserMigrationService {

    private final KeycloakSession session;

    public UserMigrationService(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Migrates a federated user to local Keycloak storage if they don't already exist locally.
     *
     * @param realm The realm where the user should be created
     * @param federatedUser The federated user model from the external database
     * @param credential The credential input containing the password
     * @return true if migration occurred, false if user already exists locally
     */
    public boolean migrateUserIfNeeded(RealmModel realm, UserModel federatedUser, CredentialInput credential) {

        if (!isFederatedUser(federatedUser)) {
            log.debugv("User is not federated, skipping migration: userId={0}", federatedUser.getId());
            return false;
        }

//        UserModel localUser = session.users().getUserByUsername(realm, federatedUser.getUsername());
//
//        if (localUser != null) {
//            log.debugv("User already exists locally, skipping migration: username={0}", federatedUser.getUsername());
//            return false;
//        }

        log.infov("Migrating federated user to local storage: username={0}", federatedUser.getUsername());

        UserModel newUser = createLocalUser(realm, federatedUser, credential);

        migrateRoles(realm, federatedUser, newUser);

        log.infov("Successfully migrated user: username={0}, id={1}", newUser.getUsername(), newUser.getId());

        return true;
    }

    /**
     * Checks if a user is a federated user (ID starts with "f:")
     */
    private boolean isFederatedUser(UserModel user) {
        return user.getId() != null && user.getId().startsWith("f:");
    }

    /**
     * Creates a local Keycloak user from a federated user
     */
    private UserModel createLocalUser(RealmModel realm, UserModel federatedUser, CredentialInput credential) {
        UserModel newUser = session.users().addUser(realm, null, federatedUser.getUsername(), true, false);

        // Set basic attributes
        newUser.setEmail(federatedUser.getEmail());
        newUser.setFirstName(federatedUser.getFirstName());
        newUser.setLastName(federatedUser.getLastName());
        newUser.setEnabled(true);

        // Set password
        newUser.credentialManager().updateCredential(credential);

        log.infov("Created local user: username={0}, email={1}", newUser.getUsername(), newUser.getEmail());

        return newUser;
    }

    /**
     * Migrates roles from federated user to local user based on roles_identifiers attribute
     */
    private void migrateRoles(RealmModel realm, UserModel federatedUser, UserModel localUser) {
        String roles = federatedUser.getFirstAttribute("roles");

        if (roles == null || roles.trim().isEmpty()) {
            log.debugv("No roles to migrate for user: username={0}", federatedUser.getUsername());
            return;
        }

        String[] roleNames = roles.split(",");

        for (String roleName : roleNames) {
            String trimmedRoleName = roleName.trim();

            if (trimmedRoleName.isEmpty()) {
                continue;
            }

            localUser.joinGroup(session.groups().getGroupByName(realm, null, roleName));
            log.debugv("Granted group: role={0}, username={1}", trimmedRoleName, localUser.getUsername());
        }
    }
}