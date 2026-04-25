package com.pawura.service;

import com.pawura.database.DatabaseManager;
import com.pawura.model.Administrator;
import com.pawura.model.User;
import com.pawura.util.PasswordUtils;
import com.pawura.util.ValidationUtils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

/**
 * AuthenticationService – login, register, and session management.
 */
public class AuthenticationService {

    private static final Logger LOG = Logger.getLogger(AuthenticationService.class.getName());

    private User currentUser;
    private final Map<String, String> otpStore = new HashMap<>(); // In-memory OTP storage: email -> OTP

    // ── Login / Logout ────────────────────────────────────────────────────────

    public Optional<User> login(String username, String password) {
        ValidationUtils.requireNonBlank(username, "Username");
        ValidationUtils.requireNonBlank(password, "Password");

        String sql = "SELECT * FROM users WHERE username = ? AND active = TRUE AND is_verified = TRUE"; // Check for active and verified users
        try (PreparedStatement ps =
                DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapRow(rs);
                if (PasswordUtils.verify(password, user.getPasswordHash())) {
                    this.currentUser = user;
                    LOG.info("Login OK: " + username);
                    return Optional.of(user);
                } else {
                    LOG.warning("Login failed: Password mismatch for user " + username);
                }
            } else {
                LOG.warning("Login failed: User [" + username + "] not found, inactive, or not verified.");
            }
        } catch (SQLException e) {
            LOG.severe("Login error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void logout() {
        LOG.info("User logged out: " +
                 (currentUser != null ? currentUser.getUsername() : "none"));
        currentUser = null;
    }

    public Optional<User> getCurrentUser()    { return Optional.ofNullable(currentUser); }
    public boolean isLoggedIn()               { return currentUser != null; }

    // ── Registration ──────────────────────────────────────────────────────────
    /**
     * Generates a random 6-digit OTP.
     * @return The generated OTP as a String.
     */
    public String generateRandomOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(otp);
    }

    /**
     * Sends an OTP to the specified email address.
     * @param email The recipient's email address.
     * @param otp The OTP to send.
     * @return true if the email was sent successfully, false otherwise.
     */
    public boolean sendOtpEmail(String email, String otp) {
        // Configure your email server properties
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com"); // e.g., smtp.gmail.com
        props.put("mail.smtp.port", "587"); // e.g., 587 for TLS
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.debug", "true"); // Enable debug logs for SMTP

        // ✅ SECURE - Read from environment variables
        String emailFromEnv = System.getenv("GMAIL_SENDER_EMAIL");
        String passFromEnv = System.getenv("GMAIL_APP_PASSWORD");

        // Fallback for development (not for production!)
        if (emailFromEnv == null || passFromEnv == null) {
            LOG.warning("Environment variables GMAIL_SENDER_EMAIL or GMAIL_APP_PASSWORD are not set. Using fallback credentials.");
            emailFromEnv = "mahelayapa5@gmail.com";
            passFromEnv = "dufw xlqz otyt rexx";
        }

        final String senderEmail = emailFromEnv;
        final String appPassword = passFromEnv;

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Pawura Network - Your One-Time Password (OTP)");
            message.setText("Your OTP for Pawura Network registration is: " + otp +
                            "\n\nThis OTP is valid for a short period. Do not share it with anyone.");

            Transport.send(message);
            otpStore.put(email, otp); // Store OTP for verification
            LOG.info("OTP sent to " + email);
            return true;
        } catch (MessagingException e) {
            LOG.severe("Failed to send OTP email to " + email + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifies the provided OTP against the stored OTP for the given email.
     * @param email The email address for which the OTP was sent.
     * @param otp The OTP entered by the user.
     * @return true if the OTP is valid, false otherwise.
     */
    public boolean verifyOtp(String email, String otp) {
        String storedOtp = otpStore.get(email);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(email); // OTP consumed
            return true;
        }
        return false;
    }

    /**
     * Registers a new user in the database. This method should only be called
     * after successful OTP verification.
     * @param username The user's chosen username.
     * @param password The user's chosen plain-text password.
     * @param email The user's email address.
     * @param fullName The user's full name.
     * @param role The user's role.
     * @return true if registration is successful, false otherwise.
     */
    public boolean register(String username, String password, String email, String fullName, User.Role role) {
        if (!ValidationUtils.isValidUsername(username))
            throw new IllegalArgumentException("Username must be 3-30 alphanumeric characters.");
        if (!ValidationUtils.isValidPassword(password))
            throw new IllegalArgumentException("Password must be at least 6 characters.");

        String hash = PasswordUtils.hash(password);
        String sql = "INSERT INTO users(username, password_hash, email, full_name, role, active, is_verified, otp_code, otp_expiry, created_at) VALUES(?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, hash);
            ps.setString(3, email.trim());
            ps.setString(4, fullName);
            ps.setString(5, role.name());
            ps.setBoolean(6, true); // active
            ps.setBoolean(7, true); // is_verified - set to true after successful OTP verification
            ps.setString(8, null);  // otp_code
            ps.setTimestamp(9, null); // otp_expiry
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Registered new user: " + username);
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOG.warning("Registration failed for " + username + ": " + e.getMessage());
            return false;
        }
    }

    // ── Row Mapper ────────────────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        // Read columns in sequential order (1, 2, 3...) to ensure compatibility 
        // with forward-only JDBC ResultSets.
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String email = rs.getString("email");
        String fullName = rs.getString("full_name");
        String roleStr = rs.getString("role");
        boolean active = rs.getBoolean("active");
        boolean verified = rs.getBoolean("is_verified");
        String otp = rs.getString("otp_code");
        Timestamp expiryTs = rs.getTimestamp("otp_expiry");
        Timestamp createdTs = rs.getTimestamp("created_at");

        User.Role role = User.Role.valueOf(roleStr);
        User user = (role == User.Role.ADMINISTRATOR) ? new Administrator() : new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(active);
        user.setVerified(verified);
        user.setOtpCode(otp);
        if (expiryTs != null) user.setOtpExpiry(expiryTs.toLocalDateTime());
        if (createdTs != null) user.setCreatedAt(createdTs.toLocalDateTime());
        return user;
    }
}
