package community.growtechsol.com.models;

public class Post {

    private String postId;
    private String postImage;
    private String postRecording;
    private String recTime;
    private String postedBy;
    private String postTitle;
    private String postDescription;
    private String createdAt;
    private long postedAt;
    private boolean solved;
    private boolean isAllowed;
    private int postLikes;
    private int postShares;
    private int commentCount;

    public Post() {
    }

    public Post(String postId, String postImage, String postedBy, String postDescription, long postedAt) {
        this.postId = postId;
        this.postImage = postImage;
        this.postedBy = postedBy;
        this.postDescription = postDescription;
        this.postedAt = postedAt;
    }

    public boolean isAllowed() {
        return isAllowed;
    }

    public void setAllowed(boolean allowed) {
        isAllowed = allowed;
    }

    public int getPostShares() {
        return postShares;
    }

    public void setPostShares(int postShares) {
        this.postShares = postShares;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public String getPostRecording() {
        return postRecording;
    }

    public void setPostRecording(String postRecording) {
        this.postRecording = postRecording;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public long getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(long postedAt) {
        this.postedAt = postedAt;
    }

    public int getPostLikes() {
        return postLikes;
    }

    public void setPostLikes(int postLikes) {
        this.postLikes = postLikes;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostImage() {
        return postImage;
    }

    public void setPostImage(String postImage) {
        this.postImage = postImage;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public String getPostDescription() {
        return postDescription;
    }

    public void setPostDescription(String postDescription) {
        this.postDescription = postDescription;
    }

    public String getRecTime() {
        return recTime;
    }

    public void setRecTime(String recTime) {
        this.recTime = recTime;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

}
