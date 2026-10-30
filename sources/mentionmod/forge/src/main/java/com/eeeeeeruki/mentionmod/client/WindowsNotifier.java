package com.eeeeeeruki.mentionmod.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Windowsのトースト通知を出す。
 * 追加のライブラリを使わず、Windows標準のPowerShellからWinRTの通知APIを呼ぶ。
 * Windows以外では何もしない。
 */
public final class WindowsNotifier {
    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final String APP_ID = "EeeeeeruKi.MentionMod";
    private static final String SOURCE_NAME = "Minecraft";
    private static final long MIN_INTERVAL_MS = 1500;
    private static final int MAX_BODY = 200;
    private static long lastShown = 0;

    private WindowsNotifier() {}

    public static synchronized void show(String title, String body) {
        if (!IS_WINDOWS) return;
        long now = System.currentTimeMillis();
        if (now - lastShown < MIN_INTERVAL_MS) return; // 連投されても通知が溜まりすぎないように
        lastShown = now;

        if (body.length() > MAX_BODY) body = body.substring(0, MAX_BODY) + "…";
        String script = buildScript(xml(title), xml(body));
        String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));

        Thread t = new Thread(() -> {
            try {
                Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                        "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded)
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                p.waitFor();
            } catch (Exception ignored) {
            }
        }, "MentionMod-Notifier");
        t.setDaemon(true);
        t.start();
    }

    private static String buildScript(String title, String body) {
        // XMLエスケープ済みなので ' は含まれない（&apos; になる）
        String toastXml = "<toast><visual><binding template=\"ToastGeneric\">"
                + "<text>" + title + "</text><text>" + body + "</text>"
                + "</binding></visual><audio src=\"ms-winsoundevent:Notification.IM\"/></toast>";
        return "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null\n"
                + "[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null\n"
                + "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument\n"
                + "$xml.LoadXml('" + toastXml + "')\n"
                // 通知の送り主を「Minecraft」にするため、自前のアプリIDを登録する（HKCUなので管理者権限は不要）
                + "$app = '" + APP_ID + "'\n"
                + "$key = 'HKCU:\\Software\\Classes\\AppUserModelId\\' + $app\n"
                + "if (-not (Test-Path $key)) { New-Item -Path $key -Force | Out-Null }\n"
                + "New-ItemProperty -Path $key -Name DisplayName -Value '" + SOURCE_NAME + "' -PropertyType String -Force | Out-Null\n"
                + "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier($app).Show("
                + "[Windows.UI.Notifications.ToastNotification]::new($xml))\n";
    }

    private static String xml(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    if (c >= 0x20 || c == '\t') sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
