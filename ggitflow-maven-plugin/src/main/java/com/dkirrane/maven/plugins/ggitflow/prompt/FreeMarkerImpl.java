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

import static com.google.common.base.Preconditions.checkNotNull;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dkirrane
 */
@Component(role = FreeMarker.class, instantiationStrategy = "per-lookup")
public class FreeMarkerImpl implements FreeMarker {

    private static final Logger LOG = LoggerFactory.getLogger(PrompterImpl.class);

    @Override
    public void logError(final String header, final String message) {
        checkNotNull(header);
        checkNotNull(message);

        try {

            freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.getVersion());
            cfg.setClassForTemplateLoading(PrompterImpl.class, "/freemarker");
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);

            Template template = cfg.getTemplate("error.ftl");

            // Build the data-model
            Map<String, Object> data = new HashMap<>();
            data.put("header", header);
            data.put("message", message);

            // Console output
            Writer out = new OutputStreamWriter(System.out);
            template.process(data, out);
            out.flush();

        } catch (TemplateException | IOException ex) {
            LOG.error("{} {}", header, message, ex);
        }
    }

    @Override
    public void logMergeConflict(String header, String message, List<File> conflictedFiles) {
        checkNotNull(header);
        checkNotNull(message);

        try {

            freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.getVersion());
            cfg.setClassForTemplateLoading(PrompterImpl.class, "/freemarker");
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);

            Template template;
            Map<String, Object> data = new HashMap<>();
            if (null == conflictedFiles || conflictedFiles.isEmpty()) {
                template = cfg.getTemplate("error.ftl");
                data.put("header", header);
                data.put("message", message);
                data.put("message", message);
            } else {
                template = cfg.getTemplate("merge-conflict.ftl");
                data.put("header", header);
                data.put("conflicts", conflictedFiles);
            }

            // Console output
            Writer out = new OutputStreamWriter(System.out);
            template.process(data, out);
            out.flush();

        } catch (TemplateException | IOException ex) {
            LOG.error("{} {}", header, message, ex);
        }
    }
}
