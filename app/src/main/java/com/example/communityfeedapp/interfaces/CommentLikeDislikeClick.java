package com.example.communityfeedapp.interfaces;

import com.example.communityfeedapp.models.Comment;

public interface CommentLikeDislikeClick {

    void handleCommentLike(Comment comment, String postId);

    void handleCommentDislike(Comment comment);
}
