package uk.openvk.android.legacy;

public class UserPostInfo {
    int postId;
    int postUserId;
    int postAuthorId;
    String authorTitle;
    public UserPostInfo(int post_id, int post_group_id, int post_author_id, String author) {
        postId = post_id;
        postUserId = post_group_id;
        postAuthorId = post_author_id;
        authorTitle = author;
    }
}
