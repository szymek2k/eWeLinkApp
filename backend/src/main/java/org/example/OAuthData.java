package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
public class OAuthData {

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("atExpiredTime")
    private long atExpiredTime;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("rtExpiredTime")
    private long rtExpiredTime;

    // uzupełniane po pobraniu /v2/user/profile
    private String apikey;
    private String region;

    // getters & setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getAtExpiredTime() {
        return atExpiredTime;
    }

    public void setAtExpiredTime(long atExpiredTime) {
        this.atExpiredTime = atExpiredTime;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getRtExpiredTime() {
        return rtExpiredTime;
    }

    public void setRtExpiredTime(long rtExpiredTime) {
        this.rtExpiredTime = rtExpiredTime;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}