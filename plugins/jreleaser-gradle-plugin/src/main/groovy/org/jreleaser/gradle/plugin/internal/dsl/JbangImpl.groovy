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
package org.jreleaser.gradle.plugin.internal.dsl

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.internal.provider.Providers
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jreleaser.gradle.plugin.dsl.CommitAuthor
import org.jreleaser.gradle.plugin.dsl.Jbang
import org.jreleaser.gradle.plugin.dsl.Tap
import org.kordamp.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class JbangImpl extends AbstractRepositoryTool implements Jbang {
    final Property<String> alias
    final CommitAuthorImpl commitAuthor
    final TapImpl catalog

    @Inject
    JbangImpl(ObjectFactory objects) {
        super(objects)
        alias = objects.property(String).convention(Providers.notDefined())
        catalog = objects.newInstance(TapImpl, objects)
        commitAuthor = objects.newInstance(CommitAuthorImpl, objects)
    }

    @Override
    @Internal
    boolean isSet() {
        super.isSet() ||
            catalog.isSet() ||
            commitAuthor.isSet()
    }

    @Override
    void catalog(Action<? super Tap> action) {
        action.execute(catalog)
    }

    @Override
    void commitAuthor(Action<? super CommitAuthor> action) {
        action.execute(commitAuthor)
    }

    @Override
    void catalog(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Tap) Closure<Void> action) {
        ConfigureUtil.configure(action, catalog)
    }

    @Override
    void commitAuthor(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CommitAuthor) Closure<Void> action) {
        ConfigureUtil.configure(action, commitAuthor)
    }

    org.jreleaser.model.Jbang toModel() {
        org.jreleaser.model.Jbang tool = new org.jreleaser.model.Jbang()
        fillToolProperties(tool)
        fillTemplateToolProperties(tool)
        if (alias.present) tool.alias = alias.get()
        if (catalog.isSet()) tool.catalog = catalog.toJbangCatalog()
        if (commitAuthor.isSet()) tool.commitAuthor = commitAuthor.toModel()
        tool
    }
}
