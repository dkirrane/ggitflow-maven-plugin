/*
 * Copyright 2016 dkirrane.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dkirrane.maven.plugins.ggitflow.prompt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import jline.internal.Configuration;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link Prompter} implementation.
 *
 * @since 3.0
 */
@Component(role = Prompter.class, instantiationStrategy = "per-lookup")
public class PrompterImpl implements Prompter {

    private static final Logger LOG = LoggerFactory.getLogger(PrompterImpl.class);

    private final ConsoleReader console;

    public PrompterImpl() throws IOException {
        TerminalFactory.configure(TerminalFactory.NONE);
        TerminalFactory.reset();
        Configuration.reset();
        this.console = new ConsoleReader();
        this.console.setHistoryEnabled(false);
        this.console.setExpandEvents(false);
    }

    public ConsoleReader getConsole() {
        return console;
    }

    @Override
    public String prompt(final String message, Character mask) throws IOException {
        checkNotNull(message);

        final String prompt = String.format("%s: ", message);
        String value;
        do {
            // mask DOES NOT WORK on WINDOWS 10 https://github.com/jline/jline2/issues/225
//            value = console.readLine(ansi().fg(GREEN).a(prompt).reset().toString(), mask);
            value = console.readLine(ansi().fg(GREEN).bold().a(prompt).boldOff().reset().toString(), null);

            // Do not LOG values read when masked
            if (mask == null) {
                LOG.debug("Read value: '{}'", value);
            } else {
                LOG.debug("Read masked chars: {}", value.length());
            }
        } while (StringUtils.isBlank(value));
        return value;
    }

    @Override
    public String prompt(final String message) throws IOException {
        checkNotNull(message);

        return prompt(message, null);
    }

    @Override
    public String promptWithDefault(final String message, final String defaultValue) throws IOException {
        checkNotNull(message);
        checkNotNull(defaultValue);

        final String prompt = String.format("%s [%s]: ", message, defaultValue);
        String value = console.readLine(ansi().fg(GREEN).bold().a(prompt).boldOff().reset().toString());
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Helper to parse an integer w/o exceptions being thrown.
     */
    private Integer parseInt(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    public Integer promptInteger(final String message, final Integer min, final Integer max) throws IOException {
        while (true) {
            String raw = prompt(message);
            Integer value = parseInt(raw);
            if (value != null) {
                if (min != null && value < min) {
                    console.println("Value must be greater than " + (min - 1) + ": " + raw);
                    continue;
                }
                if (max != null && value > max) {
                    console.println("Value must be less than " + max + ": " + raw);
                    continue;
                }

                // valid
                return value;
            }
            // else invalid, try again
            console.println("Invalid value: " + raw);
        }
    }

    @Override
    public String promptChoice(final String header, final String message, final List<String> choices) throws IOException {
        checkNotNull(header);
        checkNotNull(message);
        checkArgument(!choices.isEmpty(), "choices cannot be empty");

        if (choices.size() > 1) {
            /* Output choices using freemarker */
            try {

                freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.getVersion());
                cfg.setClassForTemplateLoading(PrompterImpl.class, "/freemarker");
                cfg.setDefaultEncoding("UTF-8");
                cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
                cfg.setLogTemplateExceptions(false);

                Template template = cfg.getTemplate("choices.ftl");

                // Build the data-model
                Map<String, Object> data = new HashMap<>();
                data.put("header", header);
                data.put("choices", choices);

                // Console output
                Writer out = new OutputStreamWriter(System.out);
                template.process(data, out);
                out.flush();

            } catch (TemplateException | IOException ex) {
                java.util.logging.Logger.getLogger(PrompterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        console.println("");

        // setup completer
        Completer completer = new StringsCompleter(choices);
        console.addCompleter(completer);

        try {
            String value;
            while (true) {
                value = promptWithDefault(message, choices.get(0)).trim();

                // check if value is an index (choice display starts with index 1)
                Integer i = parseInt(value);
                if (i != null && i > 0 && i <= choices.size()) {
                    value = choices.get(i - 1);
                    break;
                }

                // check if choice is valid
                if (choices.contains(value)) {
                    break;
                }

                console.println("Invalid selection: " + value);
            }
            return value;
        } finally {
            console.removeCompleter(completer);
        }
    }
}
