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

import java.io.File;
import java.util.List;

/**
 * Output to console using FreeMarker template
 *
 * @since 3.0
 */
public interface FreeMarker {

    void logError(final String header, final String message);

    void logGitError(final String header, final String message, final Integer exitCode, final String stout, final String sterr);

    void logMergeConflict(final String header, final String footer, final String message, List<File> conflictedFiles);
}
