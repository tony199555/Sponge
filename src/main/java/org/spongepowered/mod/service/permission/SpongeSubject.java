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

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.util.Tristate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SpongeSubject implements Subject {
    private final OpPermissionService service;

    protected SpongeSubject(OpPermissionService service) {
        this.service = service;
    }

    protected OpPermissionService getService() {
        return this.service;
    }

    @Override
    public SubjectData getTransientData() {
        return getData();
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return getPermissionValue(contexts, permission) == Tristate.TRUE;
    }

    @Override
    public boolean hasPermission(String permission) {
        return hasPermission(getActiveContexts(), permission);
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        Boolean res = getData().getPermissions(contexts).get(permission);
        if (res == null) {
            res = getData().getPermissions(SubjectData.GLOBAL_CONTEXT).get(permission);
        }
        if (res == null) {
            for (Subject parent : getData().getParents(contexts)) {
                Tristate tempRes = parent.getPermissionValue(contexts, permission);
                if (tempRes != Tristate.UNDEFINED) {
                    res = tempRes.asBoolean();
                    break;
                }
            }
        }
        if (res == null) {
            res = service.getDefaultData().getPermissions(contexts).get(permission);

        }
        return res == null ? Tristate.UNDEFINED : Tristate.fromBoolean(res);
    }

    @Override
    public boolean isChildOf(Subject parent) {
        return isChildOf(getActiveContexts(), parent);
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, Subject parent) {
        return getData().getParents(contexts).contains(parent);
    }

    @Override
    public List<Subject> getParents() {
        return getParents(getActiveContexts());
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts) {
        return getData().getParents(contexts);
    }

    @Override
    public Set<Context> getActiveContexts() {
        Set<Context> contexts = new HashSet<Context>();
        for (ContextCalculator calc : service.getContextCalculators()) {
            calc.accumulateContexts(this, contexts);
        }
        return Collections.unmodifiableSet(contexts);
    }
}
