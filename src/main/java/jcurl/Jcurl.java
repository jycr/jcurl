package jcurl;

import org.apache.commons.io.IOUtils;
import org.fusesource.jansi.AnsiConsole;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class Jcurl {

    private final HttpMethod method;

    private final Map<JcurlOption, String> opts;

    private final ResponseViewer output;

    private final OutputStream consoleOutput;

    private boolean followRedirect;

    public Jcurl(final HttpMethod method, final Map<JcurlOption, String> opts, final OutputStream output) {
        this.method = method;
        this.followRedirect = Boolean.valueOf(opts.get(JcurlOption.followRedirect));
        this.opts = opts;
        this.consoleOutput = output;
        AnsiConsole.systemInstall();
        this.output = new ResponseViewer(new OutputStreamWriter(this.consoleOutput), opts);
    }

    public void run(final URL url) {
        try {
            final HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(this.followRedirect);
            c.setRequestMethod(this.method.toString());
            if (this.method == HttpMethod.GET) {
                this.doGet(c);
            } else if (this.method == HttpMethod.PUT || this.method == HttpMethod.POST) {
                this.doOutput(c);
            } else if (this.method == HttpMethod.DELETE) {
                this.doDelete(c);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void doGet(final HttpURLConnection connection) throws IOException {
        this.output.view(connection);
    }

    public void doDelete(final HttpURLConnection connection) throws IOException {
        this.output.view(connection);
    }

    public void doOutput(final HttpURLConnection connection) throws IOException {
        connection.setDoOutput(true);
        this.sendFile(connection);
        this.output.view(connection);
    }

    public void sendFile(final HttpURLConnection connection) throws IOException {
        final String file = this.opts.get(JcurlOption.file);
        if (file == null) {
            return;
        }
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
                OutputStream connectionOutputStream = connection.getOutputStream();
        ) {
            IOUtils.copy(fileInputStream, connectionOutputStream);
        }
    }
}