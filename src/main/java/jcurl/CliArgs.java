package jcurl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class CliArgs {

	private static final Logger LOG = LoggerFactory.getLogger(CliArgs.class);

	private static final String PARAM_PREFIX = "param.";

	static final String DISPLAY_HEADER = "i";

	static final String FOLLOW_REDIRECT = "L";

	static final String HELP = "h";

	static final String MANUAL = "manual";

	static final String PRETTY_PRINT = "pp";

	static final String VERB = "X";

	static final String VERSION = "V";

	static final String INPUT_FILE = "inputFile";

	static final String INPUT_FILE_PATTERN = "inputPattern";

	static final String INPUT_FILE_TRANSFORM = "inputTransform";

	static final String PATCH_CONSOLE_ENCODING = "X_patchConsoleEncoding";

	static final String SHOW_ENV_PARAM = "X_showEnvParams";

	private final ResourceBundle i18n;

	private final Options options;

	private final PrintStream consoleOutput;

	public CliArgs(final OutputStream consoleOutput) {
		this.consoleOutput = new PrintStream(consoleOutput);
		this.i18n = ResourceBundle.getBundle(Cli.class.getName());
		this.options = new Options();
		this.options.addOption(this.createOption(HELP));
		this.options.addOption(this.createOption(VERSION));
		this.options.addOption(this.createOption(MANUAL));
		this.options.addOption(this.createOption(FOLLOW_REDIRECT));
		// groupInfo.addOption(this.createOption(PPRINT));
		this.options.addOption(this.createOption(DISPLAY_HEADER));
		this.options.addOption(this.createOption(PATCH_CONSOLE_ENCODING));
		this.options.addOption(this.createOption(SHOW_ENV_PARAM));
		this.options.addOption(this.createArgOption(INPUT_FILE));
		this.options.addOption(this.createArgOption(INPUT_FILE_PATTERN));
		this.options.addOption(this.createArgOption(INPUT_FILE_TRANSFORM));
		this.options.addOption(this.createArgOption(VERB));
	}

	protected Options getOptions() {
		return this.options;
	}

	private Option createArgOption(final String param) {
		return this.createOption(param, true);
	}

	protected Option createOption(final String param) {
		return this.createOption(param, false);
	}

	protected Option createOption(final String param, final boolean hasArg) {
		final String description = this.getString(PARAM_PREFIX + param + ".description", null);
		final String longOpt = this.getString(PARAM_PREFIX + param + ".long", null);
		final String argName = this.getString(PARAM_PREFIX + param + ".argname", null);
		Option.Builder builder = Option.builder(param);
		if (hasArg) {
			builder.hasArg();
		}
		if (description != null) {
			builder.desc(description);
		}
		if (longOpt != null) {
			builder.longOpt(longOpt);
		}
		if (argName != null) {
			builder.argName(argName);
		}
		return builder.build();
	}

	private int getInt(final String key, final int defaultValue) {
		try {
			final String str = this.i18n.getString(key);
			return Integer.parseInt(str);
		}
        catch (final MissingResourceException | NumberFormatException e) {
			return defaultValue;
		}
	}

	private String getString(final String key, final String defaultValue) {
		try {
			return this.i18n.getString(key);
		}
		catch (final MissingResourceException e) {
			return defaultValue;
		}
	}

	public void printManual() {
		final String manualFile = this.getString(MANUAL, null);
		try (final InputStream manual = ClassLoader.getSystemResourceAsStream(manualFile)) {
			if (manual == null) {
				throw new FileNotFoundException("Unable to find '" + manualFile + "'");
			}
			final String manualContent = IOUtils.toString(manual, StandardCharsets.UTF_8);
			AnsiConsole.systemInstall();
			this.consoleOutput.println(Ansi.ansi().eraseScreen().render(manualContent).reset());
			AnsiConsole.systemUninstall();
		} catch (final IOException e) {
			LOG.error("Unable du display help usage", e);
		}
	}

	public void showEnvParams() {
		final String pad = "                              ";
		final String sep = "\n======================================================\n\n";
		String n;
		AnsiConsole.systemInstall();
		final StringBuilder sb = new StringBuilder();
		sb.append("\n\n\n\nEnvironnement variables:").append(sep);
		for (final Entry<String, String> e : new TreeMap<String, String>(System.getenv()).entrySet()) {
			n = e.getKey();
			sb.append("\n- ").append(n).append(pad.length() > n.length() ? pad.substring(n.length()) : "").append(": ")
					.append(e.getValue());
		}
		sb.append("\n\n\n\nSystem properties:").append(sep);
		for (final Entry<Object, Object> e : new TreeMap<Object, Object>(System.getProperties()).entrySet()) {
			n = String.valueOf(e.getKey());
			sb.append("\n- ").append(n).append(pad.length() > n.length() ? pad.substring(n.length()) : "").append(": ")
					.append(e.getValue());
		}
		sb.append("\n\n\n\nDefault settings:").append(sep);
		n = "System encoding";
		sb.append("\n- ").append(n).append(pad.length() > n.length() ? pad.substring(n.length()) : "").append(": ")
				.append(Charset.defaultCharset().name());
		n = "Output encoding";
		sb.append("\n- ").append(n).append(pad.length() > n.length() ? pad.substring(n.length()) : "").append(": ")
				.append(ConsoleOutputFilter.getOutputEncoding(Charset.defaultCharset().name()));

		// System.out.println(encoding + " " + (this.toTranscode ? "->to transcode"
		// : ""));

		this.consoleOutput.println(sb);
		AnsiConsole.systemUninstall();
	}

	public void printUsage() {
		final Options opts = new Options();
		for (final Option o : this.getOptions().getOptions()) {
			opts.addOption(o);
		}
		final HelpFormatter formatter = new HelpFormatter();
		// formatter.setLeftPadding(4);
		// formatter.setWidth(80);
		// formatter.printHelp("java -jar gpcc-core.jar", "\n", options,
		// StringUtils.join(additionnalInformations, "\n"));
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PrintWriter pw = new PrintWriter(out);

		formatter.printHelp(pw, 80 //
				, this.getString("usage.cmdLineSyntax", "") //
				, this.getString("usage.header", "") //
				, opts //
				, this.getInt("usage.leftPad", 1) //
				, this.getInt("usage.descPad", 1) //
				, this.getString("usage.footer", "") //
		);
		pw.flush();
		AnsiConsole.systemInstall();
		this.consoleOutput.println(Ansi.ansi().a(out));
		AnsiConsole.systemUninstall();
	}

	public boolean isHelp(final CommandLine cmd) {
		return ((cmd.getOptions().length == 0) && (cmd.getArgs().length == 0)) || cmd.hasOption(CliArgs.HELP);
	}

	public Map<JcurlOption, String> getJcurlOptions(final CommandLine cmd) {
		final Map<JcurlOption, String> opts = new LinkedHashMap<>();
		if (cmd.hasOption(CliArgs.DISPLAY_HEADER)) {
			opts.put(JcurlOption.displayHeader, "true");
		}
		if (cmd.hasOption(CliArgs.PRETTY_PRINT)) {
			opts.put(JcurlOption.prettyPrint, "true");
		}
		if (cmd.hasOption(CliArgs.FOLLOW_REDIRECT)) {
			opts.put(JcurlOption.followRedirect, "true");
		}
		return opts;
	}

	public static boolean isPatchConsoleEncodingEnabled(final String... arguments) {
		return Arrays.asList(arguments).contains("-" + PATCH_CONSOLE_ENCODING);
	}

	public boolean isShowEnvParams(final CommandLine cmd) {
		return cmd.hasOption(CliArgs.SHOW_ENV_PARAM);
	}
}
