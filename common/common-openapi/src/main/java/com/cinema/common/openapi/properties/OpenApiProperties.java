package com.cinema.common.openapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "cinema.openapi")
public class OpenApiProperties {

    private String title = "Cinema Booking System API";

    private String description =
            "API documentation for Cinema Booking System";

    private String version = "1.0.0";

    private String contactName = "Cinema System";

    private String contactEmail;

    private String serverUrl;

    private String serverDescription = "Application server";

    public String getTitle() {

        return title;

    }

    public void setTitle(
            String title) {

        this.title = title;

    }

    public String getDescription() {

        return description;

    }

    public void setDescription(
            String description) {

        this.description = description;

    }

    public String getVersion() {

        return version;

    }

    public void setVersion(
            String version) {

        this.version = version;

    }

    public String getContactName() {

        return contactName;

    }

    public void setContactName(
            String contactName) {

        this.contactName = contactName;

    }

    public String getContactEmail() {

        return contactEmail;

    }

    public void setContactEmail(
            String contactEmail) {

        this.contactEmail = contactEmail;

    }

    public String getServerUrl() {

        return serverUrl;

    }

    public void setServerUrl(
            String serverUrl) {

        this.serverUrl = serverUrl;

    }

    public String getServerDescription() {

        return serverDescription;

    }

    public void setServerDescription(
            String serverDescription) {

        this.serverDescription = serverDescription;

    }

}
