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

import java.io.IOException;
import java.util.List;

/**
 * Prompt for user input
 *
 * @since 2.3
 */
public interface Prompter {

    /**
     * Prompt user for a string, optionally masking the input.
     *
     * @param message
     * @param mask
     * @return
     * @throws java.io.IOException
     */
    String prompt(String message, Character mask) throws IOException;

    /**
     * Prompt user for a string.
     *
     * @param message
     * @return
     * @throws java.io.IOException
     */
    String prompt(String message) throws IOException;

    /**
     * Prompt user for a string; if user response is blank use a default value.
     *
     * @param message
     * @param defaultValue
     * @return
     * @throws java.io.IOException
     */
    String promptWithDefault(String message, String defaultValue) throws IOException;

    /**
     * Prompt user for a string out of a set of available choices.
     *
     * Choices must not have entries with leading/trailing whitespace. At least
     * 2 choices are required.
     *
     * @param header
     * @param choices
     * @param message
     * @return
     * @throws java.io.IOException
     */
    String promptChoice(String header, String message, List<String> choices) throws IOException;

    /**
     * Prompt user for an integer.
     *
     * @param message
     * @param max
     * @param min
     * @return
     * @throws java.io.IOException
     */
    Integer promptInteger(String message, Integer min, Integer max) throws IOException;
}
