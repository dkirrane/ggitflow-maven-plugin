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

import java.util.Arrays;
import java.util.List;

/**
 * Useful for testing prompter
 * @author dkirrane
 */
public class PromptTesterMain {

    public static void main(String[] args) throws Exception {
        Prompter prompter = new PrompterImpl();

        String something = prompter.prompt("Enter something");
        System.out.println("user entered: " + something);

        String password = prompter.prompt("Enter Password", '*');
        System.out.println("user password: " + password);

        String somethingOrDefault = prompter.promptWithDefault("Enter something or choose default", "myDefault");
        System.out.println("user entered: " + somethingOrDefault);

//        List<String> choices = Arrays.asList("1.0", "1.1", "1.2", "1.3");
        List<String> choices = Arrays.asList("1.0");
        String promptChoice = prompter.promptChoice("Git tags", "Please select a Git tag", choices);
        System.out.println("user chose: " + promptChoice);
    }

}
