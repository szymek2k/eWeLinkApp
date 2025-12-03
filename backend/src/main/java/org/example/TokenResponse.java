package org.example;

public class TokenResponse {
    private int error;
    private String msg;
    private OAuthData data;


    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public OAuthData getData() {
        return data;
    }

    public void setData(OAuthData data) {
        this.data = data;
    }
}
