package com.example.ivana.oglasi.Classes;

/**
 * Created by Ivana on 5/2/2017.
 */

public class CommentListItemData {

    private String commentUser;
    private String commentTime;
    private String commentText;

    public CommentListItemData(String commentUser, String commentTime, String commentText){
        this.setCommentUser(commentUser);
        this.setCommentTime(commentTime);
        this.setCommentText(commentText);
    }

    public String getCommentUser() {
        return commentUser;
    }

    public void setCommentUser(String commentUser) {
        this.commentUser = commentUser;
    }

    public String getCommentTime() {
        return commentTime;
    }

    public void setCommentTime(String commentTime) {
        this.commentTime = commentTime;
    }

    public String getCommentText() { return commentText; }

    public void setCommentText(String commentText) { this.commentText=commentText; }
}
