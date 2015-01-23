/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.service.permission;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OpLevelCollection implements SubjectCollection {
    private final OpPermissionService service;
    private final Map<String, OpLevelSubject> levels;
    public OpLevelCollection(OpPermissionService service) {
        this.service = service;
        ImmutableMap.Builder<String, OpLevelSubject> build = ImmutableMap.builder();
        for (int i = 0; i < 4; ++i) {
            build.put("op_" + i, new OpLevelSubject(service, i));
        }
        levels = build.build();
    }

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_GROUP;
    }

    @Override
    public Subject get(String identifier) {
        return levels.containsKey(identifier) ? levels.get(identifier) : null;
    }

    @Override
    public boolean hasRegistered(String identifier) {
        return levels.containsKey(identifier);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<Subject> getAllSubjects() {
        return (Collection) levels.values();
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(String permission) {
        final Map<Subject, Boolean> ret = new HashMap<Subject, Boolean>();
        for (Subject subj : getAllSubjects()) {
            Tristate state = subj.getPermissionValue(subj.getActiveContexts(), permission);
            if (state != Tristate.UNDEFINED) {
                ret.put(subj, state.asBoolean());
            }
        }
        return Collections.unmodifiableMap(ret);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(Set<Context> contexts, String permission) {
        final Map<Subject, Boolean> ret = new HashMap<Subject, Boolean>();
        for (Subject subj : getAllSubjects()) {
            Tristate state = subj.getPermissionValue(contexts, permission);
            if (state != Tristate.UNDEFINED) {
                ret.put(subj, state.asBoolean());
            }
        }
        return Collections.unmodifiableMap(ret);
    }
}
