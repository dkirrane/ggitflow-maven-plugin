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
package com.dkirrane.maven.plugins.ggitflow.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@code java.nio.file.FileVisitor} that finds all files that match the
 * specified pattern.
 *
 * @author dkirrane
 */
public class Finder extends SimpleFileVisitor<Path> {

    private static final Logger LOG = LoggerFactory.getLogger(Finder.class.getName());

    public enum Syntax {
        glob, regex;
    }

    private final PathMatcher matcher;
    private List<Path> files = new ArrayList<>();

    /**
     *
     * @param syntax "glob" and "regex" syntax
     * @param pattern pattern to match
     */
    public Finder(Syntax syntax, String pattern) {
        matcher = FileSystems.getDefault().getPathMatcher(syntax.name() + ':' + pattern);
    }

    public void find(Path file) {
        Path name = file.getFileName();
        if (name != null && matcher.matches(name)) {
            LOG.debug("Found file " + file);
            files.add(file);
        }
    }

    /**
     * Get files matching provided pattern
     *
     * @return empty list if no files found
     */
    public List<Path> getPaths() {
        return files;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        find(file);
        return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        find(dir);
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }

}
