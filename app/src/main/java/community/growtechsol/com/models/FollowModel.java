package community.growtechsol.com.models;

public class FollowModel {

    private String followedBy;
    private Long followedAt;

    public FollowModel() {
    }

    public String getFollowedBy() {
        return followedBy;
    }

    public void setFollowedBy(String followedBy) {
        this.followedBy = followedBy;
    }

    public Long getFollowedAt() {
        return followedAt;
    }

    public void setFollowedAt(Long followedAt) {
        this.followedAt = followedAt;
    }
}
