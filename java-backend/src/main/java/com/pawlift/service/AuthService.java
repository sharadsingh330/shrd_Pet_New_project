package com.pawlift.service;

import com.pawlift.dao.ShelterDAO;
import com.pawlift.dao.UserDAO;
import com.pawlift.dao.impl.ShelterDAOImpl;
import com.pawlift.dao.impl.UserDAOImpl;
import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.ConflictException;
import com.pawlift.exception.DuplicateKeyException;
import com.pawlift.exception.UnauthorizedException;
import com.pawlift.model.Shelter;
import com.pawlift.model.User;
import com.pawlift.model.UserRole;
import com.pawlift.util.Validator;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Registration and login. The servlet calls login()/register() and then
 * stores the returned user with SessionUtil.signIn(request, user).
 */
public class AuthService {

    /** Used when the email is unknown, so login takes the same time either way. */
    private static final String DUMMY_HASH = BCrypt.hashpw("not-a-real-password", BCrypt.gensalt(10));

    private final UserDAO users;
    private final ShelterDAO shelters;

    public AuthService() {
        this(new UserDAOImpl(), new ShelterDAOImpl());
    }

    public AuthService(UserDAO users, ShelterDAO shelters) {
        this.users = users;
        this.shelters = shelters;
    }

    /**
     * Creates a new account. Only "adopter" and "shelter" can be chosen at signup;
     * admins are promoted by an existing admin, never self-registered.
     * Shelter accounts also get their shelter row (shelterName is required for them).
     */
    public User register(String fullName, String email, String password, String role,
                         String shelterName, String city) {
        String name = Validator.required(fullName, "full_name", 120);
        String cleanEmail = Validator.email(email);
        String cleanPassword = Validator.password(password);
        String cleanCity = Validator.optional(city, "city", 120);

        UserRole chosen = (role == null || role.isBlank()) ? UserRole.ADOPTER : UserRole.from(role);
        if (chosen == UserRole.ADMIN) throw new BadRequestException("You cannot register as an admin");

        String cleanShelterName = null;
        if (chosen == UserRole.SHELTER) cleanShelterName = Validator.required(shelterName, "shelter_name", 160);

        if (users.findByEmail(cleanEmail).isPresent())
            throw new ConflictException("An account with this email already exists");

        // Build the right subtype (Adopter / ShelterUser) for the chosen role.
        User user = User.forRole(chosen);
        user.fullName = name;
        user.email = cleanEmail;
        user.passwordHash = BCrypt.hashpw(cleanPassword, BCrypt.gensalt(12));
        user.city = cleanCity.isEmpty() ? null : cleanCity;

        User created;
        try {
            created = users.create(user);
        } catch (DuplicateKeyException e) {
            // Another request registered the same email a moment ago.
            throw new ConflictException("An account with this email already exists");
        }

        if (chosen == UserRole.SHELTER) {
            Shelter s = new Shelter();
            s.ownerId = created.id;
            s.name = cleanShelterName;
            s.city = cleanCity;
            s.email = cleanEmail;
            s.verified = false;
            try {
                shelters.create(s);
            } catch (RuntimeException e) {
                // The two DAOs use separate connections, so undo the user by hand
                // to avoid leaving a shelter account without a shelter.
                users.delete(created.id);
                throw e;
            }
        }
        return created;
    }

    /** Checks email + password. Returns the user (correct subtype) or throws 401. */
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty())
            throw new BadRequestException("Email and password are required");

        User user = users.findByEmail(email.trim()).orElse(null);
        String hash = user == null ? DUMMY_HASH : user.passwordHash;
        boolean ok;
        try {
            ok = BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            ok = false; // stored hash is malformed
        }
        if (user == null || !ok) throw new UnauthorizedException("Invalid email or password");
        return user;
    }

    /** Reloads the signed-in user (used by the future auth filter / "me" endpoint). */
    public User findUser(String userId) {
        if (userId == null) return null;
        return users.findById(userId).orElse(null);
    }
}
