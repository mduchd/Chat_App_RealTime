package com.teamchat.realtime_chat.security;

import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;

@Component
@Profile("swing-local")
public class SwingLocalAdmin implements ApplicationRunner {
    public static final String AUTHORITY = "ROLE_ADMIN";

    private static final Logger logger = LoggerFactory.getLogger(SwingLocalAdmin.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final String EMAIL_LABEL = "Email: ";
    private static final String PASSWORD_LABEL = "Password: ";

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Path stateDirectory;
    private volatile AdminAccount account;

    public SwingLocalAdmin(UserRepository users, PasswordEncoder passwords,
            @Value("${chat.swing-local.admin.directory:${user.home}/.chatspace}") Path stateDirectory,
            @Value("${chat.swing-local.admin.account-id:0}") long accountId,
            @Value("${chat.swing-local.admin.email:}") String email) {
        this.users = users;
        this.passwords = passwords;
        this.stateDirectory = stateDirectory;
        if (accountId != 0 || !email.isBlank()) {
            if (accountId <= 0 || email.isBlank()) {
                throw new IllegalArgumentException("Set both the local admin account ID and email.");
            }
            account = new AdminAccount(accountId, email);
        }
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (account != null) {
            return;
        }

        Files.createDirectories(stateDirectory);
        Path loginFile = stateDirectory.resolve("swing-local-admin-login.txt");
        Path configFile = stateDirectory.resolve("swing-local-admin.properties");
        AdminLogin login = loadOrCreateLogin(loginFile);
        User user = users.findByEmail(login.email()).orElseGet(() -> {
            User admin = new User();
            admin.setEmail(login.email());
            admin.setDisplayName("Quản trị local");
            admin.setPasswordHash(passwords.encode(login.password()));
            return users.saveAndFlush(admin);
        });
        if (user.getPasswordHash() == null || !passwords.matches(login.password(), user.getPasswordHash())) {
            throw new IllegalStateException("The local admin login file does not match the saved account.");
        }

        writePrivateFile(configFile, "chat.swing-local.admin.account-id=" + user.getId()
                + "\nchat.swing-local.admin.email=" + user.getEmail() + "\n");
        account = new AdminAccount(user.getId(), user.getEmail());
        logger.info("Swing local admin login is available in {}", loginFile);
    }

    private AdminLogin loadOrCreateLogin(Path loginFile) throws IOException {
        // Write the password before creating the account so an interrupted setup can resume.
        if (Files.isRegularFile(loginFile, LinkOption.NOFOLLOW_LINKS)) {
            List<String> lines = Files.readAllLines(loginFile);
            if (lines.size() != 2 || !lines.get(0).startsWith(EMAIL_LABEL)
                    || !lines.get(1).startsWith(PASSWORD_LABEL)) {
                throw new IOException("Invalid local admin login file.");
            }
            String email = lines.get(0).substring(EMAIL_LABEL.length());
            String password = lines.get(1).substring(PASSWORD_LABEL.length());
            if (!email.matches("qt[0-9]*@local\\.test") || password.length() < 12 || password.length() > 72) {
                throw new IOException("Invalid local admin login credentials.");
            }
            return new AdminLogin(email, password);
        }

        String email = "qt@local.test";
        for (int suffix = 2; users.existsByEmail(email); suffix++) {
            email = "qt" + suffix + "@local.test";
        }
        StringBuilder password = new StringBuilder(16);
        for (int index = 0; index < 16; index++) {
            password.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        AdminLogin login = new AdminLogin(email, password.toString());
        writePrivateFile(loginFile, EMAIL_LABEL + login.email() + "\n" + PASSWORD_LABEL + login.password() + "\n");
        return login;
    }

    private static void writePrivateFile(Path destination, String contents) throws IOException {
        Path staged = Files.createTempFile(destination.getParent(), ".swing-admin-", ".tmp");
        try {
            if (Files.getFileAttributeView(staged, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(staged, PosixFilePermissions.fromString("rw-------"));
            } else {
                AclFileAttributeView access = Files.getFileAttributeView(staged, AclFileAttributeView.class);
                if (access == null) {
                    throw new IOException("File permissions are unavailable for local admin setup.");
                }
                AclEntry owner = AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                        .setPrincipal(access.getOwner()).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();
                access.setAcl(List.of(owner));
            }
            Files.writeString(staged, contents);
            Files.move(staged, destination);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    public boolean matches(User user) {
        AdminAccount configured = account;
        return configured != null && Long.valueOf(configured.id()).equals(user.getId())
                && configured.email().equals(user.getEmail());
    }

    public boolean allows(Authentication authentication) {
        AdminAccount configured = account;
        return configured != null && authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority(AUTHORITY))
                && Long.toString(configured.id()).equals(authentication.getName())
                && users.findById(configured.id()).filter(this::matches).isPresent();
    }

    private record AdminAccount(long id, String email) {}
    private record AdminLogin(String email, String password) {}
}
