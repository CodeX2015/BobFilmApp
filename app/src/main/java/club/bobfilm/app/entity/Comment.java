package club.bobfilm.app.entity;

import java.io.Serializable;

import club.bobfilm.app.helpers.HTMLParser;

/**
 * Created by CodeX on 01.06.2016.
 */
public class Comment implements Serializable {
    private String mUser;
    private String mUserProfileUrl;
    private String mAvatarUrl;
    private String mCreateDate;
    private String mCommentBodyHTML;
    private String mCommentTitle;
    private String mCommentUrl;
    private String mLeftOffset;
    private boolean isReplica;

    public String getCommentUrl() {
        return mCommentUrl.contains("http") ?
                mCommentUrl : HTMLParser.SITE + mCommentUrl;
    }

    public void setCommentUrl(String mCommentUrl) {
        this.mCommentUrl = mCommentUrl;
    }

    public String getUserProfileUrl() {
        return mUserProfileUrl.contains("http") ?
                mUserProfileUrl : HTMLParser.SITE + mUserProfileUrl;
    }

    public void setUserProfileUrl(String mUserProfileUrl) {
        this.mUserProfileUrl = mUserProfileUrl;
    }

    public boolean isReplica() {
        return isReplica;
    }

    public void setReplica(boolean replica) {
        isReplica = replica;
    }

    public int getLeftOffset() {
        return Integer.parseInt(mLeftOffset);
    }

    public void setLeftOffset(String mLeftPadding) {
        this.mLeftOffset = mLeftPadding;
    }

    public String getUser() {
        return mUser;
    }

    public void setUser(String mUser) {
        this.mUser = mUser;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public void setAvatarUrl(String mAvatarUrl) {
        this.mAvatarUrl = mAvatarUrl;
    }

    public String getCreateDate() {
        return mCreateDate;
    }

    public void setCreateDate(String mCreateDate) {
        this.mCreateDate = mCreateDate;
    }

    public String getCommentBodyHTML() {
        return mCommentBodyHTML;
    }

    public void setCommentBodyHTML(String mCommentBodyHTML) {
        this.mCommentBodyHTML = mCommentBodyHTML;
    }

    public String getCommentTitle() {
        return mCommentTitle;
    }

    public void setCommentTitle(String mCommentTitle) {
        this.mCommentTitle = mCommentTitle;
    }
}
