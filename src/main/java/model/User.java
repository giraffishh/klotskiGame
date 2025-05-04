package model;

/**
 * 用户模型类，存储用户名和密码
 */
public class User {

    private String username;
    private String password;

    /**
     * 创建用户对象
     *
     * @param username 用户名
     * @param password 密码
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取密码
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }
}
