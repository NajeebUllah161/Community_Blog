package community.growtechsol.com.models;

public class Following {

    private String following;
    private Long followedAt;

    public Following() {
    }

    public String getFollowing() {
        return following;
    }

    public void setFollowing(String following) {
        this.following = following;
    }

    public Long getFollowedAt() {
        return followedAt;
    }

    public void setFollowedAt(Long followedAt) {
        this.followedAt = followedAt;
    }
}
