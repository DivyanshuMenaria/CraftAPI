package com.github.games647.craftapi.resolver;

import com.github.games647.craftapi.RateLimitException;
import com.github.games647.craftapi.UUIDAdapter;
import com.github.games647.craftapi.model.NameHistory;
import com.github.games647.craftapi.model.Profile;
import com.github.games647.craftapi.model.auth.Account;
import com.github.games647.craftapi.model.auth.AuthRequest;
import com.github.games647.craftapi.model.auth.AuthResponse;
import com.github.games647.craftapi.model.auth.VerificationResponse;
import com.github.games647.craftapi.model.skin.TexturesModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class MojangResolver extends AbstractResolver implements AuthResolver {

    //UUID profile
    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";

    //skin
    private static final String CHANGE_SKIN_URL = "https://api.mojang.com/user/profile/%s/skin";
    private static final String SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s" +
            "?unsigned=false";

    //authentication
    private static final String AUTH_URL = "https://authserver.mojang.com/authenticate";
    private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?" +
            "username=%s&serverId=%s&ip=%s";

    @Override
    public VerificationResponse hasJoinedServer(String username, String serverHash, InetAddress hostIp)
            throws IOException {
        String encodedIp = URLEncoder.encode(hostIp.getHostAddress(), StandardCharsets.UTF_8.name());
        String url = String.format(HAS_JOINED_URL, username, serverHash, encodedIp);

        HttpURLConnection conn = getConnection(url);
        return readJson(conn.getInputStream(), VerificationResponse.class);
    }

    @Override
    public Account authenticate(String email, String password) throws IOException {
        HttpURLConnection conn = getConnection(AUTH_URL);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.append(gson.toJson(new AuthRequest(email, password)));
        }

        AuthResponse authResponse = readJson(conn.getInputStream(), AuthResponse.class);
        return new Account(authResponse.getSelectedProfile(), authResponse.getAccessToken());
    }

    @Override
    public void changeSkin(Account account, String toUrl, boolean slimModel) throws IOException {
        String url = String.format(CHANGE_SKIN_URL, UUIDAdapter.toMojangId(account.getProfile().getId()));

        HttpURLConnection conn = getConnection(url);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        conn.addRequestProperty("Authorization", "Bearer " + account.getAccessToken());
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write("model=");
            if (slimModel) {
                writer.write("slim");
            }

            writer.write("&url=" + URLEncoder.encode(toUrl, StandardCharsets.UTF_8.name()));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Response code is not Ok: " + responseCode);
        }
    }

    @Override
    public void changeSkin(Account account, RenderedImage pngImage, boolean slimModel) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void resetSkin(Account account) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ImmutableSet<Profile> findProfiles(String... names) throws IOException, RateLimitException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ImmutableList<NameHistory> findNames(UUID uuid) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Profile> findProfile(String name) throws IOException, RateLimitException {
        HttpURLConnection conn = getConnection(UUID_URL + name);

        int responseCode = conn.getResponseCode();
        if (responseCode == RateLimitException.RATE_LIMIT_RESPONSE_CODE) {
            throw new RateLimitException();
        }

        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            return Optional.empty();
        }

        Profile profile = readJson(conn.getInputStream(), Profile.class);
        return Optional.of(profile);
    }

    @Override
    public Optional<Profile> findProfile(String name, Instant time) throws IOException, RateLimitException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<TexturesModel> downloadSkin(UUID uuid) throws IOException, RateLimitException {
        String url = String.format(SKIN_URL, UUIDAdapter.toMojangId(uuid));
        HttpURLConnection conn = getConnection(url);

        int responseCode = conn.getResponseCode();
        if (responseCode == RateLimitException.RATE_LIMIT_RESPONSE_CODE) {
            throw new RateLimitException();
        }

        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            return Optional.empty();
        }

        TexturesModel texturesModel = readJson(conn.getInputStream(), TexturesModel.class);
        return Optional.of(texturesModel);
    }
}