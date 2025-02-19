/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.engine.announce;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.JReleaserModel;
import org.jreleaser.model.announcer.spi.AnnounceException;
import org.jreleaser.model.announcer.spi.Announcer;
import org.jreleaser.model.announcer.spi.AnnouncerBuilder;
import org.jreleaser.model.announcer.spi.AnnouncerBuilderFactory;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public class Announcers {
    public static void announce(JReleaserContext context) throws AnnounceException {
        context.getLogger().info(RB.$("announcers.header"));
        if (!context.getModel().getAnnounce().isEnabled()) {
            context.getLogger().info(RB.$("announcers.not.enabled"));
            return;
        }

        Map<String, Announcer> announcers = Announcers.findAnnouncers(context);
        if (announcers.isEmpty()) {
            context.getLogger().info(RB.$("announcers.not.configured"));
            return;
        }

        if (!context.getIncludedAnnouncers().isEmpty()) {
            for (String announcerName : context.getIncludedAnnouncers()) {
                Announcer announcer = announcers.get(announcerName);

                if (null == announcer) {
                    context.getLogger().warn(RB.$("announcers.announcer.not.found"), announcerName);
                    continue;
                }

                announce(context, announcer);
            }
            return;
        }

        for (Map.Entry<String, Announcer> entry : announcers.entrySet()) {
            Announcer announcer = entry.getValue();

            if (context.getExcludedAnnouncers().contains(announcer.getName())) {
                context.getLogger().info(RB.$("announcers.announcer.excluded"), announcer.getName());
                continue;
            }

            announce(context, announcer);
        }
    }

    private static void announce(JReleaserContext context, Announcer announcer) {
        context.getLogger().increaseIndent();
        context.getLogger().setPrefix(announcer.getName());

        if (announcer.isEnabled()) {
            try {
                announcer.announce();
            } catch (AnnounceException e) {
                context.getLogger().warn(e.getMessage().trim());
            }
        } else {
            context.getLogger().debug(RB.$("announcers.announcer.disabled"));
        }

        context.getLogger().restorePrefix();
        context.getLogger().decreaseIndent();
    }

    private static Map<String, Announcer> findAnnouncers(JReleaserContext context) {
        JReleaserModel model = context.getModel();

        Map<String, AnnouncerBuilder> builders = StreamSupport.stream(ServiceLoader.load(AnnouncerBuilderFactory.class,
            Announcers.class.getClassLoader()).spliterator(), false)
            .collect(Collectors.toMap(AnnouncerBuilderFactory::getName, AnnouncerBuilderFactory::getBuilder));

        Map<String, Announcer> announcers = new TreeMap<>();
        builders.forEach((name, builder) -> {
            if (null != model.getAnnounce().findAnnouncer(name) &&
                !context.getExcludedAnnouncers().contains(name)) {
                announcers.put(name, builder.configureWith(context).build());
            }
        });

        return announcers;
    }
}
