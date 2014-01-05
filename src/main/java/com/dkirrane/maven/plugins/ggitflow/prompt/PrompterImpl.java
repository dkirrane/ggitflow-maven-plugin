/*
 * Copyright 2014 Desmond Kirrane.
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

import java.io.IOException;
import java.util.List;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import org.codehaus.plexus.components.interactivity.DefaultPrompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

@Component(role = Prompter.class, hint = "prompter")
public class PrompterImpl implements Prompter {

    private DefaultPrompter defaultPrompter = new DefaultPrompter();
    private final ConsoleReader console;

    public PrompterImpl() {
        try {
            this.console = new ConsoleReader();
            this.console.setHistoryEnabled(false);
            this.console.setExpandEvents(false);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String prompt(String message) throws PrompterException {
        String value = defaultPrompter.prompt(message);
        if (StringUtils.isBlank(value)) {
            value = prompt(message);
        }
        return value;
    }

    @Override
    public String prompt(String message, String defaultReply) throws PrompterException {
        String value = defaultPrompter.prompt(message, defaultReply);
        if (StringUtils.isBlank(value)) {
            value = prompt(message, defaultReply);
        }
        return value;
    }

    @Override
    public String prompt(String message, List possibleValues, String defaultReply) throws PrompterException {
        try {
            console.println(message + ":");
            for (int i = 0; i < possibleValues.size(); i++) {
                console.println(String.format("  %2d) %s", i, possibleValues.get(i)));
            }
            console.flush();

            // setup completer
            StringsCompleter completer = new StringsCompleter(possibleValues);
            console.addCompleter(completer);

            try {
                String value;
                while (true) {
                    value = prompt(message).trim();

                    // check if value is an index
                    Integer i = null;
                    try {
                        i = Integer.parseInt(value);
                    } catch (NumberFormatException numberFormatException) {
                    }
                    if (i != null && i >= 0 && i < possibleValues.size()) {
                        value = (String) possibleValues.get(i);
                        break;
                    }

                    // check if choice is valid
                    if (possibleValues.contains(value)) {
                        break;
                    }

                    console.println("Invalid selection: " + value);
                }
                return value;
            } finally {
                console.removeCompleter(completer);
            }
        } catch (IOException ex) {
            throw new PrompterException(ex.getMessage(), ex);
        }
    }

    @Override
    public String prompt(String message, List possibleValues) throws PrompterException {
        String value = defaultPrompter.prompt(message, possibleValues);
        if (StringUtils.isBlank(value)) {
            value = prompt(message);
        }
        return value;
    }

    @Override
    public String promptForPassword(String message) throws PrompterException {
        String value = defaultPrompter.promptForPassword(message);
        if (StringUtils.isBlank(value)) {
            value = prompt(message);
        }
        return value;
    }

    @Override
    public void showMessage(String message) throws PrompterException {
        defaultPrompter.showMessage(message);
    }

}
