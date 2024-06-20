package org.example;

import static java.util.Objects.isNull;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;

public class Main {
    /*
        AJ
        10:35 AM
        Create a crypto notification service as an HTTP Rest API server - Done


        Create a Notification (Input parameters: Current Price of Bitcoin, Daily Percentage Change, Trading Volume, etc)

        Send a notification to email/emails
            => change the status and returns it

        List notifications (Sent, Pending, Failed) -> { - Done
            timestamp range can be parameter
        }


        Update/Delete notification
        {
            Status:
            Sent/Failed: Update/Delete denied
            Pending: update the fields and store
        }
        AJ
        10:38 AM
        ajsrinivas@bitgo.com


     */

    public static void main(String[] args) {
        NotificationService notificationService = new NotificationService();

        Notification notification1 = new Notification();
        notification1.setPrice(71000d);
        notification1.setTradingVolume(5000000d);
        notification1.setDailyPercentageChange(1.5d);

        int notificationId = notificationService.createNotification(notification1);
        System.out.println("Notification Created with Id: " + notificationId);

        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofHours(1));
        Instant to = now.plus(Duration.ofHours(1));
        List<Notification> notificationList = notificationService.listNotification(from, to);
        System.out.println(notificationList);
        System.out.println();

        notificationService.sendNotification(notificationId);
        notificationList = notificationService.listNotification(from, to);
        System.out.println(notificationList);
        notificationService.sendNotification(notificationId);
        System.out.println();

        Notification notification2 = new Notification();
        notification2.setPrice(72000d);
        notification2.setTradingVolume(50000000d);
        notification2.setDailyPercentageChange(1.7d);

        int notificationId2 = notificationService.createNotification(notification2);
        System.out.println("Notification Created with Id: " + notificationId2);
        System.out.println();

        System.out.println(notificationService.listNotification(from, to));
        notificationService.cancelNotification(notificationId2);
        notificationService.cancelNotification(notificationId);
        System.out.println(notificationService.listNotification(from, to));
        System.out.println();
    }
}

enum Status {
    Sent, Pending, Failed;
    private static final Set<Status> TERMINAL_STATUSES = Set.of(Sent, Failed);
    public static Set<Status> getTerminalStatuses() {
        return TERMINAL_STATUSES;
    }
}

@Data
class Notification {
    private int id;
    private double price;
    private double dailyPercentageChange;
    private double tradingVolume;
    private Status status;
    private Instant creationTimestamp;
}

class NotificationService {
    private final NotificationStore notificationStore = new NotificationStore();
    private final EmailService emailService = new EmailService();

    public int createNotification(Notification requestBody) { // returns the unique id
        return notificationStore.createNotification(requestBody);
    }

    public Status sendNotification(int notificationId) {
        Notification notification = notificationStore.getNotifications(notificationId);

        if (Status.getTerminalStatuses().contains(notification.getStatus())) { // already tried sending
            System.out.printf("Notification already reached terminal state: %s\n", notification.getStatus());
            return notification.getStatus();
        }

        List<String> targetReceipientList = List.of("abcd@gmail.com", "xyz@gmail.com");
        Status status = emailService.sendNotification(notification, targetReceipientList);
        System.out.printf("sendNotification(id: %s) to %s; Status = %s\n",
                            notification.getId(), targetReceipientList, status);
        notification.setStatus(status);
        notificationStore.save(notification);
        return status;
    }

    // return the notification created between from and to
    public List<Notification> listNotification(Instant from, Instant to) {
        return notificationStore.getNotifications(from, to);
    }

    // returns whether the cancellation is successful/not
    public boolean cancelNotification(int id) {
        Notification notification = notificationStore.getNotifications(id);
        if (isNull(notification)) {
            System.out.printf("Cancelling the notification(id=%s): Failed\n", id);
            return false;
        }
        if (Status.getTerminalStatuses().contains(notification.getStatus())) {
            System.out.printf("Notification(id=%s) reached terminal status; Cancellation is failed\n", id);
            return false;
        }
        notification.setStatus(Status.Failed);
        notificationStore.save(notification);
        System.out.printf("Cancelling the notification(id=%s): Success\n", id);
        return true;
    }
}

class NotificationStore {
    // CRUD
    private final Map<Integer, Notification> notificationMap = new HashMap<>();
    private final AtomicInteger globalId = new AtomicInteger(0); // Mysql AutoIncrement alias

    public int createNotification(Notification notification) {
        notification.setId(globalId.getAndIncrement());
        notification.setStatus(Status.Pending);
        notification.setCreationTimestamp(Instant.now());
        notificationMap.put(notification.getId(), notification);
        return notification.getId();
    }

    public List<Notification> getNotifications(Instant from, Instant to) {
        return notificationMap.values().stream()
            .filter(notification -> {
                Instant creationTimestamp = notification.getCreationTimestamp();
                return from.isBefore(creationTimestamp) && creationTimestamp.isBefore(to);
            }).collect(Collectors.toList());
    }

    public Notification getNotifications(int notificationId) {
        return notificationMap.get(notificationId);
    }

    public void save(Notification notification) {
        notificationMap.put(notification.getId(), notification);
    }
}


class EmailService {
    private final EmailClient emailClient = new EmailClient();
    public Status sendNotification(Notification notification, List<String> emailsList) {
        String body = notification.toString();
        boolean isSent = emailClient.sendEmail(body, emailsList);
        return isSent ? Status.Sent: Status.Failed;
    }
}

class EmailClient {
    public boolean sendEmail(String body, List<String> receipientList) {
        return true;
    }
}











