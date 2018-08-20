package controllers.domainmodel;

public class User {

    private String username;
    private String password;
    private String email;
    private String authority;
    private String createdOn;

    public User(String username, String password, String email, String authority, String createdOn) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.authority = authority;
        this.createdOn = createdOn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public String toString() {
        return String.format(
                "User[username='%s']",
                username);
    }
}
