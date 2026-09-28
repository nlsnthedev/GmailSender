package com.automation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

public class GmailEmploymentSender {

    private static final String APPLICATION_NAME = "Selenium Gmail Automation";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static Credential getCredentials(final com.google.api.client.http.HttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = GmailEmploymentSender.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void sendEmail(String toEmail, String subject, String bodyText) throws Exception {
        final com.google.api.client.http.HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress("me"));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
        email.setSubject(subject);
        email.setText(bodyText);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());

        Message message = new Message();
        message.setRaw(encodedEmail);

        message = service.users().messages().send("me", message).execute();
        System.out.println("Message sent! ID: " + message.getId());
    }

    public static void main(String[] args) throws Exception {

        String subject
                = "Invitation to Discuss Career Opportunities";

        List<String> lines = Files.readAllLines(Paths.get("src/main/resources/new_employees.txt"));

        int total = lines.size();
        int sentCount = 0;
        int failCount = 0;
        int currentIndex = 0;

        for (String line : lines) {

            currentIndex++;
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(",");

            if (parts.length < 2) {
                System.out.println("Skipping malformed line: " + line);
                continue;
            }

            String firstName = parts[0].trim();
            String email = parts[1].trim();

            String bodyText
                    = "Dear " + firstName + ",\n\n"
                    + "Thank you for your interest in career opportunities at WorkDay Inc.\n\n"
                    + "We have reviewed your application and would like to invite you to a virtual interview to discuss your background, experience, and qualifications, as well as the opportunities currently available.\n\n"
                    + "Available roles include:\n\n"
                    + "Project Manager\n"
                    + "Senior Financial Accountant\n"
                    + "Financial Administrator\n"
                    + "Senior Data Engineer\n"
                    + "Bookkeeper\n\n"
                    + "Work arrangement: Remote\n"
                    + "Schedule: Monday through Friday, approximately 30–40 hours per week\n\n"
                    + "During the interview, we can discuss the role, your experience, any questions you may have, and the next steps in the hiring process.\n\n"
                    + "The interview will be held virtually using Microsoft Teams or Slack. Meeting details will be provided after an interview time has been confirmed.\n\n"
                    + "If you are interested in continuing, please reply with a few dates and times that are convenient for you. Our recruitment team will then coordinate the interview.\n\n"
                    + "Thank you for your time and interest in WorkDay Inc. We look forward to speaking with you.\n\n"
                    + "Kind regards,\n\n"
                    + "Recruitment Team\n"
                    + "Human Resources Department\n"
                    + "WorkDay Inc.";

            try {
                sendEmail(email, subject, bodyText);
                sentCount++;
                System.out.println("[" + currentIndex + "/" + total + "] Sent to: " + firstName + " <" + email + ">");
            } catch (Exception e) {
                failCount++;
                System.out.println("[" + currentIndex + "/" + total + "] Failed to send to " + email + ": " + e.getMessage());
            }

            Thread.sleep(5000);
        }

        System.out.println("\n--- Summary ---");
        System.out.println("Total processed: " + total);
        System.out.println("Successfully sent: " + sentCount);
        System.out.println("Failed: " + failCount);
    }
}
